# Garde-fous (toujours actifs)

## Interdits absolus

- **Ne jamais toucher** à : `Waste/**`, `developpement/**`, `WcPregnantService/target/**`, `WcPregnantApp/node_modules/**`, `.idea/**`.
- **Ne jamais régénérer ni éditer à la main** le code généré par OpenAPI : `WcPregnantService/src/main/java/org/manu/wcpregnant/web/api/**` et `.../web/dto/**`. Pour changer un contrat, modifier le YAML dans `src/main/resources/api/` et relancer la génération.
- **Ne jamais supprimer** `src/test/**` ni abaisser un seuil de couverture JaCoCo.
- **Ne jamais commiter** : `keystore.jks`, `.env`, credentials PostgreSQL, images `*.png` de `target/`.

## Prudence

- `docker-compose.yml` et `k8s/secret.yaml` contiennent des mots de passe de démonstration : ne les remplacer pas par de vrais secrets dans le dépôt, renvoyer l'utilisateur vers les variables d'environnement.
- Avant une suppression de fichier, lister le contenu et confirmer la cible exacte.
- Le projet est un monorepo : ne jamais appliquer une modification backend au frontend (et inverse) sans que la tâche le demande.
