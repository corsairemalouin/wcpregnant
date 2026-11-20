package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * Garantit la présence des comptes de démonstration dans le référentiel
 * Spring Security ({@code users} / {@code authorities}) lorsque l'application
 * tourne avec le profil {@code prod}.
 *
 * <p>En production, {@link org.manu.wcpregnant.config.AppSecurityConfiguration}
 * s'appuie sur un {@link JdbcUserDetailsManager} branché sur PostgreSQL : sans
 * compte inséré, aucun profil (USER ni ADMIN) ne peut s'authentifier et le RBAC
 * ne peut pas être vérifié. Le seed est <strong>idempotent</strong> : un compte
 * déjà présent n'est jamais écrasé, ce qui préserve les mots de passe réellement
 * changés par l'exploitant.</p>
 *
 * <p>Les identifiants sont surchargeables par les variables d'environnement
 * {@code DEMO_USER_USERNAME}, {@code DEMO_USER_PASSWORD}, {@code DEMO_ADMIN_USERNAME}
 * et {@code DEMO_ADMIN_PASSWORD} ; les valeurs par défaut ne sont que des
 * identifiants de démonstration à remplacer dans un environnement réel.</p>
 */
@Slf4j
@Component
@Profile("prod")
public class SecurityAccountSeeder {

    /** Manager Spring Security provisionnant les tables {@code users} et {@code authorities}. */
    private final JdbcUserDetailsManager userDetailsManager;

    /** Encodeur BCrypt partagé avec la procédure de réinitialisation du mot de passe. */
    private final PasswordEncoder passwordEncoder;

    /** Identifiant du compte disposant du rôle {@code USER}. */
    private final String demoUserUsername;

    /** Mot de passe de démonstration du compte {@code USER}. */
    private final String demoUserPassword;

    /** Identifiant du compte disposant des rôles {@code USER} et {@code ADMIN}. */
    private final String demoAdminUsername;

    /** Mot de passe de démonstration du compte {@code ADMIN}. */
    private final String demoAdminPassword;

    /**
     * Construit l'amorceur de comptes de sécurité.
     *
     * @param userDetailsManager  manager Spring Security du profil {@code prod}
     * @param passwordEncoder     encodeur BCrypt exposé par {@code AppSecurityConfiguration}
     * @param demoUserUsername    identifiant du compte visiteur (rôle {@code USER})
     * @param demoUserPassword    mot de passe du compte visiteur
     * @param demoAdminUsername   identifiant du compte administrateur
     * @param demoAdminPassword   mot de passe du compte administrateur
     * @throws NullPointerException si le manager ou l'encodeur est {@code null}
     */
    public SecurityAccountSeeder(JdbcUserDetailsManager userDetailsManager,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${demo.user.username:manu}") String demoUserUsername,
                                 @Value("${demo.user.password:manu}") String demoUserPassword,
                                 @Value("${demo.admin.username:admin}") String demoAdminUsername,
                                 @Value("${demo.admin.password:admin}") String demoAdminPassword) {
        this.userDetailsManager = Objects.requireNonNull(userDetailsManager, "userDetailsManager must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.demoUserUsername = Objects.requireNonNull(demoUserUsername, "demoUserUsername must not be null");
        this.demoUserPassword = Objects.requireNonNull(demoUserPassword, "demoUserPassword must not be null");
        this.demoAdminUsername = Objects.requireNonNull(demoAdminUsername, "demoAdminUsername must not be null");
        this.demoAdminPassword = Objects.requireNonNull(demoAdminPassword, "demoAdminPassword must not be null");
    }
    /**
     * Crée les comptes de démonstration s'ils sont absents du référentiel.
     *
     * <p>Le compte visiteur reçoit le rôle {@code USER} (consultation et notation),
     * le compte administrateur reçoit {@code USER} et {@code ADMIN} (création,
     * modification, suppression des équipements).</p>
     *
     * @return le nombre de comptes effectivement créés
     */
    @EventListener(ApplicationReadyEvent.class)
    public int seedSecurityAccounts() {
        int created = createIfAbsent(demoUserUsername, demoUserPassword, List.of("USER"));
        created += createIfAbsent(demoAdminUsername, demoAdminPassword, List.of("USER", "ADMIN"));
        log.info("Spring Security demo accounts ensured: {} created", created);
        return created;
    }

    /**
     * Insère un compte et ses rôles lorsque l'identifiant n'existe pas encore.
     *
     * @param username    identifiant de connexion
     * @param rawPassword mot de passe en clair, haché avant l'enregistrement
     * @param roles       rôles Spring Security à associer au compte
     * @return {@code 1} si le compte a été créé, {@code 0} s'il existait déjà
     */
    private int createIfAbsent(String username, String rawPassword, List<String> roles) {
        if (userDetailsManager.userExists(username)) {
            log.debug("Spring Security account {} already present, seeding skipped", username);
            return 0;
        }
        userDetailsManager.createUser(User.builder()
                .username(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles(roles.toArray(new String[0]))
                .build());
        log.info("Created Spring Security account {} with roles {}", username, roles);
        return 1;
    }
}

