---
paths:
  - "WcPregnantService/Dockerfile"
  - "WcPregnantService/docker-compose.yml"
  - "WcPregnantService/.github/**"
  - "k8s/**"
  - "WcPregnantService/k8s/**"
---

# DevOps & déploiement

- Dockerfile **multi-stage** : build Maven (`mvn -DskipTests package`) puis runtime `eclipse-temurin:*-jre-alpine`, utilisateur non root, `HEALTHCHECK` sur `/actuator/health` (chemin réel de l'Actuator — **pas** `/api/actuator/health`).
- Toute modification du Dockerfile doit rester compatible avec `docker-compose.yml` (ports, variables `SPRING_DATASOURCE_*`, `JAVA_OPTS`).
- Manifestes K8s : conserver les `resources` (requests/limits), les probes `liveness`/`readiness`, le `PodDisruptionBudget` et la `NetworkPolicy`. Ne jamais retirer un probe.
- Configuration applicative injectée via ConfigMap/Secret, jamais via des valeurs en dur dans le Deployment.
- Avant de modifier un manifeste, vérifier la cohérence namespace `wcpregnant` + service + ingress.
- CI (`.github/workflows/docker-image.yml`) : JDK 25, build puis push GHCR. Ne pas ajouter d'étape qui casse le build local.
- Commandes de vérification à proposer : `docker build -t wcpregnant:local .`, `kubectl apply -f k8s/`, `kubectl logs <pod> -n wcpregnant`.
