# Graphe de connaissances Graphify (toujours actif)

Le monorepo est indexé par **Graphify** (`graphify-out/graph.json`) : graphe AST des symboles,
appels, imports, implémentations et communautés. Extraction **100 % locale**, sans clé API.

## Règles d'usage

1. **S'orienter avant de chercher** : pour toute question d'architecture ou de dépendance
   (« qui appelle X ? », « quel endpoint touche ce repository ? », « que casse ce refactor ? »),
   interroger le graphe **avant** le premier `grep` ou lecture de fichier :
   - `python -m graphify query "…"` — sous-graphe centré sur la question
   - `python -m graphify path "A" "B"` — chaîne de dépendances
   - `python -m graphify explain "Concept"` / `affected "Symbole"` / `god-nodes --top 10`
2. **Après une modification de code** : `python -m graphify update .` (AST seul, coût nul)
   pour garder le graphe à jour. Le hook `post-commit` le fait déjà après chaque commit.
3. **Périmètre** : `.graphifyignore` exclut `Waste/`, `developpement/`, `target/`,
   `node_modules/` — ne jamais les réintégrer (voir `02-guardrails.md`).
4. **Ne jamais committer** `graphify-out/` (ignoré par `.gitignore`) ni un export (`*.svg`,
   `*.graphml`, Obsidian vault).
5. **Zéro cloud** : ne pas activer de backend LLM distant pour l'extraction sémantique.
   Si l'inférence sémantique est souhaitée, la brancher sur Strata AI local
   (`graphify provider add strata --base-url http://localhost:8000/v1 …`).
6. `GRAPH_REPORT.md` sert uniquement à une revue d'architecture globale ; les réponses
   ponctuelles viennent de `query`.

Détail complet : `documentation/06-outils/graphify.md`.
