import { AuthConfig } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  // Usa tu issuer SIN barra final para evitar validaciones raras
  issuer: 'http://localhost:9000/application/o/admin-web/',
  strictDiscoveryDocumentValidation: false, // authentik expone endpoints globales
  // Si quieres ser aún más permisivo: skipIssuerCheck: true,

  redirectUri: window.location.origin + '/callback',
  postLogoutRedirectUri: window.location.origin + '/',

  clientId: 'lafZo09xa8DZbhQqZ6SfJ6xqDioCZWtZMnkaeiHX',
  responseType: 'code',     // Authorization Code
  disablePKCE: false,            // PKCE

  scope: 'openid email profile offline_access',

  showDebugInformation: true, // útil en dev
  // Opcional: dónde guardar tokens (por defecto sessionStorage)
  //storage: localStorage, // si prefieres persistir
  clearHashAfterLogin: true,
};
