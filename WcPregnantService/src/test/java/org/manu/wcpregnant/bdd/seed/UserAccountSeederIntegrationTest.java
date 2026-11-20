package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.stream.StreamSupport;

import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration de {@link UserAccountSeeder} contre la base H2 réelle.
 *
 * <p>Ces tests vérifient le comportement observé au démarrage de l'application :
 * la matrice RBAC complète est amorcée dans {@code user_account}, l'amorce est
 * idempotente et un mot de passe réellement modifié n'est jamais écrasé.</p>
 */
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Seed : comptes de démonstration dans user_account (intégration)")
class UserAccountSeederIntegrationTest {

    /** Nombre de profils de la matrice RBAC amorcée par défaut. */
    private static final int DEMO_ACCOUNT_COUNT = 5;

    @Autowired
    private UserAccountSeeder userAccountSeeder;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Repart d'un référentiel vide avant chaque scénario : l'amorce est rejouée dans
     * les mêmes conditions que lors du {@code ApplicationReadyEvent}.
     */
    @BeforeEach
    void seedDemoAccountsFromScratch() {
        userAccountRepository.deleteAll();
        userAccountSeeder.seedDemoAccounts();
    }

    /**
     * Nominal : les cinq profils de la matrice RBAC sont présents, avec leurs
     * credentials hachés et leur état d'activation.
     */
    @Test
    void seedDemoAccounts_firstRun_createsAllDemoAccounts() {
        // Act
        List<UserAccount> accounts = allAccounts();

        // Assert
        assertThat(accounts).hasSize(DEMO_ACCOUNT_COUNT);
        assertThat(accounts.stream().map(UserAccount::getUsername).toList())
                .containsExactlyInAnyOrder("manu", "admin", "adminonly", "norole", "disabled");

        UserAccount visitor = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        assertThat(visitor.isEnabled()).isTrue();
        assertThat(passwordEncoder.matches("manu", visitor.getPassword())).isTrue();

        UserAccount disabled = userAccountRepository.findByUsernameIgnoreCase("disabled").orElseThrow();
        assertThat(disabled.isEnabled()).isFalse();
    }

    /**
     * Idempotence : rejouer l'amorce ne crée aucun doublon et ne modifie aucun compte.
     */
    @Test
    void seedDemoAccounts_secondRun_createsNothing() {
        // Act
        int secondRun = userAccountSeeder.seedDemoAccounts();

        // Assert
        assertThat(secondRun).isZero();
        assertThat(allAccounts()).hasSize(DEMO_ACCOUNT_COUNT);
    }

    /**
     * Préservation : un mot de passe réellement changé par l'utilisateur n'est jamais écrasé.
     */
    @Test
    void seedDemoAccounts_existingAccountWithCustomPassword_isLeftUntouched() {
        // Arrange
        String customHash = passwordEncoder.encode("motDePasseReel");
        UserAccount visitor = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        visitor.setPassword(customHash);
        userAccountRepository.save(visitor);

        // Act
        int created = userAccountSeeder.seedDemoAccounts();

        // Assert
        assertThat(created).isZero();
        UserAccount reloaded = userAccountRepository.findByUsernameIgnoreCase("manu").orElseThrow();
        assertThat(reloaded.getPassword()).isEqualTo(customHash);
        assertThat(passwordEncoder.matches("motDePasseReel", reloaded.getPassword())).isTrue();
    }

    /**
     * Comparaison insensible à la casse : un compte déjà présent en majuscules n'est pas recréé.
     */
    @Test
    void seedDemoAccounts_existingAccountIgnoringCase_isNotDuplicated() {
        // Arrange
        userAccountRepository.deleteAll();
        userAccountRepository.save(new UserAccount("ADMIN", "hash", "admin@example.com", true));

        // Act
        int created = userAccountSeeder.seedDemoAccounts();

        // Assert
        assertThat(created).isEqualTo(DEMO_ACCOUNT_COUNT - 1);
        List<UserAccount> accounts = allAccounts();
        assertThat(accounts).hasSize(DEMO_ACCOUNT_COUNT);
        assertThat(accounts.stream().map(UserAccount::getUsername).toList()).contains("ADMIN");
        assertThat(userAccountRepository.findByUsernameIgnoreCase("admin")
                .orElseThrow().getPassword()).isEqualTo("hash");
    }

    /**
     * Matérialise la liste des comptes présents : {@code CrudRepository#findAll()}
     * renvoie un {@code Iterable}, non convertible directement en {@code List}.
     *
     * @return la liste des comptes utilisateur enregistrés
     */
    private List<UserAccount> allAccounts() {
        return StreamSupport.stream(userAccountRepository.findAll().spliterator(), false).toList();
    }
}
