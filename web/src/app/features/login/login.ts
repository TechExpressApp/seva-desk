import { Component, inject, input, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { AuthService } from '../../core/auth.service';
import { BrandingService } from '../../core/branding.service';
import { Lotus } from '../../core/lotus';

@Component({
  selector: 'sd-login',
  imports: [ReactiveFormsModule, TranslatePipe, Button, InputText, Password, Lotus],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  protected readonly branding = inject(BrandingService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  /** ?next=/donations from the auth guard */
  readonly next = input<string>();

  protected readonly busy = signal(false);
  protected readonly failed = signal(false);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  async submit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.busy.set(true);
    this.failed.set(false);
    try {
      const { email, password } = this.form.getRawValue();
      await this.auth.login(email, password);
      await this.router.navigateByUrl(this.next() || '/dashboard');
    } catch {
      this.failed.set(true);
    } finally {
      this.busy.set(false);
    }
  }
}
