package org.manu.learning.flights.flights.multilegflights;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.StructuredTaskScope;

import org.manu.learning.flights.flights.FlightException;
import org.manu.learning.flights.flights.Travel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiLegFlightScope extends StructuredTaskScope<MultiLegFlight> {
    private static final Logger log = LoggerFactory.getLogger(MultiLegFlightScope.class);
    private volatile Collection<Travel> flights = new ConcurrentLinkedDeque<>();
    private volatile Collection<Throwable> exceptions = new ConcurrentLinkedDeque<>();

    @Override
    protected void handleComplete(Subtask<? extends MultiLegFlight> subtask) {
        switch (subtask.state()) {
        case SUCCESS -> flights.add(subtask.get());
        case FAILED -> exceptions.add(subtask.exception());
        case UNAVAILABLE -> throw new IllegalStateException("unexpectted state");
        }
    }

    public Travel bestTravel() {
        return flights.stream().peek(f -> log.debug(f.toString())).min(Comparator.comparing(Travel::price))
                .orElseThrow(this::exceptions);
    }

    private FlightException exceptions() {
        FlightException exception = new FlightException();
        this.exceptions.forEach(exception::addSuppressed);
        return exception;
    }
}
