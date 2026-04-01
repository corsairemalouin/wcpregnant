package org.manu.learning.java17;

import org.manu.wcpregnant.bdd.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class MyMain {
    private static final Logger log = LoggerFactory.getLogger(MyMain.class);
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy MM dd E");

    static UserRepository repo;

    // first monday of the month
    static void exo6() {
        LocalDate date = LocalDate.of(2027, Month.JULY, 10);
        TemporalAdjuster monday = TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY);
        log.info("{}", date.with(monday).format(formatter));
        // System.out.printf("%s", date.with(TemporalAdjusters.DayOfWeek.MONDAY));
        System.out.printf("%s\n", date.with(TemporalAdjusters.firstInMonth(DayOfWeek.MONDAY)));
    }

    static void genericMethod(List<?> input) {

    }

    static void exo17() {
        List<Double> genericType1 = new ArrayList<>();
        genericMethod(genericType1);
        List<String> genericType2 = new ArrayList<>();
        genericMethod(genericType2);
        List<Float> genericType3 = new ArrayList<>();
        genericMethod(genericType3);
    }

    static void exo20() {
        List<String> tags = List.of("#morning", "#coffee", "#tea");

        try (Writer w = new FileWriter("tags.txt")) {
            // tags.forEach(w::write);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        exo6();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }
}