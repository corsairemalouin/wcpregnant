---
name: llm-council
description: Passe une décision, un arbitrage technique ou une idée à risque par un « conseil » de 5 conseillers indépendants (Contrariant, First Principles, Expansionniste, Outsider, Exécuteur), une revue croisée anonymisée, puis le verdict d'un président. Déclencheurs : "council this", "run the council", "conseil this", "war room", "pressure-test", "stress-test", "débat", "je ne peux pas décider", "X ou Y ?", "quelle option est la meilleure". Ne PAS déclencher pour une question factuelle, une tâche de création, un simple oui/non ou une demande de validation d'une évidence.
---

# Conseil LLM (LLM Council)

Fork local adapté de
[aiwithremy/claude-skills-llm-council](https://github.com/aiwithremy/claude-skills-llm-council)
(skill d'Ole Lehmann, méthodologie d'Andrej Karpathy), **réadapté pour Cline** :
Cline n'a pas de sous-agents, le conseil s'exécute soit par **inférence locale**
(Strata AI, API compatible OpenAI), soit en **mode interne** (Cline incarne les 5 angles).

## Pourquoi

Une seule réponse d'un seul modèle ne permet pas de savoir si elle est solide.
Le conseil confronte 5 angles de pensée qui se contredisent volontairement, fait
relire chaque réponse par les autres **de façon anonyme**, puis synthétise un verdict
qui dit où le conseil converge, où il diverge, et quelle est la première action.

## Quand le déclencher

**Bonnes questions** (incertitude réelle, coût d'une mauvaise décision) :

- « Faut-il migrer la recherche de proximité vers un index PostGIS ou garder le calcul Haversine en mémoire ? »
- « Est-il pertinent d'ajouter l'inférence agentique avant d'avoir stabilisé la couverture de tests ? »
- « Docker multi-stage ou image unique pour le déploiement ? »
- « Ce refactor SOLID vaut-il la dette qu'il coûte ? »

**Mauvaises questions** (ne pas déclencher) :

- « Quelle est la capitale de la France ? » (une seule réponse)
- « Écris le README » (tâche de création, pas une décision)
- « Dois-je utiliser des espaces ou des tabulations ? » (compromis sans enjeu)

## Les 5 conseillers

| # | Conseiller | Angle |
|---|---|---|
| 1 | **Le Contrariant** | Cherche le défaut fatal, ce qui manque, ce qui va casser |
| 2 | **Le First Principles** | Redéfinit le problème : « que cherche-t-on vraiment à résoudre ? » |
| 3 | **L'Expansionniste** | Cherche l'opportunité cachée et le potentiel sous-évalué |
| 4 | **L'Outsider** | Zéro contexte : repère ce que l'auteur tient pour évident |
| 5 | **L'Exécuteur** | Réalisabilité et chemin le plus court : « que fait-on lundi matin ? » |

Ces cinq angles créent trois tensions : Contrariant ↔ Expansionniste (risque vs upside),
First Principles ↔ Exécuteur (tout repenser vs agir), l'Outsider gardant tout le monde honnête.


## Étape 1 — cadrer la question avec le contexte du projet

Avant de cadrer, rassembler le contexte **avec les outils Cline** (pas `Glob`/`Read`) :

- `read_files` sur `AGENTS.md`, `.clinerules/00-context-projet.md`, `.clinerules/02-guardrails.md`
- `read_files` sur le document concerné dans `documentation/` (architecture, contrat API, stratégie de tests)
- `python -m graphify query "…"` / `python -m graphify affected "Symbole"` pour mesurer l'impact réel d'une décision technique
- les fichiers explicitement cités par l'utilisateur

Puis reformuler la question de façon **neutre** : la décision, le contexte, les chiffres,
ce qui est en jeu. Pas d'opinion, pas d'orientation. Si la question est trop vague,
poser **une seule** question clarifiante via `ask_question`, puis continuer.

## Étape 2 — réunir le conseil (mode inférence locale, recommandé)

```powershell
python .cline/skills/llm-council/scripts/council.py `
  --question "Faut-il X ou Y ?" `
  --context AGENTS.md `
  --context documentation/02-conception/architecture.md `
  --stakes "Couverture JaCoCo et latence de /api/toilets"
```

Le script appelle l'endpoint local (`AGENT_INFERENCE_BASE_URL`, défaut
`http://localhost:8000/v1`, modèle `AGENT_MODEL`) **5 fois en parallèle**, une fois par
conseiller : les réponses sont réellement indépendantes.

| Option | Effet |
|---|---|
| `--context <fichier>` | enrichit la question (répetable) |
| `--stakes "…"` | rappelle ce qui est en jeu |
| `--base-url` / `--model` / `--timeout` | cible d'inférence (sinon variables d'environnement) |
| `--seed <n>` | graine reproductible de l'anonymisation |
| `--json` | sortie brute (conseillers, revues, verdict) pour post-traitement |
| `--no-transcript` | n'écrit pas de transcript |
| `--out <path>` | chemin du transcript |
| `--mock` | valide la pipeline **sans inférence** (aucun appel réseau) |

**Si le serveur local n'est pas joignable** : ne pas appeler de LLM cloud. Passer au
mode interne (étape 3) et le signaler à l'utilisateur.

## Étape 3 — mode interne (fallback sans serveur local)

Cline produit les 5 analyses lui-même, **en une seule passe, strictement séparées** :

1. Écrire les 5 blocs `### Le Contrariant` … `### L'Exécuteur` sans jamais se référer
   à un bloc précédent (aucune contamination entre angles).
2. Anonymiser en `Réponse A` … `Réponse E` (ordre mélangé).
3. Produire 5 revues, chacune répondant aux 3 questions : réponse la plus solide,
   plus grand angle mort, ce que **toutes** ont oublié.
4. Rédiger le verdict du président (étape 4).

Indiquer explicitement : « conseil exécuté en mode interne (pas de serveur d'inférence local) ».

## Étape 4 — revue croisée anonymisée

C'est l'étape qui distingue le conseil d'un simple « demander 5 fois ». Les réponses
sont anonymisées (A→E, mapping mélangé) pour que les pairs jugent sur le fond, pas
sur l'identité de l'angle.

## Étape 5 — verdict du président

Structure imposée (voir `templates/verdict.md`), en français :

1. **Où le Conseil est d'accord** — convergences indépendantes = signaux fiables.
2. **Où le Conseil diverge** — désaccords réels, les deux côtés exposés.
3. **Angles morts détectés** — ce que la revue croisée a fait émerger.
4. **La recommandation** — une réponse tranchée, pas « ça dépend ». Le président peut
   contredire la majorité si le raisonnement du dissident est meilleur.
5. **La première action concrète** — une seule action, pas une liste.

## Étape 6 — restitution

Le verdict est présenté **en conversation** (markdown), pas en fichier HTML.
Le transcript complet n'est écrit dans `council-transcripts/` (ignoré par Git) que si
l'utilisateur le demande ou si la décision est significative.

## Garde-fous

- **100 % local** : jamais de LLM distant, aucune donnée ne quitte l'environnement.
- Le conseil **ne remplace pas** `mvn -q test` / `npm run build` + `npm run test`.
- Une recommandation ne peut pas contourner les guardrails (code généré OpenAPI,
  `Waste/**`, `developpement/**`, seuil JaCoCo, RBAC).
- Pas de secret (mot de passe, token, credential) dans la question ou le contexte.
- Question triviale : répondre directement, sans conseil.
