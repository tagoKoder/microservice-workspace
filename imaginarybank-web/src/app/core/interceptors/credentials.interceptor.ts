import { HttpInterceptorFn } from '@angular/common/http';

function pathOf(url: string): string {
  try { return new URL(url, window.location.origin).pathname; } catch { return url; }
}

export const credentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const path = pathOf(req.url);
  const isBff = path.startsWith('/api/') || path.startsWith('/bff/');
  return next(isBff ? req.clone({ withCredentials: true }) : req);
};
