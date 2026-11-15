# ADR-003 : Saga orchestrée avec Camunda 8

- **Statut :** ✅ Accepté
- **Date :** 2026-11
- **Concerne :** `workflow-service`, Camunda 8 (Zeebe), processus BPMN `ticket-lifecycle`

## Contexte

Le cycle de vie d'un ticket est un **processus métier long** qui traverse plusieurs services :
1. assigner un agent disponible (`user-service`) ;
2. attendre la résolution, qui peut prendre des heures ;
3. **escalader** si le SLA est dépassé ;
4. **rejeter** le ticket (compensation) si aucun agent n'est disponible.

Ce processus a besoin d'**état** (où en est chaque ticket ?), de **timers** (SLA) et de **gestion d'erreur**.

## Décision

Orchestrer le processus avec **Camunda 8** et un modèle **BPMN** (`ticket-lifecycle.bpmn`) :
- démarrage d'une instance sur `TICKET_CREATED` (consumer Kafka dans `workflow-service`) ;
- **service tasks** exécutées par des **job workers** Spring (`assign-agent`, `escalate-ticket`, `reject-ticket`) ;
- **receive task** corrélée par le message `ticket-resolved` (clé de corrélation = `ticketId`) quand le ticket passe en `CLOSED` ;
- **boundary timer** `=slaDuration` → escalade ;
- **boundary error** `NO_AGENT` → chemin de compensation **Rejeté** (Saga).

## Alternatives considérées

| Alternative | Pourquoi écartée |
|---|---|
| **Saga chorégraphiée** (chaque service réagit aux événements des autres) | Pas de vue globale : le flux est dispersé dans le code de plusieurs services, difficile à suivre et à déboguer. Timers et compensations à coder soi-même |
| Spring State Machine / `@Scheduled` maison | Il faudrait réimplémenter la persistance de l'état, les timers, les retries et la supervision |

## Conséquences

✅ **Positives**
- Le processus est **visible** (diagramme BPMN) et **compréhensible par le métier**.
- **Supervision** de chaque instance dans **Operate** (où en est le ticket, incidents, variables), avec **retry** depuis l'interface.
- Timers, corrélation de messages, retries et erreurs BPMN sont fournis par le moteur.
- Modifier le processus = modifier le BPMN, sans toucher à la logique des autres services.

⚠️ **Négatives**
- Composant d'infrastructure **lourd** (~1-2 Go de RAM).
- **Licence** : Camunda 8 Self-Managed est gratuit en développement, mais la production nécessite une licence.
- `workflow-service` dépend de `ticket-service` et `user-service` (appels OpenFeign) pour exécuter les tâches.
- L'orchestrateur est un point central : s'il est indisponible, les processus sont mis en pause (sans perte : l'état est persisté).
