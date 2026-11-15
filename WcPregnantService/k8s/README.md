# Manifestes Kubernetes — WcPregnant

Ce dossier contient les manifestes de déploiement (namespace, secret, configmap, deployments, service, ingress, HPA, NetworkPolicy, PDB) et les scripts `deploy.ps1` / `deploy.sh`.

📂 **La documentation associée a été déplacée** :

- Guide de déploiement Kubernetes : [`documentation/05-deploiement/kubernetes.md`](../../documentation/05-deploiement/kubernetes.md)
- Build, Docker, CI/CD : [`documentation/05-deploiement/build-et-deploiement.md`](../../documentation/05-deploiement/build-et-deploiement.md)

Mise en œuvre :

```powershell
kubectl apply -f namespace.yaml
.\deploy.ps1
```