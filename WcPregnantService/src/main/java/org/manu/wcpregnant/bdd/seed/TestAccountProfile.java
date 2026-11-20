package org.manu.wcpregnant.bdd.seed;

import java.util.List;
import java.util.Objects;

/**
 * Profil de compte de démonstration utilisé pour amorcer le référentiel d'authentification.
 *
 * <p>Il décrit l'identité d'un compte test (nom, mot de passe, e-mail), les rôles RBAC qui
 * lui sont attachés et l'état d'activation du compte. Ces profils sont la source de vérité
 * unique partagée par {@link org.manu.wcpregnant.config.AppSecurityConfiguration} (comptes
 * en mémoire du profil {@code dev}), {@link SecurityAccountSeeder} (tables {@code users} /
 * {@code authorities} du profil {@code prod}) et {@link UserAccountSeeder} (table
 * {@code user_account} du flux de réinitialisation du mot de passe).</p>
 *
 * <p>Le type est immuable : la liste des rôles est normalisée par {@code List.copyOf}, ce qui
 * rend impossible toute mutation accidentelle d'un profil partagé.</p>
 *
 * @param username  identifiant de connexion, non null
 * @param password  mot de passe de démonstration en clair, non null (haché par le seedeur)
 * @param roles     rôles Spring Security attachés au compte ; {@code null} est normalisé en liste vide
 * @param enabled   {@code true} si le compte peut se connecter, {@code false} s'il est désactivé
 * @param email     e-mail de démonstration utilisé par la réinitialisation du mot de passe
 * @param description rôle attendu du profil, destiné à la documentation et aux logs
 */
public record TestAccountProfile(
        String username,
        String password,
        List<String> roles,
        boolean enabled,
        String email,
        String description) {

    /**
     * Constructeur compact : valide les champs obligatoires et rend la liste des rôles immuable.
     *
     * @param username identifiant de connexion
     * @param password mot de passe de démonstration
     * @param roles rôles Spring Security, {@code null} accepté et transformé en liste vide
     * @param enabled état d'activation du compte
     * @param email e-mail de démonstration
     * @param description description du profil
     * @throws NullPointerException si {@code username} ou {@code password} est {@code null}
     */
    public TestAccountProfile {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
        roles = (roles == null) ? List.of() : List.copyOf(roles);
    }

    /**
     * Retourne une copie du profil avec un identifiant et un mot de passe surchargés.
     *
     * <p>Permet aux seeders d'appliquer les valeurs injectées par configuration
     * ({@code demo.user.username}, {@code demo.admin.password}, etc.) sans jamais
     * muter le profil partagé de {@link TestUserProfiles}.</p>
     *
     * @param username nouvel identifiant de connexion, non null
     * @param password nouveau mot de passe en clair, non null
     * @return un nouveau {@link TestAccountProfile} immuable, rôles et état inchangés
     * @throws NullPointerException si {@code username} ou {@code password} est {@code null}
     */
    public TestAccountProfile withCredentials(String username, String password) {
        return new TestAccountProfile(username, password, roles, enabled, email, description);
    }

    /**
     * Indique si le profil dispose du rôle recherché.

     *
     * @param role nom du rôle (par exemple {@code USER} ou {@code ADMIN}), non null
     * @return {@code true} si le rôle est présent dans la liste des rôles du profil
     * @throws NullPointerException si {@code role} est {@code null}
     */
    public boolean hasRole(String role) {
        Objects.requireNonNull(role, "role must not be null");
        return roles.contains(role);
    }
}
