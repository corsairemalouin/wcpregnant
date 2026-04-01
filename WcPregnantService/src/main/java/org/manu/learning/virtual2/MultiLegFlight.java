package org.manu.learning.virtual2;

import java.util.concurrent.StructuredTaskScope;

public record MultiLegFlight(String from, String via, String to, int price, String airline) {
    public static MultiLegFlight of(Flight f1, Flight f2) {
        return new MultiLegFlight(f1.from(), f1.to(), f2.to(), f1.price() + f2.price(), f1.airline());
    }

    public static MultiLegFlight readMultiLegFlight(FlightQuery flightQuery1, FlightQuery flightQuery2) {
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
