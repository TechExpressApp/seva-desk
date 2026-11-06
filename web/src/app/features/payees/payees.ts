import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BrandingService } from '../../core/branding.service';
import { Expense, Page, PayeePage, PayeeTotal } from '../../core/models';
import { ContactDetails } from '../../shared/contact-details';

/**
 * Expenses per person: how much the temple has paid each contact, like the Donors screen for donations.
 * Click a name for contact details, or the count for every expense paid to them. Click headings to sort.
 */
@Component({
  selector: 'sd-payees',
  imports: [DatePipe, TableModule, IconField, InputIcon, InputText, Dialog, Button, ContactDetails],
  templateUrl: './payees.html',
})
export class Payees {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  protected readonly branding = inject(BrandingService);

  protected readonly rows = signal<PayeeTotal[]>([]);
  protected readonly total = signal(0);
  protected readonly grandTotal = signal(0);
  protected readonly loading = signal(false);

  /** Popups: contact details, and the list of expenses for one person */
  protected readonly profileOf = signal<PayeeTotal | null>(null);
  protected readonly selected = signal<PayeeTotal | null>(null);
  protected readonly expenses = signal<Expense[]>([]);
  protected readonly expensesTotal = signal(0);
  protected readonly expensesLoading = signal(false);

  private query = '';
  private lastEvent: TableLazyLoadEvent = { first: 0, rows: 25, sortField: 'total', sortOrder: -1 };
  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed()).subscribe((q) => {
      this.query = q;
      this.load({ ...this.lastEvent, first: 0 });
    });
  }

  onSearch(value: string): void {
    this.search$.next(value.trim());
  }

  load(event: TableLazyLoadEvent = this.lastEvent): void {
    this.lastEvent = event;
    const rows = event.rows ?? 25;
    const page = Math.floor((event.first ?? 0) / rows);
    const sort = (Array.isArray(event.sortField) ? event.sortField[0] : event.sortField) || 'total';
    const dir = event.sortOrder === 1 ? 'asc' : 'desc';
    this.loading.set(true);
    this.http
      .get<PayeePage>(`/api/expenses/by-person?q=${encodeURIComponent(this.query)}&page=${page}&size=${rows}&sort=${sort}&dir=${dir}`)
      .subscribe({
        next: (p) => {
          this.rows.set(p.content);
          this.total.set(p.totalPeople);
          this.grandTotal.set(p.grandTotal);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  resetSort(table: Table): void {
    table.sortField = 'total';
    table.sortOrder = -1;
    table.first = 0;
    this.load({ ...this.lastEvent, first: 0, sortField: 'total', sortOrder: -1 });
  }

  protected sortedBy(): string {
    const labels: Record<string, string> = {
      name: 'name', email: 'email', expenses: 'number of expenses', thisYear: 'this year',
      total: 'total paid', lastExpense: 'last expense',
    };
    const f = (this.lastEvent.sortField as string) || 'total';
    const asc = this.lastEvent.sortOrder === 1;
    const direction =
      f === 'name' || f === 'email' ? (asc ? 'A–Z' : 'Z–A')
      : f === 'lastExpense' ? (asc ? 'oldest first' : 'newest first')
      : asc ? 'lowest first' : 'highest first';
    return `${labels[f] ?? 'total paid'} (${direction})`;
  }

  // ---- popups ----

  openProfile(p: PayeeTotal): void {
    this.profileOf.set(p);
  }

  openExpenses(p: PayeeTotal): void {
    this.profileOf.set(null);
    this.expenses.set([]);
    this.expensesTotal.set(0);
    this.selected.set(p);
  }

  loadExpenses(event: TableLazyLoadEvent): void {
    const p = this.selected();
    if (!p) return;
    const rows = event.rows ?? 10;
    const page = Math.floor((event.first ?? 0) / rows);
    this.expensesLoading.set(true);
    this.http.get<Page<Expense>>(`/api/expenses?contactId=${p.contactId}&page=${page}&size=${rows}`).subscribe({
      next: (res) => {
        this.expenses.set(res.content);
        this.expensesTotal.set(res.page.totalElements);
        this.expensesLoading.set(false);
      },
      error: () => this.expensesLoading.set(false),
    });
  }

  editContact(p: PayeeTotal): void {
    this.router.navigate(['/contacts'], { queryParams: { edit: p.contactId } });
  }
}
