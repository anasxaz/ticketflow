# ADR-001 : Transactional Outbox plutôt que *dual write*

- **Statut :** ✅ Accepté
- **Date :** 2026-11
- **Concerne :** `ticket-service`

## Contexte

Quand un ticket est créé ou change de statut, `ticket-service` doit faire **deux choses** :
1. enregistrer le ticket dans **MariaDB** ;
2. publier un événement (`TICKET_CREATED`, `STATUS_CHANGED`) dans **Kafka**, consommé par `notification-service` et `workflow-service`.

Faire les deux directement (*dual write*) n'est pas atomique : la base et Kafka ne partagent pas de transaction.
- Commit en base puis Kafka indisponible → **ticket sans événement** (aucun workflow ne démarre).
- Envoi Kafka puis rollback en base → **événement fantôme** pour un ticket qui n'existe pas.

## Décision

Utiliser le pattern **Transactional Outbox** :
- Dans la **même transaction** `@Transactional`, on écrit le ticket **et** une ligne dans la table `outbox_events` (id, aggregateId, type, payload JSON, createdAt, sentAt).
- Un **relay** (`@Scheduled`) lit les événements non envoyés (`sentAt IS NULL`, par lots de 100, du plus ancien au plus récent), les publie dans Kafka avec la clé `ticketId`, puis renseigne `sentAt`.

## Alternatives considérées

| Alternative | Pourquoi écartée |
|---|---|
| *Dual write* simple | Pas atomique (voir contexte) |
| CDC avec **Debezium** (lecture du binlog) | Plus robuste et sans polling, mais ajoute Kafka Connect + Debezium : trop lourd pour ce projet |
| Transactions Kafka | Ne couvrent pas la base de données : ne résout pas le problème |

## Conséquences

✅ **Positives**
- Aucun ticket sans événement, aucun événement sans ticket (prouvé par un test Testcontainers qui force un rollback).
- Si Kafka tombe, les événements **attendent** dans la table et partent au retour de Kafka.
- La clé `ticketId` garantit l'**ordre** des événements d'un même ticket (même partition).

⚠️ **Négatives**
- Livraison **at-least-once** : un événement peut être envoyé deux fois (crash entre l'envoi et la mise à jour de `sentAt`). Les consumers doivent donc être **idempotents** (`notification-service` : contrainte d'unicité sur `eventId`).
- **Latence** d'environ 1 s (intervalle du polling).
- **Polling permanent** de la base : charge légère, et du bruit dans le tracing (filtré par un `ObservationPredicate`).
- La table `outbox_events` grossit : il faudrait un job de nettoyage des lignes envoyées.
- Avec plusieurs replicas, chaque instance exécute le relay : les doublons sont absorbés par l'idempotence, mais un verrou (ShedLock ou `SELECT … FOR UPDATE SKIP LOCKED`) serait préférable en production.
