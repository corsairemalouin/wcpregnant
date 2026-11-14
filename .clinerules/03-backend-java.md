---
paths:
  - "WcPregnantService/src/main/java/**"
  - "WcPregnantService/pom.xml"
---

# Backend Java (Spring Boot)

## Structure à respecter

- Controller : `web/XxxApiImpl implements XxxApi` (interface générée), `@RestController`, `@RequiredArgsConstructor`, dépendances `final`.
- Service : interface dans `service/`, implémentation dans `service/impl/XxxServiceImpl`.
- Accès données : uniquement via `bdd/repository` (Spring Data JPA). Aucune SQL dans les services.
- Mapping DAO ↔ DTO : via un mapper MapStruct dans `bdd/mapper` ou un `ToiletConverter`. Pas de mapping manuel dans le service.
- Config : `config/XxxConfig` alimenté par `application.properties`.

## Style

- Annotations standard : `@Service`, `@Transactional` (et `@Transactional(readOnly = true)` pour les lectures), `@Slf4j`, `@RequiredArgsConstructor`.
- Java 25 autorisé : `var`, text blocks, pattern matching, `record` pour les types immuables simples.
- Streams pour les transformations, mais garder une méthode privée nommée pour chaque étape non triviale (ex. `enrichWithDistance`).
- Calculs de distance : passer par l'interface `DistanceCalculator` (stratégie), jamais de formule inline.

## Javadoc (obligatoire sur toute méthode publique)

```java
/**
 * Description fonctionnelle : ce que fait la méthode et pourquoi.
 *
 * @param nomParam description, format attendu, null accepté ou non
 * @return description de la valeur retournée
 * @throws ResourceNotFoundException si ...
 */
```

- Utiliser `{@link Xxx}` et `{@code ...}` pour les références.
- Sur une implémentation : `{@inheritDoc}` + complément si le comportement diffère.
