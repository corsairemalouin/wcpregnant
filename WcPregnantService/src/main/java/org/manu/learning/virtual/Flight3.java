package org.manu.learning.virtual;

import java.time.Duration;
import java.time.Instant;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flight3(String from, String to, int price, String airline) implements Comparable<Flight3> {

    private static final Logger log = LoggerFactory.getLogger(Flight1.class);
    private static Random random = new Random();

    public static void main(String[] args) {
        String from = "New York";
        String to = "San Fransisco";

        var start = Instant.now();
        var bestFlight = readFlight(from, to);
        var end = Instant.now();

        log.info("Best Flight is : {} in {}", bestFlight, Duration.between(start, end).toMillis());
    }

    private static Flight3 readFlight(String from, String to) {

        FlightQuery query = new FlightQuery(from, to);
        try (var scope = new FlightScope()) {
            var f1 = scope.fork(query::readFromAlphaAirlines);
            var f2 = scope.fork(query::readFromPlanetAirlines);
            var f3 = scope.fork(query::readFromGlobalAirlines);

            scope.join();

            return scope.getBestPriceResult();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(Flight3 o) {
        return this.price - o.price;
    }

}
