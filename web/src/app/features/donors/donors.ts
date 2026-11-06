import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { BrandingService } from '../../core/branding.service';
import { DonorPage, DonorTotal } from '../../core/models';

/** Total giving per person. Donations are linked to people by email, phone or name when they are recorded. */
@Component({
  selector: 'sd-donors',
  imports: [DatePipe, FormsModule, TableModule, IconField, InputIcon, InputText, Select],
  templateUrl: './donors.html',
})
export class Donors {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);

  protected readonly rows = signal<DonorTotal[]>([]);
  protected readonly total = signal(0);
  protected readonly grandTotal = signal(0);
  protected readonly loading = signal(false);
  protected readonly sort = signal('total');

  protected readonly sorts = [
    { label: 'Most given', value: 'total' },
    { label: 'Most gifts', value: 'gifts' },
    { label: 'Most recent gift', value: 'recent' },
    { label: 'Name', value: 'name' },
  ];

  private query = '';
  private page = 0;
  private readonly search$ = new Subject<string>();

  constructor() {
    this.search$.pipe(debounceTime(300), takeUntilDestroyed()).subscribe((q) => {
      this.query = q;
      this.page = 0;
      this.load();
    });
  }

  onSearch(value: string): void {
    this.search$.next(value.trim());
  }

  onSort(value: string): void {
    this.sort.set(value);
    this.page = 0;
    this.load();
  }

  load(event?: TableLazyLoadEvent): void {
    if (event) this.page = Math.floor((event.first ?? 0) / (event.rows ?? 25));
    this.loading.set(true);
    const q = encodeURIComponent(this.query);
    this.http.get<DonorPage>(`/api/donors?q=${q}&page=${this.page}&size=25&sort=${this.sort()}`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.totalDonors);
        this.grandTotal.set(p.grandTotal);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
