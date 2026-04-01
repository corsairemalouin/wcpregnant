package org.manu.learning.virtual;

import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public record Weather(String wether, String server) {

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        main2();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }

    private static void main1() {
        var weather = Weather.readWeather();
        log.info("weather = {}", weather);
    }

    private static void main2() {
        var weather = Weather.readWeather2();
        log.info("weather = {}", weather);
    }

    private static final Logger log = LoggerFactory.getLogger(Weather.class);

    private static Random random = new Random();

    private enum WeatherForcast {
        Sunny, Rainy, Cloudy
    }

    static Weather readWeather() {
        try (var scope = new StructuredTaskScope<Weather>()) {
            var f1 = scope.fork(Weather::readFromInternationalLWF);
            var f2 = scope.fork(Weather::readFromGlobalLWF);
            var f3 = scope.fork(Weather::readFromPlanetEarthForcast);

            scope.join();

            log.info("F1: {}", f1.state());
            if (f1.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f1.get());
            }

            log.info("F2: {}", f2.state());
            if (f2.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f2.get());
            }

            log.info("F3: {}", f3.state());
            if (f3.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f3.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return readFromInternationalLWF();
    }

    static Weather readWeather2() {
        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<Weather>()) {
            var f1 = scope.fork(Weather::readFromInternationalLWF);
            var f2 = scope.fork(Weather::readFromGlobalLWF);
            var f3 = scope.fork(Weather::readFromPlanetEarthForcast);

            scope.join();

            log.info("F1: {}", f1.state());
            if (f1.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f1.get());
            }

            log.info("F2: {}", f2.state());
            if (f2.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f2.get());
            }

            log.info("F3: {}", f3.state());
            if (f3.state() == StructuredTaskScope.Subtask.State.SUCCESS) {
                log.info("{}", f3.get());
            }

            return scope.result();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

    }

    private static Weather readFromInternationalLWF() {
        Utils.sleepFor(random.nextInt(70, 120), ChronoUnit.MILLIS);
        WeatherForcast.values()[random.nextInt(0, 2)].name();
        return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(), "International Weather Forcast");

    }

    private static Weather readFromGlobalLWF() {
        throw null;
        // sleepFor(random.nextInt(80, 110), ChronoUnit.MILLIS);
        // return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(),
        // "Global Weather Forcast");

    }

    private static Weather readFromPlanetEarthForcast() {
        Utils.sleepFor(random.nextInt(80, 110), ChronoUnit.MILLIS);
        return new Weather(WeatherForcast.values()[random.nextInt(0, 3)].name(), "Planet Earth Weather Forcast");

    }

}
