import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom, Observable } from 'rxjs';
import { SessionApi, WhoamiResponseDto } from '../../api/bff';
import { CsrfService } from '../security/csrf.service';
import { environment } from '@environment/environment';
import {  AuthApi } from '../../api/bff';

@Injectable({ providedIn: 'root' })
export class AuthService {
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
    /*
    this.authApi.startWebLogin({ redirect: redirectPath }).subscribe({
      next: () => {
        // No esperamos respuesta (redirección inmediata), pero logueamos por si acaso
        console.log('Login iniciado, redirigiendo a proveedor OIDC...');
      },
      error: (err) => {
        console.error('Error al iniciar login:', err);
      }
    });*/

  }


  async logout(): Promise<void> {
    // POST /bff/session/logout (CSRF se adjunta por interceptor)
    const token = await firstValueFrom(this.csrf.getToken());
    await firstValueFrom(this.sessionApi.logoutWebSession({ xCSRFToken: token }));
    this.csrf.clear();
    await this.router.navigateByUrl('/login');
  }

  // GET /api/v1/session
  getSession(): Observable<WhoamiResponseDto>{
    return this.sessionApi.getCurrentSession();
  }
}
