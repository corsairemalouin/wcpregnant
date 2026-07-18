# .cline — Configuration Cline pour WcPregnantService

Ce répertoire contient la configuration, les prompts et les workflows Cline pour automatiser l'ajout de Javadoc et de tests unitaires sur le projet **WcPregnantService**.

## Arborescence

```
.cline/
├── README.md                  ← Ce fichier
├── config/
│   └── cline-config.json      ← Configuration générale du projet
├── prompt/
│   └── javadoc-tu.md          ← Prompt principal : Javadoc + TU >80% (avec amélioration continue)
└── workflows/
    ├── process-class.ps1      ← Script : traite UNE classe
    └── process-all.ps1        ← Script : traite TOUTES les classes
```

## Utilisation

### 1. Traiter une classe spécifique

```powershell
.cline\workflows\process-class.ps1 -ClassName "ToiletteServiceImpl"
```

Ce script affiche :
- Le chemin du fichier source
- Le chemin du fichier de test (ou "À créer")
- La liste des méthodes publiques détectées
- Les commandes Cline à exécuter
- L'étape d'amélioration continue

### 2. Traiter toutes les classes

```powershell
.cline\workflows\process-all.ps1
```

Ce script analyse toutes les classes du projet et génère un rapport complet avec :
- Le statut des tests existants
- Le nombre de méthodes par classe
- Les commandes Cline à exécuter dans l'ordre
- L'étape d'amélioration continue après chaque classe

### 3. Processus manuel avec Cline

Pour chaque classe, le processus est le suivant :

1. **Lire le fichier source** avec `read_file`
2. **Lire le fichier de test** existant avec `read_file`
3. **Appliquer le prompt** `.cline/prompt/javadoc-tu.md` qui décrit :
   - L'analyse de chaque méthode
   - L'ajout du Javadoc
   - La complétion des TU (nominal, exception, null)
   - L'amélioration continue du script
4. **Valider** avec `mvn test -Dtest=MaClasseTest -Dsurefire.useFile=false`

## Règles

- Ne jamais modifier le comportement métier existant
- Ne jamais supprimer de TU existant
- Ajouter Javadoc sur chaque méthode publique
- Cibler >80% de couverture par méthode
- Utiliser Mockito pour les dépendances
- Convention de nommage : `method_scenario_expectedBehavior`
- **Amélioration continue** : après chaque classe traitée, mettre à jour le prompt et les scripts

## Contenu du prompt `javadoc-tu.md`

Le prompt décrit en détail :

- **Étape 1** : Contexte (lire la source, l'interface, les dépendances)
- **Étape 2** : Pour chaque méthode :
  - **2a** : Analyser (nom, paramètres, retour, dépendances, exceptions)
  - **2b** : Ajouter le Javadoc (description, @param, @return, @throws)
  - **2c** : Vérifier/compléter les TU (nominal, exception, null, propagation)
- **Étape 3** : Valider avec `mvn test`
- **Étape 4** : Amélioration continue du script (difficultés, patterns, feedback)

## Exemple

Pour traiter `ToiletteServiceImpl` :

```powershell
.cline\workflows\process-class.ps1 -ClassName "ToiletteServiceImpl"
```

Puis dans Cline, exécuter les commandes indiquées par le script.