import { Routes } from '@angular/router';
import { authGuard, homeRedirect, moduleGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./features/login/login').then((m) => m.Login) },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/password/forgot-password').then((m) => m.ForgotPassword),
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/password/reset-password').then((m) => m.ResetPassword),
  },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/shell/shell').then((m) => m.Shell),
    // Each area is open only to roles that include it (see Role.java); others land on their own home page.
    canActivateChild: [moduleGuard],
    children: [
      { path: '', pathMatch: 'full', canActivate: [homeRedirect], children: [] },
      {
        path: 'dashboard',
        data: { module: 'dashboard' },
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'contacts',
        data: { module: 'contacts' },
        loadComponent: () => import('./features/contacts/contacts').then((m) => m.Contacts),
      },
      {
        path: 'events',
        data: { module: 'events' },
        loadComponent: () => import('./features/events/events').then((m) => m.Events),
      },
      {
        path: 'donors',
        data: { module: 'donors' },
        loadComponent: () => import('./features/donors/donors').then((m) => m.Donors),
      },
      {
        path: 'donations',
        data: { module: 'donations' },
        loadComponent: () => import('./features/donations/donations').then((m) => m.Donations),
      },
      {
        path: 'expenses',
        data: { module: 'finance' },
        loadComponent: () => import('./features/expenses/expenses').then((m) => m.Expenses),
      },
      {
        path: 'payees',
        data: { module: 'finance' },
        loadComponent: () => import('./features/payees/payees').then((m) => m.Payees),
      },
      {
        path: 'users',
        data: { module: 'users' },
        loadComponent: () => import('./features/users/users').then((m) => m.Users),
      },
      {
        path: 'account/password',
        loadComponent: () => import('./features/password/change-password').then((m) => m.ChangePassword),
      },
      {
        path: 'soon/:module',
        loadComponent: () => import('./features/coming-soon/coming-soon').then((m) => m.ComingSoon),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
