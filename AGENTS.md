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

## Workflow Git — un commit + un push par évolution fonctionnelle

- **Un incrément fonctionnel = validation + mise à jour de la documentation + commit + push.**
- Séquence : code → tests → validation → documentation (dans le **même** commit) → commit → `git push origin main`.
- Message de commit : `type(scope): résumé en français` (`feat`, `fix`, `refactor`, `test`, `docs`, `config`, `chore`).
- Ne jamais committer `target/**`, `dist/**`, `node_modules/**` ni un secret ; stager explicitement les fichiers de l'incrément.
- Règle exécutable : [`.clinerules/10-workflow-git.md`](.clinerules/10-workflow-git.md) · détail : [`documentation/06-outils/workflow-git.md`](documentation/06-outils/workflow-git.md).

## Objectif agentique — Tool Calling

- **Stack backend** : Java 25, Spring Boot 4.1.0, Maven, PostgreSQL. **Stack frontend** : Angular 20, TypeScript 5.8, Leaflet.
- **Inférence** : **Strata AI** (`Qwen-3.8-Flash-Next`) servi **en local** (API compatible OpenAI) — aucune donnée ne quitte l'environnement.
- **But** : transformer l'application CRUD existante en **système agentique avec Tool Calling** (concours **ASUS Agent Computer Challenge**) : recherche de proximité Haversine, gestion des équipements, vérification RBAC.
- **Contraintes** : architecture en couches existante respectée, couverture JaCoCo maintenue, **zéro fuite de données vers le cloud (100 % local)**.
- Règle exécutable : `.clinerules/09-agent-ai-tool-calling.md` · détail : [`documentation/01-exigences/objectif-agentique.md`](documentation/01-exigences/objectif-agentique.md).

## Graphe de connaissances — Graphify

- Le monorepo est indexé par **Graphify** (`graphify-out/graph.json`, ignoré par Git) : 1 426 nœuds, 3 369 arêtes, 75 communautés, extraction AST **100 % locale** (0 token).
- **Avant de `grep`** le code, interroger le graphe : `python -m graphify query "…"`, `path "A" "B"`, `affected "Symbole"`, `god-nodes`.
- Après une modification de code : `python -m graphify update .` (déjà déclenché par le hook `post-commit`).
- Périmètre défini par `.graphifyignore` (exclut `Waste/`, `developpement/`, `target/`, `node_modules/`) et réglages dans `.graphifyrc`.
- Règle exécutable : `.clinerules/11-graphify-graphe-de-connaissances.md` · détail : [`documentation/06-outils/graphify.md`](documentation/06-outils/graphify.md).

## Conseil LLM — décisions confrontées à 5 angles

- Skill `.cline/skills/llm-council/` : fork adapté de
  [aiwithremy/claude-skills-llm-council](https://github.com/aiwithremy/claude-skills-llm-council)
  (méthodologie Karpathy), **réadapté pour Cline** (pas de sous-agents).
- Pipeline : 5 conseillers indépendants (Contrariant, First Principles, Expansionniste,
  Outsider, Exécuteur) → revue croisée **anonymisée** → verdict d'un président
  (accords, divergences, angles morts, recommandation, une seule première action).
- Exécution : `python .cline/skills/llm-council/scripts/council.py --question "…" --context AGENTS.md`
  contre l'inférence **locale** (`AGENT_INFERENCE_BASE_URL`) ; fallback interne si le
  serveur local n'est pas joignable — **jamais de LLM cloud**.
- Transcripts dans `council-transcripts/` (ignorés par Git).
- Règle exécutable : `.clinerules/12-llm-council.md` · détail : [`documentation/06-outils/llm-council.md`](documentation/06-outils/llm-council.md).

## Configuration Cline

- Règles de workspace : `.clinerules/` (règles universelles + règles conditionnelles par `paths`)
- Skills spécialisés : `.cline/skills/`
- Usage détaillé : `documentation/06-outils/configuration-cline.md`
