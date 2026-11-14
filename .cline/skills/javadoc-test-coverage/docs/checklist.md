# Checklist de progression par classe

À cocher au fur et à mesure ; garder cette liste dans la conversation jusqu'à la fin de la tâche.

```
Classe : ToiletteServiceImpl
- [ ] getAllToilets   → Javadoc ✓  TU (nominal, null page/size, pagination) ✓
- [ ] getToiletById   → Javadoc ✓  TU (nominal, notFound, null id) ✓
- [ ] addToilet       → Javadoc ✓  TU (nominal, null, propagation save) ✓
- [ ] updateToilet    → Javadoc ✓  TU (nominal, notFound, null) ✓
- [ ] deleteToilet    → Javadoc ✓  TU (nominal, notFound, null) ✓
- [ ] getNearestToilets → Javadoc ✓  TU (nominal, filtre maxDistance, null position, distance null) ✓
- [ ] enrichWithDistance (privée, couverte indirectement) ✓
```

## Scénarios types par couche

| Couche | Scénarios exigés |
|---|---|
| Service CRUD | nominal, ressource absente, argument null, propagation `repository.save` |
| Service proximité | résultats triés, filtre `maxDistance`, résultat limité à `maxResult`, position null, position de la toilette null |
| Mapper MapStruct | round-trip BO → DAO → BO, champs null, normalisation du libellé |
| Strategy (Haversine) | valeurs connues, coordonnées identiques (distance 0), null |
| Controller | 201 création, 404 absence, 400 validation, 403 sans rôle |

## Pièges connus dans ce projet

- Le mapper normalise parfois le `libelle` : n'assertion pas l'égalité stricte du libellé sur l'objet sauvegardé.
- `distance == Double.MAX_VALUE` est converti en `null` : tester ce cas.
- JaCoCo exclut `**/*Mock*` : ne pas créer de classes de test commençant par `Mock`.
