---
name: bruno-api-test
description: Exécute, crée ou corrige les requêtes de la collection Bruno (dossier Bruno/) pour tester l'API WcPregnant : authentification basic, variables d'environnement, scénarios CRUD. Utiliser pour "tester l'API", "ajouter une requête", "vérifier un endpoint", "debug une réponse 401/404".
---

# Tests d'API avec Bruno

## Emplacement

- Collection : `Bruno/WcPregnant/WC Pregnant API/*.bru`
- Environnement : `Bruno/environments/local.bru` (variable `{{base_url}}`)

## Format d'une requête

Utiliser `templates/request.bru` comme base. Structure : `meta`, verbe + `url`, `params:query`, `body:json`, `auth:basic`.

## Bonnes pratiques

- Toujours utiliser `{{base_url}}` plutôt qu'un URL littéral.
- Auth `basic` avec les comptes de test du profil `dev` — ne pas mettre de vrai mot de passe dans le dépôt.
- Un fichier `.bru` par scénario (nom explicite : `createToilet.bru`, `getNearestToilets.bru`).
- Après une modification du contrat OpenAPI, ajouter/mettre à jour la requête correspondante.
- Vérifier les codes attendus : 201 création, 204 suppression, 404 absence, 400 validation, 401/403 auth.

## Alternative sans Bruno (curl)

```powershell
curl -X GET "http://localhost:14007/api/wcpregnant/toilets?page=0&size=20&latitude=48.85&longitude=2.35" `
  -H "Authorization: Basic <base64(user:pass)"
```
