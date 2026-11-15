> 📂 **Documentation** — [index](../README.md) · Phase **5 — Déploiement** · Pré-requis : [environnement-developement.md](environnement-developement.md) · Manifestes détaillés : [kubernetes.md](kubernetes.md)

# Guide de build, documentation et déploiement

> ⚠️ Les commandes de ce guide sont exécutées depuis **`WcPregnantService/`** (les chemins `k8s/…` y sont relatifs).

## Pré-requis locaux

- Java (version correspondant à `<java.version>` dans `pom.xml` — ici 25)
- Maven (option A : votre installation Maven ; option B : le Maven intégré à IntelliJ)
- Docker (pour construire les images)
- `kubectl` / accès à un cluster Kubernetes (pour le déploiement)

## 1. Construire le projet et exécuter les tests

Si vous avez `mvn` disponible (recommandé) :

```powershell
cd C:\Users\emman\IdeaProjects\WcPregnant\WcPregnantService
mvn clean package
```

Si vous utilisez le Maven intégré d'IntelliJ :

```powershell
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" clean package
```

## 2. Générer la documentation (Javadoc / site)

```powershell
mvn javadoc:javadoc
mvn site
```

Les pages seront générées dans `target/site`.

## 3. Construire l'image Docker

Après avoir exécuté `mvn package` (qui produit `target/*.jar`) :

```powershell
docker build -t ghcr.io/<OWNER>/wcpregnant:latest .
```

Remplacez `<OWNER>` par votre utilisateur ou organisation si vous publiez sur GitHub Container Registry.

## 4. Lancer en local avec docker-compose

```powershell
docker-compose up --build
```

L'application est accessible sur <http://localhost:8080>

> 💡 Les scripts racine [`start.ps1`](../../start.ps1) / [`stop.ps1`](../../stop.ps1)
> (et `start.sh` / `stop.sh`) orchestrent la même chose — base, API et frontend — avec un
> arrêt complet : voir [scripts-de-demarrage.md](scripts-de-demarrage.md).

## 5. Déployer sur Kubernetes

Éditez `k8s/deployment.yaml` pour remplacer `ghcr.io/<OWNER>/wcpregnant:latest` par votre image publiée.

```powershell
kubectl apply -f k8s/postgres-deployment.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
```

## 6. (Optionnel) Déployer sur Kubernetes — instructions avancées

Voir [kubernetes.md](kubernetes.md) pour la documentation complète de Kubernetes. En résumé :

- Manuel (appliquer chaque manifeste) :
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

Manifestes Kubernetes inclus :

- Espace de noms pour l'isolation ;
- `Secret` + `ConfigMap` pour la configuration ;
- Déploiement avec sondes de disponibilité (*readiness*) et de survie (*liveness*) ;
- Service (`ClusterIP`) ;
- Déploiement PostgreSQL + PVC (stockage persistant) ;
- `HorizontalPodAutoscaler` (2 à 5 réplicas, seuil CPU 70 %) ;
- `NetworkPolicy` (sécurité entre pods) ;
- `PodDisruptionBudget` (haute disponibilité) ;
- Ingress (nginx + cert-manager pour le TLS).

## 7. CI/CD (GitHub Actions)

Le workflow existe déjà dans `WcPregnantService/.github/workflows/docker-image.yml`. Il se déclenche sur les pushes vers `main` ou `master` :

```yaml
name: Build and publish Docker image

on:
  push:
    branches: [ main, master ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '25'

      - name: Build with Maven
        run: mvn -B -DskipTests package

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v2
        with:
          registry: ghcr.io
          username: ${{ github.repository_owner }}
          password: ${{ secrets.GHCR_TOKEN }}

      - name: Build and push Docker image
        uses: docker/build-push-action@v4
        with:
          context: .
          push: true
          tags: ghcr.io/${{ github.repository_owner }}/wcpregnant:latest
```

> Le dépôt principal est hébergé sur GitLab (`https://gitlab.com/manix35400/wcpregnant`) : ce workflow GitHub Actions ne s'exécute que si le projet est mirroré sur GitHub. Toute étape ajoutée ne doit pas casser le build local (voir [`.clinerules/07-devops-deploiement.md`](../../.clinerules/07-devops-deploiement.md)).

## Notes et recommandations

- Si votre environnement n'a pas `mvn` global, le Maven Wrapper (`mvnw`) peut être ajouté au projet sur demande.
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

