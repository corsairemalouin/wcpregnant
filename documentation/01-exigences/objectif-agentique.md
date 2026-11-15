# Objectif agentique — Tool Calling en inférence locale

> **Phase** : [1 — Exigences](../README.md) · Règle exécutable associée : [`.clinerules/09-agent-ai-tool-calling.md`](../../.clinerules/09-agent-ai-tool-calling.md)

---

## 1. Contexte

Le projet WcPregnant est aujourd'hui un **CRUD** d'équipements (toilettes publiques) avec recherche de proximité, notation et sécurité RBAC. L'objectif de l'itération en cours est de le transformer en **système agentique** : une couche d'orchestration expose les fonctions métier existantes comme des **outils** (`tools`) qu'un modèle de langage local choisit et appelle de façon autonome.

Cible : le **concours ASUS Agent Computer Challenge**.

---

## 2. Socle technique retenu

| Couche | Technologie | Statut |
|---|---|---|
| Backend | Java 25, Spring Boot 4.1.0, Maven, PostgreSQL | ✅ existant |
| Frontend | Angular 20, TypeScript 5.8, Leaflet | ✅ existant |
| Inférence | **Strata AI** (`Qwen-3.8-Flash-Next`) exposé sur une **API locale compatible OpenAI** | ✅ branchée (`agent/inference/LocalInferenceClient`) |

Le choix d'une inférence locale est un **prérequis métier**, pas une préférence technique : les données de localisation des utilisateurs et les coordonnées des équipements ne doivent jamais quitter l'environnement.

---

## 3. Capacités exposées comme outils

Les outils ne créent pas de nouvelle logique métier : ils **adaptent** les services déjà en place. Le registre implémenté (`agent/tool/ToiletToolRegistry`) expose **trois** outils au format OpenAI Function Calling, qui couvrent les capacités listées dans le brief :

| Outil implémenté | Service sous-jacent | Entrées | Rôle requis |
|---|---|---|---|
| `search_toilettes_proches` | `ToiletteProximityService#getNearestToilets(GeoPosition)` (Haversine) | `latitude`, `longitude`, `rayon_metres` (optionnel), `table_a_langer` (optionnel) | `USER` |
| `get_details_toilette` | `ToiletteCrudService#getToiletById(id)` | `id` | `USER` |
| `gerer_equipement_crud` | `ToiletteCrudService#addToilet` / `updateToilet` / `deleteToilet` | `action` (`CREATE`/`UPDATE`/`DELETE`), `id`, `payload` | `ADMIN` |

> Les capacités `grade_toilet` et `check_access` du brief sont couvertes indirectement : les notes (`grade`) transitent dans le `payload` de `gerer_equipement_crud` (service `ToiletteRatingService`), et l'accès est contrôlé par le RBAC Spring appliqué à chaque appel d'outil.

Le calcul de distance reste celui de la stratégie `DistanceCalculator` (`HaversineDistanceCalculator`, résultats en **mètres**), avec les seuils de configuration `app.distance.max` et `app.result.max`.

---

## 4. Exigences non fonctionnelles

| Exigence | Critère de réussite |
|---|---|
| **Confidentialité** | 100 % local : aucun appel sortant vers un LLM cloud, aucun géocodage externe, aucune donnée transmise hors du réseau privé |
| **Intégrité de l'architecture** | la couche agent est un adaptateur qui dépend des **interfaces** de service ; aucune SQL, aucun mapping manuel |
| **Qualité de code** | couverture JaCoCo **> 80 %** par nouvelle classe, Javadoc français, tests `method_scenario_expectedBehavior` |
| **Sécurité** | le Tool Calling hérite du RBAC existant (`@PreAuthorize`) : une écriture sans rôle `ADMIN` doit échouer (403) |
| **Contrat d'API** | tout endpoint agentique est d'abord déclaré dans `src/main/resources/api/*.yaml`, puis la collection Bruno est alignée |
| **Configurabilité** | endpoint d'inférence, modèle et timeout injectés par variables d'environnement, jamais en dur |

---

## 5. État de l'implémentation

| Écart identifié | Statut | Artefact |
|---|---|---|
| Aucune couche agent n'existait | ✅ créé | package `org.manu.wcpregnant.agent` (`AgentService`, `AgentServiceImpl`, `agent/tool/`, `agent/inference/`) |
| Le contrat OpenAPI ne décrivait pas l'orchestration | ✅ ajouté | `src/main/resources/api/AgentAPI.yaml` : `GET /agent/tools`, `POST /agent/tools/call`, `POST /agent/chat` (interfaces et DTO générés) |
| La configuration d'inférence était absente | ✅ ajoutée | clés `agent.*` dans `application.properties`, surchargées par `AGENT_INFERENCE_BASE_URL`, `AGENT_MODEL`, `AGENT_TIMEOUT_SECONDS`, `AGENT_MAX_ITERATIONS` |
| Le géocodage externe n'est pas branché | ✅ conforme à la contrainte "100 % local" | la position est fournie par le client (`position` dans la requête `/agent/chat`) |
| Collection Bruno alignée | ✅ alignée | `agentListTools`, `agentCallToolProximity`, `agentChat` |
| Tests de la couche agent | ✅ ajoutés | `ToiletToolRegistryTest`, `ToiletToolExecutorTest`, `AgentServiceImplTest`, `LocalInferenceClientTest`, `AgentApiImplTest`, `AgentApiIntegrationTest`, `AgentChatEndToEndTest` |
| Interface conversationnelle utilisateur | ✅ ajoutée | `WcPregnantApp` : `AgentChatComponent` (question, suggestions, fil des étapes, cartes de résultats), `AgentService`, `models/agent.model.ts`, proxy `/agent` et marqueur de mise en avant sur la carte Leaflet |

**Reste à faire** : brancher un serveur Strata AI réel pour la démonstration end-to-end. La boucle Reason → Act → Observe est déjà prouvée en test par `AgentChatEndToEndTest`, qui démarre un faux serveur d'inférence OpenAI-compatible sur `127.0.0.1:58301` : **aucune requête réseau n'est émise pendant `mvn test`**. Côté frontend, le panneau `Assistant agentique` est déjà branché sur `POST /agent/chat` — il suffit de démarrer le serveur d'inférence local pour voir la conversation s'afficher et la carte se synchroniser.

---

## 6. Voir aussi

- [Architecture existante](../02-conception/architecture.md) — couches, SOLID, stratégie de distance
- [Contrat API OpenAPI](../02-conception/contrat-api-openapi.md) — source de vérité des endpoints
- [Configuration Cline](../06-outils/configuration-cline.md) — règles et skills appliqués au développement
