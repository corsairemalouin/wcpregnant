# Contexte projet — WcPregnant

Application de localisation de toilettes publiques accessibles aux personnes enceintes : CRUD d'équipements, recherche de proximité (Haversine), pagination, sécurité RBAC, déploiement conteneurisé.

## Modules du monorepo

| Dossier | Rôle | Stack |
|---|---|---|
| `WcPregnantService/` | API REST | Spring Boot 4.1.0, Java 25, Maven, Lombok, MapStruct, Spring Data JPA, PostgreSQL/H2, Spring Security, Actuator, JaCoCo |
| `WcPregnantApp/` | Front utilisateur | Angular 20, TypeScript 5.8, Leaflet, Karma/Jasmine |
| `Bruno/` | Collection de tests d'API | fichiers `.bru` |
| `k8s/` | Manifestes Kubernetes | Deployment, Service, Ingress, HPA, NetworkPolicy, PDB |
| `documentation/` | Documentation unifiée | index `README.md` + phases `01-exigences` … `06-outils` + `assets/` |
| `Waste/`, `developpement/` | Code mort / archives | **ne jamais modifier** |

## Conventions de nommage

- Package racine : `org.manu.wcpregnant`
- Couches backend : `web` (controllers) → `service` (interfaces) → `service/impl` → `bdd/repository` → `bdd/dao` (entités) → `bdd/mapper` (MapStruct)
- Entités JPA suffixées `DAO` (`ToiletDAO`), DTO OpenAPI sans suffixe (`Toilet`), mappers `XxxDAOMapper`
- Tests : `XxxTest` dans `src/test/java`, même package que la classe testée

## Langue

- **Javadoc, commentaires et messages de log : français** (identifiants en anglais).
- Messages d'erreur destinés à l'API et noms de tests : anglais.
