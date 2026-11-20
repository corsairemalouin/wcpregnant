package org.manu.wcpregnant.web;

import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.manu.wcpregnant.service.PasswordResetService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.api.AuthApi;
import org.manu.wcpregnant.web.dto.PasswordResetAcknowledgement;
import org.manu.wcpregnant.web.dto.PasswordResetConfirmRequest;
import org.manu.wcpregnant.web.dto.PasswordResetRequest;
import org.manu.wcpregnant.web.dto.PasswordResetResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implémentation de l'API de réinitialisation du mot de passe.
 *
 * <p>Ces deux endpoints sont <strong>publics</strong> (aucune authentification)
 * : l'utilisateur qui a oublié son mot de passe ne peut pas se connecter. La
 * protection repose sur le code à 6 chiffres envoyé par e-mail et sur sa
 * durée de validité, et la chaîne {@code /auth/**} est explicitement autorisée
 * dans {@code AppSecurityConfiguration}.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final PasswordResetService passwordResetService;

    /**
     * {@inheritDoc}
     *
     * <p>Un identifiant absent ou vide est rejeté en {@code 400} ; un compte
     * inconnu renvoie un {@code 404}.</p>
     */
    @Override
    public ResponseEntity<PasswordResetAcknowledgement> requestPasswordReset(PasswordResetRequest passwordResetRequest) {
        Objects.requireNonNull(passwordResetRequest, "passwordResetRequest cannot be null");

        log.debug("Password reset requested for login {}", passwordResetRequest.getLogin());

        try {
            return ResponseEntity.ok(passwordResetService.requestCode(passwordResetRequest.getLogin()));
        } catch (IllegalArgumentException ex) {
            log.warn("Rejected password reset request: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new PasswordResetAcknowledgement(ex.getMessage(), null, null, null));
        } catch (ResourceNotFoundException ex) {
            log.warn("Password reset request for an unknown account: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new PasswordResetAcknowledgement(ex.getMessage(), null, null, null));
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Un code qui ne correspond pas au code haché enregistré est rejeté en
     * {@code 400} ; un compte inconnu ou un code expiré renvoie un {@code 404}.</p>
     */
    @Override
    public ResponseEntity<PasswordResetResult> confirmPasswordReset(PasswordResetConfirmRequest passwordResetConfirmRequest) {
        Objects.requireNonNull(passwordResetConfirmRequest, "passwordResetConfirmRequest cannot be null");

        log.debug("Password reset confirmation for login {}", passwordResetConfirmRequest.getLogin());

        try {
            return ResponseEntity.ok(passwordResetService.confirmCode(
                    passwordResetConfirmRequest.getLogin(),
                    passwordResetConfirmRequest.getCode(),
                    passwordResetConfirmRequest.getNewPassword()));
        } catch (IllegalArgumentException ex) {
            log.warn("Rejected password reset confirmation: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new PasswordResetResult(ex.getMessage(), null));
        } catch (ResourceNotFoundException ex) {
            log.warn("Password reset confirmation failed: {}", ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new PasswordResetResult(ex.getMessage(), null));
        }
    }
}
