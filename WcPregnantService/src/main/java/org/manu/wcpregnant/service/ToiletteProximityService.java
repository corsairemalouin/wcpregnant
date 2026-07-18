package org.manu.wcpregnant.service;

import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;

import java.util.List;

/**
 * Interface spécialisée pour la recherche de toilettes par proximité géographique.
 *
 * <p>Respecte le principe <strong>ISP</strong> (Interface Segregation Principle)
 * en séparant cette responsabilité des opérations CRUD ({@link ToiletteCrudService}).</p>
 *
 * <p>Cette interface permet de rechercher les toilettes les plus proches d'une
 * position GPS donnée, en utilisant un algorithme de calcul de distance
 * (cf. {@link org.manu.wcpregnant.service.strategy.DistanceCalculator}).</p>
 *
 * @see ToiletteCrudService
 * @see ToiletteService
 */
public interface ToiletteProximityService {

    /**
     * Recherche les toilettes les plus proches d'une position géographique donnée.
     *
     * <p>Le calcul de distance, le filtrage par distance maximale, le tri et
     * la limitation du nombre de résultats sont délégués à l'implémentation
     * concrète de ce service.</p>
     *
     * @param currentPosition la position GPS de référence (latitude, longitude).
     *                        Ne doit pas être {@code null}.
     * @return une liste triée des toilettes les plus proches, limitée selon la configuration
     */
    List<Toilet> getNearestToilets(GeoPosition currentPosition);
}