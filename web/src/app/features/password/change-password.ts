import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Password } from 'primeng/password';
import { AuthService } from '../../core/auth.service';
import { Me } from '../../core/models';

/** Change your own password. Required first, before anything else, when signing in with a temporary password. */
@Component({
  selector: 'sd-change-password',
  imports: [ReactiveFormsModule, Button, Password],
  template: `
    <div class="sd-page-head">
      <div>
        <h1>{{ forced() ? 'Choose a new password' : 'Change password' }}</h1>
        <p>Signed in as {{ auth.user()?.email }}</p>
      </div>
    </div>
    <form class="sd-card pw" [formGroup]="form" (ngSubmit)="save()" novalidate>
      @if (forced()) {
        <p class="notice">You signed in with a temporary password. Please choose your own password to continue.</p>
      }
      <label>{{ forced() ? 'Temporary password' : 'Current password' }}
        <p-password formControlName="current" [feedback]="false" [toggleMask]="true" autocomplete="current-password" fluid />
      </label>
      <label>New password (at least 12 characters)
        <p-password formControlName="password" [feedback]="false" [toggleMask]="true" autocomplete="new-password" fluid />
      </label>
      <label>Type the new password again
        <p-password formControlName="confirm" [feedback]="false" [toggleMask]="true" autocomplete="new-password" fluid />
      </label>
      @if (error()) {
        <p class="sd-error" role="alert">{{ error() }}</p>
      }
      <p class="sd-muted small">Changing your password signs you out on your other devices.</p>
      <p-button type="submit" label="Save new password" [loading]="busy()" />
    </form>
  `,
  styles: `
    .pw { max-width: 460px; display: flex; flex-direction: column; gap: 12px; }
    label { display: flex; flex-direction: column; gap: 4px; font-size: 0.85rem; color: var(--sd-ink-soft); }
    .notice { background: var(--sd-soft); border-left: 3px solid var(--sd-accent); padding: 10px 12px; border-radius: 0 8px 8px 0; margin: 0; }
    .small { font-size: 0.82rem; margin: 0; }
  `,
})
export class ChangePassword {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly toast = inject(MessageService);
  protected readonly auth = inject(AuthService);

  protected readonly forced = () => this.auth.user()?.mustChangePassword ?? false;
  protected readonly busy = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    current: ['', Validators.required],
    password: ['', Validators.required],
    confirm: ['', Validators.required],
  });

  async save(): Promise<void> {
    const { current, password, confirm } = this.form.getRawValue();
    if (!current) return this.error.set('Enter your current password.');
    if (password.length < 12) return this.error.set('Use at least 12 characters.');
    if (password !== confirm) return this.error.set("The two new passwords don't match.");
    this.busy.set(true);
    this.error.set(null);
    try {
      const me = await firstValueFrom(
        this.http.post<Me>('/api/auth/change-password', { currentPassword: current, newPassword: password }),
      );
      this.auth.setUser(me);
      this.form.reset();
      this.toast.add({ severity: 'success', summary: 'Password changed' });
      await this.router.navigateByUrl(this.auth.homePath());
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not change the password. Please try again.');
    } finally {
      this.busy.set(false);
    }
  }
}
