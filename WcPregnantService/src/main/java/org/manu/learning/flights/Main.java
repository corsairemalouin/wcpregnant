package org.manu.learning.flights;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;

import org.manu.learning.flights.flights.Travel;
import org.manu.learning.flights.flights.directFlights.FlightQuery;
import org.manu.learning.flights.weather.Weather;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static ScopedValue<String> licenceKey = ScopedValue.newInstance();

    public static void main(String[] args) {
        String from = "New York";
        String via = "Atlanta";
        String to = "San Fransisco";

        var flightQuery0 = new FlightQuery(from, to);
        var flightQuery1 = new FlightQuery(from, via);
        var flightQuery2 = new FlightQuery(via, to);

        var start = Instant.now();

        ScopedValue.runWhere(licenceKey, "KEY_1", () -> {
            Page page = readPage(flightQuery0, flightQuery1, flightQuery2);
            log.info(" Travel is : {} ", page);
        });
        var end = Instant.now();
        log.info(" execution in {}", Duration.between(start, end).toMillis());

    }

    private static Page readPage(FlightQuery flightQuery0, FlightQuery flightQuery1, FlightQuery flightQuery2) {

        try (var scope = new StructuredTaskScope<>()) {
            if (licenceKey.isBound()) {
                log.info(licenceKey.get());
            } else {
                log.error("unbound");
            }
            var t1 = scope.fork(() -> Travel.readTravel(flightQuery0, flightQuery1, flightQuery2));
            var t2 = scope.fork(Weather::readWeather);

            scope.join();

            Travel travel = (Travel) t1.get();
            Weather weather = (Weather) t2.get();
            Page page = new Page(travel, weather);
            return page;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
