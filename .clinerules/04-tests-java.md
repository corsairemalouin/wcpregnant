---
paths:
  - "WcPregnantService/src/test/**"
  - "**/*Test.java"
---

# Tests unitaires Java

- Framework : JUnit 5 + Mockito + `assertThat` (AssertJ). Dépendances (repository, mapper) mockées.
- Nom des tests : `method_scenario_expectedBehavior` (ex. `deleteToilet_shouldThrow_whenIdNotFound`).
- **Minimum 3 tests par méthode publique** : nominal, exception métier, null / cas limite.
- Structure Arrange / Act / Assert commentée.
- Vérifier les interactions avec `verify(...)` (mapper, repository) et les logs si la méthode loggue.
- Un test = un scénario. Pas de tests fleuves.
- Couverture cible **> 80 %** par classe ; après `mvn test`, le rapport est dans `WcPregnantService/target/site/jacoco/`.
- Tests d'intégration sécurité : `SecurityIntegrationTest` avec `spring-security-test`.
- Ne jamais mocker la classe testée elle-même.
