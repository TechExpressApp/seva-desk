import { HttpClient } from '@angular/common/http';
import { Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Tag } from 'primeng/tag';
import { devoteeStatusLabel } from '../core/devotee-status';
import { Contact } from '../core/models';

/**
 * A contact's complete details (contact info, devotee details, consent, extra fields, family birthdays),
 * read-only. Used in the Donors and Expenses by Person popups.
 */
@Component({
  selector: 'sd-contact-details',
  imports: [DatePipe, Tag],
  template: `
    @if (error()) {
      <p class="sd-error">{{ error() }}</p>
    } @else if (!contact()) {
      <p class="sd-muted">Loading…</p>
    } @else {
      @let c = contact()!;
      <h3 class="pf-h">Contact</h3>
      <dl class="pf">
        <div><dt>Name</dt><dd>{{ c.fullName }}</dd></div>
        <div><dt>Email</dt><dd>{{ c.email || '—' }}</dd></div>
        <div><dt>Phone / WhatsApp</dt><dd>{{ c.phone || '—' }}</dd></div>
        <div><dt>City</dt><dd>{{ c.city || '—' }}</dd></div>
        <div><dt>Date of birth</dt><dd>{{ c.birthDate ? (c.birthDate | date: 'd MMMM y') : '—' }}</dd></div>
        <div><dt>Initiated name</dt><dd>{{ c.initiatedName || '—' }}</dd></div>
        <div><dt>Spiritual master</dt><dd>{{ c.spiritualMaster || '—' }}</dd></div>
        <div><dt>Status</dt><dd>{{ statusLabel(c.devoteeStatus) || '—' }}</dd></div>
        <div><dt>Gotra</dt><dd>{{ c.gotra || '—' }}</dd></div>
        <div><dt>Nakshatra</dt><dd>{{ c.nakshatra || '—' }}</dd></div>
        <div>
          <dt>May contact by</dt>
          <dd>
            @if (c.consentEmail || c.consentWhatsapp) {
              @if (c.consentEmail) { <p-tag value="Email" severity="success" /> }
              @if (c.consentWhatsapp) { <p-tag value="WhatsApp" severity="success" /> }
            } @else {
              <span class="sd-muted">No consent recorded</span>
            }
          </dd>
        </div>
        @for (x of extraDetails(c); track x.label) {
          <div><dt>{{ x.label }}</dt><dd>{{ x.value }}</dd></div>
        }
      </dl>

      <h3 class="pf-h">Family birthdays</h3>
      @if (c.family?.spouse || c.family?.children?.length) {
        <table class="pf-family">
          <tbody>
            @if (c.family?.spouse; as s) {
              <tr><th>Spouse</th><td>{{ s.name || '—' }}</td><td>{{ s.birthDate ? (s.birthDate | date: 'd MMMM y') : '—' }}</td></tr>
            }
            @for (k of c.family?.children ?? []; track $index) {
              <tr><th>Child {{ $index + 1 }}</th><td>{{ k.name || '—' }}</td><td>{{ k.birthDate ? (k.birthDate | date: 'd MMMM y') : '—' }}</td></tr>
            }
          </tbody>
        </table>
      } @else {
        <p class="sd-muted">None recorded.</p>
      }
    }
  `,
})
export class ContactDetails {
  private readonly http = inject(HttpClient);

  readonly contactId = input.required<string>();

  protected readonly contact = signal<Contact | null>(null);
  protected readonly error = signal<string | null>(null);
  protected readonly statusLabel = devoteeStatusLabel;

  constructor() {
    effect(() => {
      const id = this.contactId();
      this.contact.set(null);
      this.error.set(null);
      this.http.get<Contact>(`/api/contacts/${id}`).subscribe({
        next: (c) => this.contact.set(c),
        error: () => this.error.set('Could not load the contact details. Please try again.'),
      });
    });
  }

  /** Extra fields with readable labels; family is shown separately. */
  protected extraDetails(c: Contact): { label: string; value: string }[] {
    const labels: Record<string, string> = {
      source: 'Added from',
      addressLine1: 'Address line 1',
      addressLine2: 'Address line 2',
      zip: 'ZIP',
      preferredLanguage: 'Preferred language',
    };
    return Object.entries(c.customFields ?? {})
      .filter(([k, v]) => k !== 'family' && v !== null && v !== '' && typeof v !== 'object')
      .map(([k, v]) => ({
        label: labels[k] ?? k.replace(/([A-Z])/g, ' $1').replace(/^./, (ch) => ch.toUpperCase()),
        value: String(v),
      }));
  }
}
