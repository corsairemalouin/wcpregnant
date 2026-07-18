package org.manu.wcpregnant.service;

import org.manu.wcpregnant.service.utils.ResourceNotFoundException;
import org.manu.wcpregnant.web.dto.Toilet;

import java.util.List;
import java.util.Optional;

/**
 * Interface spécialisée pour les opérations CRUD sur les toilettes.
 *
 * <p>Respecte le principe <strong>ISP</strong> (Interface Segregation Principle)
 * en séparant les responsabilités de lecture/écriture (CRUD) de la recherche
 * par proximité géographique ({@link ToiletteProximityService}).</p>
 *
 * <p>Les opérations CRUD incluent :</p>
 * <ul>
 *   <li>Création d'une nouvelle toilette</li>
 *   <li>Lecture d'une toilette par identifiant</li>
 *   <li>Mise à jour d'une toilette existante</li>
 *   <li>Suppression d'une toilette</li>
 *   <li>Liste paginée de toutes les toilettes</li>
 * </ul>
 *
 * @see ToiletteProximityService
 * @see ToiletteService
 */
public interface ToiletteCrudService {

    /**
     * Récupère toutes les toilettes avec pagination.
     *
     * @param page le numéro de page (0-indexé). Ne doit pas être {@code null}.
     * @param size le nombre d'éléments par page. Ne doit pas être {@code null}.
     * @return une liste (potentiellement vide) des objets {@link Toilet} pour la page demandée
     */
    List<Toilet> getAllToilets(Integer page, Integer size);

    /**
     * Ajoute une nouvelle toilette en base de données.
     *
     * @param toilet l'objet métier contenant les données de la toilette à créer
     *               (libellé, adresse, position géographique, grille d'évaluation).
     *               Ne doit pas être {@code null}.
     * @return l'objet {@link Toilet} représentant la toilette fraîchement créée,
     *         incluant son identifiant généré par la base de données
     */
    Toilet addToilet(Toilet toilet);

    /**
     * Récupère une toilette par son identifiant.
     *
     * @param id l'identifiant technique de la toilette. Ne doit pas être {@code null}.
     * @return un {@link Optional} contenant la toilette si trouvée, {@link Optional#empty()} sinon
     */
    Optional<Toilet> getToiletById(Long id);

    /**
     * Met à jour une toilette existante.
     *
     * @param id     l'identifiant technique de la toilette à mettre à jour. Ne doit pas être {@code null}.
     * @param toilet l'objet métier contenant les nouvelles données. Ne doit pas être {@code null}.
     * @throws ResourceNotFoundException si aucune toilette n'est trouvée avec l'identifiant donné
     */
    void updateToilet(Long id, Toilet toilet);

    /**
     * Supprime une toilette par son identifiant.
     *
     * @param id l'identifiant technique de la toilette à supprimer. Ne doit pas être {@code null}.
     * @throws ResourceNotFoundException si aucune toilette n'est trouvée avec l'identifiant donné
     */
    void deleteToilet(Long id);
}