package org.manu.wcpregnant.bdd.seed;

import java.util.List;

/**
 * Inventaire des profils de comptes de démonstration couvrant l'intégralité de la matrice RBAC.
 *
 * <p>Chaque profil correspond à un scénario de test vérifiable : lecture seule, écriture
 * administrative, rôle insuffisant, compte désactivé. Le profil <em>anonyme</em> (aucune
 * credential) n'est pas un compte : il est obtenu en n'envoyant simplement pas d'en-tête
 * {@code Authorization} et donne un {@code 401}.</p>
 *
 * <p>Les identifiants sont des mots de passe de démonstration. Avant toute exposition réelle,
 * désactiver les profils de test supplémentaires avec la propriété
 * {@code demo.test-accounts.enabled=false} (variable d'environnement
 * {@code DEMO_TEST_ACCOUNTS_ENABLED}) et surcharger les comptes principaux via
 * {@code demo.user.username} / {@code demo.user.password} / {@code demo.admin.username} /
 * {@code demo.admin.password}.</p>
 */
public final class TestUserProfiles {

    /**
     * Visiteur : consultation de la liste et notation d'un équipement, mais aucune création
     * ni suppression ({@code 403} sur les opérations CRUD).
     */
    public static final TestAccountProfile VISITOR = new TestAccountProfile(
            "manu", "manu", List.of("USER"), true, "manu@example.com",
            "Rôle USER : lecture + notation, CRUD interdit");

    /**
     * Administrateur complet : cumule {@code USER} et {@code ADMIN} pour pouvoir à la fois
     * créer, modifier, supprimer et noter un équipement.
     */
    public static final TestAccountProfile ADMIN = new TestAccountProfile(
            "admin", "admin", List.of("USER", "ADMIN"), true, "admin@example.com",
            "Rôles USER + ADMIN : CRUD complet et notation");

    /**
     * Administrateur strict : {@code ADMIN} seul. Démonstre que les rôles ne sont pas
     * hiérarchiques — la notation ({@code hasRole('USER')}) lui est refusée.
     */
    public static final TestAccountProfile ADMIN_ONLY = new TestAccountProfile(
            "adminonly", "adminonly", List.of("ADMIN"), true, "adminonly@example.com",
            "Rôle ADMIN seul : CRUD autorisé, notation refusée");

    /**
     * Authentifié sans rôle : le compte existe et s'authentifie, mais ne possède aucune
     * autorité, donc toute opération protégée par {@code hasRole(...)} renvoie {@code 403}.
     */
    public static final TestAccountProfile NO_ROLE = new TestAccountProfile(
            "norole", "norole", List.of(), true, "norole@example.com",
            "Authentifié sans autorité : 403 sur la notation et le CRUD");

    /**
     * Compte désactivé : l'authentification échoue ({@code 401}) même avec le bon mot de passe.
     */
    public static final TestAccountProfile DISABLED = new TestAccountProfile(
            "disabled", "disabled", List.of("USER"), false, "disabled@example.com",
            "Compte désactivé : authentification refusée (401)");

    /**
     * Socle minimal : les deux comptes de démonstration historiques, toujours amorcés.
     */
    private static final List<TestAccountProfile> CORE = List.of(VISITOR, ADMIN);

    /**
     * Matrice complète : socle minimal + profils de test supplémentaires.
     */
    private static final List<TestAccountProfile> ALL =
            List.of(VISITOR, ADMIN, ADMIN_ONLY, NO_ROLE, DISABLED);

    /**
     * Utilitaire : cette classe ne s'instancie jamais.
     */
    private TestUserProfiles() {
        // Utility holder: instantiation is deliberately forbidden.
    }

    /**
     * Retourne les profils à amorcer selon le drapeau des comptes de test.
     *
     * @param includeTestProfiles {@code true} pour la matrice RBAC complète, {@code false}
     *                            pour se limiter aux deux comptes principaux
     * @return liste immuable des profils, jamais {@code null} ni vide
     */
    public static List<TestAccountProfile> accounts(boolean includeTestProfiles) {
        return includeTestProfiles ? ALL : CORE;
    }

    /**
     * Retourne la matrice RBAC complète des profils de démonstration.
     *
     * @return liste immuable des cinq profils définis
     */
    public static List<TestAccountProfile> all() {
        return ALL;
    }

    /**
     * Retourne le socle minimal de comptes de démonstration.
     *
     * @return liste immuable des deux comptes principaux ({@code manu} et {@code admin})
     */
    public static List<TestAccountProfile> core() {
        return CORE;
    }
}
