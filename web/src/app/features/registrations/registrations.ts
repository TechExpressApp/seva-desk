import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
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
import { AuthService } from '../../core/auth.service';
import { EventOption, EventRegistration, Page, RegistrationResult } from '../../core/models';
import { ContactDetails } from '../../shared/contact-details';

const US_STATES = [
  'Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado', 'Connecticut', 'Delaware', 'Florida',
  'Georgia', 'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky', 'Louisiana', 'Maine',
  'Maryland', 'Massachusetts', 'Michigan', 'Minnesota', 'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada',
  'New Hampshire', 'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota', 'Ohio', 'Oklahoma',
  'Oregon', 'Pennsylvania', 'Rhode Island', 'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah',
  'Vermont', 'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming', 'District of Columbia',
];

/**
 * Event registrations: who signed up for which event. Registering someone also adds them to contacts
 * (or matches an existing contact) and emails the temple. Admins, event coordinators and front desk.
 */
@Component({
  selector: 'sd-registrations',
  imports: [DatePipe, FormsModule, ReactiveFormsModule, AutoComplete, Button, Dialog, InputNumber, InputText, Select,
    TableModule, ContactDetails],
  templateUrl: './registrations.html',
  styleUrl: './registrations.scss',
})
export class Registrations implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);
  protected readonly auth = inject(AuthService);
  protected readonly states = US_STATES;

  protected readonly rows = signal<EventRegistration[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly eventOptions = signal<EventOption[]>([]);
  protected readonly eventSuggestions = signal<EventOption[]>([]);
  protected readonly canDelete = computed(() => this.auth.hasAnyRole('ADMIN', 'EVENT_COORDINATOR'));

  protected filterEvent: string | null = null;

  // dialogs
  protected readonly adding = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly deleting = signal<EventRegistration | null>(null);
  protected readonly profileOf = signal<EventRegistration | null>(null);
  /** The event of the last registration, offered again for the next one */
  private lastEvent: string | null = null;

  protected readonly form = inject(FormBuilder).group({
    event: [null as EventOption | string | null, Validators.required],
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    whatsapp: ['', [Validators.required, Validators.pattern(/^\+?[0-9 ()\-.]{7,40}$/)]],
    city: ['', [Validators.required, Validators.maxLength(100)]],
    state: ['', [Validators.required, Validators.maxLength(100)]],
    age: [null as number | null, [Validators.required, Validators.min(0), Validators.max(120)]],
  });

  private page = 0;
  private pageSize = 25;

  ngOnInit(): void {
    this.loadEventOptions();
  }

  private loadEventOptions(): void {
    this.http.get<EventOption[]>('/api/event-registrations/event-options').subscribe((o) => this.eventOptions.set(o));
  }

  load(event?: TableLazyLoadEvent): void {
    if (event) {
      this.pageSize = event.rows ?? 25;
      this.page = Math.floor((event.first ?? 0) / this.pageSize);
    }
    const params = new URLSearchParams({ page: String(this.page), size: String(this.pageSize) });
    if (this.filterEvent) params.set('eventName', this.filterEvent);
    this.loading.set(true);
    this.http.get<Page<EventRegistration>>(`/api/event-registrations?${params}`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  applyFilter(): void {
    this.page = 0;
    this.load();
  }

  // ---- Event field: upcoming events and names used before, or type a new one ----

  searchEvents(e: AutoCompleteCompleteEvent): void {
    const q = e.query.trim().toLowerCase();
    this.eventSuggestions.set(q ? this.eventOptions().filter((o) => o.name.toLowerCase().includes(q)) : [...this.eventOptions()]);
  }

  private eventName(): string {
    const v = this.form.controls.event.value;
    return (typeof v === 'string' ? v : v?.name ?? '').trim();
  }

  // ---- add ----

  open(): void {
    this.error.set(null);
    const last = this.lastEvent ? this.eventOptions().find((o) => o.name === this.lastEvent) ?? this.lastEvent : null;
    this.form.reset({ event: last, firstName: '', lastName: '', email: '', whatsapp: '', city: '', state: '', age: null });
    this.adding.set(true);
  }

  async save(): Promise<void> {
    const v = this.form.getRawValue();
    const eventName = this.eventName();
    const c = this.form.controls;
    if (!eventName) return this.error.set('Choose or type the event.');
    if (c.firstName.invalid || c.lastName.invalid) return this.error.set('Enter the first and last name.');
    if (c.email.invalid) return this.error.set('Enter a valid email address.');
    if (c.whatsapp.invalid) return this.error.set('Enter the WhatsApp number, e.g. +1 732 555 0100.');
    if (c.city.invalid || c.state.invalid) return this.error.set('Enter the city and state.');
    if (c.age.invalid) return this.error.set('Enter the age (0–120).');
    const body = {
      eventName,
      firstName: v.firstName!.trim(),
      lastName: v.lastName!.trim(),
      email: v.email!.trim(),
      whatsapp: v.whatsapp!.trim(),
      city: v.city!.trim(),
      state: v.state!.trim(),
      age: v.age,
    };
    this.saving.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.http.post<RegistrationResult>('/api/event-registrations', body));
      const r = res.registration;
      const contact = res.contact === 'CREATED' ? 'Added as a new contact' : 'Matched an existing contact';
      this.toast.add({
        severity: 'success',
        summary: 'Registered',
        detail: `${r.firstName} ${r.lastName} · ${r.eventName}. ${contact}.`,
      });
      if (!res.emailSent) {
        this.toast.add({
          severity: 'warn',
          summary: 'Email not sent',
          detail: 'The registration is saved, but the temple was not emailed because email isn’t set up yet.',
          life: 8000,
        });
      }
      this.lastEvent = r.eventName;
      this.adding.set(false);
      this.load();
      this.loadEventOptions();
    } catch (e) {
      const err = (e as HttpErrorResponse).error;
      this.error.set(err?.detail ?? err?.errors?.[0]?.message ?? 'Could not save the registration. Please check the fields.');
    } finally {
      this.saving.set(false);
    }
  }

  /** wa.me link: digits only; a 10-digit US number gets the +1 country code. */
  protected whatsappLink(r: EventRegistration): string {
    const digits = r.whatsapp.replace(/[^0-9]/g, '');
    return `https://wa.me/${digits.length === 10 ? '1' + digits : digits}`;
  }

  async confirmDelete(): Promise<void> {
    const r = this.deleting();
    if (!r) return;
    await firstValueFrom(this.http.delete(`/api/event-registrations/${r.id}`));
    this.toast.add({ severity: 'info', summary: 'Registration removed', detail: `${r.firstName} ${r.lastName} · ${r.eventName}` });
    this.deleting.set(null);
    this.load();
  }
}
