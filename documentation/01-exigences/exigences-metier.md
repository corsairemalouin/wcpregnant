# Exigences métier

> **Source** : cahier des charges [`assets/DAL_WcPregnant_V1.0.docx`](../assets/DAL_WcPregnant_V1.0.docx), sections *Concepts / Business*.

---

## 1. Rôles (Business Roles)

| Rôle | Description | Droits |
|---|---|---|
| **Utilisateur** | Femme enceinte ayant une envie pressante | Recherche, notation, ajout de toilettes et de photos |
| **Administrateur** | Corrige les données | Correction / suppression des données incorrectes, modération |
| **Parasite** | Utilisateur voulant profiter du système sans être enceint | Aucun droit spécifique : détecté par la notation de confiance |
| **Propriétaire** | Personne à qui appartiennent les WC | Contacte le support, qui traite la demande en tant qu'administrateur |
| **Annonceur** | Finance le projet | Visibilité dans l'application |

---

## 2. Use cases « Toilettes »

### 2.1 Trouver des toilettes

Un utilisateur ayant besoin d'une toilette est géolocalisé ; on lui fournit la liste des toilettes connues **les plus proches de lui**.

L'affichage est scindé en **trois parties** :

1. **Une carte** où une puce identifie chacune des toilettes retournées ;
2. **Une liste des 5 toilettes les plus proches**, classées par distance ;
3. **Une zone de détails** affichant les informations de la toilette sélectionnée.

> La distance maximale à parcourir est paramétrable (`app.distance.max`, voir [architecture.md](../02-conception/architecture.md)).

### 2.2 Noter des toilettes

Après avoir utilisé une toilette, l'utilisateur peut la noter. Critères de notation :

- la **conformité** avec la description enregistrée ;
- la **qualité intrinsèque des photos** ;
- l'annotation des **caractéristiques** de la toilette (accueil, propreté, espace) ;
- l'ajout de **photos descriptives**.

### 2.3 Référencer des toilettes

- Enregistrement de nouvelles toilettes dans l'application.
- La localisation correspond à la **géolocalisation courante** de l'utilisateur, mais l'adresse peut aussi être **saisie directement**.
- Possibilité d'ajouter des **photos**.
- Note de **0 à 5**.

---

## 3. Use cases « Utilisateurs »

- Un utilisateur **s'enregistre** sur la plateforme. Il fournit :
  - un nom ;
  - un prénom ;
  - une adresse mail.
- Un utilisateur **se connecte** : il est authentifié, toutes les actions qu'il réalise lui sont attachées et **historisées**.
- Un utilisateur possède **une note sur 5** (score de confiance) dépendant de :
  - du nombre de toilettes qu'il partage et de la bonne note attribuée à celles-ci ;
  - de sa **fréquence d'utilisation** de l'application ;
  - du **faible taux de correction** sur les toilettes qu'il a partagé, en particulier si la note diminue.

---

## 4. Use cases « Administrateur »

Les administrateurs disposent d'accès privilégiés leur permettant de **corriger ou effacer des données incorrectes**, notamment :

- si des commentaires concordants montrent que les toilettes enregistrées ne correspondent pas à la réalité ;
- si le propriétaire des toilettes se plaint.

---

## 5. Exigences non fonctionnelles

| Catégorie | Exigence |
|---|---|
| Performance | Recherche de proximité en temps réel (formule Haversine, voir [modele-de-donnees.md](../02-conception/modele-de-donnees.md)) |
| Simplicité | Faible budget, pas d'infrastructure, peu de temps de maintenance |
| Confiance | Score utilisateur + modération administrative |
| Accessibilité | Interface utilisable en situation d'urgence : liste courte, tri par distance |
