// src/app/auth/token.interceptor.ts
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { v4 as uuidv4 } from 'uuid';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

export const tokenInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.accessToken;

  // Path seguro aunque URL sea absoluta
  let path = req.url;
  if (/^https?:\/\//i.test(req.url)) {
    try { path = new URL(req.url).pathname; } catch {}
  }

  const apiRoot = environment?.apiBase ?? '';
  const isApi =
    path.startsWith('/api') ||
    (!!apiRoot && req.url.startsWith(apiRoot + '/api'));

  // Correlation id por request
  const correlationId = uuidv4();
  let headers: Record<string, string> = { 'x-correlation-id': correlationId };

  if (isApi && token && !req.headers.has('Authorization')) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  req = req.clone({ setHeaders: headers });
  return next(req);
};
