---
name: spring-service-layer
description: Écrit ou refactorise du code backend Spring Boot selon les couches du projet (controller, service, repository JPA, mapper MapStruct, stratégie de distance). Utiliser pour "créer un service", "ajouter un repository", "refactoriser SOLID", "mapper un DAO", "configurer Spring Security".
---

# Couche backend Spring Boot

## Architecture cible

```
web/XxxApiImpl        → interface générée, validation, codes HTTP
service/XxxService    → contrat (interface)
service/impl/         → implémentation (@Service, @Transactional, @Slf4j)
bdd/repository        → Spring Data JPA (Repository<Entity, Long>)
bdd/dao               → entité JPA (@Entity, Lombok builder)
bdd/mapper            → MapStruct (DAO <-> DTO)
service/strategy      → algorithmes interchangeables (DistanceCalculator)
config/               → configuration typée issue de application.properties
```

## Règles SOLID appliquées

- **SRP** : extraire chaque étape non triviale dans une méthode privée nommée.
- **OCP** : injecter une interface de stratégie (`DistanceCalculator`) plutôt qu'écrire la formule.
- **ISP** : séparer les contrats (`ToiletteCrudService` / `ToiletteProximityService`).
- **DIP** : dépendre d'interfaces, jamais d'implémentations concrètes.

## Gabarits

- Service : `templates/service-impl.java`
- Mapper MapStruct : `templates/mapper.java`
- Repository : `templates/repository.java`

## Sécurité

- `@PreAuthorize("hasRole('ADMIN')")` sur les mutations, `hasRole('USER')` sur les lectures.
- Ne jamais contourner `AppSecurityConfiguration` ; les rôles viennent de `bdd/dao/Authorities`.

## Vérification

`mvn -q test` depuis `WcPregnantService/`. En cas d'échec de mock, utiliser `any(...)` plutôt qu'un objet précis.
