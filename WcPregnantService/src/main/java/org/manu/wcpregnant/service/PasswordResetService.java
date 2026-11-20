package org.manu.wcpregnant.service;

import org.manu.wcpregnant.web.dto.PasswordResetAcknowledgement;
import org.manu.wcpregnant.web.dto.PasswordResetResult;

/**
 * Contrat du service de réinitialisation du mot de passe.
 *
 * <p>Le service s'appuie sur l'entité {@code UserAccount} (table
 * {@code user_account}) et ne touche jamais à la table {@code users}
 * conservée pour la compatibilité.</p>
 */
public interface PasswordResetService {

    /**
     * Génère un code de réinitialisation à 6 chiffres, le stocke de façon
     * hachée sur le compte et l'envoie par e-mail lorsque le SMTP est configuré.
     *
     * @param login nom d'utilisateur ou adresse e-mail du compte
     * @return l'accusé d'envoi (le code n'est exposé qu'en mode démonstration)
     * @throws IllegalArgumentException si {@code login} est {@code null} ou vide
     * @throws org.manu.wcpregnant.service.utils.ResourceNotFoundException si le compte n'existe pas
     */
    PasswordResetAcknowledgement requestCode(String login);

    /**
     * Vérifie le code transmis puis remplace le mot de passe du compte.
     *
     * @param login       nom d'utilisateur ou adresse e-mail du compte
     * @param code        code reçu par l'utilisateur
     * @param newPassword nouveau mot de passe (au moins 8 caractères)
     * @return le résultat de la réinitialisation
     * @throws IllegalArgumentException si un argument est {@code null} ou mal formé
     * @throws org.manu.wcpregnant.service.utils.ResourceNotFoundException si le compte
     *         est inconnu ou si le code a expiré
     */
    PasswordResetResult confirmCode(String login, String code, String newPassword);
}
