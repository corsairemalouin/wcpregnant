package org.manu.wcpregnant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cglib.core.internal.Function;
import org.springframework.context.annotation.Bean;

/**
 * Point d'entrée de l'application WcPregnant (API REST de localisation de
 * toilettes publiques accessibles).
 *
 * <p>La application est configurée par Spring Boot : la sécurité, l'accès aux
 * données et l'agent Tool Calling sont apportés par les beans déclarés dans
 * les packages {@code config} et {@code agent}.</p>
 */
@SpringBootApplication
public class WcpregnantApplication {

    /**
     * Démarre le serveur Spring Boot.
     *
     * <p>C'est la seule méthode exécutée lors du lancement du processus : elle
     * initialise le contexte applicatif, la connexion à la base de données et
     * les filtres de sécurité avant d'écouter le port HTTP configuré.</p>
     *
     * @param args arguments de ligne de commande transmis à Spring Boot
     *             (par exemple {@code --spring.profiles.active=dev})
     */
    public static void main(String[] args) {
        SpringApplication.run(WcpregnantApplication.class, args);
    }

    /**
     * Fournit la fonction d'enrichissement des messages de log de l'application.
     *
     * <p>Chaque valeur de log transmise est préfixée par l'identifiant du
     * contexte, ce qui facilite le repérage des lignes émises par WcPregnant
     * dans les logs agrégés du cluster.</p>
     *
     * @return une fonction transformant {@code "message"} en
     *         {@code "[Baeldung] - message"}
     */
    @Bean
    public Function<String, String> enrichLogMessage() {
        return value -> "[%s] - %s".formatted("Baeldung", value);
    }

}
