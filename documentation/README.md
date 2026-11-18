# Documentation — WcPregnant

Documentation **unifiée** du projet, organisée selon les phases du cycle de vie du logiciel (SDLC).

Les documents jusqu'ici dispersés à la racine, dans `WcPregnantService/`, `WcPregnantApp/` et `architecture/` sont regroupés ici. Les artefacts non-Markdown (`.pptx`, `.docx`) sont conservés dans [`assets/`](assets/) et convertis en Markdown dans la phase correspondante.

---

## Navigation par phase

| # | Phase | Documents |
|---|-------|-----------|
| 1 | **Exigences** | [contexte-et-objectifs.md](01-exigences/contexte-et-objectifs.md) · [exigences-metier.md](01-exigences/exigences-metier.md) · [contraintes-techniques.md](01-exigences/contraintes-techniques.md) · [objectif-agentique.md](01-exigences/objectif-agentique.md) · [prompt-initial-copilot.md](01-exigences/prompt-initial-copilot.md) |
| 2 | **Conception** | [architecture.md](02-conception/architecture.md) · [modele-de-donnees.md](02-conception/modele-de-donnees.md) · [diagrammes.md](02-conception/diagrammes.md) · [contrat-api-openapi.md](02-conception/contrat-api-openapi.md) |
| 3 | **Implémentation** | [conventions-de-code.md](03-implementation/conventions-de-code.md) · [frontend-angular.md](03-implementation/frontend-angular.md) |
| 4 | **Tests** | [strategie-de-tests.md](04-tests/strategie-de-tests.md) · [javadoc-et-couverture-tests.md](04-tests/javadoc-et-couverture-tests.md) |
| 5 | **Déploiement** | [environnement-developement.md](05-deploiement/environnement-developement.md) · [build-et-deploiement.md](05-deploiement/build-et-deploiement.md) · [scripts-de-demarrage.md](05-deploiement/scripts-de-demarrage.md) · [kubernetes.md](05-deploiement/kubernetes.md) |
| 6 | **Outils & gouvernance** | [configuration-cline.md](06-outils/configuration-cline.md) · [workflow-git.md](06-outils/workflow-git.md) · [graphify.md](06-outils/graphify.md) · [llm-council.md](06-outils/llm-council.md) |

---

## Correspondance « ancien emplacement → nouveau document »

| Ancien fichier | Nouveau document | Phase |
|---|---|---|
| `README.md` (racine) | [README.md](../README.md) (racine, réécrit comme vitrine du projet) | — |
| `AGENTS.md` (racine) | [README.md](../README.md) + [conventions-de-code.md](03-implementation/conventions-de-code.md) | 3 |
| `CLINE-CONFIG.md` (racine) | [configuration-cline.md](06-outils/configuration-cline.md) | 6 |
| `architecture/DAL_WcPregnant_V1.0.docx` | [exigences-metier.md](01-exigences/exigences-metier.md) + [modele-de-donnees.md](02-conception/modele-de-donnees.md) | 1, 2 |
| `architecture/dal_extract.txt` | [exigences-metier.md](01-exigences/exigences-metier.md) | 1 |
| `architecture/Diagrammes.pptx` | [diagrammes.md](02-conception/diagrammes.md) | 2 |
| `PromptCopilot.txt` (racine) | [prompt-initial-copilot.md](01-exigences/prompt-initial-copilot.md) | 1 |
| `WcPregnantService/ARCHITECTURE.md` | [architecture.md](02-conception/architecture.md) | 2 |
| `WcPregnantService/SETUP-GUIDE.md` | [environnement-developement.md](05-deploiement/environnement-developement.md) | 5 |
| `WcPregnantService/README-DEPLOY.md` | [build-et-deploiement.md](05-deploiement/build-et-deploiement.md) | 5 |
| `WcPregnantService/k8s/README.md` | [kubernetes.md](05-deploiement/kubernetes.md) | 5 |
| `WcPregnantService/CLINE-JAVADOC-TU.md` | [javadoc-et-couverture-tests.md](04-tests/javadoc-et-couverture-tests.md) | 4 |
| `WcPregnantApp/README.md` | [frontend-angular.md](03-implementation/frontend-angular.md) | 3 |

---

## Artefacts d'origine conservés dans `assets/`

| Fichier | Nature | Document Markdown qui le reproduit |
|---|---|---|
| [`DAL_WcPregnant_V1.0.docx`](assets/DAL_WcPregnant_V1.0.docx) | Cahier des charges Word | [exigences-metier.md](01-exigences/exigences-metier.md), [modele-de-donnees.md](02-conception/modele-de-donnees.md) |
| [`Diagrammes.pptx`](assets/Diagrammes.pptx) | Slide PowerPoint | [diagrammes.md](02-conception/diagrammes.md) |
| [`dal_extract.txt`](assets/dal_extract.txt) | Extraction texte du `.docx` | [exigences-metier.md](01-exigences/exigences-metier.md) |
| [`PromptCopilot.txt`](assets/PromptCopilot.txt) | Brief initial soumis à Copilot | [prompt-initial-copilot.md](01-exigences/prompt-initial-copilot.md) |

---

## Artefacts techniques volontairement **non déplacés**

Certains fichiers restent à leur emplacement d'origine parce qu'ils sont consommés par le build ou par l'outillage de leur module ; la documentation y renvoie plutôt que de les dupliquer :

| Artefact | Emplacement | Document qui le référence |
|---|---|---|
| `start.ps1`, `stop.ps1`, `start.sh`, `stop.sh`, `.env.example` | racine du monorepo | [scripts-de-demarrage.md](05-deploiement/scripts-de-demarrage.md) |
| `ToiletAPI.yaml`, `CustomerAPI.yaml`, `ObjectDefinitions.yaml`, `Siren.json` | `WcPregnantService/src/main/resources/api/` | [contrat-api-openapi.md](02-conception/contrat-api-openapi.md) |
| `Dockerfile`, `docker-compose.yml` | `WcPregnantService/` | [build-et-deploiement.md](05-deploiement/build-et-deploiement.md) |
| Manifestes Kubernetes + `deploy.ps1` / `deploy.sh` | `WcPregnantService/k8s/` | [kubernetes.md](05-deploiement/kubernetes.md) |
| Collection Bruno (`.bru`) | `Bruno/WcPregnant/WC Pregnant API/` | [contrat-api-openapi.md](02-conception/contrat-api-openapi.md), [strategie-de-tests.md](04-tests/strategie-de-tests.md) |
| Règles et skills Cline | `.clinerules/`, `.cline/skills/` | [configuration-cline.md](06-outils/configuration-cline.md) |
| Tests JUnit / Cucumber / Karma | `WcPregnantService/src/test/`, `WcPregnantApp/src/` | [strategie-de-tests.md](04-tests/strategie-de-tests.md) |
| Workflow CI/CD (`docker-image.yml`) | `WcPregnantService/.github/workflows/` | [build-et-deploiement.md](05-deploiement/build-et-deploiement.md) |

> Le dépôt principal est hébergé sur GitLab (`https://gitlab.com/manix35400/wcpregnant`) : le workflow GitHub Actions de `WcPregnantService/.github/` ne s'exécute que si le projet est mirroré sur GitHub.

---

## Conventions rédactionnelles

- **Langue** : **français** pour l'ensemble des documents. Les termes techniques conservent leur nom officiel d'usage (Docker, Kubernetes, Javadoc, HPA…), et les identifiants de code restent en anglais conformément aux conventions de code.
- **Pas de duplication** : un sujet n'est détaillé qu'à un seul endroit ; les autres documents renvoient au fichier source.
- **Format** : titres hiérarchiques, tableaux pour les comparaisons, exemples de code fonctionnels, aucune section vide.
- **Règles de maintien** de la documentation : voir `.clinerules/08-documentation.md`.

---

## État d'avancement de la documentation

| Phase | Statut |
|---|---|
| 1 — Exigences | ✅ Rédigée (convertie depuis le `.docx` et le brief Copilot) + objectif agentique Tool Calling et son **état d'implémentation** |
| 2 — Conception | ✅ Rédigée (architecture, modèle de données, diagrammes Mermaid, contrat API) — §7.3 décrit la couche agentique (registre d'outils, boucle Reason → Act → Observe, configuration d'inférence) |
| 3 — Implémentation | ✅ Rédigée (conventions de code, frontend Angular) — frontend enrichi de l'**assistant agentique** (composant de conversation, service `/agent`, synchronisation carte Leaflet) + **socle agentique autonome** `src/app/agent/` (`AgentRequest`/`AgentResponse`, `AgentService.askAgent`, **bulle flottante `AgentChatComponent`** en Signals) testé par `agent.service.spec.ts` et `agent-chat.component.spec.ts` ; la bulle est **intégrée dans `HomeComponent`** et l'événement `(selectToilette)` **centre la carte Leaflet** sur la toilette choisie (`WcMapComponent.focusToilet` / `focusToiletById`) |
| 4 — Tests | ✅ Rédigée (stratégie de tests, couverture JaCoCo) — tests de la couche agent (registre, executor, orchestrateur, client d'inférence, API) et **test bout en bout** `AgentChatEndToEndTest` avec faux serveur d'inférence local |
| 5 — Déploiement | ✅ Rédigée (environnement, Docker, Kubernetes) + **scripts de démarrage et d'arrêt complet** de la solution (`start.ps1`/`stop.ps1`, `start.sh`/`stop.sh`) + **installation des plugins Android et Android Design Tools** dans IntelliJ IDEA (section 3 d'[environnement-developement.md](05-deploiement/environnement-developement.md)) + **sonde de santé corrigée** : `HEALTHCHECK` Docker et probes K8s pointent désormais sur `/actuator/health` et `/actuator/health/readiness` (chaîne `healthFilterChain` publique, `@Order(0)`), le conteneur est `healthy` |
| 6 — Outils | ✅ Rédigée (configuration Cline + **workflow Git : un commit/push et une maj de doc par évolution fonctionnelle** + **Graphify : graphe de connaissances local du monorepo** + **Conseil LLM : skill `llm-council` forké et adapté à Cline**) |

> **Note** : les documents historiques rédigés en anglais (`architecture.md`, `environnement-developement.md`, `kubernetes.md`, `frontend-angular.md`) ont été **traduits en français** et alignés sur le code réel (interfaces `ToiletteCrudService` / `ToiletteProximityService`, stratégie `DistanceCalculator`, Docker multi-stage Temurin 26, workflow CI existant).
