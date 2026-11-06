import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Button } from 'primeng/button';
import { Password } from 'primeng/password';
import { BrandingService } from '../../core/branding.service';

/** Opened from an emailed link (/admin/reset-password?token=…): new users and forgotten passwords. */
@Component({
  selector: 'sd-reset-password',
  imports: [ReactiveFormsModule, RouterLink, Button, Password],
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="save()" novalidate>
      <span class="temple">{{ branding.branding()?.name }}</span>
      <h1>Choose your password</h1>
      @if (done()) {
        <p class="ok" role="status">Your password is set. You can sign in now.</p>
        <a class="back" routerLink="/login">Go to sign in →</a>
      } @else if (!token()) {
        <p class="sd-error">This link is incomplete. Please open the full link from your email, or ask for a new one.</p>
        <a class="back" routerLink="/forgot-password">Ask for a new link</a>
      } @else {
        <p class="sd-muted">Use at least 12 characters. A short phrase is easy to remember and hard to guess.</p>
        <label>New password
          <p-password formControlName="password" [feedback]="false" [toggleMask]="true" autocomplete="new-password" fluid />
        </label>
        <label>Type it again
          <p-password formControlName="confirm" [feedback]="false" [toggleMask]="true" autocomplete="new-password" fluid />
        </label>
        @if (error()) {
          <p class="sd-error" role="alert">{{ error() }}</p>
        }
        <p-button type="submit" label="Save password" [loading]="busy()" fluid />
        <a class="back" routerLink="/forgot-password">Link expired? Ask for a new one</a>
      }
    </form>
  `,
  styleUrl: './password-page.scss',
})
export class ResetPassword {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);

  /** ?token=… from the email link */
  readonly token = input<string>();

  protected readonly busy = signal(false);
  protected readonly done = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(12)]],
    confirm: ['', Validators.required],
  });

  async save(): Promise<void> {
    const { password, confirm } = this.form.getRawValue();
    if (password.length < 12) return this.error.set('Use at least 12 characters.');
    if (password !== confirm) return this.error.set("The two passwords don't match.");
    this.busy.set(true);
    this.error.set(null);
    try {
      await firstValueFrom(this.http.post('/api/auth/reset-password', { token: this.token(), newPassword: password }));
      this.done.set(true);
    } catch (e) {
      this.error.set((e as HttpErrorResponse).error?.detail ?? 'Could not save. Please try again.');
    } finally {
      this.busy.set(false);
    }
  }
}
