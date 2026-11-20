# WcPregnant

Application de localisation de toilettes publiques accessibles aux personnes enceintes : CRUD d'équipements, recherche de proximité (Haversine), pagination, sécurité RBAC, déploiement conteneurisé.

> 📂 **Toute la documentation du projet est centralisée dans [`documentation/`](documentation/README.md)**, organisée selon les phases du cycle de vie du logiciel.

> 🤖 **Objectif agentique — Tool Calling (implémenté)** : une couche d'orchestration (`WcPregnantService/src/main/java/org/manu/wcpregnant/agent/`) interroge un modèle **local** servi par **Strata AI** (`Qwen-3.8-Flash-Next`, API compatible OpenAI) et exécute les appels d'outils interceptés via les services existants — recherche de proximité Haversine, gestion des équipements, vérification RBAC, **100 % local**. Détails : [documentation/01-exigences/objectif-agentique.md](documentation/01-exigences/objectif-agentique.md).

---

## Structure du monorepo

| Dossier | Rôle |
|---|---|
| `WcPregnantService/` | API REST — Spring Boot 4.1.0, Java 25, Maven, Lombok, MapStruct, Spring Data JPA, Spring Security, Actuator, JaCoCo |
| `WcPregnantApp/` | Front utilisateur — Angular 20, TypeScript 5.8, Leaflet, Karma/Jasmine |
| `Bruno/` | Collection de tests d'API (fichiers `.bru`) |
| `WcPregnantService/k8s/` | Manifestes Kubernetes (Deployment, Service, Ingress, HPA, NetworkPolicy, PDB) |
| `documentation/` | **Documentation unifiée** (exigences → conception → implémentation → tests → déploiement → outils) |
| `.clinerules/`, `.cline/skills/` | Règles et skills Cline appliqués au développement |
| `Waste/`, `developpement/` | Code mort / archives — **ne pas modifier** |

---

## Démarrage rapide

### Démarrage et arrêt complets (recommandé)

```powershell
# Windows / PowerShell
.\start.ps1     # PostgreSQL (Docker) + API sur 14007 + frontend Angular sur 4200
.\stop.ps1      # arrêt complet de la stack
```

```bash
# Linux / macOS
./start.sh      # même orchestration
./stop.sh
```

Options disponibles (`-Mode docker`, `-NoDb`, `-NoFront`, `-DryRun`, `-PurgeVolumes`, `-Status`) :
voir [documentation/05-deploiement/scripts-de-demarrage.md](documentation/05-deploiement/scripts-de-demarrage.md).

### Backend

```powershell
cd WcPregnantService
mvn clean package          # build
mvn test                   # tests unitaires + intégration
mvn test jacoco:report     # rapport de couverture (target/site/jacoco/)
docker build -t wcpregnant:local .
docker-compose up --build  # API sur http://localhost:14007
```

### Frontend

```powershell
cd WcPregnantApp
npm install
npm run build
npm run test
```

### Kubernetes

```powershell
cd WcPregnantService/k8s
kubectl apply -f namespace.yaml
.\deploy.ps1
```

### Mode agentique (Tool Calling, inférence locale)

```powershell
# 1. Démarrer le serveur d'inférence local (Strata AI, API compatible OpenAI)
$env:AGENT_INFERENCE_BASE_URL = "http://localhost:8000/v1"
$env:AGENT_MODEL              = "Qwen-3.8-Flash-Next"
$env:AGENT_MAX_ITERATIONS    = "4"

# 2. Lister les outils exposés au modèle
curl.exe -u manu:manu http://localhost:14007/agent/tools

# 3. Converser avec l'agent (le serveur exécute les tool_calls interceptés)
curl.exe -X POST http://localhost:14007/agent/chat `
  -H "Content-Type: application/json" -u manu:manu `
  -d '{"message":"Toilettes accessibles à moins de 200 m de la mairie de Paris ?","position":{"latitude":48.8566,"longitude":2.3522}}'
```

Sans serveur d'inférence joignable, l'API répond **503** (`ApiError`) — l'application reste utilisable en mode CRUD.

---

## Documentation

| Phase | Contenu |
|---|---|
| [1 — Exigences](documentation/01-exigences/) | Contexte, exigences métier, contraintes techniques, brief initial |
| [2 — Conception](documentation/02-conception/) | Architecture, modèle de données, diagrammes, contrat OpenAPI |
| [3 — Implémentation](documentation/03-implementation/) | Conventions de code, guide du frontend Angular |
| [4 — Tests](documentation/04-tests/) | Stratégie de tests, Javadoc + couverture JaCoCo |
| [5 — Déploiement](documentation/05-deploiement/) | Setup environnement, build/Docker, Kubernetes |
| [6 — Outils](documentation/06-outils/) | Configuration Cline (règles + skills) |

---

## Dépôt GitLab

<https://gitlab.com/manix35400/wcpregnant>
