# Stratégie de tests

> Synthèse des moyens de test du projet. Les règles de rédaction des tests sont dans [`.clinerules/04-tests-java.md`](../../.clinerules/04-tests-java.md) ; la méthode JaCoCo détaillée est dans [javadoc-et-couverture-tests.md](javadoc-et-couverture-tests.md).

---

## 1. Étages de test

| Étage | Outils | Où |
|---|---|---|
| **Tests unitaires** | JUnit 5, Mockito, AssertJ | `WcPregnantService/src/test/java/...` |
| **Tests d'intégration API** | `@SpringBootTest` + MockMvc | `web/ToilettesApiImplTest`, `bdd/NonRegressionApiTest`, `web/AgentApiIntegrationTest` |
| **Tests de sécurité** | `spring-security-test` | `web/SecurityIntegrationTest`, `web/RbacMatrixIntegrationTest`, `web/DefaultProfileSecurityTest` |
| **Tests agentiques (Tool Calling)** | Faux serveur d'inférence JDK `HttpServer` sur `127.0.0.1:45678` | `agent/AgentServiceImplTest`, `agent/inference/LocalInferenceClientTest`, `agent/tool/*Test`, `web/AgentChatEndToEndTest` |
| **Tests BDD** | Cucumber 8.0.4 | `src/test/resources/features/api/*.feature` + `bdd/steps/ToiletApiSteps.java` |
| **Tests d'API end-to-end** | Bruno | `Bruno/WcPregnant/WC Pregnant API/` |
| **Tests frontend** | Karma / Jasmine | `WcPregnantApp/src/**/*.spec.ts` |
| **Couverture** | JaCoCo 0.8.16 | `WcPregnantService/target/site/jacoco/` |

---

## 2. Inventaire des tests backend

| Fichier | Cible |
|---|---|
| `service/ToiletteServiceImplUnitTest.java` | Logique CRUD / proximité, dépendances mockées |
| `service/ToiletteServiceImplTest.java` | Intégration service + repository |
| `service/CustomerServiceImplTest.java` | Service clients |
| `service/strategy/HaversineDistanceCalculatorTest.java` | Formule de distance |
| `service/utils/GeoPositionUtilsTest.java` | Calculs géo, gestion des `null` |
| `bdd/mapper/ToiletteDAOMapperTest.java` | Mapping DAO ↔ DTO |
| `bdd/mapper/AdressDAOMapperTest.java` | Mapping adresses |
| `bdd/mapper/GraduationDAOMapperTest.java` | Mapping notation |
| `bdd/repository/ToiletteRepositoryTest.java` | Repository JPA |
| `bdd/NonRegressionApiTest.java` + `bdd/ApiTestContext.java` | Non-régression API |
| `web/ToilettesApiImplTest.java` | Endpoints HTTP via MockMvc |
| `web/SecurityIntegrationTest.java` | RBAC / authentification : `/auth/**` et `/error` publics, **`/` et `/index.html` publics (interface web)** — vérifié par 2 tests qui refusent toute redirection `formLogin`, `401` sur `/api/toilets` et `/toilets` sans credentials, `403` pour un `USER` qui crée ou supprime une toilette |
| `service/impl/PasswordResetServiceImplTest.java` | Réinitialisation du mot de passe : génération du code, expiration, code invalide ou consommé, mot de passe trop court |
| `bdd/seed/UserAccountSeederTest.java` | Amorce idempotente des comptes de démonstration (mots de passe hachés) |
| `bdd/seed/SecurityAccountSeederTest.java` | Amorce des comptes Spring Security du profil `prod` : création, idempotence, rôles `USER`/`ADMIN`, hachage BCrypt, refus des dépendances `null` |
| `bdd/seed/SecurityAccountSeederIntegrationTest.java` | Même amorce sur un vrai `JdbcUserDetailsManager` (H2 en mémoire, tables `users` / `authorities` créées par Hibernate) : comptes réellement écrits, rôles en base, mot de passe haché |
| `bdd/dao/UserAccountTest.java` | Modèle `UserAccount` : hachage BCrypt du code, validité du code, champs obligatoires |
| `agent/AgentServiceImplTest.java` | Boucle Reason → Act → Observe, plafond d'iterations |
| `agent/inference/LocalInferenceClientTest.java` | Appel de l'API OpenAI-compatible locale, gestion des erreurs |
| `agent/tool/ToiletToolRegistryTest.java` | Définitions d'outils exposées au modèle |
| `agent/tool/ToiletToolExecutorTest.java` | Exécution des outils, contrôle RBAC, arguments invalides |
| `web/AgentApiImplTest.java` | Validation des requêtes agentiques |
| `web/AgentApiIntegrationTest.java` | Endpoints `/agent/tools` et `/agent/chat` |
| `web/AgentChatEndToEndTest.java` | Conversation complète contre un faux serveur d'inférence local |
| `config/ServerPortConfigurationTest.java` | Port HTTP **dédié 14007** : défaut résolu par Spring, appartenance à la bande réservée (11000-65535, hors plage éphémère). Le fallback de lecture `@Value("${server.port:14007}")` est lui-même le port dédié, jamais `8080` |
| `config/ServerPortOverrideConfigurationTest.java` | Surcharge du port dédié par `API_PORT` : la configuration dédiée conserve la valeur de l'environnement et reste hors de la plage éphémère ; assertion explicite « jamais 8080 » |
| `bdd/seed/UserAccountSeederIntegrationTest.java` | Amorce des profils de test sur une vraie base H2 : écriture effective des comptes et des rôles, idempotence au second lancement |
| `web/RbacMatrixIntegrationTest.java` | Matrice RBAC bout en bout sur MockMvc : `401` sans credentials, `403` pour un `USER` qui écrit, `201`/`204` pour un `ADMIN`, lecture autorisée pour tous les comptes authentifiés |
| `utils/TestsUtils.java` | Constructeurs de données de test |
| `web/GlobalExceptionHandlerTest.java` | 12 tests du conseil global : `ConstraintViolationException`, `BindException`, `IllegalArgumentException`, `ResourceNotFoundException` → `400`/`404` avec corps `ApiError`, message de secours quand la violation n'a pas de libellé |
| `web/ValidationStatusCodesIntegrationTest.java` | 6 tests d'intégration MockMvc sur les endpoints réels : `GET /toilets/0` → `400`, `GET /toilets?size=1000` → `400`, `GET /toilets?latitude=200` → `400`, `PUT /toilets/999999/grade` → `404` (`ApiError`), `POST /toilets` au corps invalide → `400`, `GET /toilets/999999` → `404` |
| `web/FrontendControllerTest.java` | 8 tests du serveur de la SPA : `index.html` servi sur `/` avec le bon type MIME, `404` quand le build est absent ou que le chemin explicite ne contient pas `index.html`, assets racine et imbriqués (`application/javascript`, `image/png`), extension inconnue servie en `application/octet-stream`, refus de l'évasion de chemin (`../leaked.txt`) |
| `web/DefaultProfileSecurityTest.java` | 9 tests du **profil par défaut (aucun `@ActiveProfiles`)** : le bean `userDetailsServiceDev` (`@Profile("!prod")`) est bien résolu — le `UserDetailsService` injecté est un `InMemoryUserDetailsManager` et non celui auto-configuré de Spring Security (`user` introuvable) ; les comptes `manu` (`ROLE_USER`) et `admin` (`ROLE_USER` + `ROLE_ADMIN`) sont authentifiables ; `GET /toilets` avec `manu:manu` répond `200` (et non `401`), `POST /toilets` par `manu` répond `403`, par `admin` `201`, le compte `disabled` `401`, requête anonyme `401` |



**Fonctionnalités BDD** : `01-demo-data.feature`, `02-authentication.feature`.

---

## 3. Inventaire des tests frontend

| Fichier | Cible |
|---|---|
| `components/home/home.spec.ts` | Composant accueil : connexion, recherche, résultats, bulle flottante agentique intégrée, centrage de la carte via `@ViewChild('agentMap')` |
| `components/map/map.spec.ts` | Carte Leaflet (création / destruction de l'instance, index des marqueurs par id, `focusToilet` / `focusToiletById` : carte non initialisée, toilette `null`, coordonnées absentes, centrage + mise en avant) |
| `components/toilet-details/toilet-details.spec.ts` | Fiche détail |
| `components/toilet-form/toilet-form.spec.ts` | Formulaire d'ajout |
| `models/toilet.model.spec.ts` | Modèle de données |
| `services/toilet.service.spec.ts` | Appel API |
| `services/map.service.spec.ts` | Service carte |
| `services/auth.service.spec.ts` | Authentification |
| `services/settings.service.spec.ts` | Configuration |
| `agent/agent.service.spec.ts` | Socle agentique autonome : `askAgent` (endpoint, payload sans position, réponse émise, erreur HTTP, URL relative) |
| `agent/agent-chat.component.spec.ts` | Bulle flottante agentique : ouverture/fermeture, envoi de la question, timeline des étapes pendant la réflexion, mini-cartes, émission `selectToilette`, erreurs 403/503, format des distances |

> Suite frontend exécutée par `npm run test` : **105 tests**, tous verts.

---

## 4. Tester la boucle agentique sans inférence réelle

La boucle Tool Calling dépend d'un serveur d'inférence (Strata AI `Qwen-3.8-Flash-Next` en local). Pour rendre les tests déterministes et **sans aucune sortie réseau**, `AgentChatEndToEndTest` démarre un **faux serveur OpenAI-compatible** basé sur le JDK (`com.sun.net.httpserver.HttpServer`) sur `127.0.0.1:45678`.

| Élément | Détail |
|---|---|
| Point d'entrée | `/v1/chat/completions` — même forme de réponse que l'API OpenAI (`choices[].message.tool_calls`) |
| Injection de l'URL | `@SpringBootTest(properties = { "agent.inference.base-url=http://127.0.0.1:45678/v1" })` |
| Choix du port | Le port est **fixé** : il doit être connu à la compilation de l'annotation, ce qu'un port éphémère attribué à l'exécution ne permet pas. `45678` est retenu car les plages autour de `58301` sont **réservées par Hyper-V** sur les postes Windows (Virtual Machine) et rendent le `bind` impossible |
| Robustesse | `BIND_RETRY_COUNT = 5` : en cas de conflit transitoire de port, le serveur est relancé avec une attente croissante (1 s, 2 s, …) avant d'échouer explicitement |
| Comportement | 1ʳᵉ requête → réponse avec `tool_calls` ; dès qu'un message `role: "tool"` est présent → réponse textuelle |
| Scénarios couverts | appel d'outil de proximité, réinjection de l'observation, refus RBAC d'un outil CRUD, arrêt à `maxIterations` |

> Une propriété système (`-Dagent.inference.base-url=...`) **ne suffit pas** à écraser `application.properties` dans ce projet : il faut passer par `properties` de `@SpringBootTest`, dont la source de propriétés est prioritaire.

---

## 5. Règles de rédaction (backend)

- Nom : `method_scenario_expectedBehavior`, ex. `deleteToilet_shouldThrow_whenIdNotFound`.
- **Minimum 3 tests par méthode publique** : nominal, exception métier, `null` / cas limite.
- Structure **Arrange / Act / Assert** commentée.
- Vérifier les interactions avec `verify(...)` (mapper, repository) et les logs si la méthode loggue.
- **Un test = un scénario** : pas de tests fleuves.
- Ne jamais mocker la classe testée elle-même.

---

## 6. Isolation des contextes Spring : piège des beans partagés

Le contexte `@SpringBootTest` est **mis en cache** et partagé entre toutes les classes de test
dont la configuration est identique. Un bean modifié par une classe le reste pour les classes
exécutées ensuite : l'ordre d'exécution de Surefire devient alors un facteur d'échec.

Deux protections sont en place :

| Protection | Où | Effet |
|---|---|---|
| Restauration du repository authentique | `service/ToiletteServiceImplTest.java` — `@AfterEach restoreRealRepository()` | le mock injecté via `ReflectionTestUtils.setField(service, "toiletteRepository", …)` ne fuite pas vers les autres classes d'intégration |
| Contexte et base dédiés | `web/DefaultProfileSecurityTest.java` — `@SpringBootTest(properties = { "spring.datasource.url=jdbc:h2:mem:default-profile-security;DB_CLOSE_DELAY=-1" })` | un contexte Spring distinct, donc insensible aux mocks injectés ailleurs, tout en conservant le profil par défaut |

> Symptôme observé avant la correction : `NullPointerException` dans `ToiletteServiceImpl.listToilets`
> (`findAll(Pageable)` renvoyant `null` sur le mock) lors des appels MockMvc de
> `DefaultProfileSecurityTest`.

---

## 6. Mesurer la couverture

```bash
cd WcPregnantService
mvn test jacoco:report
```

Le rapport HTML est dans `WcPregnantService/target/site/jacoco/`.

Script de contrôle par classe (seuil 80 %) :

```powershell
./.cline/skills/javadoc-test-coverage/scripts/coverage-check.ps1
```

Il lit `WcPregnantService/target/site/jacoco/jacoco.csv` et liste les classes sous le seuil.

Dernière campagne (`mvn test` : **283 tests**, 0 échec, 0 erreur) — extrait pour la classe ajoutée :

| Classe | Instructions | Branches | Lignes | Méthodes |
|---|---|---|---|---|
| `web/GlobalExceptionHandler` | 153/160 (95,6 %) | 13/17 (76,5 %) | 34/35 (97,1 %) | 12/12 (100 %) |

Les quelques instructions non couvertes correspondent aux libellés de secours (`Invalid request`,
`Resource not found`) et aux branches de défense (`violations == null`, `errors == null`) : elles
sont exercées par les tests unitaires `GlobalExceptionHandlerTest`, pas par le chemin d'intégration.

---

## 7. Commandes de validation

| Zone | Commande |
|---|---|
| Backend | `mvn -q test` |
| Frontend | `npm run build` (type-check) puis `npm run test` |
