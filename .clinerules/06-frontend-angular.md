---
paths:
  - "WcPregnantApp/**"
---

# Frontend Angular

- Angular 20, TypeScript strict : **composants standalone** (`standalone: true`), pas de module racine mutualisé.
- Services : `@Injectable({ providedIn: 'root' })`, retour `Observable<T>` typé — jamais `Observable<any>`.
- Modèles de données dans `src/app/models/*.model.ts`, interfaces alignées sur les DTO du contrat OpenAPI (attention : `Adress` est orthographié ainsi côté backend, ne pas "corriger").
- **Ne jamais coder en dur l'URL de l'API** : utiliser un environnement (`environments/`) ou une variable injectée.
- Carte : Leaflet via `leaflet` + `@types/leaflet`, encapsulé dans un composant dédié, destruction propre des instances.
- Nommage : composants `PascalCase`, fichiers `kebab-case.component.ts`, services `XxxService`.
- Accessibilité : `alt` sur les images, `aria-label` sur les boutons de carte, libellés de formulaires.
- Chaque nouveau composant ou service doit venir avec son `*.spec.ts` (Karma + Jasmine).
- Validation : `npm run build` (type-check) puis `npm run test`.
