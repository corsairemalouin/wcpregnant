# Conseil LLM (LLM Council) — décisions confrontées à 5 angles

> 📂 **Documentation** — [index](../README.md) · Phase **6 — Outils & gouvernance**

## 1. Origine et adaptation

| Élément | Détail |
|---|---|
| Dépôt d'origine | [aiwithremy/claude-skills-llm-council](https://github.com/aiwithremy/claude-skills-llm-council) (skill d'Ole Lehmann) |
| Méthodologie | [LLM Council](https://github.com/karpathy/llm-council) d'Andrej Karpathy |
| Fork local | `D:/Programmation/claude-skills-llm-council` (clone du dépôt d'origine, 3 commits) |
| Adaptation Cline | `.cline/skills/llm-council/` |

Le dépôt d'origine cible **Claude Code**, qui dispose de sous-agents (`Task`) exécutés
en parallèle. **Cline n'a pas de sous-agents** : l'adaptation remplace cette mécanique
par des sessions d'inférence **locales** pilotées par un script, avec un fallback
entièrement interne.

## 2. Le principe

Une question unique est traitée par **5 conseillers indépendants**, chacun incarnant un
angle de pensée volontairement en tension avec les autres :

| # | Conseiller | Angle |
|---|---|---|
| 1 | Le Contrariant | cherche le défaut fatal, ce qui va casser |
| 2 | Le First Principles | redéfinit le problème réel |
| 3 | L'Expansionniste | cherche l'upside sous-évalué |
| 4 | L'Outsider | zéro contexte, repère la malédiction du savoir |
| 5 | L'Exécuteur | faisabilité et chemin le plus court |

Chaque réponse est ensuite **relue par les pairs de façon anonyme** (A→E, mapping
mélangé) : les évaluateurs jugent sur le fond, pas sur l'identité de l'angle. Un
**président** synthétise le tout en un verdict : convergences, divergences, angles
morts, recommandation tranchée, **une seule** première action.


## 3. Utilisation dans le projet

### 3.1 Déclenchement

Le skill se déclenche automatiquement via sa description, ou explicitement :

```text
/llm-council
```

Phrases déclencheuses : « council this », « run the council », « war room »,
« pressure-test », « stress-test », « débat », « je ne peux pas décider »,
« X ou Y ? », « quelle option est la meilleure ».

**À ne pas déclencher** : question factuelle, tâche de création, simple oui/non,
validation d'une évidence. Le conseil sert aux décisions où se tromper coûte.

### 3.2 Mode inférence locale (recommandé)

```powershell
python .cline/skills/llm-council/scripts/council.py `
  --question "Faut-il migrer la recherche de proximité vers PostGIS ou garder Haversine en mémoire ?" `
  --context AGENTS.md `
  --context documentation/02-conception/architecture.md `
  --stakes "Latence de /api/toilets et dette de migration"
```

Le script n'utilise **que la bibliothèque standard** Python (aucune dépendance ajoutée
au projet) et interroge un endpoint compatible OpenAI :

| Variable d'environnement | Défaut | Rôle |
|---|---|---|
| `AGENT_INFERENCE_BASE_URL` | `http://localhost:8000/v1` | serveur d'inférence local (Strata AI) |
| `AGENT_MODEL` | `Qwen-3.8-Flash-Next` | modèle interrogé |
| `AGENT_TIMEOUT_SECONDS` | `60` | timeout par appel |

Aucune URL, clé ou modèle n'est codé en dur : tout vient de l'environnement, conformément
à `.clinerules/09-agent-ai-tool-calling.md`.

### 3.3 Options du script

| Option | Description |
|---|---|
| `--question` | **obligatoire** — la décision à instruire |
| `--context <fichier>` | fichier de contexte (répetable, tronqué à `--max-context-chars`) |
| `--stakes "…"` | ce qui est en jeu si la décision est mauvaise |
| `--base-url`, `--model`, `--timeout`, `--temperature` | surcharge de la configuration d'inférence |
| `--seed <n>` | graine de l'anonymisation → exécution reproductible |
| `--json` | sortie brute : analyses, réponses anonymisées, revues, verdict |
| `--out <path>` | chemin du transcript Markdown |
| `--no-transcript` | n'écrit aucun fichier |
| `--mock` | exécute toute la pipeline **sans inférence** (validation hors ligne) |

Sortie : le verdict markdown du président sur `stdout`, les métadonnées sur `stderr`,
le transcript complet dans `council-transcripts/council-<horodatage>.md`.

## 4. Mode interne (fallback sans serveur local)

Cline incarne les 5 angles en une seule passe strictement séparée (aucune référence
d'un bloc à l'autre), anonymise les réponses, produit 5 revues croisées, puis le verdict
du président. Le mode utilisé est toujours annoncé. **Il n'est jamais recouru à un LLM
cloud** : la contrainte « zéro fuite de données » reste absolue.

## 5. Fichiers de l'incrément

| Fichier | Rôle |
|---|---|
| `.cline/skills/llm-council/SKILL.md` | instructions du skill (déclenchement, pipeline, garde-fous) |
| `.cline/skills/llm-council/scripts/council.py` | orchestrateur : 5 conseillers parallèles, anonymisation, revues, verdict, transcript |
| `.cline/skills/llm-council/templates/verdict.md` | modèle de restitution du verdict |
| `.clinerules/12-llm-council.md` | garde-fous toujours actifs |
| `council-transcripts/` | transcripts locaux, ignorés par Git |

## 6. Vérification

```powershell
# Compilation
python -m py_compile .cline/skills/llm-council/scripts/council.py

# Pipeline complète sans inférence (aucun appel réseau)
python .cline/skills/llm-council/scripts/council.py `
  --question "Faut-il remplacer le calcul Haversine inline par une strategie DistanceCalculator injectee ?" `
  --context AGENTS.md --mock --seed 42 --no-transcript

# Aide
python .cline/skills/llm-council/scripts/council.py --help
```

Le mode `--mock` valide l'ordonnancement (5 conseillers → anonymisation → 5 revues →
verdict) sans modèle : c'est le test de non-régression de la pipeline.

## 7. Limites assumées

- Sans serveur d'inférence local, les 5 « conseillers » sont 5 angles du **même**
  modèle : l'indépendance est rhétorique, pas statistique. Le mode `--mock` ne produit
  que des analyses de démonstration.
- Le conseil ne remplace ni les tests (`mvn -q test`), ni le contrat OpenAPI, ni le RBAC :
  il éclaire une décision, il ne valide pas une implémentation.
- Les transcripts ne sont pas versionnés : ils sont locaux et jetables.
