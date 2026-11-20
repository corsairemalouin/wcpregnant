---
name: k8s-deploy
description: Déploie, vérifie ou debug le déploiement Kubernetes/Docker de WcPregnant (manifestes dans k8s/, Dockerfile multi-stage, docker-compose, ingress, HPA, secrets, probes de santé). Utiliser pour "déployer", "build l'image", "pod ne démarre pas", "configurer ingress", "dimensionner les ressources".
---

# Déploiement WcPregnant

## Séquence de déploiement

```powershell
cd WcPregnantService
mvn -DskipTests package
docker build -t wcpregnant:latest .

cd ../k8s
kubectl apply -f namespace.yaml
kubectl apply -f secret.yaml -f configmap.yaml -n wcpregnant
kubectl apply -f postgres-pvc.yaml -f postgres-deployment.yaml -n wcpregnant
kubectl apply -f deployment.yaml -f service.yaml -n wcpregnant
kubectl apply -f hpa.yaml -f networkpolicy.yaml -f ingress.yaml -n wcpregnant
```

## Invariants à préserver

- Namespace `wcpregnant`, 2–5 réplicas, HPA sur CPU 70 %.
- Probes `liveness`/`readiness` sur `/actuator/health` et `/actuator/health/readiness`.
- `requests`/`limits` CPU et mémoire toujours définis.
- `PodDisruptionBudget` minAvailable = 1.
- PostgreSQL 18 (`postgres:18-alpine`) avec PVC 10Gi ; accès via Service ClusterIP.
- Secrets (DSN, mots de passe) uniquement via Secret, jamais en dur.

## Diagnostic

En cas de pod défaillant, suivre l'ordre : logs → describe → events → resources. Voir [docs/troubleshooting.md](docs/troubleshooting.md).
