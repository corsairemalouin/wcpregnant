---
name: graphify
description: Interroge le graphe de connaissances du monorepo (graphify-out/graph.json) avant de lire ou grepper le code. Utiliser pour "qui appelle quoi", "où est Haversine", "quel endpoint touche ce repository", "refactoriser sans casser les dépendances", "régénérer le graphe".
---

# Graphify — graphe de connaissances du code

Graphify extrait le graphe du monorepo par AST locale (**aucune clé API, aucun appel réseau**) :
symboles, fichiers, relations `calls` / `imports` / `implements`, communautés.

## Installation (une fois)

```powershell
python -m pip install -U graphifyy            # CLI : graphify / graphify-mcp
python -m graphify --version                  # si `graphify` n'est pas sur le PATH
```

Sur ce poste, `graphify.exe` est installé dans
`%APPDATA%\Python\Python314\Scripts` ; si le PATH ne le contient pas, utiliser
`python -m graphify …` (équivalent strict).

## Commandes de travail (dans la racine du monorepo)

| Besoin | Commande |
|---|---|
| Question d'architecture | `graphify query "qui appelle DistanceCalculator ?"` |
| Chaîne de dépendances entre 2 symboles | `graphify path "ToilettesApiImpl" "ToiletteServiceImpl"` |
| Focus sur un concept | `graphify explain "Haversine"` |
| Ce qu'une modification casse | `graphify affected "ToiletteProximityService"` |
| Hubs architecturaux | `graphify god-nodes --top 15` |
| Texte d'une section Markdown | `graphify section "Architecture"` |
| Reconstruire après un refactor | `graphify update .` |
| Extraction complète (CI) | `graphify extract . --code-only` |
| Export (Obsidian, SVG, GraphML, Neo4j) | `graphify export svg` |

Le périmètre est défini par `.graphifyignore` (exclut `Waste/`, `developpement/`,
`target/`, `node_modules/`) et les réglages de requête par `.graphifyrc`.

## Règles d'usage

1. **Interroger avant de chercher** : `query` / `path` / `explain` avant le premier
   `grep` ou lecture de fichier. Le sous-graphe restitué est plus petit que le rapport.
2. **Après une modification de code** : `graphify update .` (AST seul, sans coût LLM)
   pour garder `graphify-out/` à jour.
3. **Jamais de commit** de `graphify-out/` : il est ignoré par `.gitignore`.
4. **100 % local** : ne pas activer de backend LLM distant. Si l'inférence sémantique
   est voulue, la brancher sur Strata AI local :
   `graphify provider add strata --base-url http://localhost:8000/v1 --default-model Qwen-3.8-Flash-Next --env-key AGENT_INFERENCE_API_KEY`.
5. `GRAPH_REPORT.md` ne se lit que pour une revue d'architecture globale, pas pour
   répondre à une question précise.

## Serveur MCP (optionnel)

```powershell
python -m graphify.serve            # stdio (mode utilisé par Cline)
# ou, si graphify.exe est sur le PATH :
graphify-mcp serve                  # équivalent
```

Configuration Cline (`~/.cline/data/settings/cline_mcp_settings.json`) :

```json
"graphify": {
  "command": "python",
  "args": ["-m", "graphify.serve", "--graph", "graphify-out/graph.json"],
  "disabled": false,
  "autoApprove": ["query_graph", "get_node", "get_neighbors", "shortest_path", "god_nodes", "graph_stats", "get_section", "get_community"]
}
```
