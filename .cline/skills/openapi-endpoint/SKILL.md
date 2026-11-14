---
name: openapi-endpoint
description: Crée ou modifie un endpoint REST à partir du contrat OpenAPI du projet (YAML dans src/main/resources/api) puis aligne l'implémentation Spring et la collection Bruno. Utiliser pour "ajouter une route", "modifier l'API", "nouveau DTO", "faire évoluer le contrat".
---

# Ajouter / modifier un endpoint OpenAPI

## Ordre obligatoire

1. **Modifier le YAML** : `WcPregnantService/src/main/resources/api/ToiletAPI.yaml` (ou `CustomerAPI.yaml`, définitions communes dans `ObjectDefinitions.yaml`).
2. **Générer** : `mvn -q compile` régénère `web/api/*` et `web/dto/*` (ne jamais éditer ces fichiers à la main).
3. **Implémenter** dans `web/XxxApiImpl` : respecter la signature générée, valider les entrées, retourner `ResponseEntity`.
4. **Déporter la logique** dans un service (`service/` + `service/impl/`) et, si nécessaire, un repository/mapper.
5. **Tester** : controller (codes HTTP, 403 selon rôle) + service (scénarios métier).
6. **Documenter** : ajouter la requête dans `Bruno/WcPregnant/WC Pregnant API/`.

## Gabarit d'endpoint

Copier et adapter `templates/endpoint.yaml` :

```yaml
  /toilets/{id}:
    get:
      tags: [Toilet]
      summary: Récupère une toilette par identifiant
      operationId: getToiletById
      parameters:
        - name: id
          in: path
          required: true
          schema: { type: integer, format: int64, minimum: 1 }
      responses:
        '200': { description: Toilette trouvée }
        '404': { description: Toilette inexistante }
```

## Conventions

- Chemins au pluriel, sans suffixe `.json` ; base `/api/wcpregnant`.
- Pagination : `page` (≥0, défaut 0) et `size` (1–100, défaut 20).
- Rôle : lecture `hasRole('USER')`, écriture `hasRole('ADMIN')` via `@PreAuthorize`.
- Erreurs : schéma `ApiError` (400/404/500).
