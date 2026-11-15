Feature: EF-13 — Jeu de données de démonstration
  Le profil "dev" alimente la base avec 10 toilettes autour de Rennes afin
  de permettre une démonstration sans base PostgreSQL externe.

  Scenario: La base de démonstration est chargée au démarrage
    When I call GET /toilets?page=0&size=20 as manu with password manu
    Then the response status is 200
    And the response contains 10 toilets

  Scenario: Le seed n'est pas exécuté deux fois
    When I call GET /toilets?page=0&size=20 as manu with password manu
    Then the response contains 10 toilets
    When I call GET /toilets?page=0&size=20 as manu with password manu
    Then the response contains 10 toilets
