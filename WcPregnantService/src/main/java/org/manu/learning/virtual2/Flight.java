package org.manu.learning.virtual2;

import java.time.Duration;
import java.time.Instant;

import org.manu.learning.virtual.Flight1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Flight(String from, String to, int price, String airline) {

    private static final Logger log = LoggerFactory.getLogger(Flight1.class);

    public static void main(String[] args) {
        String from = "New York";
        String via = "Atlanta";
        String to = "San Fransisco";

        var start = Instant.now();

        var flightQuery1 = new FlightQuery(from, via);
        var flightQuery2 = new FlightQuery(via, to);

        var end = Instant.now();

        MultiLegFlight travel = MultiLegFlight.readMultiLegFlight(flightQuery1, flightQuery2);

        log.info(" Flight is : {} in {}", travel, Duration.between(start, end).toMillis());

    }

}
