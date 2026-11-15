# ADR-002 : Apache Kafka comme bus d'événements

- **Statut :** ✅ Accepté
- **Date :** 2026-11
- **Concerne :** `ticket-service` (producteur), `notification-service` et `workflow-service` (consommateurs)

## Contexte

Les changements d'un ticket intéressent **plusieurs services** qui ne doivent pas être couplés à `ticket-service` :
- `notification-service` enregistre une notification pour le client ;
- `workflow-service` démarre ou fait avancer le processus Camunda.

Un appel REST synchrone vers chacun rendrait `ticket-service` dépendant de leur disponibilité, et il faudrait le modifier à chaque nouveau consommateur.

## Décision

Publier les événements dans un **topic Kafka** `ticket-events` (mode **KRaft**, sans ZooKeeper), avec la clé `ticketId`. Chaque service consommateur a son **propre consumer group**.

## Alternatives considérées

| Alternative | Pourquoi écartée |
|---|---|
| Appels REST synchrones | Couplage fort, panne en cascade, `ticket-service` doit connaître tous les consommateurs |
| **RabbitMQ** | Excellent pour des files de tâches, mais un message consommé disparaît : pas de **relecture** (replay) ni de journal d'événements. Ajouter un consommateur ne permet pas de relire l'historique |

## Conséquences

✅ **Positives**
- **Découplage** : `ticket-service` ne connaît pas ses consommateurs ; ajouter un service = ajouter un consumer group.
- Chaque consumer group lit **tout** le topic **indépendamment** (notification et workflow reçoivent les mêmes événements).
- **Ordre garanti par partition** : la clé `ticketId` envoie tous les événements d'un ticket dans la même partition.
- Les événements sont **conservés** : on peut rejouer l'historique (nouveau service, bug corrigé).

⚠️ **Négatives**
- Infrastructure plus lourde que RabbitMQ (un broker à opérer).
- Livraison **at-least-once** → consumers **idempotents** obligatoires.
- **Cohérence à terme** (*eventual consistency*) : juste après la création, le ticket n'a pas encore d'agent (`agentId: null`).
- Un seul broker et un facteur de réplication à 1 : acceptable en démo, pas en production (3 brokers minimum).
