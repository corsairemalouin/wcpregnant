> 📂 **Documentation** — [index](../README.md) · Phase **6 — Outils & gouvernance**

# Configuration Cline du projet WcPregnant

Cette configuration suit la documentation officielle Cline ([Rules](https://docs.cline.bot/customization/cline-rules), [Skills](https://docs.cline.bot/customization/skills)).

## 1. Règles — `.clinerules/`

Toutes les règles du workspace sont combinées en un ensemble unique. Les fichiers **sans frontmatter** sont toujours actifs ; ceux avec un frontmatter `paths:` ne s'activent que lorsque les fichiers du contexte correspondent au glob.

| Fichier | Portée | Activation |
|---|---|---|
| `00-context-projet.md` | modules, stack, conventions de nommage | toujours |
| `01-standards-code.md` | standards transverses, validation | toujours |
| `02-guardrails.md` | zones interdites, secrets, code généré | toujours |
| `03-backend-java.md` | Spring Boot, Lombok, MapStruct, Javadoc | `WcPregnantService/src/main/java/**` |
| `04-tests-java.md` | JUnit 5 / Mockito, couverture | `WcPregnantService/src/test/**`, `**/*Test.java` |
| `05-openapi-contrat.md` | contrat d'API, génération | `src/main/resources/api/**`, `Bruno/**` |
| `06-frontend-angular.md` | Angular 20, Leaflet, Karma | `WcPregnantApp/**` |
| `07-devops-deploiement.md` | Docker, K8s, CI | `Dockerfile`, `docker-compose.yml`, `k8s/**`, `.github/**` |
| `08-documentation.md` | style documentaire | `**/*.md`, `documentation/**` |
| `09-agent-ai-tool-calling.md` | objectif agentique, Tool Calling, inférence locale Strata AI, RBAC | toujours |
| `11-graphify-graphe-de-connaissances.md` | interrogation du graphe `graphify-out/` avant toute recherche dans le code | toujours |
| `12-llm-council.md` | garde-fous du Conseil LLM : inférence locale uniquement, décisions à enjeux, transcripts non committés | toujours |

**Activer/désactiver** : icône balance en bas du panneau Cline → onglet *Rules*. Une règle conditionnelle désactivée ne s'active même si les paths correspondent.

## 2. Skills — `.cline/skills/`

Chaque skill est un dossier contenant un `SKILL.md` avec `name` (identique au nom du dossier) et `description` (≤ 1024 caractères, qui déclenche le skill). Seuls `name` et `description` sont chargés en permanence (~100 tokens/skill) ; le corps est chargé sur demande.

| Skill | Usage | Ressources |
|---|---|---|
| `javadoc-test-coverage` | Javadoc + TU jusqu'à > 80 % de couverture | `docs/checklist.md`, `scripts/coverage-check.ps1` |
| `openapi-endpoint` | ajout d'endpoint du YAML jusqu'à l'implémentation | `templates/endpoint.yaml` |
| `spring-service-layer` | création/refactor de couches backend | `templates/service-impl.java`, `templates/mapper.java` |
| `angular-component` | composants, services, modèles Angular | `templates/component.ts`, `templates/service.ts` |
| `k8s-deploy` | déploiement Docker/K8s | `docs/troubleshooting.md` |
| `bruno-api-test` | requêtes et scénarios d'API | `templates/request.bru` |
| `proximity-search` | calcul de distance, stratégies, optimisations | — |
| `graphify` | interrogation du graphe de connaissances du monorepo (dépendances, impact, hubs) | voir [graphify.md](graphify.md) |
| `llm-council` | confronte une décision à 5 conseillers indépendants, revue croisée anonymisée, verdict d'un président | `scripts/council.py`, `templates/verdict.md` — voir [llm-council.md](llm-council.md) |

**Déclenchement** : automatique via la description, ou explicite avec un slash command (ex. `/javadoc-test-coverage`).

## 3. Compatibilité multi-outils

`AGENTS.md` à la racine est lu par Cline et par les autres agents (Copilot, Junie). Il reprend l'essentiel des règles universelles.

## 4. Serveur MCP

Les serveurs MCP de Cline sont déclarés dans `~/.cline/data/settings/cline_mcp_settings.json`
(onglet *MCP Servers* du panneau Cline). Le projet y ajoute le serveur **graphify** (stdio) qui
expose le graphe de connaissances sans lecture de fichier — configuration détaillée dans
[graphify.md](graphify.md).

## 5. Vérification

```powershell
# Liste des règles et skills
Get-ChildItem .clinerules, .cline/skills -Recurse -File

# Test du script de couverture (après mvn test jacoco:report)
powershell -File .cline/skills/javadoc-test-coverage/scripts/coverage-check.ps1 -Threshold 80
```

## 6. Le dossier `WcPregnantService/.cline/`

Le sous-projet conserve un ancien dispositif (`config/cline-config.json`, `prompt/javadoc-tu.md`, `workflows/*.ps1`). Il est remplacé par les skills ci-dessus ; le prompt `javadoc-tu.md` reste utilisable comme référence, mais la source de vérité est désormais `.cline/skills/javadoc-test-coverage/`.
