package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de l'inventaire des comptes de démonstration : portée de l'amorce selon le
 * drapeau {@code demo.test-accounts.enabled}, rôles et état de chaque profil de la
 * matrice RBAC (USER, ADMIN, ADMIN seul, authentifié sans rôle, compte désactivé).
 */
@DisplayName("TestUserProfiles : matrice RBAC des comptes de démonstration")
class TestUserProfilesTest {

    /**
     * Nominal : sans les profils de test, seuls les deux comptes historiques sont amorcés.
     */
    @Test
    void accounts_whenTestAccountsDisabled_returnsOnlyCoreAccounts() {
        // Arrange & Act
        List<TestAccountProfile> profiles = TestUserProfiles.accounts(false);

        // Assert
        assertThat(profiles).containsExactly(TestUserProfiles.VISITOR, TestUserProfiles.ADMIN);
    }

    /**
     * Nominal : avec les profils de test, la matrice RBAC complète est amorcée.
     */
    @Test
    void accounts_whenTestAccountsEnabled_returnsFullRbacMatrix() {
        // Arrange & Act
        List<TestAccountProfile> profiles = TestUserProfiles.accounts(true);

        // Assert
        assertThat(profiles).hasSize(5);
        assertThat(profiles).containsExactly(
                TestUserProfiles.VISITOR,
                TestUserProfiles.ADMIN,
                TestUserProfiles.ADMIN_ONLY,
                TestUserProfiles.NO_ROLE,
                TestUserProfiles.DISABLED);
    }

    /**
     * Régression : les identifiants de démonstration attendus par les tests d'intégration
     * et la collection Bruno restent inchangés.
     */
    @Test
    void accounts_usernames_matchDemoCredentials() {
        // Arrange & Act
        List<String> usernames = TestUserProfiles.accounts(true).stream()
                .map(TestAccountProfile::username)
                .toList();

        // Assert
        assertThat(usernames).containsExactly("manu", "admin", "adminonly", "norole", "disabled");
    }

    /**
     * Cas limite : l'inventaire complet ne contient aucun doublon d'identifiant.
     */
    @Test
    void all_returnsDistinctProfiles() {
        // Arrange & Act
        List<TestAccountProfile> profiles = TestUserProfiles.all();
        long distinctUsernames = profiles.stream()
                .map(TestAccountProfile::username)
                .distinct()
                .count();

        // Assert
        assertThat(profiles).hasSize(5);
        assertThat(distinctUsernames).isEqualTo(5);
    }

    /**
     * Immutabilité : les listes exposées refusent toute mutation.
     */
    @Test
    void all_immutable_addProfile_throwsUnsupportedOperationException() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> TestUserProfiles.all().add(TestUserProfiles.VISITOR))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * RBAC : le visiteur dispose uniquement du rôle USER et est actif.
     */
    @Test
    void visitor_hasUserRoleOnlyAndIsEnabled() {
        // Arrange & Act
        TestAccountProfile visitor = TestUserProfiles.VISITOR;

        // Assert
        assertThat(visitor.roles()).containsExactly("USER");
        assertThat(visitor.hasRole("ADMIN")).isFalse();
        assertThat(visitor.enabled()).isTrue();
    }

    /**
     * RBAC : l'administrateur conserve USER et ADMIN pour pouvoir noter et modifier.
     */
    @Test
    void admin_hasUserAndAdminRoles() {
        // Arrange & Act
        TestAccountProfile admin = TestUserProfiles.ADMIN;

        // Assert
        assertThat(admin.roles()).containsExactly("USER", "ADMIN");
        assertThat(admin.enabled()).isTrue();
    }

    /**
     * RBAC : le profil ADMIN_ONLY permet d'isoler les écritures sans rôle de lecture.
     */
    @Test
    void adminOnly_hasAdminRoleWithoutUserRole() {
        // Arrange & Act
        TestAccountProfile adminOnly = TestUserProfiles.ADMIN_ONLY;

        // Assert
        assertThat(adminOnly.roles()).containsExactly("ADMIN");
        assertThat(adminOnly.hasRole("USER")).isFalse();
        assertThat(adminOnly.enabled()).isTrue();
    }

    /**
     * RBAC : le profil NO_ROLE authentifie un compte dépourvu d'autorité.
     */
    @Test
    void noRole_hasNoAuthorityButIsEnabled() {
        // Arrange & Act
        TestAccountProfile noRole = TestUserProfiles.NO_ROLE;

        // Assert
        assertThat(noRole.roles()).isEmpty();
        assertThat(noRole.enabled()).isTrue();
    }

    /**
     * RBAC : le profil DISABLED couvre le refus d'authentification d'un compte désactivé.
     */
    @Test
    void disabled_isDisabledWithUserRole() {
        // Arrange & Act
        TestAccountProfile disabled = TestUserProfiles.DISABLED;

        // Assert
        assertThat(disabled.enabled()).isFalse();
        assertThat(disabled.roles()).containsExactly("USER");
    }

    /**
     * Cohérence : les deux comptes principaux sont toujours présents, drapeau activé ou non.
     */
    @Test
    void core_containsVisitorAndAdminOnly() {
        // Arrange & Act
        List<TestAccountProfile> core = TestUserProfiles.core();

        // Assert
        assertThat(core).containsExactly(TestUserProfiles.VISITOR, TestUserProfiles.ADMIN);
        assertThat(core.stream().map(TestAccountProfile::username).collect(Collectors.toSet()))
                .contains("manu", "admin");
    }
}
