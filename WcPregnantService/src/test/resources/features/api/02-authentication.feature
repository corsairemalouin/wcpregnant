Feature: EF-01 — Authentification obligatoire de l'API
  Aucune ressource de l'API n'est accessible sans identifiants valides.

  Scenario: La liste des toilettes est refusée aux requêtes anonymes
    When I call GET /toilets without authentication
    Then the response status is 401

  Scenario: Le détail d'une toilette est refusé aux requêtes anonymes
    When I call GET /toilets/1 without authentication
    Then the response status is 401

  Scenario: Un utilisateur authentifié peut consulter la liste
    When I call GET /toilets as manu with password manu
    Then the response status is 200

  Scenario: Des identifiants invalides sont refusés
    When I call GET /toilets as manu with password wrong
    Then the response status is 401
