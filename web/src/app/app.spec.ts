import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { App } from './app';
import { BrandingService } from './core/branding.service';

describe('App', () => {
  it('shows a friendly message when the address is not linked to a temple', async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        provideTranslateService(),
        providePrimeNG(),
        MessageService,
        { provide: BrandingService, useValue: { error: () => 'This address is not linked to a temple yet.' } },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('not linked to a temple');
  });
});
