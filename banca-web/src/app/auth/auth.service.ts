// src/app/auth/auth.service.ts
import { Injectable } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import { authConfig } from './auth.config';

@Injectable({ providedIn: 'root' })
export class AuthService {
  constructor(private oauth: OAuthService) {
    this.oauth.configure(authConfig);
  }

  /** Úsalo en el callback: procesa ?code=... y carga discovery si hace falta */
  async finishLoginOnCallback(): Promise<void> {
    if (!this.oauth.discoveryDocumentLoaded) {
      await this.oauth.loadDiscoveryDocument();
    }
    await this.oauth.tryLoginCodeFlow(); // aquí se canjea el code y se guardan tokens
    // (opcional) await this.oauth.loadUserProfile();
  }

  /** Úsalo en guards (en cualquier ruta PRIVADA, excepto /callback) */
  async ensureAuthenticated(): Promise<boolean> {
    // Intenta recuperar sesión si vuelves a recargar la app
    await this.oauth.loadDiscoveryDocumentAndTryLogin();

    if (!this.oauth.hasValidAccessToken()) {
      this.oauth.initCodeFlow(); // redirige al IdP
      return false;
    }
    return true;
  }

  get idToken() { return this.oauth.getIdToken(); }
  get accessToken() { return this.oauth.getAccessToken(); }
  get claims(): any { return (this.oauth.getIdentityClaims() as any) ?? {}; }
  get isLoggedIn(): boolean { return this.oauth.hasValidAccessToken(); }

  logout() { this.oauth.logOut(); }
}
