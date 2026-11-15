# Directives agentiques — Tool Calling (toujours actives)

## Socle technique de référence

| Couche | Stack |
|---|---|
| Backend | Java 25, Spring Boot 4.1.0, Maven, PostgreSQL |
| Frontend | Angular 20, TypeScript 5.8, Leaflet |
| Inférence | **Strata AI** (`Qwen-3.8-Flash-Next`) servi **en local**, API compatible OpenAI |

## Objectif

Transformer l'application CRUD existante en **système agentique avec Tool Calling** (concours **ASUS Agent Computer Challenge**) : un agent local orchestre les capacités métier déjà implémentées — recherche de proximité Haversine, gestion des équipements, vérification RBAC.

## Capacités exposées comme outils

| Outil (implémenté) | Service / contrat existant | Rôle requis |
|---|---|---|
| `search_toilettes_proches` | `ToiletteProximityService#getNearestToilets(GeoPosition)` (Haversine via `DistanceCalculator`, distance en **mètres**) | `USER` |
| `get_details_toilette` | `ToiletteCrudService#getToiletById(id)` | `USER` |
| `gerer_equipement_crud` (`CREATE` / `UPDATE` / `DELETE`) | `ToiletteCrudService` (+ `ToiletteRatingService` via le champ `grade` du payload) | `ADMIN` |
| `check_access` | `AppSecurityConfiguration` + `bdd/dao/Authorities` | authentifié |

Les endpoints agentiques disponibles : `GET /agent/tools`, `POST /agent/tools/call`, `POST /agent/chat`.

## Contraintes de code (non négociables)

1. **Architecture en couches inchangée** : `web` → `service` → `service/impl` → `bdd/repository` → `bdd/dao`. La couche agent est un **adaptateur** qui appelle les interfaces de service existantes ; elle ne contient ni SQL, ni mapping manuel.
2. **Couverture JaCoCo maintenue > 80 %** : chaque classe agentique nouvelle arrive avec ses tests (`method_scenario_expectedBehavior`, minimum 3 tests).
3. **Zéro fuite de données vers le cloud** : inférence 100 % locale. Aucun appel sortant (pas de LLM distant, pas de géocodage externe, pas de CDN de données). Les seuls échanges réseau autorisés sont localhost / réseau privé du cluster.
4. **RBAC jamais contourné** : un outil hérite des contraintes `@PreAuthorize` existantes ; l'agent ne peut pas écrire sans rôle `ADMIN`.
5. **Configuration de l'inférence par variables d'environnement** (`AGENT_INFERENCE_BASE_URL`, `AGENT_MODEL`, `AGENT_TIMEOUT_SECONDS`) — jamais d'URL, de clé ou de modèle en dur dans le code ou les manifests.
6. **Contrat d'API** : tout nouvel endpoint agentique est d'abord déclaré dans `src/main/resources/api/*.yaml` (les classes `web/api` et `web/dto` restent générées), puis la collection Bruno est mise à jour.
7. **Validation** : `mvn -q test` (backend) et `npm run build` / `npm run test` (frontend) avant de conclure.
