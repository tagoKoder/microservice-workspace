import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

function extractCorrelationId(err: HttpErrorResponse): string | null {
  const h = err.headers?.get('X-Correlation-Id');
  if (h) return h;

  const b: any = err.error;
  return b?.correlation_id ?? b?.correlationId ?? null;
}

export const apiErrorInterceptor: HttpInterceptorFn = (_req, next) => {
  const toast = inject(MessageService);
  const router = inject(Router);

  return next(_req).pipe(
    catchError((err: HttpErrorResponse) => {
      // 401 -> login (evita loop si ya estás en /login)
      if (err.status === 401 && !router.url.startsWith('/login')) {
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }

      const code = (err.error && err.error.code) ? err.error.code : `HTTP_${err.status}`;
      const msg  = (err.error && err.error.message) ? err.error.message : 'Error inesperado';

      const corr = extractCorrelationId(err);
      const detail = corr ? `${msg} (Ref: ${corr})` : msg;

      toast.add({ severity: 'error', summary: code, detail });
      return throwError(() => err);
    })
  );
};
