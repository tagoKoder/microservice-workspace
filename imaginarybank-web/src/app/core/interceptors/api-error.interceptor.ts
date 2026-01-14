import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';

export const apiErrorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(MessageService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      // Auto-redirect en 401 (evita loop si ya estás en /login)
      if (err.status === 401 && !router.url.startsWith('/login')) {
        router.navigate(['/login'], { queryParams: { redirect: router.url } });
      }

      const code = (err.error && err.error.code) ? err.error.code : `HTTP_${err.status}`;
      const msg  = (err.error && err.error.message) ? err.error.message : 'Error inesperado';

      toast.add({ severity: 'error', summary: code, detail: msg });
      return throwError(() => err);
    })
  );
};
