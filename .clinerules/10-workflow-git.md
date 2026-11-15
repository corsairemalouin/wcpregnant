# Workflow Git — commit + push à chaque évolution fonctionnelle (toujours actif)

## 1. Ce qu'est une « évolution fonctionnelle » (incrément)

Un incrément est **une unité de valeur livrable et vérifiable** : un endpoint ajouté, un composant
frontend, une règle de validation, un refactor, un jeu de tests, une correction de config, une
modification de manifeste K8s/Docker. Une simple lecture, une analyse ou un essai non concluant
n'est **pas** un incrément et ne doit pas être committé.

## 2. Séquence obligatoire, dans cet ordre

| # | Étape | Commande / action |
|---|---|---|
| 1 | Code | implémentation de l'incrément uniquement (pas de « fourre-tout ») |
| 2 | Tests | tests ajoutés (min. 3 par méthode publique côté Java, `*.spec.ts` côté Angular) |
| 3 | Validation | `mvn -q test` (backend) et/ou `npm run build` + `npm run test` (frontend) |
| 4 | Documentation | mise à jour du/ des document(s) concerné(s) **dans le même commit** |
| 5 | Commit | `git add <fichiers explicites>` puis `git commit -m "type(scope): résumé"` |
| 6 | Push | `git push origin main` (ou la branche courante) |

**Aucun commit n'est créé si la validation de l'étape 3 échoue.** Aucun incrément ne reste
non committé à la fin d'une tâche : la tâche est considérée terminée seulement après le push.

## 3. Format du commit

- **Un incrément = un commit = un push.** Ne regrouper pas deux évolutions fonctionnelles distinctes.
- Format : `type(scope): résumé en français`
  - types : `feat`, `fix`, `refactor`, `test`, `docs`, `config`, `chore`
  - scope : `agent`, `toilettes`, `map`, `auth`, `k8s`, `docker`, `doc`, …
  - exemple : `feat(agent-chat): assistant conversationnel branché sur POST /agent/chat`
- Corps optionnel : ce qui a été validé (nombre de tests, couverture) et les documents mis à jour.
- Une évolution transversale (endpoint + composant frontend + doc) peut tenir dans un seul commit ;
  sinon séparer backend et frontend.

## 4. Mise à jour de la documentation (non négociable)

Chaque commit fonctionnel contient **au moins un fichier de `documentation/`**.

| Type d'évolution | Document à mettre à jour |
|---|---|
| Nouvelle classe, couche, refactor | `02-conception/architecture.md` |
| Nouvel endpoint / contrat | `02-conception/contrat-api-openapi.md` + collection Bruno |
| Composant ou service Angular | `03-implementation/frontend-angular.md` |
| Stratégie ou nouveaux tests | `04-tests/strategie-de-tests.md` |
| Docker, K8s, CI, prérequis | `05-deploiement/*` |
| Objectif agentique / outil IA | `01-exigences/objectif-agentique.md` |
| Règle ou skill Cline | `06-outils/configuration-cline.md` |
| N'importe quelle évolution | `documentation/README.md` (section « État d'avancement ») |

## 5. Contenu interdit dans un commit

- Artefacts de build : `WcPregnantService/target/**`, `WcPregnantApp/dist/**`, `node_modules/**`.
- Secrets : `keystore.jks`, `.env`, credentials, tokens (voir `02-guardrails.md`).
- Fichiers temporaires de validation (`*-out.txt`, `*-err.txt`, scripts de contournement) : les
  supprimer avant de committer.

Stager explicitement les fichiers de l'incrément (`git add src/... documentation/...`) plutôt que
`git add -A`, pour ne pas embarquer les rapports JaCoCo/surefire.

## 6. Si le push échoue

Authentification, réseau ou conflit de branche : **le signaler explicitement** à l'utilisateur,
indiquer le commit créé localement et la commande à relancer. Ne jamais déclarer l'évolution
« terminée » sans push effectif.
