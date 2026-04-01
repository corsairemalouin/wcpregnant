package org.manu.learning.lambda;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Hello world!");

        testComparators();
    }

    private static void testComparators() {
        User manu = User.builder().name("manu").age(49).build();
        User rim = User.builder().name("rim").age(40).build();
        User robert = User.builder().name("robert").age(42).build();
        User robert2 = User.builder().name("robert").age(41).build();
        var userList = new ArrayList<>(List.of(manu, rim, robert, robert2));

        Function<User, Integer> length = u -> u.name.length();
        Function<User, Integer> age = u -> u.age;

        Comparator<User> userComparator = createComparator(length);
        Comparator<User> ageComparator = createComparator(age);
        Comparator<User> lengtOrElseAgeComparator = userComparator.thenComparing(ageComparator);

        // Function<? super User, ? extends U> myKeyExtractor;
        // Comparator<User> testComparator = Comparator.comparing(myKeyExtractor);

        log.info(String.valueOf(userList));
        userList.sort(userComparator);
        log.info(String.valueOf(userList));
        userList.sort(ageComparator);
        log.info(String.valueOf(userList));
        userList.sort(lengtOrElseAgeComparator);
        log.info(String.valueOf(userList));
    }

    private static Comparator<User> createComparator(Function<User, Integer> compareFunction) {
        return (u1, u2) -> {
            var l1 = compareFunction.apply(u1);
            var l2 = compareFunction.apply(u2);
            return l1.compareTo(l2);
        };
    }
}