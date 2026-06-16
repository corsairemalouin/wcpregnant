# Setup Guide - WC Pregnant Service

Prerequisites and installation instructions for developers and DevOps.

## Local Development Setup

### 1. Java

This project uses Java 25 (see `pom.xml`).

**Download & Install:**
- [Eclipse Temurin JDK 25](https://adoptium.net/)
- Or: `choco install temurin25` (Windows via Chocolatey)
- Or: `brew install openjdk25` (macOS via Homebrew)

**Verify:**
```powershell
java -version
```

### 2. Maven

**Option A: Global Installation**

- [Download Maven](https://maven.apache.org/download.cgi) (3.9+)
- Or: `choco install maven` (Windows)
- Or: `brew install maven` (macOS)

**Verify:**
```powershell
mvn -version
```

**Option B: Use IntelliJ Bundled Maven**

Path on your system:
```powershell
"C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin\mvn.cmd" -version
```

Add to PATH or alias:
```powershell
# PowerShell - add to profile
$env:Path += ";C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.3\plugins\maven\lib\maven3\bin"
```

**Option C: Maven Wrapper** (planned, not yet added)

Will allow `./mvnw clean package` without global Maven.

### 3. Git

- [Download Git](https://git-scm.com/)
- Or: `choco install git` (Windows)
- Or: `brew install git` (macOS)

**Verify:**
```powershell
git --version
```

### 4. Docker (for local testing)

- [Download Docker Desktop](https://www.docker.com/products/docker-desktop)
- Or: `choco install docker-desktop` (Windows)

**Verify:**
```powershell
docker --version
docker run hello-world
```

### 5. IDE

- [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/) (you already have this)
- Visual Studio Code (optional)

### 6. Database (PostgreSQL)

For local development (not required if using docker-compose):

- [Download PostgreSQL](https://www.postgresql.org/download/)
- Or: `choco install postgresql` (Windows)
- Or use Docker: `docker run -e POSTGRES_PASSWORD=changeme -e POSTGRES_DB=wcpregnant -p 5432:5432 postgres:15-alpine`

## CI/CD & Kubernetes Setup

### 1. Kubernetes Tools

**kubectl:**
- [Download kubectl](https://kubernetes.io/docs/tasks/tools/)
- Or: `choco install kubernetes-cli` (Windows)
- Or: `brew install kubectl` (macOS)

**Verify:**
```powershell
kubectl version --client
```

**Helm** (for package management):
- [Download Helm](https://helm.sh/docs/intro/install/)
- Or: `choco install kubernetes-helm` (Windows)

### 2. Kubernetes Cluster

**Local options:**

- **Docker Desktop** (built-in Kubernetes):
  ```powershell
  # Enable in Docker Desktop settings
  # Verify:
  kubectl config current-context
  ```

- **Minikube**:
  ```powershell
  choco install minikube
  minikube start --driver=docker
  ```

- **KinD** (Kubernetes in Docker):
  ```powershell
  choco install kind
  kind create cluster
  ```

- **Cloud** (GKE, AKS, EKS):
  See cloud provider documentation.

### 3. cert-manager (for TLS in Ingress)

```powershell
# Add Helm repo
helm repo add jetstack https://charts.jetstack.io
helm repo update

# Install cert-manager
helm install cert-manager jetstack/cert-manager `
  --version v1.13.0 `
  --namespace cert-manager `
  --create-namespace `
  --set installCRDs=true
```

### 4. nginx-ingress-controller

```powershell
# Add Helm repo
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update

# Install nginx
helm install nginx-ingress ingress-nginx/ingress-nginx `
  --namespace ingress-nginx `
  --create-namespace
```

### 5. Metrics Server (for HPA)

```powershell
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

## IDE Configuration (IntelliJ IDEA)

### Spring Boot Run Configuration

1. **Run → Edit Configurations**
2. Add **Spring Boot** configuration:
   - Main class: `org.manu.wcpregnant.WcpregnantApplication`
   - Module: `wcpregnant`
   - VM options: `-Dspring.profiles.active=dev`
3. Click **Run** or press Shift+F10

### Maven Integration

1. View → Tool Windows → Maven
2. Reload projects (ensure pom.xml is recognized)
3. Run goals: Lifecycle → clean → package

## Project Setup Checklist

- [ ] Java 25 installed & JAVA_HOME configured
- [ ] Maven installed globally or alias to IntelliJ's mvn
- [ ] Git cloned the repository
- [ ] IDE (IntelliJ) opened the project
- [ ] Maven updated: `mvn -U clean install`
- [ ] Application runs locally: `mvn spring-boot:run`
- [ ] Tests pass: `mvn test`
- [ ] Docker Desktop running (for local Docker images)
- [ ] kubectl configured to a cluster (for K8s commands)

## Quick Start Commands

```powershell
# Build & package
mvn clean package

# Run tests & generate coverage
mvn test
mvn jacoco:report

# Generate documentation
mvn javadoc:javadoc
mvn site

# Build Docker image
docker build -t wcpregnant:latest .

# Run with docker-compose
docker-compose up --build

# Deploy to Kubernetes
cd k8s
.\deploy.ps1
```

## Troubleshooting

### Maven not found
- Ensure `mvn` is global or use full path to IntelliJ's mvn.cmd
- Or add Maven to PATH

### Java version mismatch
- Check `java -version` returns Java 25
- Set JAVA_HOME environment variable

### Docker image build fails
- Ensure `mvn package` has been run to produce `target/*.jar`

### kubectl context not set
- Run `kubectl config current-context`
- If none, run `kubectl config use-context <cluster-name>`

### Kubernetes pod fails to start
- Check logs: `kubectl logs <pod-name> -n wcpregnant`
- Check events: `kubectl describe pod <pod-name> -n wcpregnant`

## Next Steps

1. Read `README-DEPLOY.md` for build and deployment instructions
2. Read `k8s/README.md` for Kubernetes specifics
3. Run tests: `mvn test`
4. Generate coverage: `mvn jacoco:report` (view in `target/site/jacoco/`)
5. Deploy locally: `docker-compose up --build`

