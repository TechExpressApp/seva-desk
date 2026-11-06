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
import { DonorPage, DonorTotal } from '../../core/models';

/**
 * Total giving per person. Donations are linked to people by email, phone or name when they are recorded.
 * Click a column header to sort by it; click again to reverse.
 */
@Component({
  selector: 'sd-donors',
  imports: [DatePipe, TableModule, IconField, InputIcon, InputText],
  templateUrl: './donors.html',
})
export class Donors {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);

  protected readonly rows = signal<DonorTotal[]>([]);
  protected readonly total = signal(0);
  protected readonly grandTotal = signal(0);
  protected readonly loading = signal(false);

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
