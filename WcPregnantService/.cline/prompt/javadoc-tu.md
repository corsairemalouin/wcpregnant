# Prompt Cline : Ajout Javadoc + TU couverture >80%

## Objectif

Pour chaque classe du projet, appliquer le processus suivant de manière **itérative**, méthode par méthode.

---

## Processus itératif pour chaque classe

### Étape 1 : Contexte

> À exécuter une fois par classe avant de commencer.

1. Lister les fichiers sources (`src/main/**/*.java`) et de test (`src/test/**/*.java`)
2. Pour la classe ciblée (ex: `MaClasseServiceImpl`), lire :
   - Le fichier source `src/main/java/.../MaClasseServiceImpl.java`
   - L'interface qu'elle implémente (chercher `implements`)
   - Le fichier de test existant `src/test/java/.../MaClasseServiceImplTest.java` (s'il existe)
3. Analyser les dépendances (mappers, repositories) pour comprendre le mapping

---

### Étape 2 : Pour chaque méthode de la classe

Répéter les sous-étapes suivantes pour **chaque méthode publique** de la classe :

#### 2a. Analyser la méthode

Identifier :
- Le nom et le rôle fonctionnel
- Les paramètres (nom, type, nullable ou non)
- La valeur de retour
- Les dépendances appelées (mapper.toEntity, mapper.toDto, repository.save, repository.findById, etc.)
- Les exceptions possibles
- Les logs éventuels

#### 2b. Ajouter le Javadoc

Ajouter un header Javadoc **avant la signature de la méthode** avec la structure :

```java
/**
 * [Description fonctionnelle : ce que fait la méthode, pourquoi, dans quel contexte]
 *
 * @param [nomParam] [Description du paramètre : ce qu'il représente, format attendu, si null est accepté]
 * @return [Description de la valeur retournée, ce qu'elle contient, si null possible]
 * @throws [ExceptionType] [Quand cette exception est levée]
 */
```

#### 2c. Vérifier / Compléter les TU

1. **Lire** le fichier de test existant pour la classe
2. **Pour chaque méthode**, s'assurer qu'il existe **au moins 3 tests** :
   - ✅ **Test nominal** : cas normal → vérifie que le retour est correct et que les dépendances sont appelées avec les bons paramètres (utiliser `verify`)
   - ⚠️ **Test d'exception métier** : si la méthode peut lever une exception métier vérifier qu'elle est bien propagée
   - 🚫 **Test de null** (si applicable) : appel avec paramètre `null` → vérifie la `NullPointerException` ou l'exception appropriée
   - **Test de propagation** : si la méthode appelle `repository.save()` ou autre, vérifier que les exceptions runtime sont propagées

3. **Si le test n'existe pas**, l'**ajouter** dans le fichier de test existant

4. **Structure de test recommandée** (Mockito + JUnit 5) :
   ```java
   @Test
   void methodName_shouldDoSomething_whenCondition() {
       // Arrange
       when(repository.method(any())).thenReturn(expectedEntity);
       
       // Act
       var result = service.method(inputBO);
       
       // Assert
       assertThat(result).isEqualTo(expectedBO);
       verify(repository).method(inputEntity);
       verify(mapper).boToDao(inputBO);
   }
   ```

---

### Étape 3 : Valider

Après avoir traité **toutes les méthodes d'une classe** :

```bash
mvn test -pl . -Dtest=MaClasseServiceImplTest -Dsurefire.useFile=false
```

- ✅ Tous les tests passent (vert)
- ❌ Un test échoue → corriger le code ou le test, puis relancer

---

### Étape 4 : Amélioration continue du script

Après avoir traité une classe, analyser les points suivants pour améliorer le processus :

#### 4a. Difficultés rencontrées
- Y a-t-il eu des ambiguïtés dans l'analyse des dépendances ?
- Certains patterns de code étaient-ils difficiles à tester ?
- Y a-t-il eu des faux positifs/négatifs dans l'extraction des méthodes ?

#### 4b. Mise à jour des patterns

Ajouter ou ajuster dans `.cline/prompt/javadoc-tu.md` :
- **Nouveaux patterns de méthodes** : si des méthodes avec des signatures particulières ont été difficiles à détecter
- **Nouvelles dépendances** : si de nouveaux types de dépendances (feign clients, message queues, etc.) ont été rencontrés, ajouter leur traitement
- **Cas particuliers** : méthodes void, méthodes avec Optional, méthodes avec pagination

#### 4c. Mise à jour des scripts workflow

- Ajouter de nouveaux exclusions patterns dans les scripts `.ps1` si nécessaire
- Ajuster la détection des fichiers de test (parfois le suffixe est `UnitTest` plutôt que `Test`)
- Ajouter des logs pour mieux tracer l'avancement

#### 4d. Feedback au script

Mettre à jour `.cline/prompt/javadoc-tu.md` avec la section "Retour d'expérience" :

```markdown
### Retour d'expérience — Classe traitée : [NomClasse]

**Difficultés :**
- [Liste des difficultés rencontrées]

**Patterns améliorés :**
- [Patterns ajoutés/modifiés]

**Script amélioré :**
- [Améliorations apportées aux workflows]
```

---

## Checklist de progression

```
Classe : ToiletteServiceImpl
- [ ] addToilet      → Javadoc ✓  TU (nominal, exception, null) ✓
- [ ] getToiletById  → Javadoc ✓  TU (nominal, notFound, null) ✓
- [ ] updateToilet   → Javadoc ✓  TU (nominal, notFound, null) ✓
- [ ] deleteToilet   → Javadoc ✓  TU (nominal, notFound, null) ✓
- [ ] ...            → ...

Classe : CustomerServiceImpl
- [ ] createCustomer → Javadoc ✓  TU (nominal, conflict, null) ✓
- [ ] ...            → ...
```

---

## Règles importantes

1. **Ne jamais modifier le comportement métier** existant — seulement ajouter Javadoc et TU
2. **Ne jamais supprimer de TU existant** — seulement en ajouter
3. **Nommer les tests** selon la convention `method_scenario_expectedBehavior`
4. **Un seul test par scénario** — éviter les tests trop longs qui testent plusieurs choses
5. **Utiliser Mockito** pour mocker les dépendances (repository, mapper)
6. **Vérifier les appels** avec `verify()` pour s'assurer que les dépendances sont bien utilisées
7. **Cibler >80% de couverture** : chaque ligne de code métier doit être exécutée par au moins un test
8. **Amélioration continue** : après chaque classe traitée, mettre à jour ce prompt et les scripts pour intégrer les apprentissages

---

## Exemple de résultat attendu

### Source (avant)

```java
public Toilet addToilet(ToiletteBO toiletteBO) {
    log.info("Add toilet");
    Toilet toilet = mapper.boToDao(toiletteBO);
    Toilet saved = repository.save(toilet);
    return mapper.daoToBo(saved);
}
```

### Source (après)

```java
/**
 * Ajoute une nouvelle toilette en base de données.
 * Mappe l'objet métier vers l'entité JPA, la persiste, puis retourne l'objet avec l'ID généré.
 *
 * @param toiletteBO l'objet métier contenant les informations de la toilette (libellé, adresse, position, grade)
 *                   Ne doit pas être null.
 * @return l'objet Toilet sauvegardé, incluant l'ID généré par la base de données
 * @throws NullPointerException si toiletteBO est null
 */
public Toilet addToilet(ToiletteBO toiletteBO) {
    log.info("Add toilet");
    Toilet toilet = mapper.boToDao(toiletteBO);
    Toilet saved = repository.save(toilet);
    return mapper.daoToBo(saved);
}
```

### Test ajouté

```java
@Test
void addToilet_shouldMapSaveAndReturnToilet() {
    // Arrange
    when(mapper.boToDao(toiletteBO)).thenReturn(toiletEntity);
    when(repository.save(toiletEntity)).thenReturn(savedEntity);
    when(mapper.daoToBo(savedEntity)).thenReturn(expectedToilet);
    
    // Act
    Toilet result = toiletteService.addToilet(toiletteBO);
    
    // Assert
    assertThat(result).isEqualTo(expectedToilet);
    verify(log).info("Add toilet");
    verify(mapper).boToDao(toiletteBO);
    verify(repository).save(toiletEntity);
    verify(mapper).daoToBo(savedEntity);
}