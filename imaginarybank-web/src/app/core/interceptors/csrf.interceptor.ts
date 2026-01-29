import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, switchMap, throwError } from 'rxjs';
import { CsrfService } from '../security/csrf.service';

function parsed(url: string): URL | null {
  try { return new URL(url, window.location.origin); } catch { return null; }
}

function isSameOrigin(url: string): boolean {
  const u = parsed(url);
  if (!u) return true;
  return u.origin === window.location.origin;
}

function pathOf(url: string): string {
  const u = parsed(url);
  return u ? u.pathname : url;
}

function isApiPath(path: string): boolean {
  return path.startsWith('/api/');
}

function isMutating(method: string): boolean {
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method.toUpperCase());
}

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  // Nunca adjuntar CSRF a dominios externos (S3 presigned, etc.)
  if (!isSameOrigin(req.url)) return next(req);

  const path = pathOf(req.url);
  if (!isApiPath(path)) return next(req);
  if (!isMutating(req.method)) return next(req);

  // Si ya viene CSRF, no tocar.
  if (req.headers.has('X-CSRF-Token')) return next(req);

  // 1) Intentar sin CSRF
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // 2) Solo si el backend exige CSRF (403) reintentamos 1 vez con token
      if (err.status !== 403) return throwError(() => err);

      const csrf = inject(CsrfService);
      return csrf.getToken().pipe(
        switchMap(token =>
          next(req.clone({ setHeaders: { 'X-CSRF-Token': token } }))
        ),
        catchError(e2 => throwError(() => e2))
      );
    })
  );
};
