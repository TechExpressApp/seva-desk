import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { BrandingService } from '../../core/branding.service';

/** Asks for a reset link. Always shows the same message, so it never reveals who has an account. */
@Component({
  selector: 'sd-forgot-password',
  imports: [ReactiveFormsModule, RouterLink, Button, InputText],
  template: `
    <form class="card" [formGroup]="form" (ngSubmit)="send()" novalidate>
      <span class="temple">{{ branding.branding()?.name }}</span>
      <h1>Forgot your password?</h1>
      @if (sent()) {
        <p class="ok" role="status">
          If that email has a SevaDesk login, we've sent a link to choose a new password. It works once, for 1 hour.
          Check your spam folder if it doesn't arrive in a few minutes.
        </p>
        <p class="sd-muted">
          No email? Your temple's SevaDesk administrator can also reset your password from the Users screen.
        </p>
      } @else {
        <p class="sd-muted">Enter the email you sign in with and we'll send you a link to choose a new password.</p>
        <label>Email <input pInputText type="email" formControlName="email" autocomplete="username" fluid /></label>
        <p-button type="submit" label="Send reset link" [loading]="busy()" fluid />
      }
      <a class="back" routerLink="/login">← Back to sign in</a>
    </form>
  `,
  styleUrl: './password-page.scss',
})
export class ForgotPassword {
  private readonly http = inject(HttpClient);
  protected readonly branding = inject(BrandingService);
  protected readonly sent = signal(false);
  protected readonly busy = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({ email: ['', [Validators.required, Validators.email]] });

  async send(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    try {
      await firstValueFrom(this.http.post('/api/auth/forgot-password', this.form.getRawValue()));
    } catch {
      // same message either way
    } finally {
      this.busy.set(false);
      this.sent.set(true);
    }
  }
}
