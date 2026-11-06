import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { MessageService } from 'primeng/api';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';
import { BrandingService } from './core/branding.service';
import { sessionExpiredInterceptor } from './core/auth.guard';
import { sevaTheme } from './core/theme';
export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([sessionExpiredInterceptor])),
    // Kumkum default; BrandingService replaces it with the temple's colour at startup.
    providePrimeNG({ theme: sevaTheme('#8A1C2B') }),
    provideTranslateService({
      loader: provideTranslateHttpLoader({ prefix: '/i18n/', suffix: '.json' }),
      fallbackLang: 'en',
    }),
    MessageService,
    provideAppInitializer(() => inject(BrandingService).load()),
  ],
};
