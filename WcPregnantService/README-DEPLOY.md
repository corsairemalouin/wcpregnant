Guide de build, doc et déploiement
=================================

Pré-requis locaux
- Java (version correspondant à <java.version> dans `pom.xml` — ici 25)
- Maven (option A: utilisez votre installation Maven; option B: utilisez le Maven intégré à IntelliJ)
- Docker (pour construire des images)
- kubectl / accès à un cluster Kubernetes (pour déploiement)

1) Construire le projet et exécuter les tests

Si vous avez `mvn` disponible (recommandé) :

```powershell
cd C:\Users\emman\IdeaProjects\WcPregnant\WcPregnantService
mvn clean package
```

Si vous utilisez le Maven intégré d'IntelliJ (chemin que vous avez partagé) :

```powershell
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" clean package
```

2) Générer la documentation (javadoc / site)

```powershell
mvn javadoc:javadoc
mvn site
```

Les pages seront générées dans `target/site`.

3) Construire l'image Docker

Après avoir exécuté `mvn package` (qui produit `target/*.jar`) :

```powershell
docker build -t ghcr.io/<OWNER>/wcpregnant:latest .
```

Remplacez `<OWNER>` par votre utilisateur ou organisation si vous publiez sur GitHub Container Registry.

4) Lancer en local avec docker-compose

```powershell
docker-compose up --build
```

L'application sera accessible sur http://localhost:8080

5) Déployer sur Kubernetes

Editez `k8s/deployment.yaml` pour remplacer `ghcr.io/<OWNER>/wcpregnant:latest` par votre image publishée.

```powershell
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

6) (Optionnel) Déployer sur Kubernetes — instructions avancées

Voir `k8s/README.md` pour la documentation complète Kubernetes. En résumé :

- Manuel (applique chaque manifest) :
  ```powershell
  kubectl apply -f k8s/namespace.yaml
  kubectl apply -f k8s/secret.yaml -n wcpregnant
  kubectl apply -f k8s/configmap.yaml -n wcpregnant
  kubectl apply -f k8s/postgres-pvc.yaml -n wcpregnant
  kubectl apply -f k8s/postgres-deployment.yaml -n wcpregnant
  kubectl apply -f k8s/deployment.yaml -n wcpregnant
  kubectl apply -f k8s/service.yaml -n wcpregnant
  ```

- Automatisé (Windows PowerShell) :
  ```powershell
  cd k8s
  .\deploy.ps1
  ```

- Automatisé (Linux/Mac bash) :
  ```bash
  cd k8s
  chmod +x deploy.sh
  ./deploy.sh
  ```

Manifests inclus :
- Namespace pour isolation
- Secret + ConfigMap pour configuration
- Deployment avec Readiness/Liveness probes
- Service (ClusterIP)
- PostgreSQL Deployment + PVC (persistent storage)
- HorizontalPodAutoscaler (2-5 replicas, CPU 70%)
- NetworkPolicy (sécurité pod-to-pod)
- PodDisruptionBudget (haute disponibilité)
- Ingress (nginx + cert-manager for TLS)

7) CI/CD (GitHub Actions) — voir `.github/workflows/docker-image.yml` (exemple)

Notes et recommandations
- Si votre environnement n'a pas `mvn` global, je peux ajouter le Maven Wrapper (`mvnw`) au projet. Dites-le moi et je l'ajoute.
- Vérifiez les variables d'environnement pour la base de données en production et utilisez des `Secrets` côté CI/K8s.
- Pour Kubernetes, pré-installez cert-manager et nginx-ingress-controller :
  ```powershell
  helm repo add jetstack https://charts.jetstack.io
  helm repo update
  helm install cert-manager jetstack/cert-manager --version v1.13.0
  helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
  helm repo update
  helm install nginx-ingress ingress-nginx/ingress-nginx
  ```

