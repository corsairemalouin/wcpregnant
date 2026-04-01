package org.manu.learning.virtual2;

import static org.manu.learning.virtual.Utils.sleepFor;

import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record FlightQuery(String from, String to) {

    private static final Logger log = LoggerFactory.getLogger(FlightQuery.class);
    private static Random random = new Random();

    Flight readFromAlphaAirlines() {
        if (from().equals("New York")) {
            throw null;
        }
        sleepFor(random.nextInt(80, 100), ChronoUnit.MILLIS);
        if (from().equals("Atlanta") || to.equals("Atlanta")) {
            return new Flight(from, to, random.nextInt(30, 50), "Alpha Airlines");
        } else {
            return new Flight(from, to, random.nextInt(70, 120), "Alpha Airlines");
        }
    }

    Flight readFromPlanetAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight(from, to, random.nextInt(60, 90), "Planet Airlines");
    }

    Flight readFromGlobalAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight(from, to, random.nextInt(60, 90), "Global Airlines");
    }

}