package org.manu.wcpregnant.bdd;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * Point d'entrée de la suite de non-régression fonctionnelle (BDD) de l'API.
 *
 * <p>Chaque exigence fonctionnelle référencée dans
 * {@code documentation/01-exigences/exigences-metier.md} est couverte par au moins un
 * scénario Cucumber exécuté ici. La suite est déclenchée par {@code mvn test}
 * et doit rester verte avant toute évolution du service.</p>
 */
@RunWith(Cucumber.class)
@CucumberOptions(
        features = "src/test/resources/features/api",
        glue = "org.manu.wcpregnant.bdd.steps"
)
public class NonRegressionApiTest {
}
