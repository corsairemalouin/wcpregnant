package org.manu.learning.virtual;

import static org.manu.learning.virtual.Utils.sleepFor;

import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record FlightQuery(String from, String to) {

    private static final Logger log = LoggerFactory.getLogger(FlightQuery.class);
    private static Random random = new Random();

    Flight3 readFromAlphaAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight3(from, to, random.nextInt(60, 90), "Alpha Airlines");
    }

    Flight3 readFromPlanetAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight3(from, to, random.nextInt(60, 90), "Planet Airlines");
    }

    Flight3 readFromGlobalAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight3(from, to, random.nextInt(60, 90), "Global Airlines");
    }

}