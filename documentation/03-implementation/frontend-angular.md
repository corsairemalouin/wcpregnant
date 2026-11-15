> 📂 **Documentation** — [index](../README.md) · Phase **3 — Implémentation** · Règles de code : [conventions-de-code.md](conventions-de-code.md)

# WcPregnantApp — Guide du frontend Angular

Ce projet a été généré avec [Angular CLI](https://github.com/angular/angular-cli) version 20.0.3.

## Serveur de développement

Pour démarrer un serveur de développement local, exécutez :

```bash
ng serve --proxy-config src/proxy.conf.json
```

Le proxy transfère les requêtes `/toilets*` vers l'API Spring Boot sur `http://localhost:8081` :
le backend doit donc être démarré. `src/environments/environment.ts` contient le chemin de base de
l'API (`/toilets`) et la position de recherche par défaut.

Une fois le serveur lancé, ouvrez le navigateur sur `http://localhost:4200/`. L'application se
recharge automatiquement à chaque modification d'un fichier source. Connectez-vous avec les comptes
de démonstration du profil `dev` (`manu / manu` en lecture seule, `admin / admin` pour créer,
modifier et supprimer).

> Remarque : l'application utilise le polyfill `zone.js` (déclaré dans les cibles `build` et `test`
> de `angular.json`). Sans lui, `TestBed` et `NgZone` échouent avec `NG0908` et la page s'affiche
> vide.

### Position de recherche et mode développeur

Par défaut, la recherche part de la **géolocalisation du navigateur** (`MapService.getCurrentLocation()`) :
après la connexion, la barre d'outils affiche une puce « Position de recherche » et la carte est
centrée sur ce point. Si le navigateur refuse la géolocalisation, l'application se replie sur
`environment.defaultPosition` et affiche une notification.

Les champs de latitude/longitude n'apparaissent qu'en **mode développeur**, activable depuis
`⚙️ Paramètres`. Cette préférence est conservée par `SettingsService` dans `localStorage`
(clé `wc-pregnant-developer-mode`).

| Mode | Champs de coordonnées | Origine de la recherche |
|---|---|---|
| Par défaut | masqués | géolocalisation du navigateur, repli sur `environment.defaultPosition` |
| Développeur | visibles | saisie manuelle (utile pour debugger un point précis) |


## Génération de code (scaffolding)

Angular CLI propose des outils de génération de code. Pour créer un nouveau composant, exécutez :

```bash
ng generate component component-name
```

Pour la liste complète des schematics disponibles (`components`, `directives`, `pipes`, etc.) :

```bash
ng generate --help
```

## Build

Pour construire le projet :

```bash
ng build
```

Le projet est compilé et les artefacts sont écrits dans le dossier `dist/`. Par défaut, le build de
production optimise l'application en performance et en rapidité.

## Exécuter les tests unitaires

Pour lancer les tests unitaires avec le runner [Karma](https://karma-runner.github.io) :

```bash
ng test
```

Le runner utilise le `karma.conf.js` personnalisé référencé dans `angular.json`. La machine de
développement n'ayant pas Google Chrome, `CHROME_BIN` pointe vers Microsoft Edge (`msedge.exe`),
qui est basé sur Chromium et supporte le mode headless :

```js
const DEFAULT_BROWSER = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
process.env.CHROME_BIN = process.env.CHROME_BIN || DEFAULT_BROWSER;
```

Si Edge est installé ailleurs, remplacez la variable `CHROME_BIN` avant `ng test`, par exemple
`$env:CHROME_BIN = 'C:\Program Files\Microsoft\Edge\Application\msedge.exe'`. Les tests s'exécutent
une seule fois (`singleRun: true`), `ng test --no-watch` n'est donc pas nécessaire.

## Tests de bout en bout

Pour les tests de bout en bout (e2e) :

```bash
ng e2e
```

Angular CLI n'intègre pas de framework de tests e2e par défaut ; choisissez celui qui correspond à
vos besoins.

## Ressources supplémentaires

Pour en savoir plus sur l'utilisation d'Angular CLI, consultez la [documentation de référence des
commandes Angular CLI](https://angular.dev/tools/cli).

---

## Conventions de développement (règles du workspace)

La version exécutable de ces règles est [`.clinerules/06-frontend-angular.md`](../../.clinerules/06-frontend-angular.md).

| Règle | Détail |
|---|---|
| Composants | **standalone** (`standalone: true`), pas de module racine mutualisé |
| Services | `@Injectable({ providedIn: 'root' })`, retour `Observable<T>` typé — jamais `Observable<any>` |
| Modèles | `src/app/models/*.model.ts`, interfaces alignées sur les DTO du contrat OpenAPI. ⚠️ `Adress` est orthographié ainsi côté backend : **ne pas « corriger »** |
| URL de l'API | **jamais codée en dur** : passer par `environments/` ou une variable injectée |
| Carte | Leaflet (`leaflet` + `@types/leaflet`), encapsulée dans un composant dédié, avec **destruction propre** de l'instance |
| Nommage | composants `PascalCase`, fichiers `kebab-case.component.ts`, services `XxxService` |
| Accessibilité | `alt` sur les images, `aria-label` sur les boutons de carte, libellés de formulaires |
| Tests | chaque nouveau composant ou service vient avec son `*.spec.ts` (Karma + Jasmine) |

**Validation** : `npm run build` (type-check) puis `npm run test`. Voir [strategie-de-tests.md](../04-tests/strategie-de-tests.md).

