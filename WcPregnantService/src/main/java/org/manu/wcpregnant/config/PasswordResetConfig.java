package org.manu.wcpregnant.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la réinitialisation du mot de passe.
 *
 * <p>Toutes les valeurs proviennent de variables d'environnement : aucun secret
 * ni adresse de serveur mail n'est codé en dur dans l'application, conformément
 * aux règles du projet.</p>
 */
@Getter
@Configuration
public class PasswordResetConfig {

    /** Durée de validité du code de réinitialisation, en secondes. */
    @Value("${auth.reset.code-ttl-seconds:900}")
    private int codeTtlSeconds;

    /** Envoi réel du code par e-mail ; sinon mode démonstration (code renvoyé dans la réponse). */
    @Value("${auth.mail.enabled:false}")
    private boolean mailEnabled;

    /** Adresse d'expédition des codes de réinitialisation. */
    @Value("${auth.mail.from:wcpregnant@example.com}")
    private String mailFrom;
}
