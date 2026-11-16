# 📐 Architecture Decision Records (ADR)

Chaque ADR documente **une** décision d'architecture importante de TicketFlow : le contexte, la décision prise, les alternatives écartées et les conséquences (positives **et** négatives).

Format : [Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions). Un ADR n'est jamais modifié après acceptation : si une décision change, on écrit un nouvel ADR qui **remplace** l'ancien.

| # | Décision | Statut |
|---|---|---|
| [001](001-transactional-outbox.md) | Transactional Outbox plutôt que *dual write* | ✅ Accepté |
| [002](002-kafka-event-bus.md) | Apache Kafka comme bus d'événements | ✅ Accepté |
| [003](003-saga-orchestree-camunda.md) | Saga **orchestrée** avec Camunda 8 | ✅ Accepté |
| [004](004-securite-gateway-keycloak.md) | Sécurité centralisée à la Gateway avec Keycloak | ✅ Accepté |
| [005](005-testcontainers.md) | Tests d'intégration avec Testcontainers | ✅ Accepté |
| [006](006-eureka-sur-kubernetes.md) | Conserver Eureka sur Kubernetes | 🟡 Accepté (transitoire) |
| [007](007-triage-hybride-llm-regles-humain.md) | Triage hybride des tickets : LLM + règles ITIL + humain | ✅ Accepté |
| [008](008-replica-unique-relay-outbox.md) | Doublons du relay Outbox avec plusieurs replicas | Résolu |
