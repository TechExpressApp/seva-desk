import { HttpClient } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Button } from 'primeng/button';
import { BrandingService } from '../../core/branding.service';
import { DashboardSummary, Donation, DonationTick, Page } from '../../core/models';

/** Live totals over Server-Sent Events: the API pushes "summary" and "donation" events as gifts are saved. */
@Component({
  selector: 'sd-dashboard',
  imports: [TranslatePipe, RouterLink, Button],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit, OnDestroy {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);

  protected readonly summary = signal<DashboardSummary | null>(null);
  protected readonly feed = signal<(DonationTick & { fresh?: boolean })[]>([]);
  protected readonly connected = signal(false);

  protected readonly fundMax = computed(() =>
    Math.max(1, ...(this.summary()?.monthByFund.map((f) => f.amount) ?? [1])),
  );

  private source?: EventSource;

  ngOnInit(): void {
    this.http.get<Page<Donation>>('/api/donations?size=6').subscribe((page) =>
      this.feed.set(
        page.content.map((d) => ({
          receiptNo: d.receiptNo,
          amount: d.amount,
          fund: d.fundName,
          donor: null,
          mode: d.paymentMode,
          voided: d.status === 'VOIDED',
        })),
      ),
    );
    this.connect();
  }

  ngOnDestroy(): void {
    this.source?.close();
  }

  private connect(): void {
    this.source = new EventSource('/api/dashboard/stream');
    this.source.onopen = () => this.connected.set(true);
    this.source.onerror = () => this.connected.set(false); // EventSource reconnects by itself
    this.source.addEventListener('summary', (e) => this.summary.set(JSON.parse((e as MessageEvent).data)));
    this.source.addEventListener('donation', (e) => {
      const tick: DonationTick = JSON.parse((e as MessageEvent).data);
      if (tick.voided) return;
      this.feed.update((list) => [{ ...tick, fresh: true }, ...list].slice(0, 6));
    });
  }

  protected modeLabel(mode: string): string {
    return mode.replace('_', ' ').toLowerCase();
  }
}
