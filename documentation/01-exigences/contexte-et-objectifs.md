# Contexte et objectifs

> **Source** : cahier des charges [`assets/DAL_WcPregnant_V1.0.docx`](../assets/DAL_WcPregnant_V1.0.docx) (V1.0), sections *Contexte* et *Contraintes*. Texte brut d'origine : [`assets/dal_extract.txt`](../assets/dal_extract.txt).

---

## 1. Contexte du produit

L'idée est de fournir une application permettant de **géolocaliser rapidement des toilettes à proximité pour les femmes enceintes**.

- L'application est accessible **depuis un mobile Android ou une page web**.
- Elle géolocalise l'utilisateur et fournit **une carte avec les WC à proximité**, assortie d'une **note d'accueil** attribuée par les propriétaires (`P` si public).
- Des informations plus détaillées sont disponibles :
  - photo ;
  - propreté ;
  - accueil ;
  - espace.
- L'utilisateur peut **référencer de nouvelles toilettes** et **noter lui-même** les toilettes conseillées.

---

## 2. Objectifs

| # | Objectif | Critère de réussite |
|---|---|---|
| O1 | Trouver une toilette accessible en quelques secondes | Liste des 5 toilettes les plus proches, triées par distance |
| O2 | Fiabiliser l'information | Notation multi-critères + moderation par les administrateurs |
| O3 | Contribuer collectivement | Ajout de toilettes, de photos et de notes par les utilisateurs |
| O4 | Produire avec un budget et un temps minimaux | Stack open source, génération de code à partir d'OpenAPI, conteneurisation |
| O5 | Déployer sans infrastructure dédiée | Conteneurs Docker + manifestes Kubernetes, cible AWS via Terraform |

---

## 3. Contraintes non techniques

| Contrainte | Impact sur la solution |
|---|---|
| **Faible budget de réalisation** | Outils et frameworks open source uniquement |
| **Pas d'infrastructure** | Déploiement conteneurisé, aucune gestion de serveur à maintenir |
| **Peu de temps de production et de maintenance** | Code généré (OpenAPI), conventions strictes, automatisation du build |

---

## 4. Écart entre le cahier des charges V1.0 et l'implémentation actuelle

Le cahier des charges a été rédigé avant le développement ; certaines hypothèses ont évolué. Les points d'attention suivants doivent être connus avant toute évolution :

| Élément du cahier des charges V1.0 | Réalité du projet |
|---|---|
| Carte **Google Maps** | Carte **Leaflet** (`WcPregnantApp`) |
| Front **Node JS / Angular** | Angular 20 (TypeScript 5.8) |
| Back **Java 21 / Spring Boot 3.5** | Java 25 / Spring Boot 4.1.0 |
| Objets métier suffixés **`BO`** | Le BO intermédiaire n'est pas utilisé : mapping direct `DAO` ↔ `DTO` via MapStruct |
| 4 modules IntelliJ (front, middle, back, déploiement) | Monorepo : `WcPregnantService`, `WcPregnantApp`, `k8s` |
| Description **Terraform** pour AWS | Non implémentée ; Docker + Kubernetes uniquement |

Ces écarts sont volontaires : ils sont documentés dans [architecture.md](../02-conception/architecture.md).
