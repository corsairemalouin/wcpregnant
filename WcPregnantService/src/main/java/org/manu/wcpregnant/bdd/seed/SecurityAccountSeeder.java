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
 * <p>Les profils amorcés proviennent de {@link TestUserProfiles}, source de vérité
 * unique partagée avec le profil {@code dev} (comptes en mémoire) et la table
 * {@code user_account}. La matrice complète couvre les scénarios {@code USER},
 * {@code ADMIN}, {@code ADMIN} seul, authentifié sans rôle et compte désactivé ;
 * elle est désactivable par {@code demo.test-accounts.enabled=false}.</p>
 *
 * <p>Les identifiants des deux comptes principaux sont surchargeables par les
 * variables d'environnement {@code DEMO_USER_USERNAME}, {@code DEMO_USER_PASSWORD},
 * {@code DEMO_ADMIN_USERNAME} et {@code DEMO_ADMIN_PASSWORD} ; les valeurs par
 * défaut ne sont que des identifiants de démonstration à remplacer dans un
 * environnement réel.</p>
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
     * Drapeau des profils de test supplémentaires ({@code ADMIN_ONLY}, {@code NO_ROLE},
     * {@code DISABLED}) de la matrice RBAC.
     */
    private final boolean testAccountsEnabled;

    /**
     * Construit l'amorceur de comptes de sécurité.
     *
     * @param userDetailsManager  manager Spring Security du profil {@code prod}
     * @param passwordEncoder     encodeur BCrypt exposé par {@code AppSecurityConfiguration}
     * @param demoUserUsername    identifiant du compte visiteur (rôle {@code USER})
     * @param demoUserPassword    mot de passe du compte visiteur
     * @param demoAdminUsername   identifiant du compte administrateur
     * @param demoAdminPassword   mot de passe du compte administrateur
     * @param testAccountsEnabled {@code true} pour amorcer la matrice RBAC complète
     * @throws NullPointerException si le manager, l'encodeur ou un identifiant est {@code null}
     */
    public SecurityAccountSeeder(JdbcUserDetailsManager userDetailsManager,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${demo.user.username:manu}") String demoUserUsername,
                                 @Value("${demo.user.password:manu}") String demoUserPassword,
                                 @Value("${demo.admin.username:admin}") String demoAdminUsername,
                                 @Value("${demo.admin.password:admin}") String demoAdminPassword,
                                 @Value("${demo.test-accounts.enabled:true}") boolean testAccountsEnabled) {
        this.userDetailsManager = Objects.requireNonNull(userDetailsManager, "userDetailsManager must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "passwordEncoder must not be null");
        this.demoUserUsername = Objects.requireNonNull(demoUserUsername, "demoUserUsername must not be null");
        this.demoUserPassword = Objects.requireNonNull(demoUserPassword, "demoUserPassword must not be null");
        this.demoAdminUsername = Objects.requireNonNull(demoAdminUsername, "demoAdminUsername must not be null");
        this.demoAdminPassword = Objects.requireNonNull(demoAdminPassword, "demoAdminPassword must not be null");
        this.testAccountsEnabled = testAccountsEnabled;
    }

    /**
     * Crée les comptes de démonstration s'ils sont absents du référentiel.
     *
     * <p>Le compte visiteur reçoit le rôle {@code USER} (consultation et notation),
     * le compte administrateur reçoit {@code USER} et {@code ADMIN} (création,
     * modification, suppression des équipements). Les profils de test ajoutent
     * {@code ADMIN} seul, un compte sans rôle et un compte désactivé.</p>
     *
     * @return le nombre de comptes effectivement créés
     */
    @EventListener(ApplicationReadyEvent.class)
    public int seedSecurityAccounts() {
        int created = 0;
        for (TestAccountProfile profile : TestUserProfiles.accounts(testAccountsEnabled)) {
            created += createIfAbsent(resolve(profile));
        }
        log.info("Spring Security demo accounts ensured: {} created", created);
        return created;
    }

    /**
     * Applique les surcharges d'identifiant et de mot de passe aux comptes principaux.
     *
     * <p>Les profils de test secondaires conservent leurs credentials de démonstration.</p>
     *
     * @param profile profil déclaré dans {@link TestUserProfiles}
     * @return le profil tel qu'il doit être inscrit dans le référentiel
     */
    private TestAccountProfile resolve(TestAccountProfile profile) {
        if (profile == TestUserProfiles.VISITOR) {
            return profile.withCredentials(demoUserUsername, demoUserPassword);
        }
        if (profile == TestUserProfiles.ADMIN) {
            return profile.withCredentials(demoAdminUsername, demoAdminPassword);
        }
        return profile;
    }

    /**
     * Insère un compte et ses rôles lorsque l'identifiant n'existe pas encore.
     *
     * @param profile profil de démonstration à inscrire
     * @return {@code 1} si le compte a été créé, {@code 0} s'il existait déjà
     */
    private int createIfAbsent(TestAccountProfile profile) {
        String username = profile.username();
        if (userDetailsManager.userExists(username)) {
            log.debug("Spring Security account {} already present, seeding skipped", username);
            return 0;
        }
        List<String> roles = profile.roles();
        userDetailsManager.createUser(User.builder()
                .username(username)
                .password(passwordEncoder.encode(profile.password()))
                .roles(roles.toArray(new String[0]))
                .disabled(!profile.enabled())
                .build());
        log.info("Created Spring Security account {} with roles {}", username, roles);
        return 1;
    }
}
