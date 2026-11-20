package org.manu.wcpregnant.bdd.seed;

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
        seeder = new UserAccountSeeder(userAccountRepository, passwordEncoder);
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
}
