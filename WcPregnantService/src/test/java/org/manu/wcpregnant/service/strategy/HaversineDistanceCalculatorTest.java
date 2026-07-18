package org.manu.wcpregnant.service.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.web.dto.GeoPosition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires pour {@link HaversineDistanceCalculator}.
 *
 * <p>Vérifie le calcul de distance entre deux positions géographiques
 * avec la formule de Haversine, ainsi que la gestion des valeurs
 * {@code null} et des cas aux limites.</p>
 */
class HaversineDistanceCalculatorTest {

    private HaversineDistanceCalculator calculator;

    private static final double DELTA = 0.001;

    @BeforeEach
    void setUp() {
        calculator = new HaversineDistanceCalculator();
    }

    /**
     * Vérifie le calcul de distance entre Paris et Lyon.
     * Distance approximative : ~390 km.
     */
    @Test
    void calculate_withParisAndLyon_shouldReturnApproximateDistance() {
        GeoPosition paris = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();
        GeoPosition lyon = GeoPosition.builder()
                .latitude(45.7640)
                .longitude(4.8357)
                .build();

        double distance = calculator.calculate(paris, lyon);

        // Distance Paris-Lyon ≈ 390 km = 390 000 m
        assertTrue(distance > 380_000 && distance < 400_000,
                "Distance Paris-Lyon should be around 390 km, but was " + distance + " m");
    }

    /**
     * Vérifie que la distance entre un point et lui-même est de 0.
     */
    @Test
    void calculate_withSamePosition_shouldReturnZero() {
        GeoPosition pos = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();

        double distance = calculator.calculate(pos, pos);

        assertEquals(0.0, distance, DELTA, "Distance from a point to itself should be 0");
    }

    /**
     * Vérifie que les positions nulles retournent {@link Double#MAX_VALUE}.
     */
    @Test
    void calculate_withNullFirstPosition_shouldReturnMaxValue() {
        GeoPosition pos = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();

        double distance = calculator.calculate(null, pos);

        assertEquals(Double.MAX_VALUE, distance, "Distance with null first position should be MAX_VALUE");
    }

    /**
     * Vérifie que les deux positions nulles retournent {@link Double#MAX_VALUE}.
     */
    @Test
    void calculate_withNullSecondPosition_shouldReturnMaxValue() {
        GeoPosition pos = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();

        double distance = calculator.calculate(pos, null);

        assertEquals(Double.MAX_VALUE, distance, "Distance with null second position should be MAX_VALUE");
    }

    /**
     * Vérifie que deux positions nulles retournent {@link Double#MAX_VALUE}.
     */
    @Test
    void calculate_withBothNull_shouldReturnMaxValue() {
        double distance = calculator.calculate(null, null);

        assertEquals(Double.MAX_VALUE, distance, "Distance with both null should be MAX_VALUE");
    }

    /**
     * Vérifie le calcul de distance entre deux points très proches (quelques mètres).
     */
    @Test
    void calculate_withVeryClosePoints_shouldReturnSmallDistance() {
        GeoPosition p1 = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();
        GeoPosition p2 = GeoPosition.builder()
                .latitude(48.8567)
                .longitude(2.3523)
                .build();

        double distance = calculator.calculate(p1, p2);

        // Points très proches : distance < 50 mètres
        assertTrue(distance < 50, "Distance between very close points should be small, but was " + distance + " m");
        assertTrue(distance > 0, "Distance should be positive");
    }

    /**
     * Vérifie la symétrie du calcul de distance : distance(A,B) == distance(B,A).
     */
    @Test
    void calculate_shouldBeSymmetric() {
        GeoPosition paris = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();
        GeoPosition tokyo = GeoPosition.builder()
                .latitude(35.6762)
                .longitude(139.6503)
                .build();

        double distanceParisToTokyo = calculator.calculate(paris, tokyo);
        double distanceTokyoToParis = calculator.calculate(tokyo, paris);

        assertEquals(distanceParisToTokyo, distanceTokyoToParis, DELTA,
                "Distance should be symmetric");
    }

    /**
     * Vérifie que des coordonnées nulles partiellement positionnées retournent MAX_VALUE.
     */
    @Test
    void calculate_withNullLatitude_shouldReturnMaxValue() {
        GeoPosition incomplete = GeoPosition.builder()
                .latitude(null)
                .longitude(2.3522)
                .build();
        GeoPosition complete = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();

        double distance = calculator.calculate(incomplete, complete);

        assertEquals(Double.MAX_VALUE, distance, "Distance with null latitude should be MAX_VALUE");
    }

    /**
     * Vérifie que des coordonnées nulles partiellement positionnées retournent MAX_VALUE.
     */
    @Test
    void calculate_withNullLongitude_shouldReturnMaxValue() {
        GeoPosition incomplete = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(null)
                .build();
        GeoPosition complete = GeoPosition.builder()
                .latitude(48.8566)
                .longitude(2.3522)
                .build();

        double distance = calculator.calculate(incomplete, complete);

        assertEquals(Double.MAX_VALUE, distance, "Distance with null longitude should be MAX_VALUE");
    }
}