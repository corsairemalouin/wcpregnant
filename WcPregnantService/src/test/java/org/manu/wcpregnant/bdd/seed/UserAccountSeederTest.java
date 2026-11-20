package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.manu.wcpregnant.bdd.repository.UserAccountRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de l'amorce des comptes de démonstration : création idempotente et
 * hachage systématique des mots de passe.
 */
class UserAccountSeederTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    private UserAccountSeeder seeder;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        seeder = new UserAccountSeeder(userAccountRepository, passwordEncoder,
                "manu", "manu", "admin", "admin", false);
    }

    /**
     * Nominal : aucun compte n'existe, les deux comptes de démonstration sont créés.
     */
    @Test
    void seedDemoAccounts_whenNoAccountExists_createsTwoAccounts() {
        // Arrange
        when(userAccountRepository.findByUsernameIgnoreCase("manu")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());

        // Act
        seeder.seedDemoAccounts();

        // Assert
        verify(userAccountRepository, times(2)).save(ArgumentMatchers.any(UserAccount.class));
    }

    /**
     * Cas limite : les comptes existent déjà, aucun insert n'est effectué.
     */
    @Test
    void seedDemoAccounts_whenAccountsAlreadyPresent_createsNothing() {
        // Arrange
        when(userAccountRepository.findByUsernameIgnoreCase("manu"))
                .thenReturn(Optional.of(new UserAccount("manu", "hash", "manu@example.com", true)));
        when(userAccountRepository.findByUsernameIgnoreCase("admin"))
                .thenReturn(Optional.of(new UserAccount("admin", "hash", "admin@example.com", true)));

        // Act
        seeder.seedDemoAccounts();

        // Assert
        verify(userAccountRepository, times(0)).save(ArgumentMatchers.any(UserAccount.class));
    }

    /**
     * Sécurité : le mot de passe enregistré est un hash BCrypt, jamais la valeur en clair.
     */
    @Test
    void seedDemoAccounts_hashesPasswords_neverStoresRawPassword() {
        // Arrange
        when(userAccountRepository.findByUsernameIgnoreCase("manu")).thenReturn(Optional.empty());
        when(userAccountRepository.findByUsernameIgnoreCase("admin")).thenReturn(Optional.empty());
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);

        // Act
        seeder.seedDemoAccounts();

        // Assert
        verify(userAccountRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(account -> account.getPassword().startsWith("{bcrypt}"))
                .allMatch(account -> passwordEncoder.matches(
                        account.getUsername(), account.getPassword()));
    }

    /**
     * Nominal : drapeau activé, la matrice RBAC complète (5 comptes) est insérée.
     */
    @Test
    void seedDemoAccounts_whenTestAccountsEnabled_createsFiveAccounts() {
        // Arrange
        UserAccountSeeder fullSeeder = new UserAccountSeeder(userAccountRepository, passwordEncoder,
                "manu", "manu", "admin", "admin", true);

        // Act
        int created = fullSeeder.seedDemoAccounts();

        // Assert
        assertThat(created).isEqualTo(5);
        verify(userAccountRepository, times(5)).save(ArgumentMatchers.any(UserAccount.class));
    }

    /**
     * RBAC : l'état d'activation du compte reflète fidèlement le profil amorcé.
     */
    @Test
    void seedDemoAccounts_copiesEnabledFlag_perProfile() {
        // Arrange
        UserAccountSeeder fullSeeder = new UserAccountSeeder(userAccountRepository, passwordEncoder,
                "manu", "manu", "admin", "admin", true);
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);

        // Act
        fullSeeder.seedDemoAccounts();

        // Assert
        verify(userAccountRepository, times(5)).save(captor.capture());
        List<UserAccount> accounts = captor.getAllValues();
        assertThat(accounts).hasSize(5);
        assertThat(accounts.stream().filter(account -> "disabled".equals(account.getUsername())).findFirst()
                .orElseThrow().isEnabled()).isFalse();
        assertThat(accounts.stream().filter(account -> "norole".equals(account.getUsername())).findFirst()
                .orElseThrow().isEnabled()).isTrue();
    }

    /**
     * Configuration : les identifiants injectés remplacent les valeurs de démonstration.
     */
    @Test
    void seedDemoAccounts_configuredCredentials_overrideDemoUsernames() {
        // Arrange
        UserAccountSeeder customSeeder = new UserAccountSeeder(userAccountRepository, passwordEncoder,
                "visiteur", "p@ssw0rd", "superadmin", "s3cret", false);
        ArgumentCaptor<UserAccount> captor = ArgumentCaptor.forClass(UserAccount.class);

        // Act
        int created = customSeeder.seedDemoAccounts();

        // Assert
        assertThat(created).isEqualTo(2);
        verify(userAccountRepository, times(2)).save(captor.capture());
        assertThat(captor.getAllValues().stream().map(UserAccount::getUsername).toList())
                .containsExactly("visiteur", "superadmin");
        assertThat(captor.getAllValues().get(0).getPassword()).isNotEqualTo("manu");
    }

    /**
     * Cas limite : un repository {@code null} est refusé à la construction.
     */
    @Test
    void constructor_nullRepository_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new UserAccountSeeder(null, passwordEncoder,
                "manu", "manu", "admin", "admin", true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userAccountRepository must not be null");
    }
}
