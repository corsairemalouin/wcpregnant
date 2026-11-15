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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Remplace @EnableGlobalMethodSecurity (déprécié en Spring Boot 3)
public class AppSecurityConfiguration {

    private final DataSource datasource;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AppSecurityConfiguration(DataSource datasource, UserRepository userRepo) {
        this.datasource = datasource;
        this.userRepo = userRepo;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    // --- GESTION DES UTILISATEURS (PROFIL DEV) ---
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

    @Bean
    @Profile("prod")
    public UserDetailsService userDetailsService(DataSource dataSource) {
        return new JdbcUserDetailsManager(dataSource);
    }

    // --- CONFIGURATION API (STATELESS) ---
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/toilets/**", "/toilets", "/api/**") // Ajoute /toilets
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