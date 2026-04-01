package org.manu.learning.test;

import org.apache.kafka.common.protocol.types.Field;
import org.apache.logging.log4j.util.PropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

public class Exercise {
    private static final Logger log = LoggerFactory.getLogger(Exercise.class);


    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        exo9();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }

    //You must create a Function that searches a string of characters for a specific word.
    // You then create a BiFunction that takes the string and the searched word.
    // How can you make it a Function that takes only the string?
    private static void exo9() {
        BiFunction<String, String, String> searcher = (String phrase,String keyword) -> {
          return phrase.substring(phrase.indexOf(keyword),keyword.length());
        };
        Function<String,String> func = (String s) -> {
            return searcher.apply(s,"mykeyword");
        };

        /// You must create a Comparator<Person> using a Function<Person, String> called byName. How can you call the comparing() method?

        Function<Person, String> byName = (Person p) -> {
            return p.name();
        };

        Person toCompare = new Person("B");

        Comparator.<Person, String>comparing(byName);

        List.of(new Person("A"), new Person("B")).stream().filter(p-> 0==myComparato(p,toCompare)).findFirst();
    }

    private static int myComparato(Person p, Person toCompare) {
        return 0;
    }


    private static void exo8() {
        Holder holder = new ExtendedHolder();
        holder.addObject("one");
    }

    private static void exo1() {
        List<Double> list =List.of(3D, 5D,Double.NaN, 27D, 15D,Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,89D);

        // calcult sum list
        double sum = 0.0;
        for (double d: list) {
            if (Double.isFinite(d)) {
                sum += d;
            }
        }
        log.info("sum = {}", sum);
    }
    //  have an implementation of Holder where the convert()
    //  method returns the length of the held String as an int. How can you call this method?
    static void exo7() {
        int length = new Holder7() {
            @Override
            public Integer convert(String s) {
                return Integer.valueOf(s.length());
            }
        }.convert("one");

        log.info("length = {}", length);
    }
}
