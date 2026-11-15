# Stratégie de tests

> Synthèse des moyens de test du projet. Les règles de rédaction des tests sont dans [`.clinerules/04-tests-java.md`](../../.clinerules/04-tests-java.md) ; la méthode JaCoCo détaillée est dans [javadoc-et-couverture-tests.md](javadoc-et-couverture-tests.md).

---

## 1. Étages de test

| Étage | Outils | Où |
|---|---|---|
| **Tests unitaires** | JUnit 5, Mockito, AssertJ | `WcPregnantService/src/test/java/...` |
| **Tests d'intégration API** | `@SpringBootTest` + MockMvc | `web/ToilettesApiImplTest`, `bdd/NonRegressionApiTest` |
| **Tests de sécurité** | `spring-security-test` | `web/SecurityIntegrationTest` |
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

## 4. Règles de rédaction (backend)

- Nom : `method_scenario_expectedBehavior`, ex. `deleteToilet_shouldThrow_whenIdNotFound`.
- **Minimum 3 tests par méthode publique** : nominal, exception métier, `null` / cas limite.
- Structure **Arrange / Act / Assert** commentée.
- Vérifier les interactions avec `verify(...)` (mapper, repository) et les logs si la méthode loggue.
- **Un test = un scénario** : pas de tests fleuves.
- Ne jamais mocker la classe testée elle-même.

---

## 5. Mesurer la couverture

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

## 6. Commandes de validation

| Zone | Commande |
|---|---|
| Backend | `mvn -q test` |
| Frontend | `npm run build` (type-check) puis `npm run test` |
