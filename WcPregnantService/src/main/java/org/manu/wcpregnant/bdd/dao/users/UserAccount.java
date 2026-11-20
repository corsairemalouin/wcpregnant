package org.manu.wcpregnant.bdd.dao.users;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.manu.wcpregnant.bdd.dao.Authorities;

/**
 * Compte utilisateur de l'application WcPregnant.
 *
 * <p>Cette entité porte la feature « réinitialisation du mot de passe » (contrat
 * {@code api/AuthAPI.yaml}) : adresse e-mail du compte, code de réinitialisation
 * haché et date d'expiration de ce code.</p>
 *
 * <p>Elle est volontairement distincte de {@link org.manu.wcpregnant.bdd.dao.Users},
 * utilisée par l'authentification Spring Security existante : sans nom d'entité et
 * de table différents, Hibernate lève une {@code DuplicateMappingException}.</p>
 */
@Entity(name = "UserAccount")
@Table(name = "user_account")
public class UserAccount {

    /** Identifiant unique du compte. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de connexion, unique. */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /** Mot de passe haché (BCrypt) du compte. */
    @Column(name = "password", nullable = false)
    private String password;

    /** Adresse e-mail servant à envoyer le code de réinitialisation. */
    @Column(name = "email", unique = true, length = 120)
    private String email;


    /**
     * Construit un compte complet, notamment utilisé par le seed et les tests.
     *
     * @param username identifiant de connexion
     * @param password mot de passe déjà haché
     * @param email    adresse e-mail destinataire des codes de réinitialisation
     * @param enabled  compte activé ou non
     */
    public UserAccount(String username, String password, String email, boolean enabled) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.enabled = enabled;
    }

    /**
     * Code de réinitialisation haché (BCrypt préfixé {@code {bcrypt}} : 68 caractères),
     * d'où une longueur de colonne de 100 pour éviter toute troncature.
     */
    @Column(name = "reset_code", length = 100)
    private String resetCode;

    /** Instant d'expiration du code de réinitialisation en cours. */
    @Column(name = "reset_code_expires_at")
    private Instant resetCodeExpiresAt;

    /**
     * Rôles rattachés au compte.
     *
     * <p>Association unidirectionnelle : la table {@code authorities} reste la source
     * de vérité du RBAC et n'est pas modifiée. Le lien est matérialisé par la table
     * de jointure {@code user_account_authorities} créée par Hibernate
     * ({@code ddl-auto=update}).</p>
     */
    @OneToMany
    @JoinTable(
            name = "user_account_authorities",
            joinColumns = @JoinColumn(name = "user_account_id"),
            inverseJoinColumns = @JoinColumn(name = "authority_id"))
    private List<Authorities> authorities = new ArrayList<>();

    /** Indique si le compte peut se connecter. */
    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    /** Constructeur par défaut requis par JPA. */
    public UserAccount() {
    }

    /**
     * Construit un compte avec son identifiant, son mot de passe et son état.
     *
     * @param username identifiant du compte
     * @param password mot de passe déjà haché
     * @param enabled  {@code true} si le compte est activé
     */
    public UserAccount(String username, String password, Boolean enabled) {
        this.username = username;
        this.password = password;
        this.enabled = enabled;
    }

    /**
     * Retourne l'identifiant unique du compte.
     *
     * @return l'identifiant du compte
     */
    public Long getId() {
        return id;
    }

    /**
     * Retourne l'identifiant de connexion du compte.
     *
     * @return l'identifiant de connexion
     */
    public String getUsername() {
        return username;
    }

    /**
     * Retourne le mot de passe haché du compte.
     *
     * @return le mot de passe haché
     */
    public String getPassword() {
        return password;
    }

    /**
     * Retourne l'adresse e-mail du compte.
     *
     * @return l'adresse e-mail, peut être {@code null}
     */
    public String getEmail() {
        return email;
    }

    /**
     * Retourne le code de réinitialisation actuellement enregistré.
     *
     * @return le code haché, ou {@code null} si aucune demande n'est en cours
     */
    public String getResetCode() {
        return resetCode;
    }

    /**
     * Retourne la date d'expiration du code de réinitialisation en cours.
     *
     * @return l'instant d'expiration, ou {@code null} si aucune demande n'est en cours
     */
    public Instant getResetCodeExpiresAt() {
        return resetCodeExpiresAt;
    }

    /**
     * Retourne les rôles rattachés au compte.
     *
     * @return la liste des {@link Authorities} du compte
     */
    public List<Authorities> getAuthorities() {
        return authorities;
    }

    /**
     * Indique si le compte est activé.
     *
     * @return {@code true} lorsque le compte peut s'authentifier
     */
    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    /**
     * Change l'identifiant de connexion du compte.
     *
     * @param username le nouvel identifiant
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Change le mot de passe du compte.
     *
     * @param password le nouveau mot de passe, déjà haché
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Change l'adresse e-mail du compte.
     *
     * @param email la nouvelle adresse e-mail
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Enregistre un code de réinitialisation et sa date d'expiration.
     *
     * @param resetCode          le code haché
     * @param resetCodeExpiresAt l'instant d'expiration du code
     */
    public void setResetCode(String resetCode, Instant resetCodeExpiresAt) {
        this.resetCode = resetCode;
        this.resetCodeExpiresAt = resetCodeExpiresAt;
    }

    /**
     * Supprime le code de réinitialisation en cours.
     */
    public void clearResetCode() {
        this.resetCode = null;
        this.resetCodeExpiresAt = null;
    }

    /**
     * Remplace la liste des rôles rattachés au compte.
     *
     * @param authorities la nouvelle liste de rôles
     */
    public void setAuthorities(List<Authorities> authorities) {
        this.authorities = authorities;
    }

    /**
     * Active ou désactive le compte.
     *
     * @param enabled {@code true} pour activer le compte
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
