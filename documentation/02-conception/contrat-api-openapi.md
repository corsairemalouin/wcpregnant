# Contrat API (OpenAPI)

> **Source de vérité** : les fichiers YAML de [`WcPregnantService/src/main/resources/api/`](../../WcPregnantService/src/main/resources/api/). Les interfaces `web/api/*` et les DTO `web/dto/*` en sont **dérivés automatiquement** et ne doivent jamais être édités à la main.

---

## 1. Fichiers du contrat

| Fichier | Contenu |
|---|---|
| [`ToiletAPI.yaml`](../../WcPregnantService/src/main/resources/api/ToiletAPI.yaml) | Endpoints toilettes (v2.0.0) |
| [`CustomerAPI.yaml`](../../WcPregnantService/src/main/resources/api/CustomerAPI.yaml) | Endpoints clients (v1.0.0) |
| [`ObjectDefinitions.yaml`](../../WcPregnantService/src/main/resources/api/ObjectDefinitions.yaml) | Schémas partagés : `Toilet`, `Adress`, `Graduation`, `GeoPosition`, `Customer` |
| `Siren.json` | Données de référence |

**Serveurs déclarés** :

| Environnement | URL |
|---|---|
| Développement local | `http://localhost:8080/api/wcpregnant` |
| Cluster Kubernetes | `http://wcpregnant-service/api/wcpregnant` |

---

## 2. Endpoints toilettes

| Méthode | Chemin | `operationId` | Auth | Codes |
|---|---|---|---|---|
| `GET` | `/toilets` | `getToilets` | `basicAuth` | 200, 400, 401 |
| `POST` | `/toilets` | `createToilet` | `basicAuth` (rôle **ADMIN**) | 201, 400, 401, 403 |
| `GET` | `/toilets/{id}` | `getToiletById` | `basicAuth` | 200, 404 |
| `PUT` | `/toilets/{id}` | `updateToilet` | `basicAuth` (rôle **ADMIN**) | 200, 400, 404, 403 |
| `DELETE` | `/toilets/{id}` | `deleteToilet` | `basicAuth` (rôle **ADMIN**) | 204, 404, 403 |

### Paramètres de `GET /toilets`

| Paramètre | Type | Défaut | Contraintes |
|---|---|---|---|
| `page` | integer | 0 | `minimum: 0` |
| `size` | integer | 20 | `minimum: 1`, `maximum: 100` |
| `latitude` | number (double) | — | `-90` … `90` |
| `longitude` | number (double) | — | `-180` … `180` |

Si `latitude` **et** `longitude` sont fournis, les résultats sont **triés par proximité**.

---

## 3. Endpoints clients

| Méthode | Chemin | `operationId` | Codes |
|---|---|---|---|
| `GET` | `/customers` | `getCustomers` | 200 |
| `POST` | `/customers` | `createCustomer` | 201 |
| `GET` | `/customers/{login}` | `getCustomerByLogin` | 200 |

---

## 4. Schémas partagés

| Schéma | Champs obligatoires | Détails |
|---|---|---|
| `Toilet` | `libelle`, `adress`, `position` | `id` et `distance` en **lecture seule** (calculée côté service) |
| `Adress` | `codePostal` | `numero`, `voie`, `ville`, `pays` |
| `Graduation` | — | `proprete`, `accueil`, `espace` : entier **0-10** |
| `GeoPosition` | `latitude`, `longitude` | double, borné |
| `Customer` | `login` | `password`, `nom`, `prenom`, `adress` |
| `ApiError` | `status`, `message`, `timestamp` | Réponse d'erreur standard |

> ⚠️ Le cahier des charges V1.0 parlait d'une note **de 0 à 5** ; le contrat OpenAPI actuel borne les critères à **0-10**. Voir [modele-de-donnees.md](modele-de-donnees.md).

---

## 5. Comment faire évoluer le contrat

1. Modifier le YAML dans `src/main/resources/api/`.
2. Régénérer : la génération OpenAPI produit les interfaces dans `target/generated-sources/openapi/`.
3. Aligner l'implémentation (`web/ToilettesApiImpl`, `web/CustomerApiImpl`).
4. **Mettre à jour la collection Bruno** correspondante dans `Bruno/WcPregnant/WC Pregnant API/`.

---

## 6. Tests d'API (Bruno)

Collection : `Bruno/WcPregnant/WC Pregnant API/`

| Fichier `.bru` | Scénario |
|---|---|
| `local.bru` | Variables d'environnement locales |
| `getNearsetToilettes.bru` / `get Nearset.bru` | Recherche de proximité |
| `lireToilet.bru` | Lecture par ID |
| `addToilette.bru` | Création |
| `updateToilette.bru` / `Put Newx Data.bru` | Mise à jour |
| `deleteToilette.bru` | Suppression |
| `folder.bru` | Configuration du dossier |
