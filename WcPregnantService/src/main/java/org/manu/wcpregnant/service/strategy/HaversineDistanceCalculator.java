package org.manu.wcpregnant.service.strategy;

import org.manu.wcpregnant.web.dto.GeoPosition;
import org.springframework.stereotype.Component;

/**
 * Implémentation de {@link DistanceCalculator} utilisant la formule de Haversine.
 *
 * <p>Calcule la distance orthodromique entre deux points sur une sphère (la Terre)
 * à l'aide de la formule de Haversine. Le résultat est exprimé en mètres.</p>
 *
 * <p>Respecte le principe <strong>OCP</strong> (Open/Closed Principle) : 
 * l'ajout de nouveaux algorithmes de calcul de distance se fait par création
 * de nouvelles implémentations de {@link DistanceCalculator} sans modifier
 * le code existant.</p>
 *
 * <p>Formule utilisée :</p>
 * <pre>
 *   a = sin²(Δlat/2) + cos(lat1) · cos(lat2) · sin²(Δlon/2)
 *   c = 2 · atan2(√a, √(1−a))
 *   d = R · c · 1000
 * </pre>
 * où R = 6371 km (rayon terrestre moyen).
 *
 * @see DistanceCalculator
 */
@Component
public class HaversineDistanceCalculator implements DistanceCalculator {

    /** Rayon moyen de la Terre en kilomètres. */
    private static final int EARTH_RADIUS_KM = 6371;

    /**
     * {@inheritDoc}
     *
     * <p>Les paramètres d'altitude ne sont pas pris en compte par cette implémentation.
     * En cas de {@code null} pour l'une des positions ou l'une de leurs coordonnées,
     * la méthode retourne {@link Double#MAX_VALUE}.</p>
     */
    @Override
    public double calculate(GeoPosition p1, GeoPosition p2) {
        if (p1 == null || p2 == null
                || p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            return Double.MAX_VALUE;
        }

        double latDistance = Math.toRadians(p2.getLatitude() - p1.getLatitude());
        double lonDistance = Math.toRadians(p2.getLongitude() - p1.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(p1.getLatitude())) * Math.cos(Math.toRadians(p2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c * 1000; // conversion en mètres
    }
}