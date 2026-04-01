package org.manu.learning.flights.flights.directFlights;

import static org.manu.learning.virtual.Utils.sleepFor;

import java.time.temporal.ChronoUnit;
import java.util.Random;

import org.manu.learning.flights.flights.Travel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record FlightQuery(String from, String to) {
    private static final Logger log = LoggerFactory.getLogger(FlightQuery.class);

    private static Random random = new Random();

    public Travel readFromAlphaAirlines() {
        // if (from().equals("New York")) {
        // throw null;
        // }
        sleepFor(random.nextInt(80, 100), ChronoUnit.MILLIS);
        Flight result;
        if (from().equals("Atlanta") || to.equals("Atlanta")) {
            result = new Flight(from, to, random.nextInt(30, 50), "Alpha Airlines");
        } else {
            result = new Flight(from, to, random.nextInt(70, 120), "Alpha Airlines");
        }
        log.debug(String.valueOf(result));
        return result;
    }

    public Travel readFromPlanetAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        var result = new Flight(from, to, random.nextInt(60, 90), "Planet Airlines");
        log.debug(String.valueOf(result));
        return result;
    }

    public Travel readFromGlobalAirlines() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        var result = new Flight(from, to, random.nextInt(60, 90), "Global Airlines");
        log.debug(String.valueOf(result));
        return result;
    }

}