package org.manu.learning.virtual;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.StructuredTaskScope;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightScope extends StructuredTaskScope<Flight3> {
    private static final Logger log = LoggerFactory.getLogger(Flight1.class);
    private volatile Collection<Flight3> flights = new ConcurrentLinkedDeque<>();
    private volatile Collection<Throwable> exceptions = new ConcurrentLinkedDeque<>();

    @Override
    protected void handleComplete(Subtask<? extends Flight3> subtask) {
        switch (subtask.state()) {
        case SUCCESS -> flights.add(subtask.get());
        case FAILED -> exceptions.add(subtask.exception());
        case UNAVAILABLE -> throw new IllegalStateException("unexpectted state");
        }
    }

    public Flight3 getBestPriceResult() {
        return flights.stream().peek(f -> log.info(f.toString())).min(Comparator.comparing(Flight3::price))
                .orElseThrow(exeception());
    }

    private Supplier<? extends RuntimeException> exeception() {
        Supplier<? extends RuntimeException> exception = null;
        return exception;
    }
}
