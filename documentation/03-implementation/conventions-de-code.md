# Conventions de code

> Référence consolidée des règles de développement. La version **exécutable** (appliquée automatiquement par Cline selon les fichiers touchés) est dans [`.clinerules/`](../../.clinerules/) ; la version courte est dans [`AGENTS.md`](../../AGENTS.md). Ce document ne fait que **centraliser et expliquer** ces règles.

---

## 1. Architecture backend

```
web  →  service (interface)  →  service/impl  →  bdd/repository  →  bdd/dao
                              ↘  bdd/mapper (MapStruct)
```

- Un controller dépend **d'une interface de service**, jamais d'une implémentation concrète (DIP).
- Accès aux données **uniquement** via Spring Data JPA : aucune SQL dans les services.
- Mapping DAO ↔ DTO via MapStruct (`bdd/mapper/XxxDAOMapper`) ou `ToiletConverter` : **pas de mapping manuel** dans le service.
- Calcul de distance via l'interface `DistanceCalculator` (stratégie), **jamais une formule inline**.

| Annotation | Usage |
|---|---|
| `@RestController`, `@RequiredArgsConstructor` | Controller |
| `@Service`, `@Transactional` | Service |
| `@Transactional(readOnly = true)` | Toutes les lectures |
| `@Slf4j` | Toute classe qui loggue |

---

## 2. Style

| Règle | Exemple |
|---|---|
| Validation d'entrée en tête de méthode | `Objects.requireNonNull(toilet, "toilet must not be null")` |
| Erreur métier | `ResourceNotFoundException` → 404 |
| Logs avec placeholders | `log.debug("Fetching toilets page {}", page)` — **pas de concaténation** |
| Imports explicites | `import java.util.List;` — `import java.util.*;` **interdit** |
| Pas de `System.out.println` | Utiliser le logger |
| Pas de secrets en dur | Mots de passe, keystore, tokens → variables d'environnement |
| Java 25 autorisé | `var`, text blocks, pattern matching, `record` |
| Streams | Chaque étape non triviale devient une **méthode privée nommée** (ex. `enrichWithDistance`) |

---

## 3. Javadoc (obligatoire sur toute méthode publique)

```java
/**
 * Description fonctionnelle : ce que fait la méthode et pourquoi.
 *
 * @param nomParam description, format attendu, null accepté ou non
 * @return description de la valeur retournée
 * @throws ResourceNotFoundException si ...
 */
```

- Références via `{@link Xxx}` et `{@code ...}`.
- Sur une implémentation : `{@inheritDoc}` + complément si le comportement diffère.
- **Langue** : Javadoc, commentaires et messages de log en **français** ; identifiants en anglais ; messages d'erreur destinés à l'API et noms de tests en **anglais**.

---

## 4. Interdits absolus

| Interdit | Pourquoi |
|---|---|
| Éditer `web/api/**` et `web/dto/**` | Code **généré** depuis le YAML OpenAPI |
| Modifier `Waste/**` et `developpement/**` | Code mort / archives |
| Supprimer un test existant | La couverture ne doit jamais régresser |
| Abaisser un seuil de couverture JaCoCo | Objectif > 80 % |
| Committer `keystore.jks`, `.env`, credentials PostgreSQL | Secrets |

---

## 5. Validation obligatoire après chaque changement

| Zone | Commande |
|---|---|
| Backend | `mvn -q test` (depuis `WcPregnantService/`) |
| Frontend | `npm run build` puis `npm run test` (depuis `WcPregnantApp/`) |

Aucune tâche n'est déclarée terminée sans cette validation. Voir [strategie-de-tests.md](../04-tests/strategie-de-tests.md).
