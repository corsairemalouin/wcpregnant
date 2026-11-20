# Contraintes techniques

> **Source** : cahier des charges [`assets/DAL_WcPregnant_V1.0.docx`](../assets/DAL_WcPregnant_V1.0.docx), sections *Contraintes de techniques* et *Contraintes de développement*.

---

## 1. Socle technique exigé

| Exigence du cahier des charges | Statut dans le projet |
|---|---|
| Front Angular avec carte | ✅ Angular 20 + Leaflet |
| Back Java + Spring Boot | ✅ Java 25 + Spring Boot 4.1.0 |
| Base de données PostgreSQL | ✅ PostgreSQL 18 (image `postgres:18-alpine`) — H2 en mémoire pour les tests |
| API REST décrites en **OpenAPI**, code Java **auto-généré** à partir des YAML | ✅ plugin OpenAPI generator ; interfaces dans `web/api`, DTO dans `web/dto` |
| Build **Maven** | ✅ `WcPregnantService/pom.xml` |
| Delivery en **conteneurs Docker** + **configuration Kubernetes** | ✅ `Dockerfile` multi-stage + `WcPregnantService/k8s/` |
| Description **Terraform** pour une architecture cible AWS | ❌ non implémentée |

---

## 2. Contraintes de nommage et de découpage

Ces règles sont **toujours actives** ; la version exécutable (appliquée par Cline) est dans `.clinerules/00-context-projet.md` et `.clinerules/03-backend-java.md`.

| Règle | Application |
|---|---|
| Package global : `org.manu.wcpregnant` | ✅ |
| API REST → sous-package `web` | ✅ `org.manu.wcpregnant.web` |
| Objets renvoyés par l'API → suffixe `DTO` | Le contrat OpenAPI génère des DTO **sans suffixe** (`Toilet`, `Adress`) — écart assumé, voir [contrat-api-openapi.md](../02-conception/contrat-api-openapi.md) |
| Base de données → sous-package `bdd` | ✅ `org.manu.wcpregnant.bdd` |
| Objets correspondant aux tables → suffixe `DAO` | ✅ `ToiletDAO`, `AdressDAO`, `GeoPositionDAO`, `GraduationDAO` |
| Couche service → sous-package `service` | ✅ `service/` (interfaces) + `service/impl/` |
| Objets métier du service → suffixe `BO` | Le BO intermédiaire n'a pas été retenu : mapping direct `DAO` ↔ `DTO` |
| Mappers MapStruct `DAO` ↔ `BO` et `DTO` ↔ `BO` | ✅ MapStruct dans `bdd/mapper` (`XxxDAOMapper`) |
| `@Data` Lombok sur tous les BO/DAO/DTO | ✅ sur les DAO ; les DTO sont générés par OpenAPI |

---

## 3. Modules

Le cahier des charges prévoyait **3 modules IntelliJ** (front, middle, back) plus **un 4ᵉ module de déploiement** générant Docker, Kubernetes et Terraform. Dans le monorepo actuel, cette organisation est remplacée par :

| Dossier | Rôle |
|---|---|
| `WcPregnantService/` | API REST (middle + back) |
| `WcPregnantApp/` | Front utilisateur |
| `Bruno/` | Collection de tests d'API |
| `WcPregnantService/k8s/` | Manifestes Kubernetes |
| `documentation/` | Documentation (ce dossier) |

---

## 4. Exigences de déploiement

- Déploiement **automatisé**.
- Conteneurs Docker pour le front, le middle et le back.
- Configuration Kubernetes pour lancer les conteneurs.
- Description Terraform pour déployer l'architecture cible sur AWS *(à produire)*.

Détails opérationnels : [build-et-deploiement.md](../05-deploiement/build-et-deploiement.md) et [kubernetes.md](../05-deploiement/kubernetes.md).
