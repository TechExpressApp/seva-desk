import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { AuthService } from '../../core/auth.service';
import { BrandingService } from '../../core/branding.service';

interface NavItem {
  label: string;
  icon: string;
  link: string;
  /** Temple has this module switched on (tenant settings) */
  module?: string;
  /** This person's roles open this area (see Role.java) */
  access: string;
}

const NAV: NavItem[] = [
  { label: 'nav.dashboard', icon: 'pi pi-home', link: '/dashboard', access: 'dashboard' },
  { label: 'nav.contacts', icon: 'pi pi-users', link: '/contacts', module: 'contacts', access: 'contacts' },
  { label: 'nav.donations', icon: 'pi pi-heart', link: '/donations', module: 'donors', access: 'donations' },
  { label: 'nav.donors', icon: 'pi pi-star', link: '/donors', module: 'donors', access: 'donors' },
  { label: 'nav.events', icon: 'pi pi-calendar', link: '/events', module: 'events', access: 'events' },
  { label: 'nav.seva', icon: 'pi pi-sun', link: '/soon/seva', module: 'seva', access: 'seva' },
  { label: 'nav.guesthouse', icon: 'pi pi-building', link: '/soon/guesthouse', module: 'guesthouse', access: 'guesthouse' },
  { label: 'nav.members', icon: 'pi pi-id-card', link: '/soon/members', module: 'members', access: 'members' },
  { label: 'nav.volunteers', icon: 'pi pi-sparkles', link: '/soon/volunteers', module: 'volunteers', access: 'volunteers' },
  { label: 'nav.expenses', icon: 'pi pi-wallet', link: '/expenses', module: 'finance', access: 'finance' },
  { label: 'nav.payees', icon: 'pi pi-briefcase', link: '/payees', module: 'finance', access: 'finance' },
  { label: 'nav.users', icon: 'pi pi-shield', link: '/users', access: 'users' },
];

@Component({
  selector: 'sd-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, TranslatePipe],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  protected readonly branding = inject(BrandingService);
  protected readonly auth = inject(AuthService);

  protected readonly nav = computed(() =>
    NAV.filter((item) => (!item.module || this.branding.isEnabled(item.module)) && this.auth.can(item.access)),
  );

  /** Vocabulary for "{{seva}} & {{priest}} services" */
  protected readonly terms = computed(() => {
    const t = this.branding.branding()?.terms ?? {};
    return { seva: t['term.seva'] ?? 'Seva', priest: t['term.priest'] ?? 'Priest' };
  });

  protected readonly today = new Intl.DateTimeFormat(undefined, {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(new Date());
}
