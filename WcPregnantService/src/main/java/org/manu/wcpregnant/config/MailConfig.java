package org.manu.wcpregnant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Configuration du serveur d'envoi de mails (SMTP).
 *
 * <p>Le bean {@link JavaMailSender} est construit explicitement à partir des
 * propriétés {@code auth.mail.*} : aucun serveur, identifiant ou mot de passe
 * n'est codé en dur, tout provient de variables d'environnement. L'envoi n'est
 * réellement tenté que lorsque {@code auth.mail.enabled} vaut {@code true} :
 * le service de réinitialisation teste ce drapeau avant d'appeler le sender.</p>
 */
@Configuration
public class MailConfig {

    /**
     * Crée le sender SMTP utilisé pour transmettre les codes de réinitialisation.
     *
     * @param host     hôte SMTP (vide : localhost)
     * @param port     port SMTP
     * @param username identifiant SMTP, vide pour un serveur sans authentification
     * @param password mot de passe SMTP, fourni uniquement par variable d'environnement
     * @return un {@link JavaMailSenderImpl} configuré
     */
    @Bean
    public JavaMailSender mailSender(@Value("${auth.mail.host:localhost}") String host,
                                     @Value("${auth.mail.port:25}") int port,
                                     @Value("${auth.mail.username:}") String username,
                                     @Value("${auth.mail.password:}") String password) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);

        return sender;
    }
}