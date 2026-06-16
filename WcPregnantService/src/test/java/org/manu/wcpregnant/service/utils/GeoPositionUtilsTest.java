package org.manu.wcpregnant.service.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.manu.wcpregnant.web.dto.GeoPosition;

class GeoPositionUtilsTest {

    @Test
    void samePointShouldReturnZero() {
        GeoPosition p = GeoPosition.builder().latitude(48.8566).longitude(2.3522).build();
        double d = GeoPositionUtils.distance(p, p);
        assertEquals(0.0, d, 1e-6);
    }

    @Test
    void nullPositionsShouldReturnMaxValue() {
        double d = GeoPositionUtils.distance(null, null);
        assertEquals(Double.MAX_VALUE, d);
    }

    @Test
    void knownDistanceBetweenParisAndLondon() {
        GeoPosition paris = GeoPosition.builder().latitude(48.8566).longitude(2.3522).build();
        GeoPosition london = GeoPosition.builder().latitude(51.5074).longitude(-0.1278).build();
        double meters = GeoPositionUtils.distance(paris, london);
        // expected ~343 km
        assertTrue(meters > 300_000 && meters < 380_000, "Distance between Paris and London should be around 343km");
    }
}

