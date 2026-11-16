import Keycloak from 'keycloak-js';

/** Une seule instance pour toute l'appli. */
export const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'ticketflow',
  clientId: 'ticketflow-app',
});
