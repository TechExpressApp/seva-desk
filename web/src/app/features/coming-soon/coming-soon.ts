import { Component, computed, input } from '@angular/core';
import { Lotus } from '../../core/lotus';

const PLAN: Record<string, { title: string; weeks: string; items: string[] }> = {
  events: { title: 'Events', weeks: 'Weeks 10–13', items: ['Festival calendar and registrations', 'Sponsorships', 'One-click posting to email, Facebook, Instagram and X'] },
  seva: { title: 'Priest services', weeks: 'Weeks 14–16', items: ['Seva catalogue and fees', 'Priest availability and slot booking', 'Sankalpam sheet with gotra and nakshatra'] },
  members: { title: 'Life members', weeks: 'Weeks 14–16', items: ['Membership plans and instalments', 'QR membership cards', 'Renewal reminders'] },
  guesthouse: { title: 'Guest house', weeks: 'Weeks 17–19', items: ['Room availability grid', 'Check-in and check-out', 'Deposits and invoices'] },
  volunteers: { title: 'Volunteers', weeks: 'Weeks 20–21', items: ['Skills and availability', 'Shift sign-up links', 'Hours and certificates'] },
  finance: { title: 'Finance', weeks: 'Weeks 3–9', items: ['Expense entry screen (the API is ready: POST /api/expenses)', 'Hundi counting and bank deposits', 'Export for the accountant'] },
};

@Component({
  selector: 'sd-coming-soon',
  imports: [Lotus],
  template: `
    @let p = plan();
    <div class="soon sd-card">
      <sd-lotus [size]="56" />
      <h1>{{ p.title }}</h1>
      <p class="sd-muted">Planned for {{ p.weeks }} of the build.</p>
      <ul>
        @for (i of p.items; track i) {
          <li>{{ i }}</li>
        }
      </ul>
    </div>
  `,
  styles: `
    .soon { max-width: 560px; margin: 24px auto; text-align: center; display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 32px 24px; }
    sd-lotus { color: var(--sd-accent); }
    h1 { color: var(--sd-primary); font-size: 1.6rem; }
    ul { text-align: left; margin: 10px 0 0; }
  `,
})
export class ComingSoon {
  readonly module = input.required<string>();
  protected readonly plan = computed(() => PLAN[this.module()] ?? { title: this.module(), weeks: 'a later phase', items: [] });
}
