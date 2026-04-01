package org.manu.learning.virtual2.flights;

import java.util.concurrent.StructuredTaskScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record MultiLegFlight(String from, String via, String to, int price, String airline) implements Travel {

    private static final Logger log = LoggerFactory.getLogger(MultiLegFlight.class);
    public static MultiLegFlight of(Travel f1, Travel f2) {
        var result = new MultiLegFlight(f1.from(), f1.to(), f2.to(), f1.price() + f2.price(), f1.airline());
        log.info(String.valueOf(result));
        return result;
    }

    public static Travel readMultiLegFlight(TravelQuery flightQuery1, TravelQuery flightQuery2) {
        try (var scope = new MultiLegFlightScope()) {
            scope.fork(() -> readFromAlphaAirlines(flightQuery1, flightQuery2));
            scope.fork(() -> readFromPlanetAirlines(flightQuery1, flightQuery2));
            scope.fork(() -> readFromGlobalAirlines(flightQuery1, flightQuery2));

            scope.join();

            return scope.bestTravel();

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static MultiLegFlight readFromAlphaAirlines(TravelQuery flightQuery1, TravelQuery flightQuery2) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var t1 = scope.fork(flightQuery1::readFromAlphaAirlines);
            var t2 = scope.fork(flightQuery2::readFromAlphaAirlines);

            scope.join();
            return of(t1.get(), t2.get());
        } catch (InterruptedException | IllegalStateException e) {
            return null;
        }
    }

    private static MultiLegFlight readFromPlanetAirlines(TravelQuery flightQuery1, TravelQuery flightQuery2) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var t1 = scope.fork(flightQuery1::readFromAlphaAirlines);
            var t2 = scope.fork(flightQuery2::readFromAlphaAirlines);

            scope.join();
            return of(t1.get(), t2.get());
        } catch (InterruptedException | IllegalStateException e) {
            return null;
        }
    }

    private static MultiLegFlight readFromGlobalAirlines(TravelQuery flightQuery1, TravelQuery flightQuery2) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var t1 = scope.fork(flightQuery1::readFromAlphaAirlines);
            var t2 = scope.fork(flightQuery2::readFromAlphaAirlines);

            scope.join();
            return of(t1.get(), t2.get());
        } catch (InterruptedException | IllegalStateException e) {
            return null;
        }
    }
}
