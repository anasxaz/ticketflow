# ADR-006 : Conserver Eureka sur Kubernetes

- **Statut :** 🟡 Accepté (transitoire)
- **Date :** 2026-11
- **Concerne :** `discovery-server`, tous les services, manifests `k8s/`

## Contexte

Le projet a d'abord tourné avec **Docker Compose**, avec **Eureka** pour la découverte de services : la Gateway route vers `lb://ticket-service` et répartit la charge entre les instances enregistrées.

Lors du passage à **Kubernetes**, deux mécanismes de découverte existent : Eureka **et** les Services Kubernetes (DNS + répartition de charge intégrés).

## Décision

**Garder Eureka** sur Kubernetes, pour déployer **exactement les mêmes images et la même configuration** qu'avec Docker Compose :
- les Services Kubernetes portent les **mêmes noms** que les services Compose (`config-server`, `kafka`, `ticket-db`…), donc la configuration `*-docker.properties` fonctionne sans modification ;
- `EUREKA_INSTANCE_PREFERIPADDRESS=true` : les pods s'enregistrent avec leur **IP**, car le nom d'un pod n'est pas résolvable par DNS.

## Alternatives considérées

| Alternative | Pourquoi écartée (pour l'instant) |
|---|---|
| Supprimer Eureka et utiliser les **Services Kubernetes** (`http://ticket-service:8082`) | Solution cible, mais nécessite une configuration différente entre Compose et Kubernetes |
| **Spring Cloud Kubernetes** (découverte via l'API Kubernetes) | Ajoute une dépendance et des droits RBAC ; à envisager si l'on abandonne Compose |

## Conséquences

✅ **Positives**
- **Zéro changement** de code ou de configuration entre Compose et Kubernetes.
- Le load balancing entre replicas fonctionne (vérifié : 2 replicas de `ticket-service`, requêtes réparties).

⚠️ **Négatives**
- **Double découverte** de services : redondant, et un composant de plus à faire tourner.
- Délai de détection d'un pod mort (heartbeat Eureka) plus long que celui de Kubernetes (readiness probe).
- **À revoir** : si Kubernetes devient la seule cible de déploiement, remplacer Eureka par les Services Kubernetes (nouvel ADR).
