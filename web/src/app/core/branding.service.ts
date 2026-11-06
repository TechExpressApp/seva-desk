import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { PrimeNG } from 'primeng/config';
import { firstValueFrom } from 'rxjs';
import { Branding } from './models';
import { sevaTheme } from './theme';

/**
 * Loads the temple's branding for this address before the first screen paints, then applies it:
 * CSS colour tokens, PrimeNG's primary palette, page title, and vocabulary overrides (Priest → Archaka…).
 */
@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly http = inject(HttpClient);
  private readonly translate = inject(TranslateService);
  private readonly primeng = inject(PrimeNG);

  readonly branding = signal<Branding | null>(null);
  readonly error = signal<string | null>(null);

  readonly monogram = computed(() => this.branding()?.shortName ?? '');

  private readonly moneyFormat = computed(() => {
    const b = this.branding();
    return new Intl.NumberFormat(b?.currency === 'INR' ? 'en-IN' : undefined, {
      style: 'currency',
      currency: b?.currency ?? 'INR',
      maximumFractionDigits: 0,
    });
  });

  async load(): Promise<void> {
    try {
      const b = await firstValueFrom(this.http.get<Branding>('/api/public/branding'));
      this.branding.set(b);
      this.applyTheme(b);
      await this.applyVocabulary(b);
    } catch {
      this.error.set('This address is not linked to a temple yet.');
      await firstValueFrom(this.translate.use('en'));
    }
  }

  money(amount: number | null | undefined): string {
    return this.moneyFormat().format(amount ?? 0);
  }

  isEnabled(module: string): boolean {
    return this.branding()?.enabledModules.includes(module) ?? false;
  }

  private applyTheme(b: Branding): void {
    const root = document.documentElement.style;
    root.setProperty('--sd-primary', b.primaryColor);
    root.setProperty('--sd-accent', b.accentColor);
    root.setProperty('--sd-soft', b.softColor);
    this.primeng.theme.set(sevaTheme(b.primaryColor));
    document.title = b.name;
    document.querySelector('meta[name="theme-color"]')?.setAttribute('content', b.primaryColor);
    if (b.logoUrl) {
      document.querySelector<HTMLLinkElement>('link[rel="icon"]')?.setAttribute('href', b.logoUrl);
    }
  }

  /** Terms arrive flat ({"term.priest": "Archaka"}); ngx-translate wants them nested. */
  private async applyVocabulary(b: Branding): Promise<void> {
    const lang = b.locale || 'en';
    await firstValueFrom(this.translate.use(lang));
    type Tree = { [key: string]: string | Tree };
    const nested: Tree = { tenant: { greeting: b.greeting, name: b.name } };
    for (const [key, value] of Object.entries(b.terms ?? {})) {
      const parts = key.split('.');
      let node = nested;
      parts.slice(0, -1).forEach((p) => (node = (node[p] ??= {}) as Tree));
      node[parts[parts.length - 1]] = value;
    }
    this.translate.setTranslation(lang, nested, true);
  }
}
