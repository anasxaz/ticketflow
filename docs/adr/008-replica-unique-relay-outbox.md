# ADR-008 : Un seul replica de ticket-service tant que le relay Outbox n'est pas exclusif

- **Statut :** 🟡 Accepté (transitoire)
- **Date :** 2026-11
- **Concerne :** `ticket-service` (relay Outbox), `workflow-service`, `k8s/31-ticket-service.yml`

## Contexte

Avec **2 replicas** de `ticket-service` sur Kubernetes, chaque pod exécute le relay Outbox (`@Scheduled`). Les deux pods peuvent lire **le même événement non envoyé** avant que l'un d'eux ne le marque comme envoyé : l'événement est alors **publié deux fois** dans Kafka.

Observé en pratique : un même `TICKET_CREATED` a démarré **deux instances** du processus Camunda pour un seul ticket.
- `notification-service` n'est pas affecté : il est **idempotent** (`eventId` unique).
- `workflow-service` **ne l'est pas** : il crée une instance par message reçu.

C'était un risque déjà identifié dans l'[ADR-001](001-transactional-outbox.md).

## Décision

En attendant la correction définitive, **ramener `ticket-service` à 1 replica** (`replicas: 1` dans le manifest, qui reste la source de vérité).

## Correction cible (défense en profondeur)

| # | Où | Correction |
|---|---|---|
| A | ticket-service (source) | Le relay verrouille son lot : `SELECT … FOR UPDATE SKIP LOCKED` dans une transaction, ou un **seul relay actif** (ShedLock / élection de leader), ou le **CDC** (Debezium) |
| B | workflow-service (arrivée) | Rendre le démarrage **idempotent** : démarrer le processus par un **message** Camunda corrélé sur le `ticketId`, ou mémoriser les `eventId` déjà traités |

Les deux sont nécessaires : même avec A, Kafka reste *at-least-once* (un crash entre l'envoi et la mise à jour de `sent_at` renvoie l'événement), donc **tout consommateur doit être idempotent**.

## Conséquences

✅ Plus de doublons de processus, sans changer le code.

⚠️ Pas de haute disponibilité ni de répartition de charge pour `ticket-service` tant que A et B ne sont pas faits ; le scaling horizontal démontré à l'étape 10 est donc **suspendu** pour ce service.
