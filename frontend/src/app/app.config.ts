import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    // garde les providers qu'Angular a deja mis ici (ex. provideBrowserGlobalErrorListeners)
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
  ],
};
