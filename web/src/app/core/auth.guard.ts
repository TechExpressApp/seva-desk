import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = async (_route, state) => {
  // inject() only works before the first await, so grab both services up front.
  const auth = inject(AuthService);
  const router = inject(Router);
  const ok = await auth.restore();
  return ok ? true : router.createUrlTree(['/login'], { queryParams: { next: state.url } });
};

/** Sends the user back to sign-in when the session cookie has expired. */
export const sessionExpiredInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401 && !req.url.startsWith('/api/auth/')) {
        router.navigate(['/login']);
      }
      return throwError(() => err);
    }),
  );
};
