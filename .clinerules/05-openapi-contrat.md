---
paths:
  - "WcPregnantService/src/main/resources/api/**"
  - "Bruno/**"
---

# Contrat d'API (OpenAPI)

- Le YAML dans `src/main/resources/api/` (`ToiletAPI.yaml`, `CustomerAPI.yaml`, `ObjectDefinitions.yaml`) est la **source de vérité** : les interfaces `web/api/*` et les DTO `web/dto/*` en sont dérivés.
- Toute évolution d'endpoint se fait **d'abord dans le YAML**, puis le code d'implémentation est aligné.
- Conventions : `openapi: 3.0.0`, `operationId` en camelCase, descriptions en français, `servers` local + cluster.
- Contraintes de validation dans le schéma (`minimum`, `maximum`, `maxLength`) pour latitude/longitude, code postal, taille de page.
- Codes de retour : 201 pour création, 204 pour suppression, 404 `ApiError` pour ressource absente, 400 pour validation.
- Après modification du contrat, mettre à jour la collection Bruno correspondante dans `Bruno/WcPregnant/WC Pregnant API/`.
