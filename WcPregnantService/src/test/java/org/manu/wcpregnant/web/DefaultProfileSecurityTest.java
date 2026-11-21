package org.manu.wcpregnant.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'intégration du comportement de sécurité lorsque <strong>aucun profil</strong>
 * n'est actif, c'est-à-dire le lancement local par défaut
 * ({@code mvn spring-boot:run} sans {@code -Dspring-boot.run.profiles=dev}).
 *
 * <p>Avant la correction, Spring Security auto-configurait un
 * {@code InMemoryUserDetailsManager} avec un utilisateur {@code user} et un mot de
 * passe aléatoire : les comptes de démonstration amorcés par
 * {@link org.manu.wcpregnant.bdd.seed.UserAccountSeeder} ne pouvaient pas
 * s'authentifier (401 systématique). Le bean
 * {@code userDetailsServiceDev} est désormais déclaré avec
 * {@code @Profile("!prod")} pour couvrir ce cas.</p>
 */
@SpringBootTest(properties = {
        // Base H2 dédiée : ce contexte Spring est distinct de celui des autres classes
        // d'intégration, donc jamais pollué par les mocks injectés dans le bean
        // ToiletteServiceImpl. Le comportement testé reste celui du profil par défaut.
        "spring.datasource.url=jdbc:h2:mem:default-profile-security;DB_CLOSE_DELAY=-1"
}) // Aucun @ActiveProfiles : profil par défaut (aucun)
@DisplayName("Authentification par défaut (aucun profil actif)")
class DefaultProfileSecurityTest {

    private static final String TOILETS = "/toilets";

    /** Payload valide pour la création d'une toilette (validation interne respectée). */
    private static final String VALID_TOILET_JSON = """
        {
            "libelle": "WC profil par défaut",
            "adress": {
                "ville": "Rennes",
                "codePostal": "35000"
            },
            "position": {
                "latitude": 48.11,
                "longitude": -1.67
            },
            "grade": {
                "proprete": 5,
                "accueil": 5,
                "espace": 5
            }
        }
        """;

    @Autowired
    private WebApplicationContext webApplicationContext;

    /** Gestionnaire d'utilisateurs réellement résolu par la chaîne de sécurité. */
    @Autowired
    private UserDetailsService userDetailsService;

    /** Spring Boot 4.x : MockMvc est construit manuellement depuis le WebApplicationContext. */
    private MockMvc mockMvc;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("Aucun profil : le bean UserDetailsService est celui de démonstration, pas l'auto-config Spring Security")
    void defaultProfile_userDetailsService_isDemoInMemoryManager() {
        // Assert : le manager en mémoire d'AppSecurityConfiguration est retenu ; l'utilisateur
        // « user » généré par l'auto-config Spring Security n'existe donc pas.
        assertThat(userDetailsService).isInstanceOf(InMemoryUserDetailsManager.class);
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("user"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Aucun profil : le compte visiteur de démonstration est chargé avec le rôle ROLE_USER")
    void defaultProfile_visitorDemoAccount_isAuthenticable() {
        // Arrange & Act
        UserDetails visitor = userDetailsService.loadUserByUsername("manu");

        // Assert : User.builder().roles("USER") produit l'autorité « ROLE_USER »,
        // seule forme reconnue par hasRole("USER").
        assertThat(visitor.getUsername()).isEqualTo("manu");
        assertThat(visitor.getAuthorities().stream().map(a -> a.getAuthority()))
                .contains("ROLE_USER");
        assertThat(visitor.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Aucun profil : le compte admin de démonstration conserve ROLE_USER + ROLE_ADMIN")
    void defaultProfile_adminDemoAccount_hasAdminRole() {
        // Arrange & Act
        UserDetails admin = userDetailsService.loadUserByUsername("admin");

        // Assert
        assertThat(admin.getAuthorities().stream().map(a -> a.getAuthority()))
                .containsAll(List.of("ROLE_USER", "ROLE_ADMIN"));
        assertThat(admin.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Aucun profil : un identifiant inconnu lève UsernameNotFoundException")
    void defaultProfile_unknownUser_throwsUsernameNotFoundException() {
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("inconnu"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("Aucun profil : GET /toilets avec manu:manu répond 200 et non 401")
    void defaultProfile_getToiletsWithDemoCredentials_returns200() throws Exception {
        mockMvc.perform(get(TOILETS).with(httpBasic("manu", "manu")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Aucun profil : le visiteur manu ne peut pas créer une toilette (403)")
    void defaultProfile_visitorCannotCreateToilet_returns403() throws Exception {
        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("manu", "manu"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Aucun profil : l'administrateur admin peut créer une toilette (201)")
    void defaultProfile_adminCanCreateToilet_returns201() throws Exception {
        mockMvc.perform(post(TOILETS)
                        .with(httpBasic("admin", "admin"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_TOILET_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Aucun profil : le compte désactivé de la matrice RBAC reste refusé (401)")
    void defaultProfile_disabledAccount_isRejected() throws Exception {
        mockMvc.perform(get(TOILETS).with(httpBasic("disabled", "disabled")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Aucun profil : requête anonyme sur /toilets répond 401")
    void defaultProfile_anonymousRequest_returns401() throws Exception {
        mockMvc.perform(get(TOILETS))
                .andExpect(status().isUnauthorized());
    }
}
