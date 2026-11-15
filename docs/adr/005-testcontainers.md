# ADR-005 : Tests d'intégration avec Testcontainers

- **Statut :** ✅ Accepté
- **Date :** 2026-11
- **Concerne :** `ticket-service`, `notification-service`

## Contexte

Les garanties les plus importantes du projet ne peuvent pas être vérifiées par des tests unitaires avec des mocks :
- l'**atomicité** ticket + événement Outbox (rollback réel en base) ;
- l'arrivée réelle de l'événement dans **Kafka** ;
- l'**idempotence** du consumer face à un vrai doublon.

## Décision

Écrire des tests d'intégration avec **Testcontainers**, qui démarre de **vrais** containers jetables :
- **MariaDB 11** et **Kafka** (`apache/kafka-native`), branchés automatiquement sur Spring Boot avec `@ServiceConnection` ;
- containers **partagés** entre les classes de test (pattern singleton) pour la rapidité ;
- asynchrone vérifié avec **Awaitility** (pas de `Thread.sleep`) ;
- les **autres services** (clients OpenFeign) sont mockés avec `@MockitoBean` : chaque service est testé isolément.

## Alternatives considérées

| Alternative | Pourquoi écartée |
|---|---|
| Base **H2** en mémoire | Dialecte SQL et comportement différents de MariaDB : un test vert sur H2 peut échouer en production |
| **Kafka embarqué** (`spring-kafka-test`) | Moins fidèle qu'un vrai broker, et une dépendance de test spécifique de plus |
| Tests uniquement avec mocks | Ne prouvent ni l'atomicité, ni l'envoi Kafka, ni l'idempotence |

## Conséquences

✅ **Positives**
- Les tests utilisent **les mêmes technologies qu'en production**.
- Ils tournent en **CI** (GitHub Actions fournit Docker sur ses runners).
- Ils ont détecté un vrai problème d'image (Kafka 3.9.0, *advertised listener* `0.0.0.0`) corrigé en passant à `kafka-native:3.9.1`.

⚠️ **Négatives**
- **Docker est obligatoire** pour lancer les tests.
- Plus **lents** que des tests unitaires (démarrage des containers, ~10-20 s la première fois).
- Les contrats entre services (OpenFeign) ne sont pas testés : des **tests de contrat** (Spring Cloud Contract, Pact) seraient l'étape suivante.
