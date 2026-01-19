import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { switchMap } from 'rxjs';
import { CsrfService } from '../security/csrf.service';

function parsed(url: string): URL | null {
  try { return new URL(url, window.location.origin); } catch { return null; }
}

function isSameOrigin(url: string): boolean {
  const u = parsed(url);
  if (!u) return true; // url relativa
  return u.origin === window.location.origin;
}

function pathOf(url: string): string {
  const u = parsed(url);
  return u ? u.pathname : url;
}

function isApiPath(path: string): boolean {
  // Evita “listas por endpoint”; solo por prefijo de dominio interno
  return path.startsWith('/api/');
}

function isMutating(method: string): boolean {
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(method.toUpperCase());
}

export const csrfInterceptor: HttpInterceptorFn = (req, next) => {
  // No duplicar si ya viene el header
  if (req.headers.has('X-CSRF-Token')) return next(req);

  // Nunca adjuntar CSRF a dominios externos (ej: S3 presigned)
  if (!isSameOrigin(req.url)) return next(req);

  const path = pathOf(req.url);
  if (!isApiPath(path)) return next(req);

  if (!isMutating(req.method)) return next(req);

  const csrf = inject(CsrfService);
  return csrf.getToken().pipe(
    switchMap(token => next(req.clone({ setHeaders: { 'X-CSRF-Token': token } })))
  );
};
