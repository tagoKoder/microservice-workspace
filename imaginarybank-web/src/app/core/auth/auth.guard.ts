import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.getSession().pipe(
    map((sess) => {
      if (sess) return true;
      return router.createUrlTree(['/login'], { queryParams: { redirect: state.url } });
    })
  );
};
