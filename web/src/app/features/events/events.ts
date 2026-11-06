import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Textarea } from 'primeng/textarea';
import { EventStatus, Page, TempleEvent } from '../../core/models';

/** Events shown on the temple's public website. Saving as "Published" puts it on the site immediately. */
@Component({
  selector: 'sd-events',
  imports: [ReactiveFormsModule, DatePipe, TableModule, Button, Dialog, InputText, Select, Tag, Textarea],
  templateUrl: './events.html',
})
export class Events {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);

  protected readonly rows = signal<TempleEvent[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly editing = signal<TempleEvent | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly deleting = signal<TempleEvent | null>(null);

  protected readonly statuses: { label: string; value: EventStatus }[] = [
    { label: 'Published (on the website)', value: 'PUBLISHED' },
    { label: 'Draft (staff only)', value: 'DRAFT' },
    { label: 'Cancelled (hidden)', value: 'CANCELLED' },
  ];

  protected readonly form = inject(FormBuilder).group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    startsAt: ['', Validators.required],
    endsAt: [''],
    location: [''],
    summary: ['', Validators.maxLength(400)],
    description: [''],
    imageUrl: [''],
    registrationUrl: [''],
    status: ['PUBLISHED' as EventStatus, Validators.required],
  });

  private page = 0;

  load(event?: TableLazyLoadEvent): void {
    if (event) this.page = Math.floor((event.first ?? 0) / (event.rows ?? 25));
    this.loading.set(true);
    this.http.get<Page<TempleEvent>>(`/api/events?page=${this.page}&size=25`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  open(e: TempleEvent | 'new'): void {
    this.error.set(null);
    if (e === 'new') {
      this.form.reset({ status: 'PUBLISHED', startsAt: '', endsAt: '' });
    } else {
      this.form.reset({
        ...e,
        startsAt: toLocalInput(e.startsAt),
        endsAt: e.endsAt ? toLocalInput(e.endsAt) : '',
        summary: e.summary ?? '',
        description: e.description ?? '',
        location: e.location ?? '',
        imageUrl: e.imageUrl ?? '',
        registrationUrl: e.registrationUrl ?? '',
      });
    }
    this.editing.set(e);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const body = {
      ...v,
      startsAt: new Date(v.startsAt!).toISOString(),
      endsAt: v.endsAt ? new Date(v.endsAt).toISOString() : null,
      summary: v.summary || null,
      description: v.description || null,
      location: v.location || null,
      imageUrl: v.imageUrl || null,
      registrationUrl: v.registrationUrl || null,
    };
    const target = this.editing();
    this.saving.set(true);
    this.error.set(null);
    try {
      const saved =
        target === 'new'
          ? await firstValueFrom(this.http.post<TempleEvent>('/api/events', body))
          : await firstValueFrom(this.http.put<TempleEvent>(`/api/events/${(target as TempleEvent).id}`, body));
      this.editing.set(null);
      this.toast.add({
        severity: 'success',
        summary: saved.status === 'PUBLISHED' ? 'Event published on the website' : 'Event saved',
        detail: saved.title,
      });
      this.load();
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save the event. Please check the fields.');
    } finally {
      this.saving.set(false);
    }
  }

  async confirmDelete(): Promise<void> {
    const e = this.deleting();
    if (!e) return;
    await firstValueFrom(this.http.delete(`/api/events/${e.id}`));
    this.toast.add({ severity: 'info', summary: 'Event deleted', detail: e.title });
    this.deleting.set(null);
    this.load();
  }

  protected severity(s: EventStatus): 'success' | 'secondary' | 'danger' {
    return s === 'PUBLISHED' ? 'success' : s === 'DRAFT' ? 'secondary' : 'danger';
  }
}

/** ISO instant → value for <input type="datetime-local"> in the browser's time zone. */
function toLocalInput(iso: string): string {
  const d = new Date(iso);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
