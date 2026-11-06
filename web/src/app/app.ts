import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toast } from 'primeng/toast';
import { BrandingService } from './core/branding.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast],
  template: `
    @if (branding.error(); as message) {
      <main class="unknown">
        <h1>SevaDesk</h1>
        <p>{{ message }}</p>
        <p class="sd-muted">Check the address, or ask your temple administrator for the correct link.</p>
      </main>
    } @else {
      <router-outlet />
    }
    <p-toast position="bottom-right" />
  `,
  styles: `
    .unknown { max-width: 480px; margin: 18vh auto 0; padding: 0 16px; text-align: center; }
    .unknown h1 { color: var(--sd-primary); font-size: 2rem; margin-bottom: 8px; }
  `,
})
export class App {
  protected readonly branding = inject(BrandingService);
}
