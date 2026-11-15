# ADR-004 : Sécurité centralisée à la Gateway avec Keycloak

- **Statut :** ✅ Accepté
- **Date :** 2026-11
- **Concerne :** `api-gateway`, Keycloak, tous les services exposés

## Contexte

L'API doit être protégée : seuls les utilisateurs authentifiés peuvent l'appeler, avec des droits différents selon le rôle (`CLIENT` crée des tickets, `AGENT` les traite). Gérer les mots de passe et les sessions dans chaque service serait risqué et dupliqué.

## Décision

- **Keycloak** est le fournisseur d'identité (OAuth2 / OpenID Connect) : il gère les utilisateurs, les rôles et émet des **JWT** signés.
- L'**API Gateway** est configurée en **OAuth2 Resource Server** : elle valide chaque token (signature vérifiée avec les clés publiques **JWKS** de Keycloak, expiration) et rejette les requêtes non authentifiées (`401`).
- L'identité de l'utilisateur est **tirée du token** (email), jamais du corps de la requête : un client ne peut pas créer un ticket au nom d'un autre.
- Les endpoints techniques (`/actuator/**`, `/error`) sont ouverts pour le monitoring.

## Alternatives considérées

| Alternative | Pourquoi écartée |
|---|---|
| Authentification maison (Spring Security + table users + JWT généré soi-même) | Réinventer la gestion des mots de passe, du refresh token et de la rotation des clés : risqué |
| Valider le JWT dans **chaque** service | Plus sûr (*zero trust*), mais configuration dupliquée partout ; écarté pour la simplicité du projet |
| Sessions HTTP | Incompatible avec des services sans état (*stateless*) et répartis sur plusieurs instances |

## Conséquences

✅ **Positives**
- **Un seul point d'entrée** sécurisé : les services métier restent simples.
- Validation **sans appel réseau** à Keycloak à chaque requête (clés JWKS mises en cache).
- Standard (OAuth2 / OIDC) : le futur front Angular utilisera le flux **Authorization Code + PKCE**.

⚠️ **Négatives**
- Les services internes **font confiance au réseau** : un appel qui contourne la Gateway n'est pas vérifié. En production : valider aussi le JWT dans chaque service, et/ou **mTLS** et **NetworkPolicies** Kubernetes.
- Keycloak devient une dépendance critique pour obtenir un token (mais pas pour valider ceux déjà émis).
- Un token reste valide jusqu'à son expiration, même après un changement de rôle (durée de vie courte recommandée).
