package org.manu.wcpregnant.service.utils;


import org.manu.wcpregnant.web.dto.GeoPosition;
import org.manu.wcpregnant.web.dto.Toilet;

/**
 * Boîte à outils de calcul et de comparaison de distances géographiques.
 *
 * <p>Ces helpers historiques travaillent directement sur les DTO de l'API
 * ({@link GeoPosition}, {@link Toilet}). La recherche de proximité "officielle"
 * passe par la stratégie {@link org.manu.wcpregnant.service.strategy.DistanceCalculator} ;
 * ces méthodes restent utiles pour les traitements rapides et les tests.</p>
 *
 * <p>Convention : une distance est exprimée en <strong>mètres</strong>. Une
 * position incomplète est traitée comme "très loin" ({@link Double#MAX_VALUE}),
 * ce qui la place en fin de tri sans la faire disparaître.</p>
 */
public class GeoPositionUtils {

    /**
     * Calcule la distance orthodromique entre deux coordonnées, corrigée par la différence d'altitude.
     *
     * <p>La formule de Haversine donne la distance à la surface de la Terre
     * (rayon de 6371 km) ; la différence d'altitude est ensuite ajoutée en
     * quadrature, ce qui correspond à la distance "à vol d'oiseau" réelle
     * entre les deux points.</p>
     *
     * @param lat1 latitude du premier point, en degrés décimaux
     * @param lon1 longitude du premier point, en degrés décimaux
     * @param lat2 latitude du second point, en degrés décimaux
     * @param lon2 longitude du second point, en degrés décimaux
     * @param el1  altitude du premier point, en mètres
     * @param el2  altitude du second point, en mètres
     * @return la distance en mètres entre les deux points
     */
    // corrected parameter order: lat1, lon1, lat2, lon2, el1, el2
    public static double distance(double lat1, double lon1, double lat2, double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    /**
     * Calcule la distance en mètres entre deux positions de l'API.
     *
     * <p>Les altitudes ne sont pas connues dans le modèle de données, elles
     * sont donc considérées comme nulles.</p>
     *
     * @param p1 première position, peut être {@code null}
     * @param p2 seconde position, peut être {@code null}
     * @return la distance en mètres, ou {@link Double#MAX_VALUE} si l'une des
     *         positions ou l'une de ses coordonnées est {@code null}
     */
    public static double distance(GeoPosition p1, GeoPosition p2) {
        if (p1 == null || p2 == null || p1.getLatitude() == null || p1.getLongitude() == null
                || p2.getLatitude() == null || p2.getLongitude() == null) {
            // treat missing data as very far
            return Double.MAX_VALUE;
        }
        return distance(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude(), 0, 0);
    }

    /**
     * Calcule et renseigne la distance d'une toilette par rapport à la position de l'utilisateur.
     *
     * <p>La valeur {@link Double#MAX_VALUE} (position inconnue) est convertie en
     * {@code null} afin de ne pas afficher une distance absurde dans la réponse JSON.</p>
     *
     * @param currentToilet  la toilette à enrichir, ignorée si {@code null}
     * @param currentPosition la position de référence, peut être {@code null}
     */
    public static void setDistance(Toilet currentToilet, GeoPosition currentPosition) {
        if (currentToilet == null) {
            return;
        }
        double d = distance(currentToilet.getPosition(), currentPosition);
        // wrap primitive in Double for DTO
        currentToilet.setDistance(d == Double.MAX_VALUE ? null : d);
    }

    /**
     * Comparateur de toilettes fondé sur la distance déjà calculée.
     *
     * <p>Destiné aux tris du type {@code list.sort(GeoPositionUtils::compareWithDistance)}.
     * Une toilette sans distance est considérée comme infiniment éloignée et finit
     * donc en dernière position.</p>
     *
     * @param t1 première toilette, peut être {@code null}
     * @param t2 seconde toilette, peut être {@code null}
     * @return un nombre négatif si {@code t1} est plus proche, positif si elle est
     *         plus éloignée, {@code 0} si les distances sont égales
     */
    public static int compareWithDistance(Toilet t1, Toilet t2) {
        double d1 = t1 == null || t1.getDistance() == null ? Double.MAX_VALUE : t1.getDistance();
        double d2 = t2 == null || t2.getDistance() == null ? Double.MAX_VALUE : t2.getDistance();
        return Double.compare(d1, d2);

    }
}
