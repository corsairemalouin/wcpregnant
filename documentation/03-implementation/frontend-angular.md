> 📂 **Documentation** — [index](../README.md) · Phase **3 — Implémentation** · Règles de code : [conventions-de-code.md](conventions-de-code.md)

# WcPregnantApp — Guide du frontend Angular

Ce projet a été généré avec [Angular CLI](https://github.com/angular/angular-cli) version 20.0.3.

## Serveur de développement

Pour démarrer un serveur de développement local, exécutez :

```bash
ng serve --proxy-config src/proxy.conf.json
```

Le proxy transfère les requêtes `/toilets*`, `/actuator*` et `/agent*` vers l'API Spring Boot sur
`http://localhost:14007` (port dédié au projet) : le backend doit donc être démarré. `src/environments/environment.ts`
contient le chemin de base de l'API (`/toilets`), celui des endpoints agentiques (`/agent`) et la
position de recherche par défaut.

Une fois le serveur lancé, ouvrez le navigateur sur `http://localhost:4200/`. L'application se
recharge automatiquement à chaque modification d'un fichier source. Connectez-vous avec les comptes
de démonstration du profil `dev` (`manu / manu` en lecture seule, `admin / admin` pour créer,
modifier et supprimer).

> Remarque : l'application utilise le polyfill `zone.js` (déclaré dans les cibles `build` et `test`
> de `angular.json`). Sans lui, `TestBed` et `NgZone` échouent avec `NG0908` et la page s'affiche
> vide.

### Interface web servie par le backend (port 14007)

Le build Angular est embarqué dans le service Spring : ouvrir `http://localhost:14007/` affiche
directement le menu de l'application, sans `ng serve` ni page de connexion Spring.

```powershell
npm run build              # produit dist/WcPregnantApp/browser
cd ..\WcPregnantService
mvn spring-boot:run        # sert la SPA sur / (port 14007)
```

| Environnement | Emplacement du build |
|---|---|
| Local | `WcPregnantApp/dist/WcPregnantApp/browser`, résolu automatiquement par `FrontendController` |
| Docker / Kubernetes | `/app/frontend-dist`, fixé par la variable `FRONTEND_DIST_PATH` |

`FrontendController` sert `index.html` sur `/`, les fichiers racine (`main-*.js`,
`styles-*.css`, `favicon.ico`) et les chemins imbriqués d'**un niveau**
(`/media/marker-icon.png`, motif `/{dir}/{file}`), avec le type MIME adapté ; toute tentative
d'évasion de chemin (`..`, `%2e%2e`) ou chemin plus profond renvoie `404`. Ces règles sont
couvertes par `WcPregnantService/src/test/java/org/manu/wcpregnant/web/FrontendControllerTest.java`
(8 tests) et par deux tests d'intégration de `SecurityIntegrationTest` qui vérifient que
`/` et `/index.html` ne déclenchent plus la chaîne `formLogin`.

> Le proxy `src/proxy.conf.json` reste utile en développement : il cible toujours
> `http://localhost:14007` pour `/toilets*`, `/actuator*` et `/agent*`.

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

## Assistant agentique (Tool Calling)

Le panneau **Assistant agentique** (bouton en haut de la page après la connexion) permet de poser
une question en langage naturel — par exemple « toilettes accessibles avec table à langer dans un
rayon de 500 m » — et de laisser l'agent du backend choisir et exécuter les outils métier.

### Comment ça marche

1. `AgentChatComponent` envoie `POST /agent/chat` avec `{ message, position }` via `AgentService`
   (la position est celle de la recherche : géolocalisation ou position de référence).
2. Le serveur orchestre la boucle **Reason → Act → Observe** contre le modèle servi localement
   (Strata AI) et renvoie `answer`, `model`, `iterations` et la trace `toolCalls`.
3. Le composant affiche les étapes de raisonnement, la synthèse du modèle et des cartes
   d'équipements ; les résultats remontent à `HomeComponent` par l'`@Output() results`.
4. `HomeComponent` remplace la liste des résultats et passe l'équipement mis en avant au composant
   carte (`[highlight]`), qui centre la carte et dessine un marqueur distinctif.

| Fichier | Contenu |
|---|---|
| `src/app/models/agent.model.ts` | contrats `AgentChatRequest`, `AgentChatResponse`, `AgentToolCallTrace`, `AgentToolResult` alignés sur `AgentAPI.yaml` |
| `src/app/services/agent.service.ts` | `chat(request)` et `getTools()`, en-tête `Authorization` repris de `AuthService` |
| `src/app/components/agent-chat/` | `agent-chat.ts` / `.html` / `.css` : formulaire, suggestions, fil d'étapes, cartes de résultats |
| `src/app/components/map/map.ts` | `@Input() highlight` et méthode `renderHighlight()` (marqueur `agent-highlight`) ; API de navigation `focusToilet(toilet)` / `focusToiletById(id)` (centrage + ouverture du tooltip) |

### Socle agentique autonome (`src/app/agent/`)

Le dossier `src/app/agent/` isole la brique de conversation agentique dans un module autonome,
prêt à être branché sur de nouveaux écrans sans dépendre du composant `agent-chat` existant.

| Fichier | Contenu |
|---|---|
| `src/app/agent/agent.models.ts` | `AgentRequest` (`message`, `latitude?`, `longitude?`) et `AgentResponse` (`reply`, `steps: string[]`, `toilettes: any[]`) |
| `src/app/agent/agent.service.ts` | `askAgent(request): Observable<AgentResponse>` — `POST` sur l'endpoint de conversation ; la base d'URL vient de `environment.agentApiUrl` (repli `/api/agent`), jamais codée en dur |
| `src/app/agent/agent.service.spec.ts` | 5 tests : endpoint appelé, payload sans position, émission de la réponse, propagation d'une erreur HTTP, URL relative |
| `src/app/agent/agent-chat.component.{ts,html,scss}` | Bulle flottante de conversation pilotée par des Signals (voir ci-dessous) |
| `src/app/agent/agent-chat.component.spec.ts` | 12 tests : ouverture/fermeture, envoi de la question, timeline pendant la réflexion, mini-cartes, émission `selectToilette`, erreurs 403/503 |

> ⚠️ **Coexistence** : `src/app/services/agent.service.ts` reste le client aligné sur
> `AgentAPI.yaml` (`AgentChatRequest` / `AgentChatResponse`, trace `toolCalls`) utilisé par
> `AgentChatComponent` (panneau inline). Le contrat du dossier `agent/` est une **forme simplifiée**
> (position aplatie en `latitude`/`longitude`, étapes en `string[]`) et porte sa propre bulle
> flottante `wc-agent-chat-fab` : il ne remplace pas le service existant.

### Bulle flottante agentique pilotée par des Signals

`src/app/agent/agent-chat.component.ts` (classe `AgentChatComponent`, sélecteur
`wc-agent-chat-fab`) est une **bulle flottante** fixée en bas à droite de l'écran : un lanceur
(`.agent-bubble`) qui ouvre ou ferme une fenêtre de conversation (`.agent-window`), avec un champ
de saisie, des suggestions, une timeline des étapes et des mini-cartes cliquables.

| Élément | Implémentation |
|---|---|
| Question, ouverture, mode « réflexion », erreur, synthèse, étapes, résultats | `signal` (`question`, `open`, `busy`, `error`, `reply`, `steps`, `toilettes`) |
| Timeline affichée | `computed` : étapes génériques (« Analyse de la position… », « Recherche géospatiale des équipements… », « Interrogation du modèle local… ») pendant l'appel, étapes réellement renvoyées par l'agent ensuite |
| Nombre de résultats, bouton « Envoyer » activable | `computed` (`resultCount`, `canAsk`) |
| Position de recherche | `input<GeoPosition \| null>(null)` (input signal-based) |
| Sélection d'un équipement | `@Output() selectToilette = new EventEmitter<any>()` émis par les mini-cartes |
| Saisie sans `FormsModule` | `[value]="question()"` + `(input)="question.set(questionInput.value)"` |

Aucune logique métier n'y figure : le composant délègue à `AgentService.askAgent()` et capture
les erreurs 403/503 pour les afficher dans la fenêtre. Il est **branché dans `HomeComponent`**
en complément du panneau inline, après l'`<wc-agent-chat>` existant :

```html
<wc-agent-chat-fab [position]="searchPosition"
                   (selectToilette)="selectFromAgent($event)"></wc-agent-chat-fab>
```

Les deux entrées agentiques coexistent : le panneau inline reste le fil de discussion détaillé
(trace des outils), la bulle flottante est un accès permanent en bas à droite de l'écran. Les
mini-cartes de la bulle sélectionnent l'équipement affiché dans la fiche détail et mis en avant
sur la carte Leaflet (`selectFromAgent`).

### Centrage de la carte Leaflet depuis l'agent

La bulle flottante n'est pas isolée du parent : elle est **branchée dans `HomeComponent`**, le
composant qui héberge la carte Leaflet, et l'événement `(selectToilette)` déclenche maintenant un
**centrage de la carte** sur les coordonnées de l'équipement choisi, avec ouverture du tooltip de
son marqueur.

`WcMapComponent` expose pour cela une API de navigation publique :

| Méthode | Effet | Retour |
|---|---|---|
| `focusToilet(toilet: Toilet \| null)` | `panTo` sur `position.latitude`/`longitude`, dessin du halo de mise en avant, ouverture du tooltip du marqueur existant (ou du halo si l'équipement n'est pas dans la liste affichée) | `true` si la carte a été recentrée |
| `focusToiletById(id: number)` | résout l'identifiant dans `toilets` puis délègue à `focusToilet` | `true` si l'identifiant existe |

Pour rendre cela possible, les marqueurs sont désormais **indexés par identifiant** dans un
`Map<number, L.Marker>` interne (`markersById`), alimenté par `renderMarkers()` et vidé dans
`ngOnDestroy()` : les marqueurs restent atteignables directement sans parcourir le tableau.

Le parent récupère l'instance de la carte via un `@ViewChild` et réagit à l'événement :

```html
<wc-map #agentMap [toilets]="toilets" [origin]="searchPosition" [highlight]="agentHighlight"></wc-map>
<wc-agent-chat-fab [position]="searchPosition"
                   (selectToilette)="selectFromAgent($event)"></wc-agent-chat-fab>
```

```ts
@ViewChild('agentMap') map: WcMapComponent | undefined;

selectFromAgent(toilet: Toilet): void {
  this.selectedToilet = toilet;
  this.agentHighlight = toilet;

  if (this.map && !this.map.focusToilet(toilet)) {
    this.notice = 'Impossible de centrer la carte : les coordonnées de cette toilette sont absentes.';
  }
}
```

La méthode est **défensive** : le payload renvoyé par les outils de l'agent peut être partiel
(coordonnées absentes) et la carte n'est pas encore créée avant la première connexion ; dans ces
cas `focusToilet` ne fait rien et retourne `false`, sans jamais casser le rendu existant.


### Configuration

Aucune URL d'inférence n'apparaît dans le frontend : le proxy Angular route `/agent` vers le
backend, et le backend pointe vers le serveur local via `AGENT_INFERENCE_BASE_URL`, `AGENT_MODEL`
et `AGENT_TIMEOUT_SECONDS`. Pour la démonstration :

```bash
# 1. serveur d'inférence local (API compatible OpenAI) sur 127.0.0.1:8000
# 2. backend
mvn -q spring-boot:run
# 3. frontend
ng serve --proxy-config src/proxy.conf.json
```

Si le serveur d'inférence n'est pas joignable, l'API répond **503** et le panneau affiche
« Le serveur d'inférence local est injoignable » ; une opération d'écriture sans rôle `ADMIN`
renvoie **403** avec un message explicite.

### Tests

`agent.service.spec.ts`, `agent-chat.spec.ts`, `home.spec.ts` et `map.spec.ts` couvrent les appels
HTTP, la construction des étapes, la remontée des résultats, la gestion des erreurs 403/503 et le
rendu du marqueur de mise en avant **et le centrage de la carte** (`focusToilet`, `focusToiletById`,
indexation des marqueurs par identifiant). Le socle autonome est testé par
`src/app/agent/agent.service.spec.ts` (endpoint appelé, payload sans position, réponse émise,
erreur HTTP propagée, URL relative) et par `src/app/agent/agent-chat.component.spec.ts`
(ouverture/fermeture de la bulle, envoi de la question positionnée, timeline pendant la réflexion,
mini-cartes rendues, émission `selectToilette`, erreurs 403/503, formats de distance).



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

