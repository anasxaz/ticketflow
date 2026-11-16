import { Injectable } from '@angular/core';
import { keycloak } from './keycloak';

@Injectable({ providedIn: 'root' })
export class AuthService {
  get username(): string {
    return (keycloak.tokenParsed?.['preferred_username'] as string) ?? '';
  }

  /** Roles du realm, lus DANS le token (CLIENT, AGENT...). */
  get roles(): string[] {
    return keycloak.tokenParsed?.realm_access?.roles ?? [];
  }

  hasRole(role: string): boolean {
    return this.roles.includes(role);
  }

  /** Renvoie un token valide : le renouvelle s'il expire dans moins de 30 s. */
  async token(): Promise<string> {
    await keycloak.updateToken(30);
    return keycloak.token!;
  }

  logout(): void {
    keycloak.logout({ redirectUri: window.location.origin });
  }
  
  get email(): string {
    return (keycloak.tokenParsed?.['email'] as string) ?? '';
  }
}
