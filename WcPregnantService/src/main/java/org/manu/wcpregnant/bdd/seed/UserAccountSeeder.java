package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.Objects;

import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Amorce la table {@code user_account} avec les comptes de démonstration décrits par
 * {@link TestUserProfiles}, la source de vérité partagée avec
 * {@link org.manu.wcpregnant.config.AppSecurityConfiguration} (comptes en mémoire du
 * profil {@code dev}) et {@link SecurityAccountSeeder} (tables {@code users} /
 * {@code authorities} du profil {@code prod}).
 *
 * <p>Le drapeau {@code demo.test-accounts.enabled} (variable d'environnement
 * {@code DEMO_TEST_ACCOUNTS_ENABLED}) décide de la portée de l'amorce : {@code false}
 * limite aux deux comptes historiques {@code manu} et {@code admin}, {@code true} amorce
 * la matrice RBAC complète (ADMIN seul, sans rôle, désactivé). Les credentials des deux
 * comptes principaux restent surchargeables par {@code demo.user.username} /
 * {@code demo.user.password} / {@code demo.admin.username} / {@code demo.admin.password}.</p>
 *
 * <p>L'amorce est idempotente : un compte déjà présent (comparaison insensible à la casse)
 * n'est jamais écrasé, ce qui préserve un mot de passe réellement modifié par l'utilisateur.
 * Le mot de passe est systématiquement haché avant l'insertion.</p>
 */
@Component
public class UserAccountSeeder {

    private static final Logger log = LoggerFactory.getLogger(UserAccountSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean testAccountsEnabled;
    private final String userUsername;
    private final String userPassword;
    private final String adminUsername;
    private final String adminPassword;

    /**
     * Constructeur injecté par Spring : les identifiants de démonstration et le drapeau
     * des comptes de test proviennent de la configuration, jamais du code.
     *
     * @param userAccountRepository repository des comptes, non null
     * @param passwordEncoder      encodeur partagé avec le flux de réinitialisation, non null
     * @param userUsername         identifiant effectif du compte visiteur
     * @param userPassword         mot de passe effectif du compte visiteur
     * @param adminUsername        identifiant effectif du compte administrateur
     * @param adminPassword        mot de passe effectif du compte administrateur
     * @param testAccountsEnabled  {@code true} pour amorcer la matrice RBAC complète
     * @throws NullPointerException si le repository ou l'encodeur est {@code null}
     */
    public UserAccountSeeder(UserAccountRepository userAccountRepository,
                             PasswordEncoder passwordEncoder,
                             @Value("${demo.user.username:manu}") String userUsername,
                             @Value("${demo.user.password:manu}") String userPassword,
                             @Value("${demo.admin.username:admin}") String adminUsername,
                             @Value("${demo.admin.password:admin}") String adminPassword,
                             @Value("${demo.test-accounts.enabled:true}") boolean testAccountsEnabled) {
        this.userAccountRepository = Objects.requireNonNull(userAccountRepository,
                "userAccountRepository must not be null");
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder,
                "passwordEncoder must not be null");
        this.userUsername = Objects.requireNonNull(userUsername, "userUsername must not be null");
        this.userPassword = Objects.requireNonNull(userPassword, "userPassword must not be null");
        this.adminUsername = Objects.requireNonNull(adminUsername, "adminUsername must not be null");
        this.adminPassword = Objects.requireNonNull(adminPassword, "adminPassword must not be null");
        this.testAccountsEnabled = testAccountsEnabled;
    }

    /**
     * Amorche les comptes de démonstration dans {@code user_account}, sans jamais
     * écraser un compte existant.
     *
     * @return le nombre de comptes effectivement créés (0 si tous existent déjà)
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public int seedDemoAccounts() {
        List<TestAccountProfile> profiles = TestUserProfiles.accounts(testAccountsEnabled);
        int created = 0;

        for (TestAccountProfile profile : profiles) {
            TestAccountProfile effective = applyConfiguredCredentials(profile);

            if (userAccountRepository.findByUsernameIgnoreCase(effective.username()).isPresent()) {
                log.debug("Account {} already present in user_account, leaving it untouched",
                        effective.username());
                continue;
            }

            String hashedPassword = passwordEncoder.encode(effective.password());
            userAccountRepository.save(new UserAccount(
                    effective.username(),
                    hashedPassword,
                    effective.email(),
                    effective.enabled()));
            log.info("Seeded demo account {} in user_account ({})",
                    effective.username(), effective.description());
            created++;
        }

        log.debug("user_account seeding finished: {} account(s) created", created);
        return created;
    }

    /**
     * Applique les credentials configurés aux deux comptes principaux.
     *
     * <p>Les profils de test supplémentaires conservent leurs identifiants de
     * démonstration : ils ne servent qu'à vérifier la matrice RBAC.</p>
     *
     * @param profile profil déclaré dans {@link TestUserProfiles}
     * @return le profil tel qu'il doit être inséré, jamais {@code null}
     */
    private TestAccountProfile applyConfiguredCredentials(TestAccountProfile profile) {
        if (profile == TestUserProfiles.VISITOR) {
            return profile.withCredentials(userUsername, userPassword);
        }
        if (profile == TestUserProfiles.ADMIN) {
            return profile.withCredentials(adminUsername, adminPassword);
        }
        return profile;
    }
}
