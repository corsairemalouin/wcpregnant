package org.manu.wcpregnant.bdd.repository;

import java.util.Optional;

import org.manu.wcpregnant.bdd.dao.users.UserAccount;
import org.springframework.data.repository.CrudRepository;

/**
 * Repository d'accès aux comptes utilisateur ({@link UserAccount}).
 *
 * <p>Ce repository est distinct de {@link UserRepository} (table {@code users}),
 * conservé pour la compatibilité des données existantes : {@link UserAccount}
 * porte la réinitialisation du mot de passe et ne génère aucun conflit de
 * mapping Hibernate avec les entités {@code AdressDAO} / {@code CustomerDAO}.</p>
 */
public interface UserAccountRepository extends CrudRepository<UserAccount, Long> {

    /**
     * Recherche un compte par nom d'utilisateur, sans tenir compte de la casse.
     *
     * @param username nom d'utilisateur recherché
     * @return le compte correspondant, ou {@code Optional.empty()} s'il n'existe pas
     */
    Optional<UserAccount> findByUsernameIgnoreCase(String username);

    /**
     * Recherche un compte par adresse e-mail, sans tenir compte de la casse.
     *
     * @param email adresse e-mail recherchée
     * @return le compte correspondant, ou {@code Optional.empty()} s'il n'existe pas
     */
    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
