import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.getSession().pipe(
    // Si HAY sesión => no dejes entrar a /login ni /register
    map(() => router.createUrlTree(['/home'])),
    // Si NO hay sesión => sí puede ver /login o /register
    catchError(() => of(true))
  );
};
