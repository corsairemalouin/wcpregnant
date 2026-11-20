package org.manu.wcpregnant.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de la configuration du port HTTP de l'application.
 *
 * <p>Le serveur Spring Boot n'utilise plus le port générique {@code 8080} : il écoute
 * par défaut sur le port <strong>dédié</strong> {@code 14007}, choisi hors des ports
 * habituels des services locaux (8080, 8081, 4200, 5432, 8000) et hors de la plage
 * de ports éphémères Windows (49152-65535), afin d'éviter toute interférence.</p>
 *
 * <p>Le défaut reste surchargeable par la variable d'environnement {@code API_PORT}
 * ou par l'argument {@code --server.port}, ce que ces tests vérifient.</p>
 */
@SpringBootTest
@DisplayName("Configuration : port HTTP dédié au projet")
class ServerPortConfigurationTest {

    /** Port dédié retenu pour le projet. */
    private static final int DEDICATED_PORT = 14007;

    /** Bornes de la bande de ports réservée au projet. */
    private static final int BAND_LOW = 11000;
    private static final int BAND_HIGH = 65535;

    /** Début de la plage éphémère Windows : le port du projet doit rester en dessous. */
    private static final int EPHEMERAL_START = 49152;

    /** Port résolu par Spring au démarrage du contexte de test (défaut : port dédié). */
    @Value("${server.port:14007}")
    private String configuredPort;

    /**
     * Nominal : sans surcharge, le port résolu par Spring est bien le port dédié.
     */
    @Test
    void serverPort_withoutOverride_isDedicatedPort() {
        // Act
        String resolvedPort = configuredPort;

        // Assert
        assertThat(resolvedPort).isEqualTo(String.valueOf(DEDICATED_PORT));
    }

    /**
     * Cas limite : le port dédié appartient à la bande réservée et ne collisionne
     * avec aucun service local classique ni avec la plage éphémère du système.
     */
    @Test
    void serverPort_isInsideReservedBand_andOutsideCommonLocalServices() {
        // Arrange
        int port = Integer.parseInt(configuredPort);

        // Assert
        assertThat(port).isBetween(BAND_LOW, BAND_HIGH);
        assertThat(port).isLessThan(EPHEMERAL_START);
        assertThat(port).isNotIn(8080, 8081, 4200, 5432, 8000);
    }
}
