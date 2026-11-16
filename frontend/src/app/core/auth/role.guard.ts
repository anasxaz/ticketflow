import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/** Bloque une route si l'utilisateur n'a pas le role demande. */
export const roleGuard =
  (role: string): CanActivateFn =>
  () => {
    const auth = inject(AuthService);
    return auth.hasRole(role) ? true : inject(Router).createUrlTree(['/']);
  };
