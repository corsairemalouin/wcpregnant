---
name: javadoc-test-coverage
description: Ajoute du Javadoc et des tests unitaires Java pour atteindre >80% de couverture JaCoCo. Utiliser pour "ajouter des TU", "augmenter la couverture", "documenter une classe", "compléter les tests d'un service", ou quand un rapport JaCoCo montre des classes sous le seuil.
---

# Javadoc + couverture de tests (>80%)

Objectif : documenter et tester une classe **méthode par méthode**, sans jamais modifier le comportement métier.

## Processus

1. **Contexte** — lire la classe cible, l'interface qu'elle implémente, son fichier de test existant, ses mappers/repository.
2. **Inventaire** — lister les méthodes publiques et, pour chacune, les scénarios manquants :
   - nominal (retour + `verify` des dépendances)
   - exception métier (`ResourceNotFoundException`, conflit)
   - null / cas limite
   - propagation des exceptions runtime du repository
3. **Javadoc** — ajouter le bloc avant chaque méthode publique :
   ```java
   /**
    * Description fonctionnelle (quoi, pourquoi, dans quel contexte).
    *
    * @param x description, format, null accepté ou non
    * @return valeur retournée
    * @throws ResourceNotFoundException si ...
    */
   ```
4. **Tests** — les ajouter dans le fichier de test existant (JUnit 5 + Mockito), nommés `method_scenario_expectedBehavior`, structurés Arrange/Act/Assert.
5. **Valider** :
   ```powershell
   mvn -q test -Dtest=ToiletteServiceImplTest
   mvn jacoco:report
   ```
6. **Mesurer** — lancer le script de contrôle :
   ```powershell
   .cline/skills/javadoc-test-coverage/scripts/coverage-check.ps1 -Threshold 80
   ```
   Il liste les classes sous le seuil ; reprendre l'étape 2 pour les classes restantes.

## Règles non négociables

- Aucun test supprimé, aucun comportement métier modifié.
- Un test par scénario ; mocker repository/mapper, jamais la classe testée.
- Pour les mocks fragiles : `when(repository.save(any(ToiletDAO.class))).thenReturn(...)` plutôt qu'un objet précis.

Pour la checklist détaillée par classe, voir [docs/checklist.md](docs/checklist.md).
