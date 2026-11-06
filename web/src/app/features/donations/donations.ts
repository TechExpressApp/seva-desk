import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, input, signal } from '@angular/core';
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

  protected readonly modes: { label: string; value: PaymentMode }[] = [
    { label: 'Cash', value: 'CASH' },
    { label: 'UPI', value: 'UPI' },
    { label: 'Card', value: 'CARD' },
    { label: 'Cheque', value: 'CHEQUE' },
    { label: 'Bank transfer', value: 'BANK_TRANSFER' },
    { label: 'Online', value: 'ONLINE' },
    { label: 'In kind', value: 'IN_KIND' },
  ];

  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.group({
    contactId: [null as string | null],
    fundId: [null as string | null, Validators.required],
    amount: [null as number | null, [Validators.required, Validators.min(1)]],
    paymentMode: ['CASH' as PaymentMode, Validators.required],
    reference: [''],
    notes: [''],
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
      const d = await firstValueFrom(this.http.post<Donation>('/api/donations', this.form.getRawValue()));
      this.dialogOpen.set(false);
      this.toast.add({ severity: 'success', summary: 'Donation saved', detail: `Receipt ${d.receiptNo} · ${this.branding.money(d.amount)}` });
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

  protected contactName(id: string | null): string {
    if (!id) return 'Anonymous';
    return this.contacts().find((c) => c.id === id)?.fullName ?? '—';
  }

  protected modeLabel(mode: PaymentMode): string {
    return this.modes.find((m) => m.value === mode)?.label ?? mode;
  }
}
