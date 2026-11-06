import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Table, TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BrandingService } from '../../core/branding.service';
import { Contact, Donation, DonorPage, DonorTotal, Page } from '../../core/models';
import { Router } from '@angular/router';
import { devoteeStatusLabel } from '../../core/devotee-status';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Tag } from 'primeng/tag';

/**
 * Total giving per person. Donations are linked to people by email, phone or name when they are recorded.
 * Click a column header to sort by it; click again to reverse.
 */
@Component({
  selector: 'sd-donors',
  imports: [DatePipe, TableModule, IconField, InputIcon, InputText, Dialog, Tag, Button],
  templateUrl: './donors.html',
})
export class Donors {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);

  protected readonly rows = signal<DonorTotal[]>([]);
  protected readonly total = signal(0);
  protected readonly grandTotal = signal(0);
  protected readonly loading = signal(false);

  /** The donor whose donations are shown in the popup */
  protected readonly selected = signal<DonorTotal | null>(null);
  protected readonly donations = signal<Donation[]>([]);
  protected readonly donationsTotal = signal(0);
  protected readonly donationsLoading = signal(false);

  /** The donor whose contact details are shown, and the full contact record once loaded */
  protected readonly profileOf = signal<DonorTotal | null>(null);
  protected readonly profile = signal<Contact | null>(null);
  protected readonly profileError = signal<string | null>(null);
  private readonly router = inject(Router);
  protected readonly statusLabel = devoteeStatusLabel;

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

  /** Called by the table on first show, paging and header clicks. */
  load(event: TableLazyLoadEvent = this.lastEvent): void {
    this.lastEvent = event;
    const rows = event.rows ?? 25;
    const page = Math.floor((event.first ?? 0) / rows);
    const sort = (Array.isArray(event.sortField) ? event.sortField[0] : event.sortField) || 'total';
    const dir = event.sortOrder === 1 ? 'asc' : 'desc';
    const q = encodeURIComponent(this.query);
    this.loading.set(true);
    this.http.get<DonorPage>(`/api/donors?q=${q}&page=${page}&size=${rows}&sort=${sort}&dir=${dir}`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.totalDonors);
        this.grandTotal.set(p.grandTotal);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  /** Opens the popup with this donor's complete contact details. */
  openProfile(d: DonorTotal): void {
    this.profile.set(null);
    this.profileError.set(null);
    this.profileOf.set(d);
    this.http.get<Contact>(`/api/contacts/${d.contactId}`).subscribe({
      next: (c) => this.profile.set(c),
      error: () => this.profileError.set('Could not load the contact details. Please try again.'),
    });
  }

  /** From the contact popup to the donations popup for the same person. */
  showDonationsOf(d: DonorTotal): void {
    this.profileOf.set(null);
    this.openDonations(d);
  }

  /** Opens the Contacts page with this person's edit dialog open. */
  editContact(d: DonorTotal): void {
    this.router.navigate(['/contacts'], { queryParams: { edit: d.contactId } });
  }

  /** Extra fields worth showing, with readable labels; "family" and internal keys are shown elsewhere or hidden. */
  protected extraDetails(c: Contact): { label: string; value: string }[] {
    const labels: Record<string, string> = {
      initiatedName: 'Initiated name',
      spiritualMaster: 'Spiritual master',
      source: 'Added from',
    };
    return Object.entries(c.customFields ?? {})
      .filter(([k, v]) => k !== 'family' && v !== null && v !== '' && typeof v !== 'object')
      .map(([k, v]) => ({
        label: labels[k] ?? k.replace(/([A-Z])/g, ' $1').replace(/^./, (ch) => ch.toUpperCase()),
        value: String(v),
      }));
  }

  /** Opens the popup listing every donation of this person (voided ones included and marked). */
  openDonations(d: DonorTotal): void {
    this.donations.set([]);
    this.donationsTotal.set(0);
    this.selected.set(d);
  }

  loadDonations(event: TableLazyLoadEvent): void {
    const d = this.selected();
    if (!d) return;
    const rows = event.rows ?? 10;
    const page = Math.floor((event.first ?? 0) / rows);
    this.donationsLoading.set(true);
    this.http.get<Page<Donation>>(`/api/donations?contactId=${d.contactId}&page=${page}&size=${rows}`).subscribe({
      next: (p) => {
        this.donations.set(p.content);
        this.donationsTotal.set(p.page.totalElements);
        this.donationsLoading.set(false);
      },
      error: () => this.donationsLoading.set(false),
    });
  }

  protected modeLabel(mode: string): string {
    return this.branding.branding()?.paymentModes?.find((m) => m.value === mode)?.label
      ?? mode.replace('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
  }

  /** Back to the default order: biggest givers first. */
  resetSort(table: Table): void {
    table.sortField = 'total';
    table.sortOrder = -1;
    table.first = 0;
    this.load({ ...this.lastEvent, first: 0, sortField: 'total', sortOrder: -1 });
  }

  protected sortedBy(): string {
    const labels: Record<string, string> = {
      name: 'name', email: 'email', gifts: 'number of donations', thisYear: 'this year',
      total: 'total given', lastGift: 'last donation',
    };
    const f = (this.lastEvent.sortField as string) || 'total';
    const asc = this.lastEvent.sortOrder === 1;
    const direction =
      f === 'name' || f === 'email' ? (asc ? 'A–Z' : 'Z–A')
      : f === 'lastGift' ? (asc ? 'oldest first' : 'newest first')
      : asc ? 'lowest first' : 'highest first';
    return `${labels[f] ?? 'total given'} (${direction})`;
  }
}
