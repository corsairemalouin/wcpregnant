package org.manu.wcpregnant.bdd.dao.users;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.bdd.dao.Authorities;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du modèle {@link UserAccount} : intégrité des champs de connexion,
 * cycle de vie du code de réinitialisation et gestion des rôles.
 */
@DisplayName("Modèle UserAccount")
class UserAccountTest {

    /**
     * Nominal : le constructeur complet initialise tous les champs du compte.
     */
    @Test
    @DisplayName("Constructeur - compte complet : tous les champs sont renseignés")
    void constructor_fullArguments_initializesEveryField() {
        // Arrange & Act
        UserAccount account = new UserAccount("manu", "{bcrypt}hash", "manu@example.com", true);

        // Assert
        assertThat(account.getUsername()).isEqualTo("manu");
        assertThat(account.getPassword()).isEqualTo("{bcrypt}hash");
        assertThat(account.getEmail()).isEqualTo("manu@example.com");
        assertThat(account.isEnabled()).isTrue();
        assertThat(account.getResetCode()).isNull();
        assertThat(account.getResetCodeExpiresAt()).isNull();
        assertThat(account.getAuthorities()).isEmpty();
    }

    /**
     * Cas limite : un compte désactivé ou construit sans flag explicite n'est
     * jamais considéré comme activé.
     */
    @Test
    @DisplayName("isEnabled - compte désactivé ou sans flag : le compte est inactif")
    void isEnabled_whenDisabledOrNotProvided_returnsFalse() {
        // Arrange
        UserAccount disabled = new UserAccount("bob", "hash", "bob@example.com", false);
        UserAccount withoutFlag = new UserAccount();

        // Act & Assert
        assertThat(disabled.isEnabled()).isFalse();
        assertThat(withoutFlag.isEnabled()).isFalse();

        withoutFlag.setEnabled(true);
        assertThat(withoutFlag.isEnabled()).isTrue();
    }

    /**
     * Le mot de passe est toujours stocké tel qu'il a été encodé : les setters
     * ne ré-encodent jamais la valeur en clair.
     */
    @Test
    @DisplayName("Setters - modification du compte : les nouvelles valeurs sont conservées")
    void setters_updateValues_reflectNewState() {
        // Arrange
        UserAccount account = new UserAccount("manu", "oldHash", "old@example.com", true);

        // Act
        account.setUsername("MANU");
        account.setPassword("newHash");
        account.setEmail("new@example.com");
        account.setEnabled(Boolean.FALSE);

        // Assert
        assertThat(account.getUsername()).isEqualTo("MANU");
        assertThat(account.getPassword()).isEqualTo("newHash");
        assertThat(account.getEmail()).isEqualTo("new@example.com");
        assertThat(account.isEnabled()).isFalse();
    }

    /**
     * Cycle de vie du code de réinitialisation : pose puis purge du code et de
     * sa date d'expiration.
     */
    @Test
    @DisplayName("Reset code - pose puis purge : les deux champs sont remis à null")
    void resetCodeLifecycle_setThenClear_resetsBothFields() {
        // Arrange
        UserAccount account = new UserAccount("manu", "hash", "manu@example.com", true);
        Instant expiry = Instant.parse("2026-11-19T23:59:59Z");

        // Act
        account.setResetCode("{bcrypt}codeHash", expiry);

        // Assert
        assertThat(account.getResetCode()).isEqualTo("{bcrypt}codeHash");
        assertThat(account.getResetCodeExpiresAt()).isEqualTo(expiry);

        // Act
        account.clearResetCode();

        // Assert
        assertThat(account.getResetCode()).isNull();
        assertThat(account.getResetCodeExpiresAt()).isNull();
    }

    /**
     * RBAC : la liste des rôles est remplaçable, ce qui permet au seed ou à
     * l'administration de faire évoluer les autorisations d'un compte.
     */
    @Test
    @DisplayName("Authorities - remplacement de la liste : les rôles sont appliqués")
    void setAuthorities_newList_replacesRoles() {
        // Arrange
        UserAccount account = new UserAccount("admin", "hash", "admin@example.com", true);

        // Act
        account.setAuthorities(List.of(
                new Authorities(1L, "admin", "ROLE_ADMIN"),
                new Authorities(2L, "admin", "ROLE_USER")));

        // Assert
        assertThat(account.getAuthorities())
                .hasSize(2)
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_ADMIN", "ROLE_USER");
    }
}
