# Stratégie de tests

> Synthèse des moyens de test du projet. Les règles de rédaction des tests sont dans [`.clinerules/04-tests-java.md`](../../.clinerules/04-tests-java.md) ; la méthode JaCoCo détaillée est dans [javadoc-et-couverture-tests.md](javadoc-et-couverture-tests.md).

---

## 1. Étages de test

| Étage | Outils | Où |
|---|---|---|
| **Tests unitaires** | JUnit 5, Mockito, AssertJ | `WcPregnantService/src/test/java/...` |
| **Tests d'intégration API** | `@SpringBootTest` + MockMvc | `web/ToilettesApiImplTest`, `bdd/NonRegressionApiTest`, `web/AgentApiIntegrationTest` |
| **Tests de sécurité** | `spring-security-test` | `web/SecurityIntegrationTest` |
| **Tests agentiques (Tool Calling)** | Faux serveur d'inférence JDK `HttpServer` sur `127.0.0.1` | `agent/AgentServiceImplTest`, `agent/inference/LocalInferenceClientTest`, `agent/tool/*Test`, `web/AgentChatEndToEndTest` |
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
| `web/SecurityIntegrationTest.java` | RBAC / authentification |
| `agent/AgentServiceImplTest.java` | Boucle Reason → Act → Observe, plafond d'iterations |
| `agent/inference/LocalInferenceClientTest.java` | Appel de l'API OpenAI-compatible locale, gestion des erreurs |
| `agent/tool/ToiletToolRegistryTest.java` | Définitions d'outils exposées au modèle |
| `agent/tool/ToiletToolExecutorTest.java` | Exécution des outils, contrôle RBAC, arguments invalides |
| `web/AgentApiImplTest.java` | Validation des requêtes agentiques |
| `web/AgentApiIntegrationTest.java` | Endpoints `/agent/tools` et `/agent/chat` |
| `web/AgentChatEndToEndTest.java` | Conversation complète contre un faux serveur d'inférence local |
| `utils/TestsUtils.java` | Constructeurs de données de test |

**Fonctionnalités BDD** : `01-demo-data.feature`, `02-authentication.feature`.

---

## 3. Inventaire des tests frontend

| Fichier | Cible |
|---|---|
| `components/home/home.spec.ts` | Composant accueil |
| `components/map/map.spec.ts` | Carte Leaflet (création / destruction de l'instance) |
| `components/toilet-details/toilet-details.spec.ts` | Fiche détail |
| `components/toilet-form/toilet-form.spec.ts` | Formulaire d'ajout |
| `models/toilet.model.spec.ts` | Modèle de données |
| `services/toilet.service.spec.ts` | Appel API |
| `services/map.service.spec.ts` | Service carte |
| `services/auth.service.spec.ts` | Authentification |
| `services/settings.service.spec.ts` | Configuration |

---

## 4. Tester la boucle agentique sans inférence réelle

La boucle Tool Calling dépend d'un serveur d'inférence (Strata AI `Qwen-3.8-Flash-Next` en local). Pour rendre les tests déterministes et **sans aucune sortie réseau**, `AgentChatEndToEndTest` démarre un **faux serveur OpenAI-compatible** basé sur le JDK (`com.sun.net.httpserver.HttpServer`) sur `127.0.0.1:58301`.

| Élément | Détail |
|---|---|
| Point d'entrée | `/v1/chat/completions` — même forme de réponse que l'API OpenAI (`choices[].message.tool_calls`) |
| Injection de l'URL | `@SpringBootTest(properties = { "agent.inference.base-url=http://127.0.0.1:58301/v1" })` |
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

---

## 7. Commandes de validation

| Zone | Commande |
|---|---|
| Backend | `mvn -q test` |
| Frontend | `npm run build` (type-check) puis `npm run test` |
