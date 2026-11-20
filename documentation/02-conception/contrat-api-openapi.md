# Contrat API (OpenAPI)

> **Source de vérité** : les fichiers YAML de [`WcPregnantService/src/main/resources/api/`](../../WcPregnantService/src/main/resources/api/). Les interfaces `web/api/*` et les DTO `web/dto/*` en sont **dérivés automatiquement** et ne doivent jamais être édités à la main.

---

## 1. Fichiers du contrat

| Fichier | Contenu |
|---|---|
| [`ToiletAPI.yaml`](../../WcPregnantService/src/main/resources/api/ToiletAPI.yaml) | Endpoints toilettes (v2.0.0) |
| [`CustomerAPI.yaml`](../../WcPregnantService/src/main/resources/api/CustomerAPI.yaml) | Endpoints clients (v1.0.0) |
| [`AuthAPI.yaml`](../../WcPregnantService/src/main/resources/api/AuthAPI.yaml) | Réinitialisation du mot de passe (v1.0.0) |
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
| `PUT` | `/toilets/{id}/grade` | `gradeToilet` | `basicAuth` (rôle **USER**) | 200, 400, 401, 403, 404 |

### Notation : `PUT /toilets/{id}/grade`

| Élément | Détail |
|---|---|
| Rôle requis | `USER` (contrairement à `PUT /toilets/{id}`, réservé à `ADMIN`) |
| Corps | `Graduation` : `proprete`, `accueil`, `espace` |
| Bornage | chaque critère est ramené dans l'intervalle **`[0, 10]`** |
| Effet | la note est **ajoutée** à `graduationHistory` (de la plus récente à la plus ancienne) |
| Note affichée | moyenne de **toutes** les entrées de l'historique, arrondie à l'entier le plus proche |
| Réponse | la `Toilet` complète, `grade` recalculé et `graduationHistory` horodaté |

> La note agrégée n'est jamais stockée telle quelle : elle est **recalculée à chaque dépôt** à partir de l'historique, ce qui évite toute dérive entre `grade` et `graduationHistory`.

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

## 4. Endpoints d'authentification (réinitialisation du mot de passe)

| Méthode | Chemin | `operationId` | Auth | Codes |
|---|---|---|---|---|
| `POST` | `/auth/password-reset/request` | `requestPasswordReset` | **aucune** (flux de récupération) | 200, 400, 404 |
| `POST` | `/auth/password-reset/confirm` | `confirmPasswordReset` | **aucune** | 200, 400, 404 |

| Élément | Comportement |
|---|---|
| Identifiant accepté | nom d'utilisateur **ou** adresse e-mail, recherche insensible à la casse |
| Code | 6 chiffres générés par `SecureRandom`, **hachés en BCrypt** avant stockage, TTL `auth.reset.code-ttl-seconds` (900 s) |
| Mode démonstration | si `auth.mail.enabled=false` (local, tests), le code est renvoyé dans `demoCode` ; sinon il est envoyé par SMTP et la réponse ne contient que le message |
| Confirmation | le code doit être valide et non expiré ; le nouveau mot de passe doit comporter **8 caractères minimum** (72 maximum) ; le code est consommé |
| Erreurs | `400` pour un champ invalide ou un code erroné, `404` pour un compte inconnu, un code absent ou expiré — le champ `message` porte l'explication |

> La chaîne `/auth/**` est explicitement `permitAll()` dans `AppSecurityConfiguration` : un utilisateur qui a oublié son mot de passe ne peut pas s'authentifier. `/error` l'est aussi, afin que les `400`/`401` ne soient pas transformés en redirection `302` vers `/login`.

---

## 5. Schémas partagés

| Schéma | Champs obligatoires | Détails |
|---|---|---|
| `Toilet` | `libelle`, `adress`, `position` | `id` et `distance` en **lecture seule** (calculée côté service) |
| `Adress` | `codePostal` | `numero`, `voie`, `ville`, `pays` |
| `Graduation` | — | `proprete`, `accueil`, `espace` : entier **0-10** |
| `GeoPosition` | `latitude`, `longitude` | double, borné |
| `Customer` | `login` | `password`, `nom`, `prenom`, `adress` |
| `ApiError` | `status`, `message`, `timestamp` | Réponse d'erreur standard |
| `PasswordResetRequest` | `login` | 3-60 caractères, identifiant ou e-mail |
| `PasswordResetAcknowledgement` | — | `message`, `email`, `expiresInSeconds`, `demoCode` (mode démonstration) |
| `PasswordResetConfirmRequest` | `login`, `code`, `newPassword` | `code` 6 chiffres, `newPassword` 8-72 caractères |
| `PasswordResetResult` | — | `message` + `login` du compte mis à jour |

> ⚠️ Le cahier des charges V1.0 parlait d'une note **de 0 à 5** ; le contrat OpenAPI actuel borne les critères à **0-10**. Voir [modele-de-donnees.md](modele-de-donnees.md).

---

## 6. Comment faire évoluer le contrat

1. Modifier le YAML dans `src/main/resources/api/`.
2. Régénérer : la génération OpenAPI produit les interfaces dans `target/generated-sources/openapi/`.
3. Aligner l'implémentation (`web/ToilettesApiImpl`, `web/CustomerApiImpl`).
4. **Mettre à jour la collection Bruno** correspondante dans `Bruno/WcPregnant/WC Pregnant API/`.

---

## 7. Tests d'API (Bruno)

Collection : `Bruno/WcPregnant/WC Pregnant API/`

| Fichier `.bru` | Scénario |
|---|---|
| `local.bru` | Variables d'environnement locales |
| `getNearsetToilettes.bru` / `get Nearset.bru` | Recherche de proximité |
| `lireToilet.bru` | Lecture par ID |
| `addToilette.bru` | Création |
| `updateToilette.bru` / `Put Newx Data.bru` | Mise à jour |
| `deleteToilette.bru` | Suppression |
| `gradeToilette.bru` | Dépôt d'une note (`PUT /toilets/{id}/grade`) avec le compte `manu/manu` (rôle `USER`) |
| `authRequestPasswordReset.bru` | Demande d'un code de réinitialisation (`POST /auth/password-reset/request`) |
| `authConfirmPasswordReset.bru` | Confirmation du code et changement de mot de passe (`POST /auth/password-reset/confirm`) |
| `folder.bru` | Configuration du dossier |
