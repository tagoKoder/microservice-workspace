import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionApi } from '../../api/bff';
import { CsrfService } from '../security/csrf.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(
    private router: Router,
    private sessionApi: SessionApi,
    private csrf: CsrfService
  ) {}

  // OIDC start: navegación (no XHR)
  startLogin(redirectPath: string = '/home'): void {
    const url = `/api/v1/auth/oidc/start?redirect=${encodeURIComponent(redirectPath)}`;
    window.location.assign(url);
  }


  async logout(): Promise<void> {
    // POST /bff/session/logout (CSRF se adjunta por interceptor)
    const token = await firstValueFrom(this.csrf.getToken());
    await firstValueFrom(this.sessionApi.logoutWebSession({ xCSRFToken: token }));
    this.csrf.clear();
    await this.router.navigateByUrl('/login');
  }

  // GET /api/v1/session
  getSession() {
    return this.sessionApi.getCurrentSession();
  }
}
