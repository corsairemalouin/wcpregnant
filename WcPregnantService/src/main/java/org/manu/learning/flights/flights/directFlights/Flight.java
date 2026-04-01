package org.manu.learning.flights.flights.directFlights;

import org.manu.learning.flights.flights.Travel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flight(String from, String to, int price, String airline) implements Travel {

    private static final Logger log = LoggerFactory.getLogger(Flight.class);

    public static Travel readFlight(FlightQuery query) {
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
