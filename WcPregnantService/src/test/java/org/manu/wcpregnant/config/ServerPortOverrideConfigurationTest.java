package org.manu.wcpregnant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la surcharge du port HTTP dédié par la variable {@code API_PORT}.
 *
 * <p>Le port {@code 14007} n'est qu'un défaut : il doit pouvoir être remplacé sans
 * toucher au code ni à {@code application.properties}, ce qui est indispensable en
 * conteneur (Docker, Kubernetes) où le port est injecté par l'environnement.</p>
 *
 * @see ServerPortConfigurationTest
 */
@SpringBootTest(properties = "API_PORT=15999")
@DisplayName("Configuration : surcharge du port dédié par API_PORT")
class ServerPortOverrideConfigurationTest {

    /** Port résolu lorsque {@code API_PORT} est fourni (défaut : port dédié). */
    @Value("${server.port:14007}")
    private String overriddenPort;

    /**
     * Nominal : la valeur de {@code API_PORT} prime sur le défaut {@code 14007}.
     */
    @Test
    void serverPort_whenApiPortIsProvided_usesOverride() {
        // Act
        String resolvedPort = overriddenPort;

        // Assert
        assertThat(resolvedPort).isEqualTo("15999");
    }

    /**
     * Cas limite : la surcharge reste dans la bande réservée au projet et ne
     * retombe jamais sur le port générique {@code 8080}.
     */
    @Test
    void serverPort_whenApiPortIsProvided_staysOutsideGenericPorts() {
        // Arrange
        int port = Integer.parseInt(overriddenPort);

        // Assert
        assertThat(port).isBetween(11000, 49151);
        assertThat(port).isNotEqualTo(8080);
    }
}
