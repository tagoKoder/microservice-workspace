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
import { BASE_PATH } from './api/bff';
export const appConfig: ApplicationConfig = {
  providers: [
    { provide: BASE_PATH, useValue: environment.bffBasePath || '' },
    provideRouter(routes),
    provideAnimations(),

    MessageService,
    
    providePrimeNG({
      theme: {
        preset: Lara
      },
      ripple: true,
      inputVariant: 'filled',
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
