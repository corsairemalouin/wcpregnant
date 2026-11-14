---
name: proximity-search
description: Travail sur le calcul de distance et la recherche de proximité (Haversine, DistanceCalculator, seuils maxDistance/maxResult, GeoPosition). Utiliser pour "trier par distance", "ajouter une stratégie de distance", "optimiser la recherche géographique", "bug de distance null".
---

# Recherche de proximité

## Mécanisme

1. `ToiletteServiceImpl.getNearestToilets(GeoPosition)` charge les toilettes,
2. enrichit chacune via `enrichWithDistance` (méthode privée),
3. filtre `distance < config.getMaxDistance()`,
4. trie croissant (`Comparator.nullsLast`),
5. limite à `config.getMaxResult()`.

## Invariants

- Unité : **mètres** (`EARTH_RADIUS_KM * c * 1000`).
- Position ou coordonnée `null` → `Double.MAX_VALUE` renvoyé par le calculateur, converti en `distance = null` côté service.
- La distance n'est **pas** persistée : elle est calculée à la demande.
- Toute nouvelle algorithmique s'ajoute comme implémentation de `DistanceCalculator` (`@Component`), sans modifier le service (OCP).

## Optimisations envisageables

- Filtre SQL `bbox` (latitude/longitude ± delta) avant le tri, au lieu de `findAll()`.
- Index PostgreSQL sur les coordonnées, ou `PostGIS` si le projet bascule sur `geography`.
- Cache des positions chargées pour les requêtes répétées.

## Tests exigés

- Distance connue (2 points référencés) avec tolérance.
- Points identiques → 0.
- Coordonnées null → `Double.MAX_VALUE`.
- Service : exclusion des toilettes au-delà de `maxDistance`, résultat trié, limite appliquée.
