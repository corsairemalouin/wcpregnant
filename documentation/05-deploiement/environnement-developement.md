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

### 1.7 Serveur d'inférence local (Strata AI) — optionnel

La couche agentique (`/agent/tools`, `/agent/tools/call`, `/agent/chat`) fonctionne sans base externe, mais `/agent/chat` nécessite un serveur d'inférence **accessible sur le réseau privé**, exposant une API compatible OpenAI (`POST {base-url}/chat/completions`).

| Variable d'environnement | Clé `application.properties` | Défaut |
|---|---|---|
| `AGENT_INFERENCE_BASE_URL` | `agent.inference.base-url` | `http://localhost:8000/v1` |
| `AGENT_MODEL` | `agent.inference.model` | `Qwen-3.8-Flash-Next` |
| `AGENT_TIMEOUT_SECONDS` | `agent.inference.timeout-seconds` | `30` |
| `AGENT_MAX_ITERATIONS` | `agent.max-iterations` | `4` |

```powershell
$env:AGENT_INFERENCE_BASE_URL = "http://localhost:8000/v1"
$env:AGENT_MODEL              = "Qwen-3.8-Flash-Next"
mvn spring-boot:run
```

> **Aucune clé API n'est requise** : le serveur est local. Si le modèle n'est pas joignable, `POST /agent/chat` renvoie **503** et le CRUD reste pleinement utilisable. Les tests (`mvn test`) ne contactent jamais le serveur : l'inférence est mockée (Mockito) ou pointée vers un port fermé.

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

### 2.5 Metrics Server (nécessaire à l'HPA et à `kubectl top`)

```powershell
kubectl apply -f WcPregnantService/k8s/metrics-server.yaml
```

Le manifeste local est **nécessaire** : le `components.yaml` upstream échoue sur le
cluster Docker Desktop, parce que

1. Kubernetes 1.34+ exige un objet **`APIService`** explicite pour enregistrer le
   groupe `metrics.k8s.io` ;
2. le RBAC doit couvrir le sous-ressource **`nodes/metrics`** (sinon `403` du kubelet) ;
3. les sondes doivent être **TCP** (une sonde HTTP sur `/` renvoie `403` et crash le pod) ;
4. le Service doit avoir un **sélecteur** pour que l'agrégateur ait des endpoints ;
5. `insecureSkipTLSVerify: true` est requis en local (certificat auto-signé `CN=localhost`).

Vérification :

```powershell
kubectl top nodes
kubectl top pods -n wcpregnant
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

### Plugins Android (optionnel — développement mobile)

Le support Android **n'est pas bundled** avec IntelliJ IDEA : il faut installer les deux
plugins du JetBrains Marketplace, comme l'indique la documentation officielle
([Create your first Android application](https://jetbrains.com/help/idea/create-your-first-android-application.html)).

| Plugin | Page Marketplace | Plugin ID | Version compatible IDEA 2026.2 (build `262.10968.63`) |
|---|---|---|---|
| **Android** | [plugin/22989-android](https://plugins.jetbrains.com/plugin/22989-android) | `org.jetbrains.android` | `262.10968.63` |
| **Android Design Tools** | [plugin/22990-android-design-tools](https://plugins.jetbrains.com/plugin/22990-android-design-tools) | `com.android.tools.design` | `262.10968.63` |

> ⚠️ La compatibilité est indexée sur le **build de l'IDE** : les versions `263.x` (Android
> Studio Quail 2) visent IDEA 2026.3 et **ne s'installent pas** sur IDEA 2026.2. Choisir la
> version dont la plage `since … until` couvre le build affiché dans
> *Aide → À propos* (ici `IU-262.10968.63`).

**Méthode recommandée (interface)** : *Réglages → Plugins → Marketplace*, rechercher
« Android » puis « Android Design Tools », **Install**, puis **Restart IDE**.

**Méthode par archive** (utilisée lorsque l'interface n'est pas accessible) : télécharger les
`.zip` du Marketplace et les déposer dans le dossier des plugins de l'instance, puis relancer IDEA :

```powershell
# Dossier des plugins de l'instance en cours
$plugins = "$env:LOCALAPPDATA\JetBrains\IntelliJIdea2026.2\plugins"

curl.exe -L "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=1171966" `
  -o "$plugins\android-262.10968.63.zip"          # Android (org.jetbrains.android)

curl.exe -L "https://plugins.jetbrains.com/plugin/download?rel=true&updateId=1171993" `
  -o "$plugins\design-tools-262.10968.63.zip"     # Android Design Tools (com.android.tools.design)
```

Les archives sont chargées au **démarrage** suivant de l'IDE ; elles apparaissent ensuite dans
*Réglages → Plugins → Installées*. Le plugin *Android Design Tools* est notamment requis pour
les aperçus **Jetpack Compose** et les outils de layout.

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
- [ ] Plugins **Android** et **Android Design Tools** installés dans l'IDE si un module Android est prévu

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
| `wsl --terminate docker-desktop` renvoie `Wsl/0x80070422` (« Le service ne peut pas être démarré… ») | Le service WSL est désactivé sur Windows : relancer `wsl --install` / activer la fonctionnalité *Windows Subsystem for Linux*, ou basculer Docker Desktop sur l'arrière-plan **Hyper-V** (*Settings → Resources → Virtualization*). Sans WSL ni Hyper-V, Docker Desktop ne peut pas démarrer son moteur |
| Plugin Marketplace non chargé après installation | Redémarrer l'IDE : les archives déposées dans `%LOCALAPPDATA%\JetBrains\IntelliJIdea<version>\plugins` ne sont lues qu'au démarrage |
| Le pod Kubernetes ne démarre pas | Consulter les logs : `kubectl logs <nom-du-pod> -n wcpregnant` ; consulter les événements : `kubectl describe pod <nom-du-pod> -n wcpregnant` |

---

## 7. Prochaines étapes

1. Lire [build-et-deploiement.md](build-et-deploiement.md) pour le build, Docker et la CI/CD ;
2. Lire [kubernetes.md](kubernetes.md) pour les détails Kubernetes ;
3. Exécuter les tests : `mvn test` ;
4. Générer la couverture : `mvn jacoco:report` (rapport dans `target/site/jacoco/`) ;
5. Déployer en local : `docker-compose up --build`.

