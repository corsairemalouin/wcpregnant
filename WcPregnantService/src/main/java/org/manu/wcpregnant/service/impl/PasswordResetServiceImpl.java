package org.manu.wcpregnant.service.impl;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.manu.wcpregnant.config.PasswordResetConfig;
import org.manu.wcpregnant.service.PasswordResetService;
import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.PasswordResetAcknowledgement;
import org.manu.wcpregnant.web.dto.PasswordResetResult;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de réinitialisation du mot de passe.
 *
 * <p>Le code à 6 chiffres n'est jamais stocké en clair : il est haché avec le
 * même {@link PasswordEncoder} (BCrypt) que les mots de passe, ce qui rend
 * impossible la lecture du code depuis la base et rend chaque demande
 * indépendante.</p>
 *
 * <p>L'envoi du code passe par le SMTP configuré via {@code spring.mail.*}.
 * Lorsque {@code auth.mail.enabled} est {@code false} (cas des environnements
 * locaux et des tests), le code est renvoyé dans la réponse pour permettre la
 * démonstration de la chaîne complète demande → confirmation.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_BOUND = 1_000_000;
    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetConfig passwordResetConfig;
    private final JavaMailSender mailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * {@inheritDoc}
     *
     * <p>Le compte est recherché par nom d'utilisateur puis par e-mail, sans
     * tenir compte de la casse. Le code généré remplace toute demande
     * précédente non consommée.</p>
     */
    @Override
    @Transactional
    public PasswordResetAcknowledgement requestCode(String login) {
        Objects.requireNonNull(login, "login must not be null");

        String trimmedLogin = login.trim();
        if (trimmedLogin.isEmpty()) {
            throw new IllegalArgumentException("login must not be blank");
        }

        UserAccount account = findAccount(trimmedLogin);
        String code = generateCode();

        account.setResetCode(
                passwordEncoder.encode(code),
                Instant.now().plusSeconds(passwordResetConfig.getCodeTtlSeconds()));
        userAccountRepository.save(account);

        log.debug("Reset code generated for account {}", account.getUsername());

        if (passwordResetConfig.isMailEnabled()) {
            sendCodeByEmail(account.getEmail(), code);
            return new PasswordResetAcknowledgement(
                    "A reset code has been sent to " + account.getEmail(),
                    account.getEmail(),
                    passwordResetConfig.getCodeTtlSeconds(),
                    null);
        }

        log.warn("SMTP sending disabled: reset code returned in the response (demo mode)");
        return new PasswordResetAcknowledgement(
                "SMTP is not configured, the reset code is returned for demonstration purposes",
                account.getEmail(),
                passwordResetConfig.getCodeTtlSeconds(),
                code);
    }
    /**
     * {@inheritDoc}
     *
     * <p>Le code est comparé à sa version hachée. Une fois le mot de passe
     * remplacé, le code et sa date d'expiration sont effacés : la demande ne
     * peut plus être rejouée.</p>
     */
    @Override
    @Transactional
    public PasswordResetResult confirmCode(String login, String code, String newPassword) {
        Objects.requireNonNull(login, "login must not be null");
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(newPassword, "newPassword must not be null");

        UserAccount account = findAccount(login.trim());

        if (account.getResetCode() == null || account.getResetCodeExpiresAt() == null) {
            throw new ResourceNotFoundException("No reset code pending for account " + account.getUsername());
        }

        if (Instant.now().isAfter(account.getResetCodeExpiresAt())) {
            account.clearResetCode();
            userAccountRepository.save(account);
            throw new ResourceNotFoundException("Reset code expired for account " + account.getUsername());
        }

        if (!passwordEncoder.matches(code, account.getResetCode())) {
            log.warn("Invalid reset code submitted for account {}", account.getUsername());
            throw new IllegalArgumentException("Reset code is invalid");
        }

        if (newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "New password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.clearResetCode();
        userAccountRepository.save(account);

        log.info("Password successfully reset for account {}", account.getUsername());

        return new PasswordResetResult("Password successfully reset", account.getUsername());
    }

    /**
     * Retrouve le compte par nom d'utilisateur, puis par e-mail.
     *
     * @param login identifiant déjà nettoyé
     * @return le compte correspondant
     * @throws ResourceNotFoundException si aucun compte ne correspond
     */
    private UserAccount findAccount(String login) {
        return userAccountRepository.findByUsernameIgnoreCase(login)
                .or(() -> userAccountRepository.findByEmailIgnoreCase(login))
                .orElseThrow(() -> new ResourceNotFoundException("Account not found for login: " + login));
    }

    /**
     * Envoie le code de réinitialisation à l'adresse e-mail du compte.
     *
     * @param to   destinataire
     * @param code code à 6 chiffres
     * @throws IllegalStateException si le serveur SMTP refuse l'envoi
     */
    private void sendCodeByEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(passwordResetConfig.getMailFrom());
        message.setTo(to);
        message.setSubject("WcPregnant - password reset code");
        message.setText("Your password reset code is " + code
                + ". It is valid for " + passwordResetConfig.getCodeTtlSeconds() + " seconds.");

        try {
            mailSender.send(message);
            log.debug("Reset code sent to {}", to);
        } catch (Exception ex) {
            log.error("Failed to send the reset code to {}: {}", to, ex.getMessage());
            throw new IllegalStateException("Failed to send the reset code by e-mail", ex);
        }
    }

    /**
     * Génère un code numérique à {@value CODE_LENGTH} chiffres.
     *
     * @return le code, complété par des zéros si nécessaire
     */
    private String generateCode() {
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(CODE_BOUND));
    }
}

