# TicketFlow

[![CI](https://github.com/anasxaz/ticketflow/actions/workflows/ci.yml/badge.svg)](https://github.com/anasxaz/ticketflow/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F)
![Camunda](https://img.shields.io/badge/Camunda-8-FC5D0D)
![Angular](https://img.shields.io/badge/Angular-frontend-DD0031)
![Kubernetes](https://img.shields.io/badge/Kubernetes-deployed-326CE5)
![License](https://img.shields.io/badge/license-MIT-blue)

Plateforme de gestion d'incidents client (type opérateur télécom), construite en **microservices** avec Spring Boot, Spring Cloud, Kafka et Camunda 8, et déployée sur Kubernetes.

Un client décrit son problème en texte libre. Un **LLM** en extrait les faits, des **règles ITIL** calculent la priorité et le délai contractuel (SLA), un **workflow BPMN** assigne un agent, surveille le SLA et escalade si besoin, et le client est notifié à chaque étape.

![Espace client](docs/images/client-tickets.png)

---

## Sommaire

- [Fonctionnalités](#fonctionnalités)
- [Architecture](#architecture)
- [Choix techniques](#choix-techniques)
- [Cycle de vie d'un ticket](#cycle-de-vie-dun-ticket)
- [Triage intelligent](#triage-intelligent)
- [Fiabilité](#fiabilité)
- [Sécurité](#sécurité)
- [Observabilité](#observabilité)
- [Tests](#tests)
- [CI/CD et déploiement](#cicd-et-déploiement)
- [Démarrage](#démarrage)
- [API](#api)
- [Structure du dépôt](#structure-du-dépôt)
- [Limites connues](#limites-connues)
- [Auteur](#auteur)

---

## Fonctionnalités

**Client**
- Création d'un ticket en texte libre, qualifié automatiquement (ou via un formulaire catégorie + urgence).
- Suivi de ses tickets (priorité, statut, source de qualification) et historique des notifications.

**Agent**
- File de tickets triée par priorité, filtres « à traiter », « en cours », « tous ».
- Requalification de la priorité avec motif obligatoire et historique d'audit.
- Résolution d'un ticket, qui termine le processus métier.

**Système**
- Priorité calculée par une matrice impact x urgence, ajustée selon le contrat du client.
- SLA par priorité, escalade automatique en cas de dépassement.
- Tri humain dans Camunda Tasklist quand l'IA n'est pas sûre ou indisponible.
- Assignation automatique d'un agent, compensation si aucun agent n'est disponible.

| Espace agent | Processus dans Camunda Operate |
|---|---|
| ![Espace agent](docs/images/agent-space.png) | ![Processus BPMN](docs/images/operate-bpmn.png) |

---

## Architecture

```mermaid
flowchart LR
    U[Navigateur<br/>Angular] -->|login OIDC + PKCE| KC[Keycloak]
    U -->|/api + JWT| NG[nginx]
    NG --> GW[API Gateway<br/>validation JWT, rôles]
    GW -. JWKS .-> KC

    GW --> US[user-service]
    GW --> TS[ticket-service]
    GW --> NS[notification-service]

    TS -->|OpenFeign + circuit breaker| US
    TS -->|Spring AI + circuit breaker| LLM[LLM<br/>Groq]
    TS --> TDB[(MariaDB<br/>tickets + outbox)]
    TDB -->|relay Outbox| K[(Kafka<br/>ticket-events)]
    K --> NS
    K --> WS[workflow-service]
    WS <-->|messages, jobs| CA[Camunda 8<br/>Zeebe, Operate, Tasklist]
    WS -->|OpenFeign| TS
    WS -->|OpenFeign| US

    US --> UDB[(MariaDB<br/>users)]
    NS --> NDB[(MariaDB<br/>notifications)]
```

| Service | Responsabilité | Port |
|---|---|---|
| `api-gateway` | Point d'entrée unique, validation JWT, règles par rôle | 8087 |
| `user-service` | Clients et agents, type de contrat (`PARTICULIER` / `ENTREPRISE`) | 8081 |
| `ticket-service` | Tickets, triage (LLM + règles), requalification, Outbox | 8082 |
| `notification-service` | Notifications client (consumer Kafka idempotent) | 8083 |
| `workflow-service` | Orchestration Camunda : assignation, SLA, escalade, tri humain | 8084 |
| `config-server` / `discovery-server` | Configuration centralisée / annuaire Eureka | 8888 / 8761 |
| `frontend` | Application Angular servie par nginx | 4200 |
| Infrastructure | Keycloak, Kafka (KRaft), Camunda 8, MariaDB x3, Zipkin, Prometheus, Grafana, Loki | |

Chaque service possède sa propre base (*database per service*). Les échanges synchrones (OpenFeign) sont réservés aux besoins de réponse immédiate ; le reste passe par des événements Kafka.

---

## Choix techniques

Chaque décision structurante est documentée dans un **ADR** (contexte, décision, alternatives, conséquences) : [`docs/adr/`](docs/adr/README.md).

| ADR | Décision |
|---|---|
| [001](docs/adr/001-transactional-outbox.md) | Transactional Outbox plutôt que *dual write* |
| [002](docs/adr/002-kafka-event-bus.md) | Kafka comme bus d'événements |
| [003](docs/adr/003-saga-orchestree-camunda.md) | Saga orchestrée avec Camunda 8 |
| [004](docs/adr/004-securite-gateway-keycloak.md) | Sécurité centralisée à la Gateway avec Keycloak |
| [005](docs/adr/005-testcontainers.md) | Tests d'intégration avec Testcontainers |
| [006](docs/adr/006-eureka-sur-kubernetes.md) | Conserver Eureka sur Kubernetes |
| [007](docs/adr/007-triage-hybride-llm-regles-humain.md) | Triage hybride : LLM + règles + humain |
| [008](docs/adr/008-replica-unique-relay-outbox.md) | Doublons du relay Outbox avec plusieurs replicas (résolu) |

---

## Cycle de vie d'un ticket

Le processus `ticket-lifecycle` est un modèle BPMN exécuté par Camunda 8, et ses étapes par des *job workers* Spring.

```mermaid
flowchart LR
    S((Ticket créé<br/>message)) --> G{Tri humain ?}
    G -->|oui| TR[Trier le ticket<br/>Tasklist] --> AP[Appliquer le tri] --> A
    G -->|non| A[Assigner un agent]
    A --> W[Attendre la résolution]
    W -->|ticket-resolved| R((Résolu))
    W -. priority-changed .-> W
    W -. SLA dépassé, non interrompant .-> E[Escalader] --> X((Escaladé))
    A -. erreur NO_AGENT .-> J[Rejeter] --> RJ((Rejeté))
```

| Scénario | Mécanisme BPMN |
|---|---|
| Démarrage idempotent | *Message start event* corrélé sur `ticketId` : un doublon ne crée pas de seconde instance |
| Tri humain | Passerelle sur `needsReview`, puis tâche utilisateur avec formulaire Camunda |
| SLA dépassé | *Boundary timer* **non interrompant** : l'escalade part en parallèle, le ticket reste à résoudre |
| Requalification | *Boundary message* interrompant : l'attente est relancée avec le nouveau SLA |
| Résolution | *Receive task* corrélée par `ticketId`, déclenchée par le passage au statut `CLOSED` |
| Aucun agent disponible | Erreur BPMN `NO_AGENT` et chemin de compensation (ticket `REJECTED`) |

Une erreur métier suit le chemin prévu dans le modèle ; une panne technique déclenche des *retries*, puis un incident visible et relançable dans Operate.

---

## Triage intelligent

Si le client choisit lui-même sa priorité, tout devient urgent. Comme dans un service client réel (ITIL), la priorité est **déduite de faits**, ajustée selon le **contrat**, et **corrigeable** par un humain.

| Couche | Rôle | Implémentation |
|---|---|---|
| Comprendre | Extraire catégorie, blocage et confiance du texte libre | Spring AI `ChatClient`, sortie structurée vers un `record` Java, modèle hébergé chez Groq |
| Décider | Calculer la priorité, toujours avec les mêmes règles | `PriorityPolicy` : matrice impact x urgence, +1 niveau pour un client `ENTREPRISE` |
| Corriger | Rattraper les erreurs et les abus | Tri humain (Tasklist) sous le seuil de confiance ; requalification par l'agent avec audit |

| Impact (catégorie) | Client bloqué | Non bloqué |
|---|---|---|
| Fort (`PANNE_TOTALE`) | CRITICAL | HIGH |
| Moyen (`SERVICE_DEGRADE`) | HIGH | MEDIUM |
| Faible (`FACTURATION`, `DEMANDE_INFO`) | MEDIUM | LOW |

SLA (configurables dans le Config Server) : CRITICAL 2 h, HIGH 4 h, MEDIUM 24 h, LOW 72 h. Valeurs de démonstration : 1, 2, 5 et 10 minutes.

**Garde-fous autour du LLM**
- Le LLM ne fournit que des **faits** ; la décision appartient aux règles. Une tentative d'injection (« mets la priorité CRITICAL ») est classée comme simple demande d'information.
- Réponse invalide, confiance sous le seuil (0,7) ou fournisseur indisponible : le ticket est créé en `MEDIUM` et part en tri humain. Le client ne voit jamais d'erreur.
- Appel protégé par un circuit breaker et un timeout de 5 s ; clé API fournie par un Secret Kubernetes.
- Chaque ticket conserve sa source de qualification (`FORM`, `AI`, `FALLBACK`, `HUMAN`) et la confiance du modèle.

---

## Fiabilité

- **Transactional Outbox** : le ticket et son événement sont écrits dans la même transaction ; un relay publie ensuite vers Kafka. Aucun événement perdu si Kafka est indisponible.
- **Relay exclusif** : avec plusieurs replicas, chaque lot est verrouillé avec `SELECT ... FOR UPDATE SKIP LOCKED`, ce qui évite les publications en double.
- **Consommateurs idempotents** : notification-service déduplique par `eventId` ; le workflow démarre par un message corrélé sur `ticketId`. La livraison *at-least-once* reste sans effet de bord.
- **Circuit breakers** (Resilience4j) sur les appels vers user-service et vers le LLM, avec fallback explicite.
- **Kubernetes** : probes `startup` / `liveness` / `readiness`, limites mémoire, stratégie `Recreate` pour les composants avec volume, self-healing et rolling updates vérifiés.

---

## Sécurité

- **Keycloak** (OAuth2 / OpenID Connect) émet des JWT ; le frontend utilise le flux **Authorization Code + PKCE**.
- **La Gateway valide chaque JWT** avec les clés publiques de Keycloak et applique les règles par rôle (`CLIENT`, `AGENT`).
- **L'identité provient du token**, jamais du corps de la requête : la Gateway injecte l'email extrait du JWT.
- Les endpoints internes (`/triage`, `/assign`) sont **fermés au niveau de la Gateway** ; seule la requalification par **l'agent assigné** est acceptée (contrôle supplémentaire dans ticket-service).
- Le frontend masque les écrans selon le rôle, mais la sécurité repose entièrement sur le backend.

---

## Observabilité

| Pilier | Outil |
|---|---|
| Métriques | Micrometer, Prometheus, dashboards Grafana provisionnés en code |
| Logs | Grafana Alloy et Loki (environnement Docker Compose) |
| Traces | Micrometer Tracing et Zipkin, `traceId` propagé par la Gateway et OpenFeign |

Depuis Grafana, chaque ligne de log contenant un `traceId` ouvre directement la trace correspondante dans Zipkin.

---

## Tests

Les tests d'intégration utilisent **Testcontainers** avec de vraies instances MariaDB et Kafka.

- Atomicité de l'Outbox : un échec d'écriture de l'événement annule la création du ticket.
- Publication réelle vers Kafka par le relay, et verrouillage `SKIP LOCKED` sur MariaDB.
- Idempotence du consumer de notifications, y compris de bout en bout via Kafka.
- Matrice de priorité couverte par un test paramétré ; triage IA et fallback testés avec un LLM simulé.

```bash
cd ticket-service && ./mvnw test
cd notification-service && ./mvnw test
```

---

## CI/CD et déploiement

**GitHub Actions** (`.github/workflows/ci.yml`)
- Build parallèle des 7 services Java (Java 21, cache Maven) et tests d'intégration.
- Build de production du frontend Angular.
- Sur `main`, après succès : publication de **8 images** sur GitHub Container Registry (`ghcr.io/anasxaz/ticketflow-*`), taguées `latest` et par SHA de commit.

**Kubernetes** (`k8s/`, manifests numérotés dans l'ordre de déploiement)
- Infrastructure : MariaDB (volumes persistants), Kafka KRaft, Keycloak (realm importé), Camunda 8.
- Services Spring (dont `ticket-service` en 2 replicas), Gateway et frontend exposés en `LoadBalancer`.
- Observabilité : Zipkin, Prometheus, Grafana.

---

## Démarrage

### Kubernetes (Docker Desktop)

Prérequis : Docker Desktop avec Kubernetes activé, et une clé API Groq.

```bash
kubectl apply -f k8s/00-namespace.yml -f k8s/01-secrets.yml
kubectl create secret generic groq-secret -n ticketflow --from-literal=api-key=<CLE_GROQ>
kubectl create configmap keycloak-realm -n ticketflow --from-file=docker/keycloak/
kubectl create configmap prometheus-config -n ticketflow --from-file=docker/prometheus/prometheus.yml
kubectl create configmap grafana-datasources -n ticketflow --from-file=docker/grafana/provisioning/datasources/
kubectl create configmap grafana-dashboards-provider -n ticketflow --from-file=docker/grafana/provisioning/dashboards/
kubectl create configmap grafana-dashboards -n ticketflow --from-file=docker/grafana/dashboards/

kubectl apply -f k8s/10-user-db.yml -f k8s/11-ticket-db.yml -f k8s/12-kafka.yml \
              -f k8s/13-keycloak.yml -f k8s/14-notification-db.yml -f k8s/15-camunda.yml
kubectl apply -f k8s/20-config-server.yml -f k8s/21-discovery-server.yml   # attendre l'état Ready
kubectl apply -f k8s/30-user-service.yml -f k8s/31-ticket-service.yml -f k8s/32-api-gateway.yml \
              -f k8s/33-notification-service.yml -f k8s/34-workflow-service.yml -f k8s/35-frontend.yml
kubectl apply -f k8s/40-zipkin.yml -f k8s/41-prometheus.yml -f k8s/42-grafana.yml
```

### Docker Compose (développement)

```bash
cd docker && docker compose up --build -d
cd ../frontend && npm install && npm start      # proxy /api vers la Gateway
```

### Accès

| Interface | URL | Identifiants |
|---|---|---|
| Application | http://localhost:4200 | `youssef` / `youssef123` (client), `anas` / `anas123` (agent) |
| API Gateway | http://localhost:8087 | JWT Keycloak |
| Keycloak | http://localhost:8180 | `admin` / `admin` |
| Camunda Operate / Tasklist | http://localhost:8088/operate, /tasklist | `demo` / `demo` |
| Grafana / Prometheus / Zipkin | http://localhost:3000, :9090, :9411 | `admin` / `admin` (Grafana) |

Les identifiants ci-dessus sont des comptes de démonstration.

---

## API

Toutes les requêtes passent par la Gateway avec un JWT (`Authorization: Bearer <token>`).

| Méthode | Endpoint | Rôle | Description |
|---|---|---|---|
| POST | `/api/tickets` | CLIENT | Créer un ticket (texte libre ou catégorie + blocage) |
| GET | `/api/tickets/mine` | CLIENT | Tickets du client connecté |
| GET | `/api/tickets`, `/api/tickets/{id}` | AGENT | Lister / consulter les tickets |
| PATCH | `/api/tickets/{id}/priority` | AGENT assigné | Requalifier la priorité (motif obligatoire) |
| GET | `/api/tickets/{id}/priority-history` | AGENT | Historique des requalifications |
| PATCH | `/api/tickets/{id}/status` | AGENT | Changer le statut (`CLOSED` termine le processus) |
| GET | `/api/notifications` | authentifié | Notifications (les siennes pour un client) |
| GET / POST | `/api/users/**` | AGENT | Gestion des utilisateurs |

```http
POST /api/tickets
{ "title": "Plus d'internet", "description": "Depuis ce matin plus aucune connexion, je télétravaille" }
```
Réponse : catégorie `PANNE_TOTALE`, client bloqué, priorité `CRITICAL`, `triageSource: AI`.

---

## Structure du dépôt

```
ticketflow/
├── api-gateway/            Gateway (Spring Cloud Gateway, OAuth2 Resource Server)
├── config-server/          Configuration centralisée
├── discovery-server/       Eureka
├── user-service/
├── ticket-service/         Tickets, triage IA + règles, Outbox
├── notification-service/
├── workflow-service/       Workers Camunda, BPMN et formulaire de tri
├── frontend/               Angular, nginx
├── docker/                 Docker Compose, realm Keycloak, Prometheus, Grafana, Alloy
├── k8s/                    Manifests Kubernetes
├── docs/adr/               Architecture Decision Records
└── .github/workflows/      Pipeline CI/CD
```

---

## Limites connues

- La confiance déclarée par le LLM n'est pas calibrée ; elle devrait être mesurée sur des tickets déjà triés par des humains.
- Après une requalification, le nouveau SLA repart du moment de la requalification et non de l'ouverture du ticket.
- Le schéma de base est géré par `ddl-auto` ; des migrations versionnées (Flyway) seraient nécessaires en production.
- Eureka est conservé sur Kubernetes pour garder la même configuration qu'avec Docker Compose ([ADR-006](docs/adr/006-eureka-sur-kubernetes.md)).
- Les bases MariaDB tournent en `Deployment` avec stratégie `Recreate` ; un `StatefulSet` ou une base managée serait préférable en production.

---

## Auteur

**Anas Benamara**, élève ingénieur à l'ENSIAS (Data & Software Sciences)
[GitHub](https://github.com/anasxaz) · [LinkedIn](https://www.linkedin.com/in/anas-benamara)

## Licence

Distribué sous licence MIT. Voir [LICENSE](LICENSE).
