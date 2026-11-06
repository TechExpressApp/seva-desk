import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BrandingService } from '../../core/branding.service';
import { Contact, Expense, ExpensePurpose, ExpenseSummary, Page } from '../../core/models';

/** A contact as chosen in the Name field */
interface Payee {
  id: string;
  fullName: string;
  hint?: string;
}

/** Expenses: who was paid (a contact), when, how much and for what. Treasurers and admins. */
@Component({
  selector: 'sd-expenses',
  imports: [DatePipe, FormsModule, ReactiveFormsModule, AutoComplete, Button, Dialog, InputNumber, InputText, Select, TableModule],
  templateUrl: './expenses.html',
  styleUrl: './expenses.scss',
})
export class Expenses implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);
  protected readonly branding = inject(BrandingService);

  protected readonly rows = signal<Expense[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly summary = signal<ExpenseSummary | null>(null);
  protected readonly purposes = signal<{ value: ExpensePurpose; label: string }[]>([]);

  // filters
  protected filterPurpose: ExpensePurpose | null = null;
  protected filterFrom = '';
  protected filterTo = '';

  // dialog
  protected readonly editing = signal<Expense | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly deleting = signal<Expense | null>(null);
  protected readonly suggestions = signal<Payee[]>([]);
  protected readonly lastQuery = signal('');
  protected readonly addingContact = signal(false);

  protected readonly form = inject(FormBuilder).group({
    payee: [null as Payee | null, Validators.required],
    spentOn: ['', Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(0.01)]],
    purpose: [null as ExpensePurpose | null, Validators.required],
    notes: ['', Validators.maxLength(500)],
  });

  private page = 0;
  private pageSize = 25;

  ngOnInit(): void {
    this.http.get<{ value: ExpensePurpose; label: string }[]>('/api/expenses/purposes').subscribe((p) => this.purposes.set(p));
    this.loadSummary();
  }

  load(event?: TableLazyLoadEvent): void {
    if (event) {
      this.pageSize = event.rows ?? 25;
      this.page = Math.floor((event.first ?? 0) / this.pageSize);
    }
    const params = new URLSearchParams({ page: String(this.page), size: String(this.pageSize) });
    if (this.filterPurpose) params.set('purpose', this.filterPurpose);
    if (this.filterFrom) params.set('from', this.filterFrom);
    if (this.filterTo) params.set('to', this.filterTo);
    this.loading.set(true);
    this.http.get<Page<Expense>>(`/api/expenses?${params}`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyFilters(): void {
    this.page = 0;
    this.load();
  }

  clearFilters(): void {
    this.filterPurpose = null;
    this.filterFrom = '';
    this.filterTo = '';
    this.applyFilters();
  }

  private loadSummary(): void {
    this.http.get<ExpenseSummary>('/api/expenses/summary').subscribe((s) => this.summary.set(s));
  }

  // ---- Name: search contacts ----

  searchContacts(event: AutoCompleteCompleteEvent): void {
    const q = event.query.trim();
    this.lastQuery.set(q);
    this.http.get<Page<Contact>>(`/api/contacts?q=${encodeURIComponent(q)}&size=15`).subscribe((p) =>
      this.suggestions.set(
        p.content.map((c) => ({ id: c.id, fullName: c.fullName, hint: [c.phone, c.email, c.city].filter(Boolean).join(' · ') })),
      ),
    );
  }

  /** The typed name isn't a contact yet: add it as one and choose it. */
  async addAsContact(): Promise<void> {
    const name = this.lastQuery().trim().replace(/\s+/g, ' ');
    if (!name) return;
    const space = name.indexOf(' ');
    this.addingContact.set(true);
    try {
      const c = await firstValueFrom(
        this.http.post<Contact>('/api/contacts', {
          firstName: space > 0 ? name.slice(0, space) : name,
          lastName: space > 0 ? name.slice(space + 1) : null,
        }),
      );
      this.form.controls.payee.setValue({ id: c.id, fullName: c.fullName });
      this.toast.add({ severity: 'success', summary: 'Contact added', detail: c.fullName });
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not add the contact.');
    } finally {
      this.addingContact.set(false);
    }
  }

  protected canAddTyped(): boolean {
    const q = this.lastQuery().trim().toLowerCase();
    return q.length >= 2 && !this.form.controls.payee.value?.id
      && !this.suggestions().some((s) => s.fullName.toLowerCase() === q);
  }

  // ---- add / edit / delete ----

  open(e: Expense | 'new'): void {
    this.error.set(null);
    this.lastQuery.set('');
    if (e === 'new') {
      this.form.reset({ payee: null, spentOn: this.todayIso(), amount: null, purpose: null, notes: '' });
    } else {
      this.form.reset({
        payee: e.contactId ? { id: e.contactId, fullName: e.name } : null,
        spentOn: e.spentOn,
        amount: e.amount,
        purpose: e.purpose,
        notes: e.notes ?? '',
      });
    }
    this.editing.set(e);
  }

  async save(): Promise<void> {
    const v = this.form.getRawValue();
    if (!v.payee?.id) return this.error.set('Choose who was paid from your contacts (or add them as a new contact).');
    if (!v.spentOn) return this.error.set('Choose the expense date.');
    if (!v.amount || v.amount <= 0) return this.error.set('Enter the amount.');
    if (!v.purpose) return this.error.set('Choose a purpose.');
    const body = { contactId: v.payee.id, spentOn: v.spentOn, amount: v.amount, purpose: v.purpose, notes: v.notes?.trim() || null };
    const target = this.editing();
    this.saving.set(true);
    this.error.set(null);
    try {
      if (target === 'new') await firstValueFrom(this.http.post<Expense>('/api/expenses', body));
      else if (target) await firstValueFrom(this.http.put<Expense>(`/api/expenses/${target.id}`, body));
      this.toast.add({
        severity: 'success',
        summary: target === 'new' ? 'Expense added' : 'Expense updated',
        detail: `${v.payee.fullName} · ${this.branding.money(v.amount)}`,
      });
      this.editing.set(null);
      this.load();
      this.loadSummary();
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save the expense. Please check the fields.');
    } finally {
      this.saving.set(false);
    }
  }

  async confirmDelete(): Promise<void> {
    const e = this.deleting();
    if (!e) return;
    await firstValueFrom(this.http.delete(`/api/expenses/${e.id}`));
    this.toast.add({ severity: 'info', summary: 'Expense deleted', detail: `${e.name} · ${this.branding.money(e.amount)}` });
    this.deleting.set(null);
    this.load();
    this.loadSummary();
  }

  private todayIso(): string {
    const d = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }
}
