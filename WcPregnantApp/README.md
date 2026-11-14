# WcPregnantApp

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.0.3.

## Development server

To start a local development server, run:

```bash
ng serve --proxy-config src/proxy.conf.json
```

The proxy forwards `/toilets*` to the Spring Boot API on `http://localhost:8081`, so the backend
must be running. `src/environments/environment.ts` holds the API base path (`/toilets`) and the
default search position.

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The
application will automatically reload whenever you modify any of the source files. Sign in with the
`dev` demo accounts (`manu / manu` for read access, `admin / admin` for create/update/delete).

> Note: the app uses the `zone.js` polyfill (declared in the `build` and `test` targets of
> `angular.json`). Without it, `TestBed` and `NgZone` fail with `NG0908` and the page renders blank.

### Search position and developer mode

By default the search starts from the **browser geolocation** (`MapService.getCurrentLocation()`):
after signing in, the toolbar shows a "Position de recherche" chip and the map is centred on that
point. If the browser refuses geolocation, the app falls back to `environment.defaultPosition`
and displays a notice.

The latitude/longitude inputs are only exposed in **developer mode**, toggled from
`⚙️ Paramètres`. The preference is persisted by `SettingsService` in `localStorage`
(key `wc-pregnant-developer-mode`).

| Mode | Coordinates fields | Search origin |
|---|---|---|
| Default | hidden | browser geolocation, fallback `environment.defaultPosition` |
| Developer | visible | manual entry (useful to debug a specific point) |


## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

```bash
ng test
```

The runner uses the custom `karma.conf.js` referenced in `angular.json`. Because the development
machine has no Google Chrome, `CHROME_BIN` is pointed at Microsoft Edge (`msedge.exe`), which is
Chromium-based and supports headless mode:

```js
const DEFAULT_BROWSER = 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe';
process.env.CHROME_BIN = process.env.CHROME_BIN || DEFAULT_BROWSER;
```

Override it with the `CHROME_BIN` environment variable if Edge is installed elsewhere, e.g.
`$env:CHROME_BIN = 'C:\Program Files\Microsoft\Edge\Application\msedge.exe'` before running `ng test`.
Tests run once (`singleRun: true`), so `ng test --no-watch` is not required.

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
