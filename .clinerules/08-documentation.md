---
paths:
  - "**/*.md"
  - "documentation/**"
---

# Documentation

- Toute la documentation est centralisée dans **`documentation/`**, par phase SDLC : `01-exigences`, `02-conception`, `03-implementation`, `04-tests`, `05-deploiement`, `06-outils`, `assets` (artefacts `.docx`/`.pptx` d'origine). Index : `documentation/README.md`.
- Maintenir **toute** la documentation **en français** : aucun document de `documentation/` ne doit être rédigé en anglais. Les termes techniques (Docker, Kubernetes, HPA, Javadoc…) et les identifiants de code conservent leur nom d'usage ; les messages d'erreur de l'API et les noms de tests restent en anglais.
- Toute amélioration technique (nouvelle classe, refactor, endpoint) doit être reflétée dans le document concerné : `02-conception/architecture.md` (design, SOLID, couches), `README.md` racine (usage), `05-deploiement/environnement-developement.md` (prérequis).
- Format : titres hiérarchiques, tableaux pour les comparaisons, exemples de code fonctionnels, aucune section vide.
- Ne pas dupliquer : renvoyer aux fichiers existants plutôt que recopier un contenu.
- Mettre à jour la date/section "État d'avancement" quand le projet progresse.
- **Chaque évolution fonctionnelle** (endpoint, composant, refactor, config, test) doit être
  documentée **dans le même commit** que le code qui la réalise : voir
  `.clinerules/10-workflow-git.md` (séquence code → tests → validation → documentation → commit → push).

