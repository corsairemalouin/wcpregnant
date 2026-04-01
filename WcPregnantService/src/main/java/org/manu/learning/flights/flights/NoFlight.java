package org.manu.learning.flights.flights;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoFlight implements Travel {
    private static final Logger log = LoggerFactory.getLogger(NoFlight.class);

    @Override
    public int price() {
        return 0;
    }

    @Override
    public String from() {
        return "";
    }

    @Override
    public String to() {
        return "";
    }

    @Override
    public String airline() {
        return "";
    }
}
