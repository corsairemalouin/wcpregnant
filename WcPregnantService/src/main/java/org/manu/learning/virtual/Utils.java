package org.manu.learning.virtual;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class Utils {
    private static Random random = new Random();

    public static void sleepFor(int amount, ChronoUnit unit) {
        try {
            Thread.sleep(Duration.of(amount, unit));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    static Flight1 readFromAlphaAirlines(String from, String to) {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight1(from, to, random.nextInt(60, 90), "Alpha Airlines");
    }

    static Flight1 readFromPlanetAirlines(String from, String to) {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight1(from, to, random.nextInt(60, 90), "Planet Airlines");
    }

    static Flight1 readFromGlobalAirlines(String from, String to) {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        return new Flight1(from, to, random.nextInt(60, 90), "Global Airlines");
    }

}
