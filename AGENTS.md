# AGENTS.md — WcPregnant

Monorepo : `WcPregnantService` (Spring Boot 4.1 / Java 25 / Maven), `WcPregnantApp` (Angular 20), `Bruno` (tests d'API), `WcPregnantService/k8s` + `Dockerfile` (déploiement), `documentation/` (documentation unifiée par phase SDLC). `Waste/` et `developpement/` sont du code mort : **ne pas toucher**.

## Documentation

Toute la documentation vit dans [`documentation/`](documentation/README.md), organisée par phase : `01-exigences`, `02-conception`, `03-implementation`, `04-tests`, `05-deploiement`, `06-outils`, `assets` (artefacts `.docx`/`.pptx` d'origine).

## Règles clés

- Le contrat OpenAPI (`WcPregnantService/src/main/resources/api/*.yaml`) est la source de vérité ; les classes sous `web/api` et `web/dto` sont **générées** et ne doivent pas être éditées.
- Architecture backend : `web` → `service` (interface) → `service/impl` → `bdd/repository` → `bdd/dao`, mapping via MapStruct dans `bdd/mapper`.
- Javadoc français obligatoire sur toute méthode publique ; tests JUnit 5 + Mockito nommés `method_scenario_expectedBehavior`, **couverture > 80 %**, aucun test supprimé.
- Ne jamais modifier le comportement métier existant lors d'ajouts de documentation ou de tests.
- Validation : `mvn -q test` (backend), `npm run build` / `npm run test` (frontend) avant de conclure.

## Objectif agentique — Tool Calling

- **Stack backend** : Java 25, Spring Boot 4.1.0, Maven, PostgreSQL. **Stack frontend** : Angular 20, TypeScript 5.8, Leaflet.
- **Inférence** : **Strata AI** (`Qwen-3.8-Flash-Next`) servi **en local** (API compatible OpenAI) — aucune donnée ne quitte l'environnement.
- **But** : transformer l'application CRUD existante en **système agentique avec Tool Calling** (concours **ASUS Agent Computer Challenge**) : recherche de proximité Haversine, gestion des équipements, vérification RBAC.
- **Contraintes** : architecture en couches existante respectée, couverture JaCoCo maintenue, **zéro fuite de données vers le cloud (100 % local)**.
- Règle exécutable : `.clinerules/09-agent-ai-tool-calling.md` · détail : [`documentation/01-exigences/objectif-agentique.md`](documentation/01-exigences/objectif-agentique.md).

## Configuration Cline

- Règles de workspace : `.clinerules/` (règles universelles + règles conditionnelles par `paths`)
- Skills spécialisés : `.cline/skills/`
- Usage détaillé : `documentation/06-outils/configuration-cline.md`
