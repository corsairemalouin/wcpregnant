package org.manu.wcpregnant.config;

import org.manu.wcpregnant.bdd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.manu.wcpregnant.bdd.seed.TestAccountProfile;
import org.manu.wcpregnant.bdd.seed.TestUserProfiles;

import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.sql.DataSource;
import java.util.List;


/**
 * Configuration de la sécurité de l'API WcPregnant.
 *
 * <p>Deux chaînes de filtres sont définies : l'une protège les endpoints REST
 * ({@code /toilets/**}, {@code /agent/**}, {@code /api/**}) en mode stateless
 * avec l'authentification Basic, l'autre couvre l'interface web via un formulaire
 * de connexion. Le RBAC appliqué au niveau des méthodes ({@code @PreAuthorize})
 * complète ces règles.</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Remplace @EnableGlobalMethodSecurity (déprécié en Spring Boot 3)
public class AppSecurityConfiguration {

    private final DataSource datasource;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Construit la configuration de sécurité.
     *
     * <p>Le {@link PasswordEncoder} utilisé est le encodeur délégant de Spring
     * Security (BCrypt par défaut), ce qui permet de reconnaître les mots de
     * passe préfixés comme {@code {noop}...} utilisés en démonstration.</p>
     *
     * @param datasource source de données utilisée pour la gestion des utilisateurs en production
     * @param userRepo   repository des utilisateurs de l'application
     */
    @Autowired
    public AppSecurityConfiguration(DataSource datasource, UserRepository userRepo) {
        this.datasource = datasource;
        this.userRepo = userRepo;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Expose l'encodeur de mots de passe comme bean applicatif.
     *
     * <p>Le service de réinitialisation ({@code PasswordResetServiceImpl}) et le
     * seed des comptes partagent ce bean : un seul encodeur est donc utilisé pour
     * hacher les mots de passe et vérifier les codes de réinitialisation.</p>
     *
     * @return l'encodeur délégant (BCrypt par défaut) de Spring Security
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return this.passwordEncoder;
    }

    // --- GESTION DES UTILISATEURS (PROFIL DEV) ---
    /**
     * Fournit le gestionnaire d'utilisateurs de démonstration pour le profil {@code dev}.
     *
     * <p>Les comptes sont créés en mémoire à partir de
     * {@link org.manu.wcpregnant.bdd.seed.TestUserProfiles}, la source de vérité partagée
     * avec l'amorce du profil {@code prod} et la table {@code user_account} : le visiteur
     * {@code manu} (rôle {@code USER}), l'administrateur {@code admin} (rôles {@code USER}
     * et {@code ADMIN}) et, lorsque {@code demo.test-accounts.enabled} est vrai, les profils
     * {@code adminonly} (ADMIN seul), {@code norole} (aucun rôle) et {@code disabled}
     * (compte désactivé). Les mots de passe restent non hachés ({@code {noop}}) pour
     * faciliter les tests locaux.</p>
     *
     * @param userUsername        identifiant du compte visiteur, surchargeable par
     *                            {@code DEMO_USER_USERNAME}
     * @param userPassword        mot de passe du compte visiteur, surchargeable par
     *                            {@code DEMO_USER_PASSWORD}
     * @param adminUsername       identifiant du compte administrateur, surchargeable par
     *                            {@code DEMO_ADMIN_USERNAME}
     * @param adminPassword       mot de passe du compte administrateur, surchargeable par
     *                            {@code DEMO_ADMIN_PASSWORD}
     * @param testAccountsEnabled {@code true} pour amorcer la matrice RBAC complète
     * @return un {@link InMemoryUserDetailsManager} contenant les comptes de démonstration
     */
    @Bean
    @Profile("dev")
    public UserDetailsService userDetailsServiceDev(
            @Value("${demo.user.username:manu}") String userUsername,
            @Value("${demo.user.password:manu}") String userPassword,
            @Value("${demo.admin.username:admin}") String adminUsername,
            @Value("${demo.admin.password:admin}") String adminPassword,
            @Value("${demo.test-accounts.enabled:true}") boolean testAccountsEnabled) {
        List<UserDetails> users = TestUserProfiles.accounts(testAccountsEnabled).stream()
                .map(profile -> toInMemoryUser(profile, userUsername, userPassword, adminUsername, adminPassword))
                .toList();
        return new InMemoryUserDetailsManager(users);
    }

    /**
     * Matérialise un profil de démonstration en utilisateur Spring Security en mémoire.
     *
     * <p>Les deux comptes principaux ({@link org.manu.wcpregnant.bdd.seed.TestUserProfiles#VISITOR}
     * et {@link org.manu.wcpregnant.bdd.seed.TestUserProfiles#ADMIN}) reçoivent les credentials
     * injectés par la configuration ; les profils de test conservent les leurs. Le mot de passe
     * est préfixé {@code {noop}} (pas de hachage) et l'état d'activation du profil est respecté.</p>
     *
     * @param profile       profil déclaré dans {@link org.manu.wcpregnant.bdd.seed.TestUserProfiles}
     * @param userUsername  identifiant effectif du compte visiteur
     * @param userPassword  mot de passe effectif du compte visiteur
     * @param adminUsername identifiant effectif du compte administrateur
     * @param adminPassword mot de passe effectif du compte administrateur
     * @return un {@link User} Spring Security prêt à être enregistré en mémoire
     */
    private UserDetails toInMemoryUser(TestAccountProfile profile,
                                String userUsername, String userPassword,
                                String adminUsername, String adminPassword) {
        TestAccountProfile effective = (profile == TestUserProfiles.VISITOR)
                ? profile.withCredentials(userUsername, userPassword)
                : (profile == TestUserProfiles.ADMIN)
                        ? profile.withCredentials(adminUsername, adminPassword)
                        : profile;

        return User.builder()
                .username(effective.username())
                .password("{noop}" + effective.password())
                .roles(effective.roles().toArray(new String[0]))
                .disabled(!effective.enabled())
                .build();
    }

    /**
     * Fournit le gestionnaire d'utilisateurs basé sur la base relationnelle pour le profil {@code prod}.
     *
     * <p>Spring Security crée et utilise les tables {@code users} et
     * {@code authorities} dans le schéma pointé par {@code dataSource}, ce qui
     * permet une gestion durable des comptes et des rôles.</p>
     *
     * <p>Le bean est typé {@link JdbcUserDetailsManager} (et non
     * {@code UserDetailsService}) afin que {@link org.manu.wcpregnant.bdd.seed.SecurityAccountSeeder}
     * puisse injecter le même bean pour appeler {@code userExists} et
     * {@code createUser} : {@link JdbcUserDetailsManager} implémente
     * {@link UserDetailsService}, le comportement de l'authentification est donc
     * strictement identique.</p>
     *
     * @param dataSource source de données PostgreSQL contenant les comptes utilisateurs
     * @return un {@link JdbcUserDetailsManager} branché sur la base de production
     */
    @Bean
    @Profile("prod")
    public JdbcUserDetailsManager userDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    // --- SONDE DE SANTÉ (ACTUATOR) ---
    /**
     * Chaîne de filtres dédiée aux sondes de santé Actuator.
     *
     * <p>Le {@code HEALTHCHECK} du Dockerfile et les probes {@code liveness} /
     * {@code readiness} du manifeste Kubernetes interrogent l'application sans
     * en-tête d'authentification : la sonde est donc laissée accessible
     * publiquement. Elle n'expose que l'état de disponibilité du service,
     * aucune donnée métier ni information sensible.</p>
     *
     * <p>Le reste de l'Actuator et tous les endpoints métier ({@code /toilets/**},
     * {@code /agent/**}) restent protégés par les chaînes suivantes. La réinitialisation
     * du mot de passe ({@code /auth/**}) et la page d'erreur interne ({@code /error}),
     * utilisée par le dispatch des réponses 400/401, sont explicitement publiques.</p>
     *
     * @param http constructeur de chaîne fourni par Spring Security
     * @return la chaîne de filtres autorisant uniquement les sondes de santé
     */
    @Bean
    @Order(0)
    public SecurityFilterChain healthFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/health", "/actuator/health/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // --- CONFIGURATION API (STATELESS) ---
    /**
     * Chaîne de filtres stateless de l'API métier.
     *
     * <p>Les endpoints {@code /toilets/**}, {@code /api/**} et {@code /agent/**}
     * exigent une authentification Basic avec le RBAC associé, tandis que
     * {@code /auth/**} (réinitialisation du mot de passe) est laissé public :
     * un utilisateur qui a oublié son mot de passe ne peut pas s'authentifier.</p>
     *
     * @param http constructeur de chaîne fourni par Spring Security
     * @return la chaîne de filtres appliquée à l'API
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/toilets/**", "/toilets", "/api/**", "/agent/**", "/auth/**", "/error",
                        // Interface web (SPA Angular) : racine et assets du build
                        "/", "/index.html", "/**/*.js", "/**/*.css", "/**/*.png", "/**/*.jpg",
                        "/**/*.svg", "/**/*.ico", "/**/*.woff", "/**/*.woff2", "/**/*.json", "/**/*.map")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // L'interface web et ses assets sont publics : l'authentification
                        // Basic est effectuée par le formulaire de l'application Angular.
                        .requestMatchers("/", "/index.html", "/**/*.js", "/**/*.css", "/**/*.png",
                                "/**/*.jpg", "/**/*.svg", "/**/*.ico", "/**/*.woff", "/**/*.woff2",
                                "/**/*.json", "/**/*.map").permitAll()
                        // La réinitialisation du mot de passe est accessible sans session
                        .requestMatchers("/auth/**").permitAll()
                        // Le dispatch interne vers /error (400, 401, 500) doit rester public :
                        // sans cette règle, la chaîne « formLogin » intercepte la page d'erreur
                        // et renvoie une redirection 302 vers /login au lieu du code HTTP réel.
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/toilets/**", "/toilets").hasRole("ADMIN")
                        // Les visiteurs (rôle USER) peuvent uniquement déposer une note
                        .requestMatchers(HttpMethod.PUT, "/toilets/*/grade").hasRole("USER")
                        .requestMatchers(HttpMethod.PUT, "/toilets/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/toilets/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    // --- CONFIGURATION PAR DÉFAUT (WEB / LOGIN FORM) ---
    @Bean
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Filtre de journalisation des requêtes HTTP entrantes.
     *
     * <p>Enregistre la chaîne de requête et le corps des appels (limité à
     * 10 000 caractères) dans les logs, ce qui facilite le diagnostic des
     * appels agentiques et des erreurs de validation. Les en-têtes ne sont
     * pas journalisés afin de ne pas exposer les informations d'authentification.</p>
     *
     * @return le filtre configuré, à injecter dans la chaîne de sécurité
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }
}