import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable, of } from 'rxjs';
import { catchError, shareReplay } from 'rxjs/operators';

import { SessionApi, WhoamiResponseDto, AuthApi } from '../../api/bff';
import { CsrfService } from '../security/csrf.service';
import { environment } from '@environment/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private sessionOnce$?: Observable<WhoamiResponseDto | null>;

  constructor(
    private router: Router,
    private sessionApi: SessionApi,
    private csrf: CsrfService,
    private authApi: AuthApi
  ) {}

  // OIDC start: navegación (no XHR)
  startLogin(redirectPath: string = '/home'): void {
    const base = environment.bffBasePath || '';
    const url = `${base}/api/v1/auth/oidc/start?redirect=${encodeURIComponent(redirectPath)}`;
    window.location.assign(url);
  }

  async logout(): Promise<void> {
    const token = await firstValueFrom(this.csrf.getToken());
    await firstValueFrom(this.sessionApi.logoutWebSession({ xCSRFToken: token }));
    this.csrf.clear();

    // Importantísimo: invalida cache para que no “crea” que sigue logueado
    this.invalidateSessionCache();

    await this.router.navigateByUrl('/login');
  }

  /**
   * Session "a prueba de lluvia":
   * - 1 sola llamada real a /session
   * - si falla (401/otros), NO lanza error, devuelve null
   * - se cachea el resultado (incluye null)
   */
  getSession(): Observable<WhoamiResponseDto | null> {
    if (!this.sessionOnce$) {
      this.sessionOnce$ = this.sessionApi.getCurrentSession().pipe(
        catchError(() => of(null)),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.sessionOnce$;
  }

  /** Úsalo cuando sabes que la sesión cambió (login callback, logout) */
  invalidateSessionCache(): void {
    this.sessionOnce$ = undefined;
  }
}
