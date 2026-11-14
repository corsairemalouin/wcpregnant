# Standards de code (toujours actifs)

- **Ne jamais changer le comportement métier existant** lors d'une amélioration de docs ou de tests : les ajouts sont additifs.
- **Ne jamais supprimer un test existant** ; en ajouter ou corriger uniquement.
- Avant toute modification, lire le fichier cible et ses dépendances (interface implémentée, mapper, repository).
- Préférer la **composition par interfaces** (DIP) : un controller dépend d'interfaces de service, jamais d'une implémentation concrète.
- Validation d'arguments en entrée de méthode : `Objects.requireNonNull(x, "x must not be null")` ou `Assert.notNull(...)`.
- Erreur métier : `ResourceNotFoundException` (404) — ne pas inventer d'autres types d'erreur sans accord.
- Logs : `@Slf4j` + placeholders (`log.debug("Fetching toilets page {}", page)`), jamais de concatination dans le log.
- Imports explicites, **pas de wildcard** (`import java.util.*` interdit).
- Pas de `System.out.println` : utiliser le logger.
- Pas de secrets en dur (mots de passe, keystore, tokens) dans le code ni dans les manifests.
- Toute nouvelle dépendance Maven/npm doit être justifiée et ajoutée avec une version explicite.

## Validation obligatoire après chaque changement

- Backend : `mvn -q test` (depuis `WcPregnantService/`) — tous les tests doivent passer.
- Frontend : `npm run test` ou `npm run build` (depuis `WcPregnantApp/`).
- Ne jamais déclarer une tâche terminée sans avoir exécuté la validation correspondante.
