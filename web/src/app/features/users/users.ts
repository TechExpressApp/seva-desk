import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { InputText } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { AuthService } from '../../core/auth.service';
import { PasswordIssued, RoleInfo, UserAccount } from '../../core/models';

/** Staff logins: who can sign in, and which modules each person can use. Admins only. */
@Component({
  selector: 'sd-users',
  imports: [DatePipe, FormsModule, ReactiveFormsModule, Button, Checkbox, Dialog, InputText, TableModule, Tag],
  templateUrl: './users.html',
  styleUrl: './users.scss',
})
export class Users implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(MessageService);
  protected readonly auth = inject(AuthService);

  protected readonly users = signal<UserAccount[]>([]);
  protected readonly roles = signal<RoleInfo[]>([]);
  protected readonly loading = signal(false);
  protected readonly editing = signal<UserAccount | 'new' | null>(null);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  /** Shown once after creating a user or resetting a password */
  protected readonly issued = signal<PasswordIssued | null>(null);
  protected readonly resetting = signal<UserAccount | null>(null);
  protected readonly selectedRoles = signal<string[]>([]);

  protected readonly roleLabel = computed(() => new Map(this.roles().map((r) => [r.role, r.label])));

  protected readonly form = inject(FormBuilder).nonNullable.group({
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email]],
    active: [true],
  });

  ngOnInit(): void {
    this.http.get<RoleInfo[]>('/api/users/roles').subscribe((r) => this.roles.set(r));
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.http.get<UserAccount[]>('/api/users').subscribe({
      next: (u) => {
        this.users.set(u);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  open(user: UserAccount | 'new'): void {
    this.error.set(null);
    if (user === 'new') {
      this.form.reset({ displayName: '', email: '', active: true });
      this.form.controls.email.enable();
      this.selectedRoles.set(['FRONT_DESK']);
    } else {
      this.form.reset({ displayName: user.displayName, email: user.email, active: user.active });
      this.form.controls.email.disable();
      this.selectedRoles.set([...user.roles]);
    }
    this.editing.set(user);
  }

  toggleRole(role: string, on: boolean): void {
    this.selectedRoles.update((list) => (on ? [...new Set([...list, role])] : list.filter((r) => r !== role)));
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.selectedRoles().length === 0) {
      this.error.set('Choose at least one area this person can use.');
      return;
    }
    const target = this.editing();
    const v = this.form.getRawValue();
    this.saving.set(true);
    this.error.set(null);
    try {
      if (target === 'new') {
        const res = await firstValueFrom(
          this.http.post<PasswordIssued>('/api/users', { displayName: v.displayName, email: v.email, roles: this.selectedRoles() }),
        );
        this.editing.set(null);
        this.issued.set(res);
      } else if (target) {
        await firstValueFrom(
          this.http.put<UserAccount>(`/api/users/${target.id}`, {
            displayName: v.displayName,
            roles: this.selectedRoles(),
            active: v.active,
          }),
        );
        this.editing.set(null);
        this.toast.add({ severity: 'success', summary: 'Login updated', detail: 'Changes apply the next time they sign in.' });
      }
      this.load();
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save. Please try again.');
    } finally {
      this.saving.set(false);
    }
  }

  async confirmReset(): Promise<void> {
    const user = this.resetting();
    if (!user) return;
    try {
      const res = await firstValueFrom(this.http.post<PasswordIssued>(`/api/users/${user.id}/reset-password`, {}));
      this.resetting.set(null);
      this.issued.set(res);
      this.load();
    } catch (e) {
      this.resetting.set(null);
      this.toast.add({ severity: 'error', summary: 'Could not reset', detail: (e as HttpErrorResponse).error?.detail });
    }
  }

  async copy(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.toast.add({ severity: 'success', summary: 'Copied' });
    } catch {
      this.toast.add({ severity: 'info', summary: 'Select the password and copy it' });
    }
  }

  protected isMe(u: UserAccount): boolean {
    return u.email === this.auth.user()?.email;
  }
}
