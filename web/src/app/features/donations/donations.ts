import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { AuthService } from '../../core/auth.service';
import { BrandingService } from '../../core/branding.service';
import { Contact, Donation, Fund, Page, PaymentMode } from '../../core/models';

@Component({
  selector: 'sd-donations',
  imports: [ReactiveFormsModule, DatePipe, TableModule, Button, Dialog, InputNumber, InputText, Select, Tag],
  templateUrl: './donations.html',
})
export class Donations implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);
  protected readonly branding = inject(BrandingService);
  protected readonly auth = inject(AuthService);

  /** /donations?new=1 opens the entry dialog straight away (from the dashboard button). */
  readonly new = input<string>();

  protected readonly rows = signal<Donation[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly funds = signal<Fund[]>([]);
  protected readonly contacts = signal<Contact[]>([]);
  protected readonly dialogOpen = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly voiding = signal<Donation | null>(null);

  /** Payment modes for this temple's country, from the branding (UPI in India, Zelle and "Check" in the US). */
  protected readonly modes = computed<{ value: PaymentMode; label: string }[]>(() => {
    const b = this.branding.branding();
    if (b?.paymentModes?.length) return b.paymentModes;
    // Fallback if the branding came from an older server or a cached copy: never show an empty list.
    const usa = b?.currency === 'USD';
    const india = b?.currency === 'INR';
    return [
      { value: 'CASH', label: 'Cash' },
      ...(india ? [{ value: 'UPI' as PaymentMode, label: 'UPI' }] : []),
      ...(usa ? [{ value: 'ZELLE' as PaymentMode, label: 'Zelle' }] : []),
      { value: 'CARD', label: 'Card' },
      { value: 'CHEQUE', label: usa ? 'Check' : 'Cheque' },
      { value: 'BANK_TRANSFER', label: 'Bank transfer' },
      { value: 'ONLINE', label: 'Online' },
      { value: 'IN_KIND', label: 'In kind' },
    ];
  });

  /** Example references for the reference field, matching the modes offered. */
  protected readonly referenceHint = computed(() => {
    const values = this.modes().map((m) => m.value);
    const transfer = values.includes('ZELLE') ? 'Zelle' : values.includes('UPI') ? 'UPI' : 'transfer';
    const cheque = this.modes().find((m) => m.value === 'CHEQUE')?.label.toLowerCase() ?? 'cheque';
    return `${transfer} / ${cheque} no.`;
  });

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.group({
    contactId: [null as string | null],
    fundId: [null as string | null, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    paymentMode: ['CASH' as PaymentMode, Validators.required],
    reference: [''],
    notes: [''],
    // Used when no existing donor is picked: matched by email, phone or name, else a new contact.
    donorFirstName: ['', Validators.maxLength(100)],
    donorLastName: ['', Validators.maxLength(100)],
    donorEmail: ['', [Validators.email, Validators.maxLength(200)]],
    donorPhone: ['', Validators.maxLength(30)],
  });
  protected readonly voidReason = this.fb.nonNullable.control('', Validators.required);

  private lastPage = 0;

  ngOnInit(): void {
    this.http.get<Fund[]>('/api/funds').subscribe((f) => {
      this.funds.set(f);
      // The dialog may already be open (?new=1) before funds arrive: pick the first fund then.
      if (!this.form.controls.fundId.value) this.form.controls.fundId.setValue(f[0]?.id ?? null);
    });
    this.http.get<Page<Contact>>('/api/contacts?size=100').subscribe((p) => this.contacts.set(p.content));
    if (this.new()) this.openNew();
  }

  load(event?: TableLazyLoadEvent): void {
    const rowsPerPage = event?.rows ?? 25;
    this.lastPage = Math.floor((event?.first ?? this.lastPage * rowsPerPage) / rowsPerPage);
    this.loading.set(true);
    this.http.get<Page<Donation>>(`/api/donations?page=${this.lastPage}&size=${rowsPerPage}`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  openNew(): void {
    this.form.reset({ paymentMode: 'CASH', fundId: this.funds()[0]?.id ?? null });
    this.error.set(null);
    this.dialogOpen.set(true);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    this.error.set(null);
    try {
      const v = this.form.getRawValue();
      const blankToNull = (x: string | null) => (x && x.trim() ? x.trim() : null);
      const body = v.contactId
        ? { ...v, donorFirstName: null, donorLastName: null, donorEmail: null, donorPhone: null }
        : { ...v, donorFirstName: blankToNull(v.donorFirstName), donorLastName: blankToNull(v.donorLastName),
            donorEmail: blankToNull(v.donorEmail), donorPhone: blankToNull(v.donorPhone) };
      const d = await firstValueFrom(this.http.post<Donation>('/api/donations', body));
      this.dialogOpen.set(false);
      this.toast.add({ severity: 'success', summary: 'Donation saved', detail: `Receipt ${d.receiptNo} · ${this.branding.money(d.amount)}` });
      if (d.donorLink) {
        const how: Record<string, string> = { EMAIL: 'same email', PHONE: 'same phone number', NAME: 'same name' };
        this.toast.add({
          severity: 'info',
          summary: d.donorLink.matchedBy === 'CREATED' ? 'New contact created' : 'Linked to existing contact',
          detail: d.donorLink.matchedBy === 'CREATED' ? d.donorLink.name : `${d.donorLink.name} (${how[d.donorLink.matchedBy]})`,
        });
        this.http.get<Page<Contact>>('/api/contacts?size=100').subscribe((p) => this.contacts.set(p.content));
      }
      this.load();
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save the donation. Please try again.');
    } finally {
      this.saving.set(false);
    }
  }

  async confirmVoid(): Promise<void> {
    const d = this.voiding();
    if (!d || this.voidReason.invalid) return;
    await firstValueFrom(this.http.post(`/api/donations/${d.id}/void`, { reason: this.voidReason.value }));
    this.toast.add({ severity: 'info', summary: 'Donation voided', detail: d.receiptNo });
    this.voiding.set(null);
    this.voidReason.reset();
    this.load();
  }

  protected modeLabel(mode: PaymentMode): string {
    return this.modes().find((m) => m.value === mode)?.label ?? mode;
  }
}
