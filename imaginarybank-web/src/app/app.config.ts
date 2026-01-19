import { ApplicationConfig, importProvidersFrom, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideAnimations } from '@angular/platform-browser/animations';
import { MessageService } from 'primeng/api';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

// Interceptors
import { credentialsInterceptor } from './core/interceptors/credentials.interceptor';
import { csrfInterceptor } from './core/interceptors/csrf.interceptor';
import { apiErrorInterceptor } from './core/interceptors/api-error.interceptor';
import { ApiModule, Configuration } from './api/bff';
import { environment } from '../environments/environment';
import Lara from '@primeuix/themes/lara'
import { providePrimeNG } from 'primeng/config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),

    MessageService,
    
    providePrimeNG({
      theme: {
        preset: Lara // 👈 aquí defines el tema
      },
      ripple: true,           // opcional
      inputVariant: 'filled', // opcional
    }),

    provideHttpClient(
      withInterceptors([credentialsInterceptor, csrfInterceptor, apiErrorInterceptor])
    ),

    importProvidersFrom(
      ApiModule.forRoot(() => new Configuration({ basePath: environment.bffBasePath }))
    ),
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
  ]
};
