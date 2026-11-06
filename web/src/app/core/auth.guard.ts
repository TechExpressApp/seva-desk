import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Signed in? Also sends people with a temporary password to the change-password page first. */
export const authGuard: CanActivateFn = async (_route, state) => {
  // inject() only works before the first await, so grab both services up front.
  const auth = inject(AuthService);
  const router = inject(Router);
  const ok = await auth.restore();
  if (!ok) return router.createUrlTree(['/login'], { queryParams: { next: state.url } });
  if (auth.user()?.mustChangePassword && !state.url.startsWith('/account/password')) {
    return router.createUrlTree(['/account/password']);
  }
  return true;
};

/** Route data { module: 'events' }: only for people whose roles open that area; others go to their home page. */
export const moduleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  // planned areas (/soon/events …) are checked by the module in their URL
  const module = (route.data['module'] as string | undefined) ?? route.paramMap.get('module') ?? undefined;
  return !module || auth.can(module) ? true : router.parseUrl(auth.homePath());
};

/** The empty path: send each person to the first area they can use. */
export const homeRedirect: CanActivateFn = () => {
  const auth = inject(AuthService);
  return inject(Router).parseUrl(auth.homePath());
};

/** Session ended (401) → sign in again; temporary password still in use (403) → change it. */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const auth = inject(AuthService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.startsWith('/api/auth/')) {
        auth.user.set(null);
        router.navigate(['/login']);
      } else if (err.status === 403 && String(err.error?.detail ?? '').includes('new password')) {
        router.navigate(['/account/password']);
      }
      return throwError(() => err);
    }),
  );
};
