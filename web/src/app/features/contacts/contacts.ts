import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime, firstValueFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Contact, Page } from '../../core/models';

@Component({
  selector: 'sd-contacts',
  imports: [ReactiveFormsModule, TableModule, Button, Checkbox, Dialog, IconField, InputIcon, InputText],
  templateUrl: './contacts.html',
})
export class Contacts {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);

  protected readonly rows = signal<Contact[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);
  protected readonly editing = signal<Contact | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);

  private query = '';
  private page = 0;
  private readonly search$ = new Subject<string>();

  protected readonly form = inject(FormBuilder).group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: [''],
    phone: [''],
    email: ['', Validators.email],
    city: [''],
    gotra: [''],
    nakshatra: [''],
    birthDate: [null as string | null],
    consentEmail: [false],
    consentWhatsapp: [false],
    // Family birthdays (optional): spouse and up to three children
    spouseName: ['', Validators.maxLength(100)],
    spouseBirthDate: [null as string | null],
    child1Name: ['', Validators.maxLength(100)],
    child1BirthDate: [null as string | null],
    child2Name: ['', Validators.maxLength(100)],
    child2BirthDate: [null as string | null],
    child3Name: ['', Validators.maxLength(100)],
    child3BirthDate: [null as string | null],
  });

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

  load(event?: TableLazyLoadEvent): void {
    if (event) this.page = Math.floor((event.first ?? 0) / (event.rows ?? 25));
    this.loading.set(true);
    const q = encodeURIComponent(this.query);
    this.http.get<Page<Contact>>(`/api/contacts?q=${q}&page=${this.page}&size=25`).subscribe({
      next: (p) => {
        this.rows.set(p.content);
        this.total.set(p.page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  open(contact: Contact | 'new'): void {
    this.error.set(null);
    if (contact === 'new') {
      this.form.reset({ consentEmail: false, consentWhatsapp: false });
    } else {
      const kids = contact.family?.children ?? [];
      this.form.reset({
        ...contact,
        spouseName: contact.family?.spouse?.name ?? '',
        spouseBirthDate: contact.family?.spouse?.birthDate ?? null,
        child1Name: kids[0]?.name ?? '',
        child1BirthDate: kids[0]?.birthDate ?? null,
        child2Name: kids[1]?.name ?? '',
        child2BirthDate: kids[1]?.birthDate ?? null,
        child3Name: kids[2]?.name ?? '',
        child3BirthDate: kids[2]?.birthDate ?? null,
      });
    }
    this.editing.set(contact);
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const target = this.editing();
    const {
      spouseName, spouseBirthDate, child1Name, child1BirthDate,
      child2Name, child2BirthDate, child3Name, child3BirthDate, ...rest
    } = this.form.getRawValue();
    const member = (name: string | null, birthDate: string | null) => ({ name: name?.trim() || null, birthDate: birthDate || null });
    const body = {
      ...rest,
      family: {
        spouse: member(spouseName, spouseBirthDate),
        children: [member(child1Name, child1BirthDate), member(child2Name, child2BirthDate), member(child3Name, child3BirthDate)]
          .filter((c) => c.name || c.birthDate),
      },
    };
    this.saving.set(true);
    try {
      if (target === 'new') {
        await firstValueFrom(this.http.post<Contact>('/api/contacts', body));
      } else if (target) {
        await firstValueFrom(this.http.put<Contact>(`/api/contacts/${target.id}`, body));
      }
      this.toast.add({ severity: 'success', summary: 'Contact saved', detail: `${body.firstName} ${body.lastName ?? ''}` });
      this.editing.set(null);
      this.load();
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save. Please check the fields and try again.');
    } finally {
      this.saving.set(false);
    }
  }
}
