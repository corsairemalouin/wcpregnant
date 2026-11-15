> 📂 **Documentation** — [index](../README.md) · Phase **5 — Déploiement** · Suite : [build-et-deploiement.md](build-et-deploiement.md), [kubernetes.md](kubernetes.md)

# Guide d'installation — WcPregnant Service

Prérequis et instructions d'installation pour les développeurs et les équipes DevOps.

---

## 1. Environnement de développement local

### 1.1 Java

Le projet utilise **Java 25** (voir `pom.xml`).

**Téléchargement et installation :**

- [Eclipse Temurin JDK 25](https://adoptium.net/) ;
- ou `choco install temurin25` (Windows via Chocolatey) ;
- ou `brew install openjdk25` (macOS via Homebrew).

**Vérification :**

```powershell
java -version
```

### 1.2 Maven

**Option A — installation globale**

- [Télécharger Maven](https://maven.apache.org/download.cgi) (3.9+) ;
- ou `choco install maven` (Windows) ;
- ou `brew install maven` (macOS).

**Vérification :**

```powershell
mvn -version
```

**Option B — Maven intégré à IntelliJ IDEA**

Chemin sur la machine :

```powershell
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" -version
```

Ajouter au `PATH` ou créer un alias :

```powershell
# PowerShell — ajouter au profil
$env:Path += ";C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin"
```

**Option C — Maven Wrapper** (envisagé, non encore ajouté)

Permettrait `./mvnw clean package` sans Maven installé globalement.

### 1.3 Git

- [Télécharger Git](https://git-scm.com/) ;
- ou `choco install git` (Windows) ;
- ou `brew install git` (macOS).

**Vérification :**

```powershell
git --version
```

### 1.4 Docker (tests locaux)

- [Télécharger Docker Desktop](https://www.docker.com/products/docker-desktop) ;
- ou `choco install docker-desktop` (Windows).

**Vérification :**

```powershell
docker --version
docker run hello-world
```

### 1.5 IDE

- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) (déjà installé) ;
- Visual Studio Code (optionnel).

### 1.6 Base de données (PostgreSQL)

Pour le développement local (non requis si l'on utilise docker-compose) :

- [Télécharger PostgreSQL](https://www.postgresql.org/download/) ;
- ou `choco install postgresql` (Windows) ;
- ou via Docker :

```powershell
docker run -e POSTGRES_PASSWORD=changeme -e POSTGRES_DB=wcpregnant -p 5432:5432 postgres:15-alpine
```

> En local sans PostgreSQL, `application.properties` utilise H2 en mémoire (`spring.h2.console.enabled=true`).

---

## 2. Outillage CI/CD et Kubernetes

### 2.1 Outils Kubernetes

**kubectl :**

- [Télécharger kubectl](https://kubernetes.io/docs/tasks/tools/) ;
- ou `choco install kubernetes-cli` (Windows) ;
- ou `brew install kubectl` (macOS).

**Vérification :**

```powershell
kubectl version --client
```

**Helm (gestion de paquets) :**

- [Télécharger Helm](https://helm.sh/docs/intro/install/) ;
- ou `choco install kubernetes-helm` (Windows).

### 2.2 Cluster Kubernetes

**Options locales :**

- **Docker Desktop** (Kubernetes intégré) :

  ```powershell
  # Activer dans les paramètres de Docker Desktop
  # Vérification :
  kubectl config current-context
  ```

- **Minikube** :

  ```powershell
  choco install minikube
  minikube start --driver=docker
  ```

- **KinD** (Kubernetes in Docker) :

  ```powershell
  choco install kind
  kind create cluster
  ```

- **Cloud** (GKE, AKS, EKS) : se référer à la documentation du fournisseur.

### 2.3 cert-manager (TLS de l'Ingress)

```powershell
# Ajouter le dépôt Helm
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Installer cert-manager
helm install cert-manager jetstack/cert-manager `
  --version v1.13.0 `
  --namespace cert-manager `
  --create-namespace `
  --set installCRDs=true
```

### 2.4 nginx-ingress-controller

```powershell
# Ajouter le dépôt Helm
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# Installer nginx
helm install nginx-ingress ingress-nginx/ingress-nginx `
  --namespace ingress-nginx `
  --create-namespace
```

### 2.5 Metrics Server (nécessaire à l'HPA)

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

---

## 3. Configuration de l'IDE (IntelliJ IDEA)

### Configuration d'exécution Spring Boot

1. **Exécuter → Modifier les configurations** ;
2. Ajouter une configuration **Spring Boot** :
   - Classe principale : `org.manu.wcpregnant.WcpregnantApplication` ;
   - Module : `wcpregnant` ;
   - Options JVM : `-Dspring.profiles.active=dev` ;
3. Cliquer sur **Exécuter** ou presser `Shift+F10`.

### Intégration Maven

1. Affichage → Fenêtres d'outils → Maven ;
2. Recharger les projets (vérifier que `pom.xml` est reconnu) ;
3. Exécuter les objectifs : Lifecycle → `clean` → `package`.

---

## 4. Checklist de mise en place

- [ ] Java 25 installé et `JAVA_HOME` configuré
- [ ] Maven installé globalement ou alias vers le `mvn` d'IntelliJ
- [ ] Dépôt cloné via Git
- [ ] Projet ouvert dans l'IDE (IntelliJ)
- [ ] Maven actualisé : `mvn -U clean install`
- [ ] Application lancée en local : `mvn spring-boot:run`
- [ ] Tests passants : `mvn test`
- [ ] Docker Desktop démarré (pour les images locales)
- [ ] `kubectl` configuré vers un cluster (pour les commandes K8s)

---

## 5. Commandes de démarrage rapide

```powershell
# Construire et empaqueter
mvn clean package

# Exécuter les tests et générer la couverture
mvn test
mvn jacoco:report

# Générer la documentation
mvn javadoc:javadoc
mvn site

# Construire l'image Docker
docker build -t wcpregnant:latest .

# Lancer avec docker-compose
docker-compose up --build

# Déployer sur Kubernetes
cd k8s
.\deploy.ps1
```

---

## 6. Diagnostic

| Symptôme | Solution |
|---|---|
| `mvn` introuvable | Utiliser Maven globalement ou le chemin complet du `mvn.cmd` d'IntelliJ, ou ajouter Maven au `PATH` |
| Version de Java différente | Vérifier que `java -version` renvoie Java 25 ; définir la variable `JAVA_HOME` |
| Échec de construction de l'image Docker | S'assurer que `mvn package` a bien été exécuté pour produire `target/*.jar` |
| Contexte `kubectl` non défini | Exécuter `kubectl config current-context`, puis `kubectl config use-context <nom-du-cluster>` |
| Le pod Kubernetes ne démarre pas | Consulter les logs : `kubectl logs <nom-du-pod> -n wcpregnant` ; consulter les événements : `kubectl describe pod <nom-du-pod> -n wcpregnant` |

---

## 7. Prochaines étapes

1. Lire [build-et-deploiement.md](build-et-deploiement.md) pour le build, Docker et la CI/CD ;
2. Lire [kubernetes.md](kubernetes.md) pour les détails Kubernetes ;
3. Exécuter les tests : `mvn test` ;
4. Générer la couverture : `mvn jacoco:report` (rapport dans `target/site/jacoco/`) ;
5. Déployer en local : `docker-compose up --build`.

