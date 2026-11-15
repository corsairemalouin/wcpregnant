# WcPregnant

Application de localisation de toilettes publiques accessibles aux personnes enceintes : CRUD d'équipements, recherche de proximité (Haversine), pagination, sécurité RBAC, déploiement conteneurisé.

> 📂 **Toute la documentation du projet est centralisée dans [`documentation/`](documentation/README.md)**, organisée selon les phases du cycle de vie du logiciel.

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

### Backend

```powershell
cd WcPregnantService
mvn clean package          # build
mvn test                   # tests unitaires + intégration
mvn test jacoco:report     # rapport de couverture (target/site/jacoco/)
docker build -t wcpregnant:local .
docker-compose up --build  # API sur http://localhost:8080
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
