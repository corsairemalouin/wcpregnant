> 📂 **Documentation** — [index](../README.md) · Phase **2 — Conception** · Documents liés : [modele-de-donnees.md](modele-de-donnees.md), [diagrammes.md](diagrammes.md), [contrat-api-openapi.md](contrat-api-openapi.md)

# Architecture et conception — WcPregnant Service

## 1. Vue d'ensemble

`WcPregnantService` est l'API backend qui fournit les services de localisation de toilettes accessibles aux femmes enceintes.

| Profil | Usage |
|---|---|
| **Administrateur** | Gère les toilettes depuis l'IHM |
| **Propriétaire de toilette** | Dépose une demande via le support, traitée par un administrateur |
| **Femme enceinte** | Consulte la carte et la liste des toilettes les plus proches |

---

## 2. Pattern d'architecture : couches (Clean Architecture)

```text
┌─────────────────────────────────────┐
│        Couche API REST (web)        │  ← Controllers (ToilettesApiImpl, CustomerApiImpl)
├─────────────────────────────────────┤
│        Couche métier (service)      │  ← Interfaces + implémentations (ToiletteServiceImpl)
├─────────────────────────────────────┤
│        Couche d'accès (bdd)         │  ← Repositories Spring Data JPA
├─────────────────────────────────────┤
│        Base de données (PostgreSQL) │  ← Entités JPA (ToiletDAO, …)
└─────────────────────────────────────┘
```

Composants transverses :

- **Mappers** — MapStruct pour la conversion DTO ↔ DAO ;
- **Stratégie de distance** — `DistanceCalculator` / `HaversineDistanceCalculator` ;
- **Utils** — calculs géographiques (`GeoPositionUtils`) ;
- **Configuration** — `ToiletteServiceConfig`, `AppSecurityConfiguration` ;
- **Sécurité** — Spring Security (Basic auth + RBAC).

---

## 3. Composants clés

### 3.1 Modèles de domaine

**`Toilet`** (DTO généré par OpenAPI)

- Champs : `id`, `libelle`, `adress`, `grade`, `position`, `distance` ;
- Utilisé dans les réponses REST ;
- La `distance` est calculée par le service et n'est **jamais** persistée.

**`ToiletDAO`** (entité JPA)

- Relations `@OneToOne(cascade = ALL, optional = false, orphanRemoval = true)` vers `AdressDAO`, `GraduationDAO` et `GeoPositionDAO` ;
- Le détail des tables est décrit dans [modele-de-donnees.md](modele-de-donnees.md).

### 3.2 Services

L'interface historique `ToiletteService` est **dépréciée** : elle agrège désormais deux interfaces distinctes (principe ISP) :

| Interface | Rôle | Méthodes |
|---|---|---|
| `ToiletteCrudService` | CRUD des toilettes | `addToilet`, `getAllToilets(page, size)`, `lireToilet(id)`, `updateToilet(id, toilet)`, `deleteToilet(id)` |
| `ToiletteProximityService` | Recherche de proximité | `getNearestToilets(currentPosition)` |
| `ToiletteRatingService` | Notation après visite (rôle `USER`) | `gradeToilet(id, graduation)` |
| `CustomerService` | Comptes clients | lecture des clients |

`ToiletteServiceImpl` implémente les trois interfaces (via l'interface héritière `ToiletteService`, déclarée `@Deprecated` pour la compatibilité). La recherche de proximité suit quatre étapes : chargement des toilettes, enrichissement de la distance, filtre `distance < app.distance.max`, tri croissant puis limitation à `app.result.max`. Le dépôt d'une note (`gradeToilet`) s'appuie sur `ToiletteDAOMapper#gradeToilet` : bornage des critères, ajout à `graduationHistory` et recalcul de la moyenne.

### 3.3 Contrôleurs (API REST)

**`ToilettesApiImpl`** implémente l'interface `ToiletApi` générée depuis le YAML OpenAPI :

| Méthode | Endpoint | Accès | Description |
|---|---|---|---|
| `GET` | `/toilets` | `USER` | Liste paginée, ou recherche par coordonnées (`latitude` / `longitude`) |
| `POST` | `/toilets` | `ADMIN` | Création |
| `GET` | `/toilets/{id}` | `USER` | Lecture d'une toilette |
| `PUT` | `/toilets/{id}` | `ADMIN` | Mise à jour |
| `DELETE` | `/toilets/{id}` | `ADMIN` | Suppression |
| `PUT` | `/toilets/{id}/grade` | `USER` | Dépôt d'une note après visite ; la note affichée est la moyenne de l'historique |

**`CustomerApiImpl`** expose les endpoints clients (`GET /customers`, `POST /customers`, `GET /customers/{login}`).

**Sécurité** (voir `AppSecurityConfiguration`) :

- `@PreAuthorize("hasRole('USER')")` pour les lectures et pour le dépôt de note (`PUT /toilets/{id}/grade`) ;
- `@PreAuthorize("hasRole('ADMIN')")` pour les créations, mises à jour complètes et suppressions ;
- Le chemin `/toilets/*/grade` est autorisé pour `USER` dans `AppSecurityConfiguration`, les autres `PUT` restent réservés à `ADMIN` ;
- Basic auth sans session (`SessionCreationPolicy.STATELESS`) ; CSRF désactivé sur la chaîne API ;
- Tout profil **autre que `prod`** — y compris **aucun profil**, le lancement local par défaut (`mvn spring-boot:run`) : utilisateurs en mémoire `manu/manu` (rôle `USER`) et `admin/admin` (rôles `USER` + `ADMIN`) fournis par `userDetailsServiceDev` annoté `@Profile("!prod")` ; profil `prod` : `JdbcUserDetailsManager`. Sans ce bean, Spring Security auto-configurerait un `InMemoryUserDetailsManager` avec un utilisateur `user` et un mot de passe aléatoire : les comptes de démonstration seraient inutilisables (`401` systématique).

### 3.4 Gestion centralisée des erreurs HTTP (`web/GlobalExceptionHandler`)

Les interfaces d'API générées (`web/api/*`) sont annotées `@Validated` : les contraintes `@Min`, `@Max` et `@DecimalMax` portées par les paramètres de chemin et de requête lèvent une `ConstraintViolationException` **avant** d'atteindre l'implémentation. Sans conseil global, Spring renvoie `500 Internal Server Error`, ce qui contredit le contrat OpenAPI qui annonce `400` avec un corps `ApiError`.

| Exception traitée | Code HTTP | Corps de réponse |
|---|---|---|
| `ConstraintViolationException` (`id`, `page`, `size`, `latitude`, `longitude`) | `400` | `ApiError` : `status`, `message` = « `propertyPath: message` » trié par `propertyPath`, `timestamp` |
| `BindException` / `MethodArgumentNotValidException` (corps JSON `@Valid`) | `400` | `ApiError` : « `champ: message` » |
| `IllegalArgumentException` (argument métier `null`) | `400` | `ApiError` |
| `ResourceNotFoundException` | `404` | `ApiError` : message porté par l'exception |

- `@ControllerAdvice(name = "productionGlobalExceptionHandler")` + `@Order(Ordered.HIGHEST_PRECEDENCE)` : la traduction s'applique avant tout autre handler ;
- chaque rejet est journalisé en `WARN` avec des placeholders (`log.warn("Constraint violation rejected request: {}", details)`), sans concatination ;
- libellé de secours (`Invalid request`, `Resource not found`) quand l'exception ne porte aucun détail ;
- les libellés des violations sont **localisés par Hibernate Validator** (locale de la JVM) et portent le préfixe de la méthode générée : `getToiletById.id: doit être supérieur ou égal à 1` ;
- le comportement métier existant est inchangé : `GET /toilets/{id}` inconnu continue de retourner `404` construit par le contrôleur (corps vide), tandis que `PUT /toilets/{id}/grade` sur une ressource absente retourne un `404` `ApiError` via le conseil global ;
- vérification live (profil `dev`, `mvn spring-boot:run` sur le port `14107`) : `curl -i -u manu:manu http://localhost:14107/toilets/0` → `400` + corps `ApiError` ; l'image Docker encore lancée sur `14007` date d'avant le conseil et doit être reconstruite ;
- tests : `web/GlobalExceptionHandlerTest.java` (12 tests unitaires) et `web/ValidationStatusCodesIntegrationTest.java` (6 tests d'intégration MockMvc).

### 3.5 Accès aux données


**Repositories** (Spring Data JPA) :

- `ToiletteRepository extends JpaRepository<ToiletDAO, Long>` ;
- `CustomerRepository`, `UserRepository` ;
- CRUD de base, extensible par des requêtes personnalisées.

**Entités JPA** :

- Suppression en cascade : `ToiletDAO` → `AdressDAO`, `GraduationDAO`, `GeoPositionDAO` ;
- Toutes portent un `id` généré et les annotations JPA standard.

**`DataSeeder`** (profil `dev`) : alimente la base avec un jeu de démonstration autour de Rennes, uniquement si elle est vide.

**`UserAccountSeeder`** (profil `dev`) : crée `manu` et `admin` dans `user_account` avec mot de passe **haché**, de façon idempotente.

**`SecurityAccountSeeder`** (profil `prod`) : garantit la présence des comptes de démonstration dans le référentiel Spring Security (tables `users` / `authorities`) à l'événement `ApplicationReadyEvent`, sans jamais écraser un compte déjà existant. Les identifiants sont surchargeables par `DEMO_USER_USERNAME`, `DEMO_USER_PASSWORD`, `DEMO_ADMIN_USERNAME` et `DEMO_ADMIN_PASSWORD`.

> Pour que l'amorceur puisse injecter le manager, le bean `userDetailsService` du profil `prod` est typé `JdbcUserDetailsManager` (et non `UserDetailsService`) : `JdbcUserDetailsManager` implémente `UserDetailsService`, le comportement d'authentification est donc strictement inchangé.

### 3.6 Mappers (MapStruct)

| Mapper | Transformation |
|---|---|
| `ToiletteDAOMapper` | `daoToBo()` : `ToiletDAO` → `Toilet` (DTO) ; `boToDao()` : DTO → DAO ; `updateToiletteDAOFromBO()` : mise à jour partielle ; `gradeToilet()` : ajout de la note à `graduationHistory` + recalcul de la moyenne (bornée à `[0, 10]`) |
| `AdressDAOMapper` | Met `voie` et `ville` en majuscules lors du mapping |
| `GraduationDAOMapper` | Mapping simple des champs `accueil`, `espace`, `proprete` |
| `ToiletConverter` / `ToiletConverterImpl` | Conversion objet métier ↔ DTO |

### 3.7 Stratégie de distance

**`DistanceCalculator`** (interface) et **`HaversineDistanceCalculator`** (implémentation `@Component`) :

- Formule de Haversine, unité **mètres** (`EARTH_RADIUS_KM * c * 1000`) ;
- Position ou coordonnée `null` → `Double.MAX_VALUE`, converti en `distance = null` côté service ;
- Toute nouvelle algorithmique s'ajoute comme une implémentation supplémentaire, sans modifier le service (OCP).

**`GeoPositionUtils`** :

- `distance(lat1, lon1, lat2, lon2, el1, el2)` : formule de Haversine ;
- `distance(GeoPosition p1, GeoPosition p2)` : surcharge de confort ;
- `setDistance(toilet, position)` : calcule et affecte la distance d'une toilette ;
- `compareWithDistance(t1, t2)` : comparateur pour le tri.

**Correctifs déjà appliqués** : ordre des paramètres de la méthode primitive corrigé ; contrôles de nullité ajoutés (retour de `Double.MAX_VALUE` en cas de donnée manquante).

### 3.8 Configuration

**`ToiletteServiceConfig`** lit les propriétés d'`application.properties` :

| Propriété | Défaut code | Valeur locale | Effet |
|---|---|---|---|
| `app.distance.max` | `100.0` km | `50` | Distance maximale de recherche |
| `app.result.max` | `5` | `10` | Nombre maximal de résultats renvoyés |

### 3.9 Documentation API (OpenAPI / Swagger)

**Fichiers** : `src/main/resources/api/*.yaml`

- `ToiletAPI.yaml` : opérations CRUD toilettes ;
- `CustomerAPI.yaml` : opérations clients ;
- `ObjectDefinitions.yaml` : schémas partagés.

**Code généré** (plugin Maven) : interfaces dans `target/generated-sources/openapi/`, DTO dans `org.manu.wcpregnant.web.dto`. Les contrôleurs implémentent ces interfaces et **ne doivent jamais être édités à la main**. Détail du contrat : [contrat-api-openapi.md](contrat-api-openapi.md).

### 3.10 Réinitialisation du mot de passe

**Chaîne** : `web/AuthApiImpl` (implémentation de l'interface générée `AuthApi`) → `service/PasswordResetService` → `service/impl/PasswordResetServiceImpl` → `bdd/repository/UserAccountRepository`. Contrat : [`AuthAPI.yaml`](../../WcPregnantService/src/main/resources/api/AuthAPI.yaml).

| Élément | Détail |
|---|---|
| Compte concerné | `UserAccount` (table `user_account`), entité dédiée qui n'entre pas en conflit avec la table `users` de Spring Security |
| Génération du code | `SecureRandom`, code numérique à **6 chiffres** |
| Stockage du code | **haché en BCrypt** (`resetCode`), jamais en clair ; expiration horodatée `resetCodeExpiresAt` |
| Validité du code | `auth.reset.code-ttl-seconds` (900 s par défaut, surchargeable via `AUTH_RESET_CODE_TTL_SECONDS`) |
| Envoi | SMTP (`spring-boot-starter-mail`) lorsque `auth.mail.enabled=true` ; sinon **mode démonstration** : le code est renvoyé dans `demoCode` |
| Confirmation | contrôle de la date d'expiration, `passwordEncoder.matches(code, resetCode)`, nouveau mot de passe ≥ 8 caractères, code consommé |
| Codes HTTP | `200`, `400` (identifiant, code ou mot de passe invalide), `404` (compte inconnu, code absent ou expiré) |
| Amorce des comptes | `bdd/seed/UserAccountSeeder` (profil `dev`) crée `manu` et `admin` avec mot de passe **haché**, de façon idempotente ; `bdd/seed/SecurityAccountSeeder` (profil `prod`) amorce les mêmes comptes dans `users` / `authorities` au démarrage |
| Sécurité | `/auth/**` et `/error` explicitement `permitAll()` dans la chaîne API (`@Order(1)`) ; `/toilets/**`, `/api/**` et `/agent/**` restent protégés |

> **Limite connue** : en profil `dev`, l'authentification s'appuie sur `InMemoryUserDetailsManager` (`manu` / `admin` en `{noop}`) et en profil `prod` sur `JdbcUserDetailsManager` (table `users`). Le mot de passe réinitialisé est écrit dans `user_account` : la réinitialisation n'affecte la connexion que lorsque le `UserDetailsService` est branché sur `user_account`.

### 3.11 Serveur de l'interface web (`web/FrontendController`)

Le build Angular est servi par le backend lui-même sur le port **14007** : ouvrir
`http://localhost:14007/` affiche directement le menu de l'application, sans `ng serve`
(port 4200) ni page de connexion Spring.

| Point | Détail |
|---|---|
| Racine | `GET /` et `GET /index.html` → contenu de `<dist>/index.html` |
| Assets racine | `GET /main-XXXXXXXX.js`, `GET /styles.css`, `GET /favicon.ico` |
| Assets imbriqués | `GET /media/marker-icon.png` — un seul niveau de dossier, motif `/{dir}/{file}` |
| Résolution du dossier | `app.frontend.dist` (variable `FRONTEND_DIST_PATH`), sinon `WcPregnantApp/dist/WcPregnantApp/browser` (build local), puis `dist/WcPregnantApp` et `dist/browser` |
| Protection | `Path.normalize()` + contrôle `startsWith(distRoot)` : toute évasion hors du dossier de build (`..`, `%2e%2e`) renvoie `404` |
| Types MIME | `application/javascript`, `text/css`, `image/svg+xml`, `image/png`, `image/jpeg`, `font/woff`, `font/woff2`, `application/json`, `application/octet-stream` |
| Build absent | `404` + message invitant à lancer `npm run build` dans `WcPregnantApp/` |
| Tests | `web/FrontendControllerTest.java` (8 tests) + règles d'accès dans `web/SecurityIntegrationTest.java` |

> L'authentification de l'interface reste celle de l'API : la chaîne de filtres
> `apiFilterChain` autorise explicitement `/`, `/index.html` et les extensions d'assets
> (`*.js`, `*.css`, `*.png`, `*.svg`, `*.ico`, `*.woff2`, `*.json`, `*.map`), tandis que
> `/toilets/**`, `/api/**` et `/agent/**` exigent Basic auth et respectent le RBAC.


---

## 4. Stratégie de tests

| Niveau | Outils | Cibles principales |
|---|---|---|
| **Tests unitaires** | JUnit 5, Mockito, AssertJ | `GeoPositionUtils`, `ToiletteServiceImpl`, mappers, services |
| **Tests d'intégration API** | `@SpringBootTest` + MockMvc | `ToilettesApiImplTest` |
| **Tests de sécurité** | `spring-security-test` | `SecurityIntegrationTest`, `RbacMatrixIntegrationTest`, `DefaultProfileSecurityTest` (résolution du bean `userDetailsServiceDev` `@Profile("!prod")`) |
| **Tests BDD** | Cucumber | `src/test/resources/features/api/*.feature` |
| **Couverture** | JaCoCo | rapport dans `target/site/jacoco/` |

Objectif : **plus de 80 %** de couverture, en priorité sur la logique métier (services, utilitaires) et la sécurité (endpoints). La stratégie complète est détaillée dans [strategie-de-tests.md](../04-tests/strategie-de-tests.md).

---

## 5. CI/CD

Le workflow `WcPregnantService/.github/workflows/docker-image.yml` se déclenche sur les pushes vers `main` ou `master` :

1. Récupération du code (`actions/checkout@v4`) ;
2. Installation du JDK 25 (`actions/setup-java@v4`, distribution Temurin) ;
3. Build Maven (`mvn -B -DskipTests package`) ;
4. Connexion à GHCR (`docker/login-action@v2`) ;
5. Build et push de l'image Docker (`docker/build-push-action@v4`, tag `ghcr.io/<owner>/wcpregnant:latest`).

> Le dépôt est hébergé sur GitLab (`https://gitlab.com/manix35400/wcpregnant`) : ce workflow GitHub Actions n'est actif que si le dépôt est mirroré sur GitHub.

---

## 6. Architecture de déploiement

### 6.1 Docker

`WcPregnantService/Dockerfile` — **multi-stage** :

| Étape | Base | Rôle |
|---|---|---|
| `builder` | `eclipse-temurin:26-jdk-alpine` | `mvn -DskipTests package -q` |
| `frontend-builder` | `node:22-alpine` | `npm ci --legacy-peer-deps` puis `npm run build` → `dist/WcPregnantApp/browser` |
| `runtime` | `eclipse-temurin:26-jre-alpine` | Copie du jar **et** du build Angular dans `/app/frontend-dist`, utilisateur non root `appuser` |

Le contexte de build est la **racine du monorepo**, parce que l'image doit embarquer les sources
Angular :

```powershell
docker build -t wcpregnant:local -f WcPregnantService/Dockerfile .
```

L'image définit `FRONTEND_DIST_PATH=/app/frontend-dist` : `http://localhost:14007/` affiche
l'interface Angular au lieu de la page de connexion Spring (voir §3.11). Le fichier
`.dockerignore` racine exclut `target/`, `node_modules/`, `dist/`, `Waste/`, `developpement/`
et `documentation/` du contexte.

> **Pourquoi `npm ci --legacy-peer-deps` ?** Le `package-lock.json` fige
> `@angular/common` **20.0.4** alors que `@angular/platform-browser-dynamic` **20.0.7**
> déclare un pair strict `@angular/common@20.0.7`. npm 10 rejette cette incohérence
> (`ERESOLVE`) : `npm install` échoue dans l'image, `npm ci` seul aussi. Le drapeau
> `--legacy-peer-deps` reproduit la résolution utilisée lors de l'installation locale,
> qui produit un build Angular fonctionnel.

- Port exposé : **14007** (port dédié au projet, défini par `server.port=${API_PORT:14007}`
  dans `application.properties` — ni 8080/8081, ni la plage éphémère Windows 49152-65535) ;
- `JAVA_OPTS` configurable (`-Xms256m -Xmx512m`) ;
- `HEALTHCHECK` toutes les 30 s sur `http://localhost:14007/actuator/health`
  (la sonde est explicitement **publique** : voir la chaîne de filtres
  `healthFilterChain` dans `AppSecurityConfiguration`, sans authentification).

> ⚠️ Chemin exact : l'Actuator est servi sous `/actuator`, **pas** `/api/actuator`.
> Un `HEALTHCHECK` pointant sur `/api/actuator/health` renvoie un `302` vers `/login`,
> busybox `wget` échoue (« too many redirections ») et Docker marque le conteneur
> `unhealthy`.

### 6.2 Kubernetes

**Espace de noms** : `wcpregnant` (isolation).

**Déploiement applicatif** (`k8s/deployment.yaml`) :

- 2 réplicas (minimum), 5 maximum via HPA ;
- Sonde de disponibilité : `GET /actuator/health/readiness` (20 s initiales, 5 s d'intervalle) ;
- Sonde de survie : `GET /actuator/health` (30 s initiales, 10 s d'intervalle) ;
- Ressources demandées : 256 Mi mémoire, 250 m CPU ;
- Limites : 512 Mi mémoire, 500 m CPU.

**Déploiement base de données** : PostgreSQL 18 Alpine (`postgres:18-alpine`), 1 réplica (point unique de failure dans la configuration de base), volume persistant de 10 Gi, contrôle de santé `pg_isready -U wcpregnant`.

**Service** : `ClusterIP` (port 80 → 14007).

**Ingress** (nginx, optionnel) : hôte `wcpregnant.example.com` configurable, TLS via cert-manager (Let's Encrypt).

**Réseau** : la `NetworkPolicy` restreint la communication entre pods — l'application accède à PostgreSQL sur le port 5432, et accepte les requêtes venant du frontend / de l'Ingress.

**Haute disponibilité** :

- `PodDisruptionBudget` : au moins 1 pod disponible ;
- Autoscaling : montée en charge à 70 % d'utilisation CPU.

**Secrets et configuration** : les identifiants de base de données et les URL sont dans un `Secret` ; les propriétés applicatives dans un `ConfigMap`.

Les manifestes détaillés sont décrits dans [kubernetes.md](../05-deploiement/kubernetes.md).

---

## 7. Axes d'optimisation

### 7.1 Améliorations déjà réalisées

1. Correction du bug de distance (ordre des paramètres) ;
2. Robustesse face aux `null` dans les utilitaires ;
3. Élargissement de la couverture des tests unitaires ;
4. Intégration de JaCoCo pour le suivi de la couverture ;
5. Séparation des interfaces CRUD / proximité (ISP) ;
6. Stratégie de distance injectable (`DistanceCalculator`).

### 7.2 Pistes futures

| Piste | Détail |
|---|---|
| **Indexation** | Index sur `adress.codePostal` et `position.latitude/longitude` |
| **Cache** | Redis pour les positions consultées fréquemment |
| **Pagination** | Pagination repository pour les gros volumes |
| **Requêtes** | JPQL personnalisé au lieu du filtrage en stream |
| **Validation** | Annotations JSR-380 plus poussées |
| **Logs** | Logging structuré pour l'observabilité |
| **Monitoring** | Export des métriques Prometheus |
| **Sécurité** | Validation de tokens OAuth2 / JWT |
| **Agent / Tool Calling** | ✅ **Implémenté** — couche d'orchestration **locale** (Strata AI, API compatible OpenAI) exposant les services existants comme outils, sans modifier les couches `web`/`service` — voir §7.3 et [objectif-agentique.md](../01-exigences/objectif-agentique.md) |

### 7.3 Couche agentique (Tool Calling)

La couche agent est un **adaptateur** posé au-dessus des interfaces de service existantes : elle ne contient ni SQL, ni mapping manuel, et hérite du RBAC déjà en place.

| Élément | Rôle |
|---|---|
| `agent/tool/ToolCall`, `agent/tool/ToolResult` | types immuables représentant un appel d'outil et son résultat |
| `agent/tool/ToiletToolRegistry` | registre des 3 outils (`search_toilettes_proches`, `get_details_toilette`, `gerer_equipement_crud`) au format OpenAI Function Calling |
| `agent/tool/CrudAction` | énumération des actions CRUD acceptées (`CREATE`, `UPDATE`, `DELETE`) |
| `agent/tool/ToiletToolExecutor` | validation stricte des arguments puis délégation à `ToiletteCrudService`, `ToiletteProximityService`, `ToiletteRatingService` |
| `agent/AgentService` (interface) | contrat de la conversation agentique : `chat(message, position, maxIterations)` |
| `agent/AgentServiceImpl` | orchestrateur **Reason → Act → Observe** : prompt système, envoi au modèle, interception des `tool_calls`, exécution, réinjection des observations |
| `agent/inference/InferenceClient` (interface) | abstraction du serveur d'inférence (DIP) — testable sans réseau |
| `agent/inference/LocalInferenceClient` | appel HTTP `POST {base-url}/chat/completions` du serveur **local** Strata AI via le `HttpClient` JDK, timeout explicite |
| `agent/inference/ChatMessage`, `InferenceRequest`, `InferenceResponse`, `ToolCallPayload` | records du payload OpenAI-compatible (rôles `system`/`user`/`assistant`/`tool`) |
| `agent/inference/InferenceException` | erreur métier traduite en **503** quand le serveur local est injoignable |
| `web/AgentApiImpl` | endpoints `GET /agent/tools`, `POST /agent/tools/call` et `POST /agent/chat` (contrat `api/AgentAPI.yaml`) |
| `config/AgentToolConfig` | rayon par défaut / maximal, nombre maximal de résultats, URL du modèle, timeout et nombre maximal d'iterations, alimenté par `application.properties` ; fabrique le bean `InferenceClient` |

#### Boucle agentique `POST /agent/chat`

| Étape | Ce qui se passe |
|---|---|
| 1. Seed | un message `system` (rôle, limites, langue) est ajouté ; si une `position` est fournie, elle est injectée dans le message `user` pour permettre les recherches de proximité |
| 2. Reason | `LocalInferenceClient` envoie `{model, messages, tools}` au serveur local ; le modèle répond soit par du texte, soit par des `tool_calls` |
| 3. Act | chaque `tool_call` est converti en `ToolCall` et exécuté par `ToiletToolExecutor` → appel direct des services métier existants (RBAC appliqué) |
| 4. Observe | le résultat (JSON de l'outil ou `ApiError`) est réinjecté comme message `tool` et la boucle recommence |
| 5. Stop | la boucle s'arrête dès qu'une réponse textuelle est produite ou quand `maxIterations` est atteint (message de garde-fou) |

La réponse expose `answer`, `model`, `iterations` et la trace `toolCalls` (`tool`, `arguments`, `status`, `result`, `message`) — utile pour l'audit et la démonstration du concours.

#### Configuration de l'inférence (100 % locale)

| Clé `application.properties` | Variable d'environnement | Défaut |
|---|---|---|
| `agent.inference.base-url` | `AGENT_INFERENCE_BASE_URL` | `http://localhost:8000/v1` |
| `agent.inference.model` | `AGENT_MODEL` | `Qwen-3.8-Flash-Next` |
| `agent.inference.timeout-seconds` | `AGENT_TIMEOUT_SECONDS` | `30` |
| `agent.max-iterations` | `AGENT_MAX_ITERATIONS` | `4` |
| `agent.tools.default-radius` / `agent.tools.max-radius` | — | `200.0` / `5000.0` m |
| `agent.tools.max-results` | — | `10` |

Aucune URL, clé ou nom de modèle n'est écrit en dur dans le code : `docker-compose.yml` et `k8s/configmap.yaml` injectent ces valeurs, et les tests d'intégration pointent volontairement vers un port fermé (`http://127.0.0.1:59999/v1`) pour vérifier le comportement **503** sans appel réseau.
#### Intégration frontend (Angular)

Le frontend ne réimplémente aucune logique agentique : il consomme les endpoints déjà exposés et rend la boucle lisible pour l'utilisateur.

| Artefact | Rôle |
|---|---|
| `src/app/models/agent.model.ts` | interfaces `AgentChatRequest`, `AgentChatResponse`, `AgentToolCallTrace`, `AgentToolResult` calquées sur `AgentAPI.yaml` — aucun champ inventé |
| `src/app/services/agent.service.ts` | client HTTP `POST /agent/chat` et `GET /agent/tools`, en-tête `Authorization` repris de `AuthService`, URL issue de `environment.agentApiUrl` |
| `src/app/components/agent-chat/` | panneau de conversation : saisie de la question, suggestions, fil des étapes (position transmise, interrogation du modèle, un pas par outil exécuté, synthèse), cartes de résultats cliquables |
| `src/environments/environment.ts` | clé `agentApiUrl: '/agent'` (chemin relatif, jamais d'host codé en dur) |
| `src/proxy.conf.json` | règle `/agent` → `http://localhost:14007`, alignée sur celle de `/toilets` |
| `src/app/components/map/map.ts` | nouvel `@Input() highlight` : marqueur distinctif (classe `agent-highlight`) centré sur l'équipement choisi par l'agent |
| `src/app/components/home/home.ts` | `onAgentResults(toilets)` remplace la liste et synchronise la carte, `selectFromAgent(toilet)` ouvre la fiche de détails |

La position de recherche (`searchPosition`, géolocalisée ou de référence) est passée au composant via `[position]` et transmise dans `AgentChatRequest.position` : le modèle local n'a donc besoin d'aucun géocodage externe. Les erreurs sont traduites en messages lisibles (403 → rôle `ADMIN` requis, 503 → serveur d'inférence injoignable) et aucune exception n'est propagée au composant parent.


#### Contrat d'erreur des appels d'outils

Toute invalidité d'un appel d'outil est renvoyée sous la forme du schéma `ApiError` (`status`, `message`, `timestamp`) afin que le LLM puisse se corriger :

| Cas | Code HTTP | Message |
|---|---|---|
| nom d'outil absent ou vide | 400 | `tool name is mandatory` |
| `arguments` qui n'est pas un objet JSON | 400 | `arguments must be a JSON object` |
| outil inconnu | 400 | `Unknown tool 'x'. Available tools: [...]` |
| action CRUD inconnue | 400 | `Unknown action 'TELEPORT'. Allowed values: [CREATE, UPDATE, DELETE]` |
| latitude / longitude hors bornes | 400 | `latitude must be between -90 and 90` |
| rayon nul ou trop grand | 400 | `rayon_metres must be greater than 0` / `rayon_metres must not exceed 5000.0` |
| `id` manquant pour `UPDATE` / `DELETE` | 400 | `id is required` |
| `payload` absent ou mal formé | 400 | `payload is required for CREATE and UPDATE actions` |
| `libelle` vide | 400 | `libelle is mandatory` |
| `adress` / `codePostal` absents | 400 | `adress is mandatory`, `codePostal is mandatory` |
| `grade` absent ou incomplet | 400 | `grade is mandatory (proprete, accueil, espace)`, `grade.proprete is mandatory` |
| équipement inexistant | 404 | `Toilette not found with id: 999` |
| rôle insuffisant pour une écriture | 403 | `The tool gerer_equipement_crud requires the ADMIN role` |

---

## 8. Structure du projet

```text
WcPregnant/
├── WcPregnantService/
│   ├── src/
│   │   ├── main/java/org/manu/wcpregnant/
│   │   │   ├── WcpregnantApplication.java      ← point d'entrée Spring Boot
│   │   │   ├── config/                         ← ToiletteServiceConfig, AppSecurityConfiguration
│   │   │   ├── service/                        ← interfaces, bo/, strategy/, utils/
│   │   │   │   └── impl/                       ← ToiletteServiceImpl, CustomerServiceImpl
│   │   │   ├── agent/                          ← AgentService, AgentServiceImpl
│   │   │   │   ├── tool/                       ← ToolCall, ToolResult, ToiletToolRegistry, ToiletToolExecutor
│   │   │   │   └── inference/                  ← InferenceClient, LocalInferenceClient, ChatMessage, InferenceRequest/Response
│   │   │   ├── web/                            ← ToilettesApiImpl, CustomerApiImpl, AgentApiImpl, GlobalExceptionHandler, dto/ (généré)
│   │   │   └── bdd/
│   │   │       ├── dao/                        ← ToiletDAO, AdressDAO, GeoPositionDAO, …
│   │   │       ├── mapper/                     ← MapStruct
│   │   │       ├── repository/                 ← Spring Data JPA
│   │   │       └── seed/                       ← DataSeeder
│   │   └── resources/
│   │       ├── application.properties
│   │       └── api/                            ← YAML OpenAPI (source de vérité)
│   ├── src/test/java/org/manu/wcpregnant/      ← tests unitaires, intégration, BDD
│   ├── src/test/resources/features/api/        ← scénarios Cucumber
│   ├── k8s/                                    ← manifestes Kubernetes + deploy.ps1 / deploy.sh
│   ├── .github/workflows/docker-image.yml      ← CI/CD
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── pom.xml
├── WcPregnantApp/                              ← frontend Angular 20
├── Bruno/WcPregnant/WC Pregnant API/           ← collection de tests d'API
├── documentation/                              ← documentation unifiée (ce dossier)
├── .clinerules/ , .cline/skills/               ← règles et skills Cline
└── Waste/ , developpement/                     ← code mort / archives (ne pas toucher)
```

---

## 9. Considérations de performance

### 9.1 Goulets d'étranglement actuels

1. **Filtrage en stream** : `getNearestToilettes()` charge, mappe puis filtre l'ensemble des toilettes — acceptable pour un petit jeu de données, problématique à l'échelle ;
2. **Absence d'index** : les requêtes ne s'appuient sur aucun index explicite ;
3. **Absence de cache** : les distances sont recalculées à chaque requête.

### 9.2 Optimisations recommandées

| Optimisation | Détail |
|---|---|
| **Requêtes spatiales** | Extension PostGIS pour les requêtes géographiques côté serveur |
| **Pagination** | Pagination repository pour les grands volumes |
| **Cache** | Redis pour les positions (invalidation à la mise à jour) |
| **Pool de connexions** | HikariCP (déjà actif par défaut dans Spring Boot) |
| **Timeouts** | Fixer des délais d'expiration sur les requêtes |

---

## 10. Considérations de sécurité

### 10.1 Mise en œuvre actuelle

- Spring Security avec Basic auth et `@EnableMethodSecurity` ;
- Autorisation par rôle via `@PreAuthorize` ;
- CSRF désactivé sur la chaîne API (stateless) ;
- Mots de passe hachés via `PasswordEncoderFactories.createDelegatingPasswordEncoder()` (préfixe `{bcrypt}`) ;
- **Code de réinitialisation lui-même haché** (BCrypt) et limité dans le temps (TTL configurable) ;
- Filtre de logs de requêtes (`CommonsRequestLoggingFilter`) sans journalisation des en-têtes d'authentification ;
- Chaîne de filtres séparée : `@Order(0)` santé (publique), `@Order(1)` API stateless (`/auth/**` public, RBAC sur les mutations), formulaire de connexion pour le reste.

### 10.2 Recommandations

- [ ] Valider toutes les entrées (Bean Validation JSR-380) ;
- [ ] HTTPS uniquement (imposé par le reverse proxy / Ingress K8s) ;
- [ ] Prévention des injections SQL (JPA/Hibernate s'en charge ; éviter le SQL brut) ;
- [ ] Prévention XSS (backend API uniquement, pas de templates) ;
- [ ] Limitation de débit (nginx-ingress ou Spring Cloud) ;
- [ ] Journalisation d'audit (toute mutation tracée avec utilisateur / horodatage) ;
- [ ] Rotation des secrets (gestion des `Secret` Kubernetes).

---

## 11. Monitoring et observabilité

| Axe | État |
|---|---|
| **Logs** | Niveaux configurés dans `application.properties` ; logging structuré (JSON) recommandé |
| **Métriques** | Endpoints Actuator : `/actuator/health`, `/actuator/health/readiness` (publics, sondes Docker/K8s) ; le reste de l'Actuator reste protégé ; export Prometheus + Grafana possible |
| **Traçabilité** | Non implémenté (Spring Cloud Sleuth envisagé) |

---

## 12. Checklist de déploiement

- [ ] Les tests passent en local : `mvn test`
- [ ] Le jar est construit : `mvn clean package`
- [ ] L'image Docker se construit : `docker build -t wcpregnant:latest .`
- [ ] `docker-compose up` fonctionne
- [ ] Les manifestes K8s sont validés : `kubectl apply --dry-run=client -f k8s/`
- [ ] Les secrets sont configurés proprement (aucun identifiant dans les manifestes)
- [ ] Les migrations de base sont gérées (Flyway ou Liquibase recommandés)
- [ ] Les contrôles de santé sont accessibles
- [ ] Les logs sont accessibles et structurés
- [ ] Le monitoring / les alertes sont configurés

---

## 13. Prochaines étapes

1. Exécuter la suite de tests complète et analyser le rapport JaCoCo ;
2. Ajouter les tests manquants sur les DTO et utilitaires non couverts ;
3. Mettre en place un cluster K8s local (Docker Desktop, Minikube ou KinD) ;
4. Déployer en local avec docker-compose et les scripts K8s ;
5. Créer un environnement de pré-production ;
6. Tests de performance en charge (JMeter, Gatling) ;
7. Audit de sécurité et tests d'intrusion.

