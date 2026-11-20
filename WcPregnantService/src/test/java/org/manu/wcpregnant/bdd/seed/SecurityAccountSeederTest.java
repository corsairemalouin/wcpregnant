package org.manu.wcpregnant.bdd.seed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de l'amorce des comptes Spring Security du profil {@code prod} :
 * création idempotente, rôles attribués et hachage systématique des mots de passe.
 */
class SecurityAccountSeederTest {

    @Mock
    private JdbcUserDetailsManager userDetailsManager;

    private SecurityAccountSeeder seeder;
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        seeder = new SecurityAccountSeeder(userDetailsManager, passwordEncoder, "manu", "manu", "admin", "admin", false);
    }

    /**
     * Nominal : aucun compte n'existe, les deux profils de démonstration sont créés.
     */
    @Test
    void seedSecurityAccounts_whenNoAccountExists_createsTwoAccounts() {
        // Arrange
        when(userDetailsManager.userExists("manu")).thenReturn(false);
        when(userDetailsManager.userExists("admin")).thenReturn(false);

        // Act
        int created = seeder.seedSecurityAccounts();

        // Assert
        assertThat(created).isEqualTo(2);
        verify(userDetailsManager, times(2)).createUser(org.mockito.ArgumentMatchers.any(User.class));
    }

    /**
     * Cas limite : les comptes existent déjà, aucune écriture n'est effectuée.
     */
    @Test
    void seedSecurityAccounts_whenAccountsAlreadyPresent_createsNothing() {
        // Arrange
        when(userDetailsManager.userExists("manu")).thenReturn(true);
        when(userDetailsManager.userExists("admin")).thenReturn(true);

        // Act
        int created = seeder.seedSecurityAccounts();

        // Assert
        assertThat(created).isZero();
        verify(userDetailsManager, never()).createUser(org.mockito.ArgumentMatchers.any(User.class));
    }

    /**
     * Sécurité : le mot de passe enregistré est haché, jamais la valeur en clair.
     */
    @Test
    void seedSecurityAccounts_hashesPasswords_neverStoresRawPassword() {
        // Arrange
        when(userDetailsManager.userExists("manu")).thenReturn(false);
        when(userDetailsManager.userExists("admin")).thenReturn(false);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Act
        seeder.seedSecurityAccounts();

        // Assert
        verify(userDetailsManager, times(2)).createUser(captor.capture());
        assertThat(captor.getAllValues())
                .allMatch(user -> user.getPassword().startsWith("{bcrypt}"))
                .allMatch(user -> passwordEncoder.matches(user.getUsername(), user.getPassword()));
    }

    /**
     * RBAC : le compte administrateur porte les deux rôles, le visiteur uniquement USER.
     */
    @Test
    void seedSecurityAccounts_assignsRoles_perProfile() {
        // Arrange
        when(userDetailsManager.userExists("manu")).thenReturn(false);
        when(userDetailsManager.userExists("admin")).thenReturn(false);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Act
        seeder.seedSecurityAccounts();

        // Assert
        verify(userDetailsManager, times(2)).createUser(captor.capture());
        User visitor = captor.getAllValues().get(0);
        User administrator = captor.getAllValues().get(1);
        assertThat(visitor.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_USER");
        assertThat(administrator.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    /**
     * Cas limite : un collaborateur {@code null} est refusé à la construction.
     */
    @Test
    void constructor_nullDependencies_throwsNullPointerException() {
        // Arrange & Act
        assertThatThrownBy(() -> new SecurityAccountSeeder(
                null, passwordEncoder, "manu", "manu", "admin", "admin", true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("userDetailsManager must not be null");
    }

    /**
     * Cas limite : un encodeur {@code null} est refusé à la construction.
     */
    @Test
    void constructor_nullPasswordEncoder_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new SecurityAccountSeeder(
                userDetailsManager, null, "manu", "manu", "admin", "admin", true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("passwordEncoder must not be null");
    }

    /**
     * Nominal : drapeau activé, la matrice RBAC complète (5 profils) est inscrite.
     */
    @Test
    void seedSecurityAccounts_whenTestAccountsEnabled_createsFiveAccounts() {
        // Arrange
        SecurityAccountSeeder fullSeeder = new SecurityAccountSeeder(
                userDetailsManager, passwordEncoder, "manu", "manu", "admin", "admin", true);

        // Act
        int created = fullSeeder.seedSecurityAccounts();

        // Assert
        assertThat(created).isEqualTo(5);
        verify(userDetailsManager, times(5)).createUser(org.mockito.ArgumentMatchers.any(User.class));
    }

    /**
     * Portée du drapeau : désactivé, les profils de test ne sont ni interrogés ni créés.
     */
    @Test
    void seedSecurityAccounts_whenTestAccountsDisabled_skipsTestProfiles() {
        // Arrange & Act
        seeder.seedSecurityAccounts();

        // Assert
        verify(userDetailsManager, never()).userExists("adminonly");
        verify(userDetailsManager, never()).userExists("norole");
        verify(userDetailsManager, never()).userExists("disabled");
    }

    /**
     * RBAC : le profil {@code ADMIN_ONLY} est inscrit avec le seul rôle ADMIN.
     */
    @Test
    void seedSecurityAccounts_adminOnlyProfile_hasAdminRoleOnly() {
        // Arrange
        SecurityAccountSeeder fullSeeder = new SecurityAccountSeeder(
                userDetailsManager, passwordEncoder, "manu", "manu", "admin", "admin", true);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Act
        fullSeeder.seedSecurityAccounts();

        // Assert
        verify(userDetailsManager, times(5)).createUser(captor.capture());
        User adminOnly = captor.getAllValues().stream()
                .filter(user -> "adminonly".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(adminOnly.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_ADMIN");
    }

    /**
     * Sécurité : le profil {@code DISABLED} est inscrit comme compte désactivé.
     */
    @Test
    void seedSecurityAccounts_disabledProfile_isRegisteredDisabled() {
        // Arrange
        SecurityAccountSeeder fullSeeder = new SecurityAccountSeeder(
                userDetailsManager, passwordEncoder, "manu", "manu", "admin", "admin", true);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        // Act
        fullSeeder.seedSecurityAccounts();

        // Assert
        verify(userDetailsManager, times(5)).createUser(captor.capture());
        User disabled = captor.getAllValues().stream()
                .filter(user -> "disabled".equals(user.getUsername()))
                .findFirst()
                .orElseThrow();
        assertThat(disabled.isEnabled()).isFalse();
    }

    /**
     * Configuration : les identifiants surchargés remplacent les valeurs de démonstration.
     */
    @Test
    void seedSecurityAccounts_configuredCredentials_overrideDemoUsernames() {
        // Arrange
        SecurityAccountSeeder customSeeder = new SecurityAccountSeeder(
                userDetailsManager, passwordEncoder, "visiteur", "p@ssw0rd", "superadmin", "s3cret", false);

        // Act
        int created = customSeeder.seedSecurityAccounts();

        // Assert
        assertThat(created).isEqualTo(2);
        verify(userDetailsManager).userExists("visiteur");
        verify(userDetailsManager).userExists("superadmin");
        verify(userDetailsManager, never()).userExists("manu");
    }
}
