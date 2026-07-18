package org.manu.wcpregnant.service.strategy;

import org.manu.wcpregnant.web.dto.GeoPosition;

/**
 * Interface stratégique pour le calcul de distance entre deux positions géographiques.
 *
 * <p>Respecte les principes SOLID suivants :</p>
 * <ul>
 *   <li><strong>OCP</strong> (Open/Closed Principle) : permet d'ajouter de nouveaux algorithmes
 *       de calcul de distance sans modifier le code existant.</li>
 *   <li><strong>DIP</strong> (Dependency Inversion Principle) : les clients dépendent de cette
 *       abstraction plutôt que d'une implémentation concrète.</li>
 * </ul>
 *
 * @see HaversineDistanceCalculator
 */
@FunctionalInterface
public interface DistanceCalculator {

    /**
     * Calcule la distance entre deux positions géographiques.
     *
     * @param p1 la première position (latitude, longitude). Ne doit pas être {@code null}.
     * @param p2 la seconde position (latitude, longitude). Ne doit pas être {@code null}.
     * @return la distance en mètres entre les deux positions, ou {@link Double#MAX_VALUE}
     *         si l'une des positions est {@code null} ou a des coordonnées {@code null}
     */
    double calculate(GeoPosition p1, GeoPosition p2);
}