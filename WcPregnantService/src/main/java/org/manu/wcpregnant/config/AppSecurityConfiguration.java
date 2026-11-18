package org.manu.wcpregnant.config;

import org.manu.wcpregnant.bdd.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import javax.sql.DataSource;

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

    // --- GESTION DES UTILISATEURS (PROFIL DEV) ---
    /**
     * Fournit le gestionnaire d'utilisateurs de démonstration pour le profil {@code dev}.
     *
     * <p>Deux comptes sont créés en mémoire : {@code manu} (rôle {@code USER})
     * et {@code admin} (rôles {@code USER} et {@code ADMIN}), avec des mots de
     * passe non hachés pour faciliter les tests locaux.</p>
     *
     * @return un {@link InMemoryUserDetailsManager} contenant les comptes de démonstration
     */
    @Bean
    @Profile("dev")
    public UserDetailsService userDetailsServiceDev() {
        return new InMemoryUserDetailsManager(
                User.builder()
                        .username("manu")
                        .password("{noop}manu")
                        .roles("USER")
                        .build(),
                User.builder()
                        .username("admin")
                        .password("{noop}admin")
                        .roles("USER", "ADMIN")
                        .build()
        );
    }

    /**
     * Fournit le gestionnaire d'utilisateurs basé sur la base relationnelle pour le profil {@code prod}.
     *
     * <p>Spring Security crée et utilise les tables {@code users} et
     * {@code authorities} dans le schéma pointé par {@code dataSource}, ce qui
     * permet une gestion durable des comptes et des rôles.</p>
     *
     * @param dataSource source de données PostgreSQL contenant les comptes utilisateurs
     * @return un {@link JdbcUserDetailsManager} branché sur la base de production
     */
    @Bean
    @Profile("prod")
    public UserDetailsService userDetailsService(DataSource dataSource) {
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
     * {@code /agent/**}) restent protégés par les chaînes suivantes.</p>
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
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/toilets/**", "/toilets", "/api/**", "/agent/**") // Ajoute /toilets et les endpoints agentiques
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
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