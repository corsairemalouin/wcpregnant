# Archive — brief initial soumis à Copilot

> **Source** : [`assets/PromptCopilot.txt`](../assets/PromptCopilot.txt) (fichier brut, encodage Latin-1). Ce document est la transcription lisible du brief qui a servi d'amorce au développement, complété par la réponse de Copilot. Il n'a aucune valeur normative : les exigences validées sont dans [exigences-metier.md](exigences-metier.md).

---

## 1. Le brief

> Ceci est le code d'une application (le backend) dont l'objectif est de fournir un service de localisation de toilettes pour les femmes enceintes.
>
> 3 profils d'utilisateurs :
> - **admin** qui, à travers une IHM dédiée, administre les BO ;
> - **propriétaire de toilette** (via contact vers le support, qui traite les demandes en tant qu'admin) ;
> - **usager femme enceinte** (via application mobile).
>
> Je veux une optimisation complète du code.
> Je veux une couverture maximale du code en tests unitaires.
>
> Adaptez mon IHM pour couvrir les besoins de l'application et interagir avec le backend. L'application sert à géolocaliser des toilettes sur une carte : elle nous géolocalise et fournit une carte avec les toilettes à proximité, avec la possibilité de définir la distance maximale à parcourir. L'IHM est divisée en 2 : la carte et la liste des toilettes avec leur détail (note, photos…) dans l'ordre de proximité.

Le brief se poursuit avec le même cahier des charges que celui décrit dans [contexte-et-objectifs.md](contexte-et-objectifs.md) et [contraintes-techniques.md](contraintes-techniques.md), et se termine par :

> Optimise et améliore le code de cette application Angular ; formate-la à un niveau professionnel.

---

## 2. Réponse de Copilot (synthèse de l'époque)

Copilot proposait une architecture en trois briques :

| Brique | Proposition de Copilot | Retenue finalement |
|---|---|---|
| Frontend | Node.js + React ou Vue.js, Leaflet ou Google Maps | Angular 20 + Leaflet |
| Backend | Java 21 + Spring Boot 3.5, API RESTful CRUD, services de sécurité | Java 25 + Spring Boot 4.1.0, Spring Security |
| Base de données | PostgreSQL avec index géospatiaux | PostgreSQL (index géospatiaux : encore à faire) |

**Fonctionnalités principales retenues** : trouver des toilettes (géolocalisation, carte, liste des 5 plus proches), noter des toilettes (conformité, qualité des photos, commentaires, photos), référencer des toilettes (formulaire avec géolocalisation automatique ou saisie d'adresse).

**Rôles et permissions** : utilisateur (rechercher, noter, référencer, historique), administrateur (corriger les données, modérer), propriétaire (gérer ses toilettes), annonceurs (financer contre visibilité).

**Contraintes techniques rappelées** : faible budget et peu de temps → open source ; pas d'infrastructure → cloud managé ; facilité de développement → pratiques agiles.

---

## 3. Ce qui en a découlé

| Élément du brief | Trace dans le code actuel |
|---|---|
| « couverture maximale du code en TU » | JaCoCo, seuil > 80 %, voir [javadoc-et-couverture-tests.md](../04-tests/javadoc-et-couverture-tests.md) |
| « distance maximale à parcourir » | `app.distance.max` dans `ToiletteServiceConfig` |
| « IHM divisée en 2 : carte + liste » | Composant Leaflet + liste de proximité dans `WcPregnantApp` |
| « administre les BO » | Rôle `ADMIN` requis sur POST/PUT/DELETE, voir [contrat-api-openapi.md](../02-conception/contrat-api-openapi.md) |
