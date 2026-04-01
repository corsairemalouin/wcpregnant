package org.manu.learning.virtual;

import static org.manu.learning.virtual.Utils.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flight1(String from, String to, int price, String airline) {

    private static final Logger log = LoggerFactory.getLogger(Flight1.class);

    public static void main(String[] args) {
        String from = "New York";
        String to = "San Fransisco";

        var start = Instant.now();
        var flight1 = readFromAlphaAirlines(from, to);
        var flight2 = readFromPlanetAirlines(from, to);
        var flight3 = readFromGlobalAirlines(from, to);
        var end = Instant.now();

        log.info("Flight 1 = {}", flight1);
        log.info("Flight 2 = {}", flight2);
        log.info("Flight 3 = {}", flight3);

        Flight1 bestFlignt = Stream.of(flight1, flight2, flight3).min(Comparator.comparing(Flight1::price)).get();
        log.info("Best Flight is : {} in {}", bestFlignt, Duration.between(start, end).toMillis());

    }

}
