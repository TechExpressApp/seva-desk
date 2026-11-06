import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login').then((m) => m.Login) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard) },
      { path: 'contacts', loadComponent: () => import('./features/contacts/contacts').then((m) => m.Contacts) },
      { path: 'events', loadComponent: () => import('./features/events/events').then((m) => m.Events) },
      { path: 'donations', loadComponent: () => import('./features/donations/donations').then((m) => m.Donations) },
      {
        path: 'soon/:module',
        loadComponent: () => import('./features/coming-soon/coming-soon').then((m) => m.ComingSoon),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
