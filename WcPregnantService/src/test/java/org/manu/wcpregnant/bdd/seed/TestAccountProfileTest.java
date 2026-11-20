package org.manu.wcpregnant.bdd.seed;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests du profil immuable de compte de démonstration : validation des champs
 * obligatoires, normalisation et immutabilité des rôles, surcharge des credentials.
 */
@DisplayName("TestAccountProfile : profil de compte de démonstration")
class TestAccountProfileTest {

    /** Profil mixte de référence réutilisé par plusieurs scénarios. */
    private static final TestAccountProfile MIXED = new TestAccountProfile(
            "tester", "s3cret", List.of("USER", "ADMIN"), true, "tester@example.com", "Profil mixte");

    /**
     * Nominal : chaque composant du profil est restitué tel qu'il a été déclaré.
     */
    @Test
    void constructor_validProfile_exposesEveryComponent() {
        // Arrange & Act
        TestAccountProfile profile = MIXED;

        // Assert
        assertThat(profile.username()).isEqualTo("tester");
        assertThat(profile.password()).isEqualTo("s3cret");
        assertThat(profile.roles()).containsExactly("USER", "ADMIN");
        assertThat(profile.enabled()).isTrue();
        assertThat(profile.email()).isEqualTo("tester@example.com");
        assertThat(profile.description()).isEqualTo("Profil mixte");
    }

    /**
     * Cas limite : une liste de rôles {@code null} est normalisée en liste vide.
     */
    @Test
    void constructor_nullRoles_normalizesToEmptyList() {
        // Arrange & Act
        TestAccountProfile profile = new TestAccountProfile(
                "guest", "guest", null, true, "guest@example.com", "Authentifié sans rôle");

        // Assert
        assertThat(profile.roles()).isEmpty();
    }

    /**
     * Immutabilité : la liste d'origine est copiée, sa mutation ultérieure n'atteint pas le profil.
     */
    @Test
    void constructor_rolesAreCopied_mutatingSourceDoesNotAffectProfile() {
        // Arrange
        List<String> mutableRoles = new ArrayList<>();
        mutableRoles.add("USER");

        // Act
        TestAccountProfile profile = new TestAccountProfile(
                "guest", "guest", mutableRoles, true, "guest@example.com", "Visiteur");
        mutableRoles.add("ADMIN");

        // Assert
        assertThat(profile.roles()).containsExactly("USER");
    }

    /**
     * Immutabilité : la liste des rôles exposée refuse toute mutation.
     */
    @Test
    void roles_immutable_addRole_throwsUnsupportedOperationException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> MIXED.roles().add("ADMIN"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Validation : un identifiant {@code null} est refusé à la construction.
     */
    @Test
    void constructor_nullUsername_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new TestAccountProfile(
                null, "x", List.of("USER"), true, "x@example.com", "Profil invalide"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("username must not be null");
    }

    /**
     * Validation : un mot de passe {@code null} est refusé à la construction.
     */
    @Test
    void constructor_nullPassword_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> new TestAccountProfile(
                "tester", null, List.of("USER"), true, "tester@example.com", "Profil invalide"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("password must not be null");
    }

    /**
     * RBAC : un rôle présent dans la liste est reconnu.
     */
    @Test
    void hasRole_existingRole_returnsTrue() {
        // Arrange & Act & Assert
        assertThat(MIXED.hasRole("ADMIN")).isTrue();
    }

    /**
     * RBAC : un rôle absent de la liste n'est pas reconnu.
     */
    @Test
    void hasRole_missingRole_returnsFalse() {
        // Arrange & Act & Assert
        assertThat(MIXED.hasRole("MODERATOR")).isFalse();
    }

    /**
     * Cas limite : un rôle {@code null} est refusé.
     */
    @Test
    void hasRole_nullRole_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> MIXED.hasRole(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("role must not be null");
    }

    /**
     * Nominal : la surcharge remplace les credentials sans toucher les rôles ni l'état.
     */
    @Test
    void withCredentials_overloadsCredentials_keepsRolesAndState() {
        // Arrange & Act
        TestAccountProfile overloaded = MIXED.withCredentials("visiteur", "p@ssw0rd");

        // Assert
        assertThat(overloaded.username()).isEqualTo("visiteur");
        assertThat(overloaded.password()).isEqualTo("p@ssw0rd");
        assertThat(overloaded.roles()).containsExactly("USER", "ADMIN");
        assertThat(overloaded.enabled()).isTrue();
        assertThat(overloaded.email()).isEqualTo("tester@example.com");
        assertThat(overloaded.description()).isEqualTo("Profil mixte");
    }

    /**
     * Cas limite : une surcharge avec un identifiant {@code null} est refusée.
     */
    @Test
    void withCredentials_nullUsername_throwsNullPointerException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> MIXED.withCredentials(null, "p@ssw0rd"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("username must not be null");
    }

    /**
     * Immutabilité : la surcharge ne modifie jamais le profil partagé d'origine.
     */
    @Test
    void withCredentials_doesNotMutateOriginalProfile() {
        // Arrange & Act
        MIXED.withCredentials("autre", "autre");

        // Assert
        assertThat(MIXED.username()).isEqualTo("tester");
        assertThat(MIXED.password()).isEqualTo("s3cret");
    }
}
