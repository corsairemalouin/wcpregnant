package org.manu.learning.virtual2.flights;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flight(String from, String to, int price, String airline) implements Travel {

    private static final Logger log = LoggerFactory.getLogger(Flight.class);

    public static Travel readFlight(TravelQuery query) {
        try (var scope = new TravelScope()) {
            scope.fork(query::readFromAlphaAirlines);
            scope.fork(query::readFromPlanetAirlines);
            scope.fork(query::readFromGlobalAirlines);

            scope.join();

            return scope.bestTravel();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
