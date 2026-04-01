package org.manu.learning.virtual2.flights;

import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        String from = "New York";
        String via = "Atlanta";
        String to = "San Fransisco";

        var flightQuery0 = new TravelQuery(from, to);
        var flightQuery1 = new TravelQuery(from, via);
        var flightQuery2 = new TravelQuery(via, to);

        var start = Instant.now();

        try (var scope = new TravelScope()) {
            var t1 = scope.fork(() -> Flight.readFlight(flightQuery0));
            var t2 = scope.fork(() -> MultiLegFlight.readMultiLegFlight(flightQuery1, flightQuery2));

            scope.join();

            var bestTravel = scope.bestTravel();
            var end = Instant.now();
            log.info(" Flight is : {} in {}", bestTravel, Duration.between(start, end).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

}
