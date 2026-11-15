# Workflow Git — un commit + un push par évolution fonctionnelle

Le projet applique une règle simple : **chaque évolution fonctionnelle est validée, documentée,
committée et poussée avant de passer à la suivante**. La règle exécutable suivie par l'agent est
[`.clinerules/10-workflow-git.md`](../../.clinerules/10-workflow-git.md) ; ce document en est la
version lisible pour les contributeurs humains.

---

## 1. La séquence d'un incrément

```text
code  →  tests  →  validation  →  documentation  →  commit  →  push
```

| Étape | Ce qu'on fait | Commande de contrôle |
|---|---|---|
| **Code** | un seul incrément fonctionnel (endpoint, composant, refactor, config) | — |
| **Tests** | tests ajoutés, jamais supprimés (min. 3 par méthode publique Java) | — |
| **Validation** | la compilation et les tests doivent passer | `mvn -q test` · `npm run build` · `npm run test` |
| **Documentation** | mise à jour du document de la phase concernée **dans le même commit** | — |
| **Commit** | stage explicite + message structuré | `git add …` puis `git commit -m "feat(scope): …"` |
| **Push** | poussée immédiate de l'incrément | `git push origin main` |

> Un commit n'est créé que si la validation est verte. Une évolution non poussée n'est pas
> considérée comme terminée.

## 2. Format des messages de commit

`type(scope): résumé en français`

| `type` | Usage |
|---|---|
| `feat` | nouvelle fonctionnalité |
| `fix` | correction d'un comportement |
| `refactor` | réorganisation sans changement de comportement |
| `test` | ajout ou correction de tests |
| `docs` | documentation seule |
| `config` | `application.properties`, proxy, env, manifestes |
| `chore` | outillage, nettoyage |

Exemples réels du dépôt :

```text
feat(agent-chat): assistant conversationnel branché sur POST /agent/chat
fix(map): le marqueur de mise en avant est centré sur l'équipement sélectionné
docs(agent): description de la boucle Reason → Act → Observe
```

## 3. Quelle documentation mettre à jour ?

| Évolution | Document concerné |
|---|---|
| Nouvelle classe, nouvelle couche, refactor | [`02-conception/architecture.md`](../02-conception/architecture.md) |
| Nouvel endpoint ou changement de contrat | [`02-conception/contrat-api-openapi.md`](../02-conception/contrat-api-openapi.md) + collection Bruno |
| Composant ou service Angular | [`03-implementation/frontend-angular.md`](../03-implementation/frontend-angular.md) |
| Nouveaux tests, couverture | [`04-tests/strategie-de-tests.md`](../04-tests/strategie-de-tests.md) |
| Docker, Kubernetes, CI, prérequis | [`05-deploiement/`](../05-deploiement/) |
| Fonctionnalité agentique (outils, inférence) | [`01-exigences/objectif-agentique.md`](../01-exigences/objectif-agentique.md) |
| Règle ou skill Cline | [`06-outils/configuration-cline.md`](configuration-cline.md) |
| Toute évolution | [`README.md`](../README.md) — section « État d'avancement » |

## 4. Ce qui ne doit jamais être committé

- artefacts de build : `WcPregnantService/target/**`, `WcPregnantApp/dist/**`, `node_modules/**` ;
- secrets : `keystore.jks`, `.env`, credentials, tokens ;
- fichiers temporaires de validation (`*-out.txt`, `*-err.txt`) ou scripts de contournement.

Le dépôt contient historiquement des rapports de build **trackés** sous
`WcPregnantService/target/` (surefire, JaCoCo). Pour des commits propres, stagez explicitement les
fichiers sources et la documentation :

```bash
git add WcPregnantApp/src documentation .clinerules
git commit -m "feat(agent-chat): assistant conversationnel branché sur POST /agent/chat"
git push origin main
```

Pour cesser de suivre ces artefacts :

```bash
git rm --cached -r WcPregnantService/target
echo "/WcPregnantService/target/" >> .gitignore
```

## 5. En cas d'échec du push

Authentification GitHub expirée, réseau indisponible ou conflit de branche : conserver le commit
local, indiquer clairement la commande à relancer (`git push origin main`) et ne pas déclarer
l'évolution comme livrée.
