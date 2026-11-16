import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';
import { keycloak } from './app/core/auth/keycloak';

// 1. D'abord se connecter (redirection vers Keycloak si besoin)...
keycloak
  .init({ onLoad: 'login-required', pkceMethod: 'S256', checkLoginIframe: false })
  // 2. ...puis seulement demarrer Angular, avec un utilisateur connecte
  .then(() => bootstrapApplication(App, appConfig))
  .catch((err: unknown) => console.error('Echec de connexion a Keycloak', err));

