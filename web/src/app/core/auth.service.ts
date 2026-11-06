import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { Me } from './models';

/** The token lives in an httpOnly cookie set by the API; this service only tracks who is signed in. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  readonly user = signal<Me | null>(null);
  readonly firstName = computed(() => this.user()?.displayName.split(' ')[0] ?? '');

  async login(email: string, password: string): Promise<void> {
    this.user.set(await firstValueFrom(this.http.post<Me>('/api/auth/login', { email, password })));
  }

  async logout(): Promise<void> {
    await firstValueFrom(this.http.post('/api/auth/logout', {}));
    this.user.set(null);
    await this.router.navigateByUrl('/login');
  }

  /** Restores the session after a page reload. Resolves false if not signed in. */
  async restore(): Promise<boolean> {
    if (this.user()) return true;
    try {
      this.user.set(await firstValueFrom(this.http.get<Me>('/api/auth/me')));
      return true;
    } catch {
      return false;
    }
  }

  hasAnyRole(...roles: string[]): boolean {
    return this.user()?.roles.some((r) => roles.includes(r)) ?? false;
  }
}
