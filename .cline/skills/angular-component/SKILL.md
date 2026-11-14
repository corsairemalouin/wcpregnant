---
name: angular-component
description: Crée ou fait évoluer un composant, un service ou un modèle Angular du frontend WcPregnantApp (Leaflet, HttpClient, standalone components, specs Karma). Utiliser pour "ajouter une page", "créer un composant", "brancher l'API", "écrire un test Angular".
---

# Frontend Angular (WcPregnantApp)

## Structure

```
src/app/
├── components/     composants standalone
├── services/       services @Injectable({ providedIn: 'root' })
├── models/         interfaces alignées sur les DTO backend
├── app.config.ts   providers (provideHttpClient, etc.)
└── app.routes.ts   routing
```

## Règles

- Composants **standalone**, typage strict, pas de `any`.
- Les appels API passent par un service ; l'URL vient d'un environnement, jamais d'une chaîne en dur.
- Noms de méthodes de service en anglais : `getNearbyToilets`, `addToilet`.
- Les modèles reflètent le backend **tel quel** (ex. `Adress`, `libelle`, `distance` en mètres).
- Leaflet : créer l'instance dans `ngAfterViewInit`, la détruire dans `ngOnDestroy`, gérer l'absence de position.
- Tests : `*.spec.ts` avec `TestBed`, `componentFactory.createComponent` et mocks de `HttpClient` (`provideHttpClient` + spy).

## Gabarit

Copier `templates/component.ts` et `templates/service.ts`, puis adapter les noms.

## Vérification

```powershell
cd WcPregnantApp
npm run build
npm run test
```
