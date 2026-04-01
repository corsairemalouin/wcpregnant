package org.manu.learning.flights.flights;

import org.manu.learning.flights.flights.directFlights.Flight;
import org.manu.learning.flights.flights.directFlights.FlightQuery;
import org.manu.learning.flights.flights.directFlights.TravelScope;
import org.manu.learning.flights.flights.multilegflights.MultiLegFlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Travel {
    static final Logger log = LoggerFactory.getLogger(Travel.class);

    int price();

    String from();

    String to();

    String airline();

    static Travel readTravel(FlightQuery flightQuery0, FlightQuery flightQuery1, FlightQuery flightQuery2) {

        try (var scope = new TravelScope()) {
            var t1 = scope.fork(() -> Flight.readFlight(flightQuery0));
            var t2 = scope.fork(() -> MultiLegFlight.readMultiLegFlight(flightQuery1, flightQuery2));

            scope.join();

            var bestTravel = scope.bestTravel();
            return bestTravel;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
