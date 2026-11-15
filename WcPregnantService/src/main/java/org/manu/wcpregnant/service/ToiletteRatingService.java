package org.manu.wcpregnant.service;

import org.manu.wcpregnant.web.dto.Graduation;
import org.manu.wcpregnant.web.dto.Toilet;

/**
 * Interface spécialisée pour la notation des toilettes par les visiteurs.
 *
 * <p>Sépare la responsabilité "notation après visite" du CRUD administratif
 * ({@link ToiletteCrudService}) et de la recherche de proximité
 * ({@link ToiletteProximityService}), conformément au principe <strong>ISP</strong>.</p>
 *
 * <p>Contrairement à la mise à jour complète d'une toilette, réservée au rôle
 * ADMIN, le dépôt d'une note est ouvert au rôle USER : c'est le parcours
 * "Rechercher → Naviguer → Noter" de l'application mobile.</p>
 *
 * @see ToiletteCrudService
 * @see ToiletteProximityService
 */
public interface ToiletteRatingService {

    /**
     * Enregistre la note déposée par un visiteur sur une toilette.
     *
     * <p>La note est ajoutée à l'historique des visites et la note affichée sur
     * la fiche est recalculée comme la moyenne de toutes les visites.</p>
     *
     * @param id         l'identifiant technique de la toilette notée. Ne doit pas être {@code null}.
     * @param graduation la note déposée (0-10 par critère). Ne doit pas être {@code null}.
     * @return la toilette mise à jour, avec sa note moyenne
     * @throws org.manu.wcpregnant.service.utils.ResourceNotFoundException
     *         si aucune toilette n'existe avec l'identifiant donné
     */
    Toilet gradeToilet(Long id, Graduation graduation);
}
