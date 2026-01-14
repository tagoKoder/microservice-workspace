import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { switchMap } from 'rxjs';
import { CsrfService } from '../security/csrf.service';

function pathOf(url: string): string {
  try { return new URL(url, window.location.origin).pathname; } catch { return url; }
}

const CSRF_REQUIRED_PREFIXES = [
  '/bff/session/logout',
  '/api/v1/beneficiaries',
  '/api/v1/payments',
  '/api/v1/profile',
  '/api/v1/admin/sandbox/topups'
];

function requiresCsrf(url: string, method: string): boolean {
  const m = method.toUpperCase();
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(m)) return false;

  const path = pathOf(url);
  return CSRF_REQUIRED_PREFIXES.some(p => path.startsWith(p));
}

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  if (!requiresCsrf(req.url, req.method) || req.headers.has('X-CSRF-Token')) {
    return next(req);
  }

  const csrf = inject(CsrfService);
  return csrf.getToken().pipe(
    switchMap(token => next(req.clone({ setHeaders: { 'X-CSRF-Token': token } })))
  );
};
