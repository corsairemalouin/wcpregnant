package org.manu.learning.flights.weather;

import static org.manu.learning.virtual.Utils.sleepFor;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

import org.manu.learning.flights.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Weather(String wether, String server) {

    private static final Logger log = LoggerFactory.getLogger(Weather.class);
    private static Random random = new Random();

    private enum WeatherForcast {
        Sunny, Rainy, Cloudy
    }

    public Weather {
        if (!Main.licenceKey.isBound() || !Main.licenceKey.get().equals("KEY_1")) {
            throw new IllegalStateException("Incorrect licence");
        }
    }

    public static Weather readWeather() {

        if (Main.licenceKey.isBound()) {
            log.info(Main.licenceKey.get());
        } else {
            log.error("unbound");
        }
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<Weather>()) {
            var f1 = scope.fork(Weather::readFromInternationalLWF);
            var f2 = scope.fork(Weather::readFromGlobalLWF);
            var f3 = scope.fork(Weather::readFromPlanetEarthForcast);

            scope.join();

            log.debug("F1: {}", f1.state());
            if (f1.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.debug("{}", f1.get());
            }

            log.debug("F2: {}", f2.state());
            if (f2.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.debug("{}", f2.get());
            }

            log.debug("F3: {}", f3.state());
            if (f3.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.debug("{}", f3.get());
            }

            return scope.result();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    private static Weather readFromInternationalLWF() {
        sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        WeatherForcast.values()[random.nextInt(0, 2)].name();
        return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(), "International Weather Forcast");

    }

    private static Weather readFromGlobalLWF() {
        // throw null;
        sleepFor(random.nextInt(80, 110), ChronoUnit.MILLIS);
        return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(), "Global Weather Forcast");
    }

    private static Weather readFromPlanetEarthForcast() {
        sleepFor(random.nextInt(80, 110), ChronoUnit.MILLIS);
        return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(), "Planet Earth Weather Forcast");

    }

}
