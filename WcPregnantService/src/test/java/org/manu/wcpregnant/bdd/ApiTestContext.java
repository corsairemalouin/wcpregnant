package org.manu.wcpregnant.bdd;

import org.manu.wcpregnant.WcpregnantApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Contexte d'intégration partagé par toutes les étapes Cucumber de la suite
 * de non-régression de l'API.
 *
 * <p>L'application Spring Boot est démarrée une seule fois (base H2 en mémoire
 * dédiée, profil {@code dev} pour disposer des comptes de démonstration et du
 * jeu de données autour de Rennes). {@code MockMvc} permet ensuite d'exercer
 * l'API réelle — sécurité, validation, services, repository — sans réseau.</p>
 */
public final class ApiTestContext {

    /** Instance unique de MockMvc construite sur l'application de test. */
    private static MockMvc mockMvc;

    private ApiTestContext() {
        throw new IllegalStateException("ApiTestContext is a utility class and must not be instantiated");
    }

    /**
     * Retourne le {@code MockMvc} de l'application de non-régression, en le
     * créant au premier appel.
     *
     * <p>La configuration applicative est fixée explicitement pour rendre les
     * scénarios déterministes : distance maximale de 50 km et 5 résultats
     * maximum pour la recherche de proximité.</p>
     *
     * @return le {@code MockMvc} configuré avec la chaîne de sécurité Spring
     */
    public static synchronized MockMvc mockMvc() {
        if (mockMvc == null) {
            ApplicationContext application = SpringApplication.run(
                    WcpregnantApplication.class,
                    "--spring.profiles.active=dev",
                    "--spring.datasource.url=jdbc:h2:mem:bdddb;DB_CLOSE_DELAY=-1",
                    "--spring.jpa.hibernate.ddl-auto=create",
                    "--app.distance.max=50",
                    "--app.result.max=5"
            );

            mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) application)
                    .apply(SecurityMockMvcConfigurers.springSecurity())
                    .build();
        }
        return mockMvc;
    }
}
