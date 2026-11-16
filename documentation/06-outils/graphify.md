> 📂 **Documentation** — [index](../README.md) · Phase **6 — Outils & gouvernance**

# Graphify — graphe de connaissances du monorepo

[Graphify](https://graphifyy.com) indexe le code du monorepo en un **graphe de connaissances**
(symboles, fichiers, relations `calls` / `imports` / `implements`, communautés) exploitable par
l'agent **sans aucune lecture de fichier**. L'extraction est réalisée par AST locale
(tree-sitter) : **aucune clé API, aucun appel réseau, aucun token consommé**.

---

## 1. Installation

```powershell
python -m pip install -U "graphifyy[mcp,watch,leiden]"   # CLI + serveur MCP
python -m pip install -U uvicorn                          # uniquement pour le transport HTTP
python -m graphify --version        # invocation fiable (graphify.exe n'est pas toujours sur le PATH)
```

Le CLI est installé dans `%APPDATA%\Python\Python314\Scripts` (`graphify.exe`, `graphify-mcp.exe`) ;
ce dossier a été ajouté au **PATH utilisateur** du poste. Si le raccourci `graphify` n'est pas
reconnu, utiliser systématiquement `python -m graphify …` (strictement équivalent).

---

## 2. Configuration du projet

| Fichier | Rôle |
|---|---|
| `.graphifyignore` | périmètre d'analyse (syntaxe gitignore) : exclut `Waste/`, `developpement/`, `WcPregnantService/target/`, `WcPregnantApp/dist/`, `node_modules/`, `.idea/`, les binaires et médias |
| `.graphifyrc` | `query_budget=2500`, `query_depth=3`, `viz_node_limit=3000`, `language.bru=json` (les requêtes Bruno sont classées comme JSON) |
| `graphify-out/` | artefacts régénérés (`graph.json`, `graph.html`, `GRAPH_REPORT.md`, cache AST) — **ignoré par Git** |

---

## 3. Commandes de travail

| Besoin | Commande |
|---|---|
| Question d'architecture | `python -m graphify query "qui appelle DistanceCalculator ?"` |
| Chaîne de dépendances | `python -m graphify path "ToilettesApiImpl" "ToiletteService"` |
| Comprendre un concept | `python -m graphify explain "Haversine"` |
| Impact d'un refactor | `python -m graphify affected "ToiletteProximityService"` |
| Hubs architecturaux | `python -m graphify god-nodes --top 15` |
| Section d'un document | `python -m graphify section "Architecture"` |
| Reconstruire après un changement | `python -m graphify update .` |
| Extraction complète | `python -m graphify extract . --code-only` |
| Export (SVG, GraphML, Neo4j, Obsidian) | `python -m graphify export svg` |
| Statuts des hooks Git | `python -m graphify hook status` |

---

## 4. Hooks Git installés

`python -m graphify hook install` a enregistré :

- `post-commit`, `post-checkout`, `post-merge`, `post-rewrite` → reconstruction **incrémentale**
  du graphe (AST seul, exécution en arrière-plan, journal
  `~/.cache/graphify-rebuild.log`) ;
- un **merge driver** `graphify` pour fusionner `graphify-out/graph.json` (utile si le graphe
  venait à être versionné).

Désactivation ponctuelle : variable `GRAPHIFY_SKIP_HOOK=1`.

---

## 5. Intégration agentique

### Skill Cline

`.cline/skills/graphify/SKILL.md` : déclenché par les questions de dépendances, d'impact de
refactor ou de régénération du graphe.

### Serveur MCP (activé)

Lancement du serveur stdio :

```powershell
python -m graphify.serve --graph graphify-out/graph.json
# ou : graphify-mcp serve
```

Le serveur expose 11 outils de requête (`query_graph`, `get_node`, `get_neighbors`,
`shortest_path`, `god_nodes`, `graph_stats`, `get_section`, `get_community`, `get_pr_impact`,
`list_prs`, `triage_prs`).

Dans `~/.cline/data/settings/cline_mcp_settings.json` :

```json
"graphify": {
  "command": "C:\\Python314\\python.exe",
  "args": ["-m", "graphify.serve", "--graph", "D:\\Programmation\\wcpregnant\\graphify-out\\graph.json"],
  "disabled": false,
  "autoApprove": ["query_graph", "get_node", "get_neighbors", "shortest_path", "god_nodes", "graph_stats", "get_section", "get_community"]
}
```

Alternative HTTP (client MCP externe) : `python -m graphify.serve --transport http --port 8080`
→ `http://127.0.0.1:8080/mcp` (nécessite `uvicorn`).

Le serveur a été **testé** (échange JSON-RPC `initialize` → `tools/list` → `tools/call
query_graph`) : il répond correctement avec le protocole `2025-06-30` et renvoie les
1 426 nœuds du graphe. Après ajout de l'entrée, recharger les serveurs MCP dans Cline
(panneau MCP) pour que les outils `graphify` apparaissent.

---

## 6. État du graphe (16/11/2026)

- **1 426 nœuds · 3 369 arêtes · 75 communautés**
- Extraction 92 % `EXTRACTED` / 8 % `INFÉRÉE`, **0 token** consommé
- Communautés les plus denses : stratégies de distance, services toilettes, couche agent
  (`ToolCall`, `ToiletToolExecutor`), composants Angular (`AgentChatComponent`)
- Hubs : `ToiletDAO` (43 arêtes), `ToiletteServiceImplUnitTest` (40), `ToolCall` (36),
  `Toilet` (33), `ToiletToolExecutor` (27)

---

## 7. Cohérence avec les contraintes du projet

- **100 % local** : aucune donnée ne quitte le poste, conformément à
  `.clinerules/09-agent-ai-tool-calling.md`. Aucun backend LLM distant n'est activé.
- **Garde-fous respectés** : `Waste/` et `developpement/` sont exclus du périmètre d'analyse.
- **Règle d'usage** : `.clinerules/11-graphify-graphe-de-connaissances.md` impose d'interroger
  le graphe avant de `grep` le code.

## 8. Vérification

```powershell
python -m graphify --version
python -m graphify hook status
python -m graphify god-nodes --top 5
python -m graphify query "comment la recherche de proximite est-elle implementee ?"
```
