import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs/operators';
import { AuthService } from './auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.getSession().pipe(
    map((sess) => {
      // Si HAY sesión => fuera de /login y /register
      if (sess) return router.createUrlTree(['/home']);
      // Si NO hay sesión => sí entra
      return true;
    })
  );
};
