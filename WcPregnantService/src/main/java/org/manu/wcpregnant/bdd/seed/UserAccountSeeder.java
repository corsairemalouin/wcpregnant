package org.manu.wcpregnant.bdd.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Amorce les comptes de démonstration du profil {@code dev} dans la table
 * {@code user_account}, afin que la procédure de réinitialisation du mot de passe
 * ({@code /auth/password-reset/**}) puisse être testée de bout en bout en local.
 *
 * <p>Les identifiants de démonstration sont les mêmes que ceux déclarés en mémoire
 * dans {@link org.manu.wcpregnant.config.AppSecurityConfiguration} ; le mot de passe est
 * systématiquement <strong>haché en BCrypt</strong> avant l'enregistrement, jamais stocké
 * en clair. Le seed est idempotent : un compte déjà présent n'est jamais écrasé.</p>
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class UserAccountSeeder {

    /** Repository des comptes utilisateur. */
    private final UserAccountRepository userAccountRepository;

    /** Encodeur BCrypt utilisé pour hacher les mots de passe de démonstration. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Crée les comptes de démonstration si aucun n'existe encore.
     *
     * <p>Idempotent : un compte déjà présent n'est jamais écrasé, ce qui évite de
     * remplacer un mot de passe changé par un test précédent.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedDemoAccounts() {
        int created = 0;
        created += createIfAbsent("manu", "manu", "manu@example.com");
        created += createIfAbsent("admin", "admin", "admin@example.com");
        log.info("Demo user accounts seeded: {}", created);
    }

    /**
     * Insère un compte lorsque l'identifiant est inconnu.
     *
     * @param username    identifiant de connexion du compte de démonstration
     * @param rawPassword mot de passe de démonstration, haché avant l'enregistrement
     * @param email       adresse e-mail destinataire des codes de réinitialisation
     * @return {@code 1} si le compte a été créé, {@code 0} s'il existait déjà
     */
    private int createIfAbsent(String username, String rawPassword, String email) {
        if (userAccountRepository.findByUsernameIgnoreCase(username).isPresent()) {
            log.debug("Demo account {} already present, seeding skipped", username);
            return 0;
        }
        userAccountRepository.save(new UserAccount(username, passwordEncoder.encode(rawPassword), email, true));
        return 1;
    }
}
