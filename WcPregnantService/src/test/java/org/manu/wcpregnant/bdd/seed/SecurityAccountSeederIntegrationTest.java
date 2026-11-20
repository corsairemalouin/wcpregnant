package org.manu.wcpregnant.bdd.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration de l'amorce des comptes Spring Security du profil
 * {@code prod} : le {@link JdbcUserDetailsManager} est branché sur une base H2
 * en mémoire dont Hibernate crée les tables {@code users} et {@code authorities}
 * à partir des entités {@code Users} et {@code Authorities}.
 *
 * <p>Ce test valide le chemin réel de production (JDBC, pas de manager en
 * mémoire) : création effective des comptes, idempotence, rôles écrits dans
 * {@code authorities} et mots de passe hachés.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:securityseed;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create"
})
@ActiveProfiles("prod")
@DisplayName("Intégration : amorce des comptes Spring Security (profil prod, H2 en mémoire)")
class SecurityAccountSeederIntegrationTest {

    @Autowired
    private JdbcUserDetailsManager userDetailsManager;

    @Autowired
    private SecurityAccountSeeder seeder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Nominal : les deux comptes de démonstration sont bien présents dans le
     * référentiel JDBC après l'amorce.
     */
    @Test
    void seedSecurityAccounts_onJdbcRepository_createsDemoAccounts() {
        // Arrange & Act
        seeder.seedSecurityAccounts();

        // Assert
        assertThat(userDetailsManager.userExists("manu")).isTrue();
        assertThat(userDetailsManager.userExists("admin")).isTrue();
    }

    /**
     * Idempotence : une seconde invocation ne crée aucun doublon.
     */
    @Test
    void seedSecurityAccounts_secondInvocation_createsNothing() {
        // Arrange
        seeder.seedSecurityAccounts();

        // Act
        int created = seeder.seedSecurityAccounts();

        // Assert
        assertThat(created).isZero();
    }

    /**
     * RBAC : les rôles sont réellement écrits dans la table {@code authorities}.
     */
    @Test
    void seedSecurityAccounts_writesAuthorities_perProfile() {
        // Arrange & Act
        seeder.seedSecurityAccounts();

        // Assert
        UserDetails visitor = userDetailsManager.loadUserByUsername("manu");
        UserDetails administrator = userDetailsManager.loadUserByUsername("admin");

        assertThat(visitor.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .containsExactly("ROLE_USER");
        assertThat(administrator.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .contains("ROLE_USER", "ROLE_ADMIN");
    }

    /**
     * Sécurité : le mot de passe stocké est haché et vérifiable par l'encodeur
     * partagé avec la réinitialisation du mot de passe.
     */
    @Test
    void seedSecurityAccounts_storesHashedPassword_verifiableByEncoder() {
        // Arrange & Act
        seeder.seedSecurityAccounts();

        // Assert
        String stored = userDetailsManager.loadUserByUsername("admin").getPassword();
        assertThat(stored).startsWith("{bcrypt}");
        assertThat(passwordEncoder.matches("admin", stored)).isTrue();
        assertThat(stored).isNotEqualTo("admin");
    }

    /**
     * Câblage : avec le profil {@code prod}, le bean injecté est bien un
     * {@link JdbcUserDetailsManager}, type attendu par l'amorceur de comptes.
     */
    @Test
    void userDetailsServiceBean_whenProdProfile_isJdbcUserDetailsManager() {
        // Arrange & Act
        Object manager = userDetailsManager;

        // Assert
        assertThat(manager).isInstanceOf(JdbcUserDetailsManager.class);
        assertThat(seeder).isNotNull();
    }
}
