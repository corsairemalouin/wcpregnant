package org.manu.learning.lambda;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.manu.learning.lambda.inheritance.Holder;
import org.manu.wcpregnant.client.Person;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Exercices {
    private static final Logger log = LoggerFactory.getLogger(Exercices.class);

    // You have a List of strings of characters.
    // You must put all these strings in upper case.
    // How would you proceed?
    static void test1() {
        List<String> listfString = List.of("chaine1", "french", "ma chaine avec des eSpaces",
                "Ma CHAINE AVEC DesUperCases");
        log.info(String.valueOf(listfString));
        List<String> newList = listfString.stream().map(String::toUpperCase).toList();
        log.info(String.valueOf(newList));
    }

    static void test1Result() {
        List<String> listfString = List.of("chaine1", "french", "ma chaine avec des eSpaces",
                "Ma CHAINE AVEC DesUperCases");
        log.info(String.valueOf(listfString));
        List<String> newList = new LinkedList<>(listfString);
        newList.replaceAll(String::toUpperCase);
        log.info(String.valueOf(newList));
    }

    // You run the code:
    // Holder holder = new Holder(20);
    // In what order are the elements called?
    static void test2() {
        Holder holder = new Holder(20);
    }

    // 10. A method takes a parameter of type Function.
    // Other than a Function, what type of lambda can you pass to this method?
    static void test3(Function f) {

    }

    // 5. What requirement must an abstract method meet?
    public static void main3(String[] args) {
        UnaryOperator f = null;
        test3(f);
    }

    // . You are using the reduce() method to calculate the combined age of people
    // in a group. The following code tries to do that:
    //// assume implementation of a Person class that has a constructor for name
    //// and age, along with appropriate set and get methods
    // List<Person> people = Arrays.asList(new Person("Person 1", 20), new
    // Person("Person 2", 25));
    // int result = people.stream().reduce(0, (currAge, user) -> currAge +
    // user.getAge());
    // log.info(result);
    // This results in a syntax error. How can you change this to make it print out
    // 45?

    static void test4() {
        List<Person> people = Arrays.asList(new Person("Person 1", 20), new Person("Person 2", 25));
        // int result = people.stream().reduce(0, (currAge, user) -> currAge +
        // user.getAge());
        Optional<Integer> result2 = people.stream().map(p -> p.age()).reduce((currAge, user) -> {
            int i = currAge + user;
            return i;
        });
        // int result3 = people.stream().reduce((currAge, user) -> currAge +
        // user.getAge());
        int result4 = people.stream().reduce(0, (currAge, user) -> currAge + user.age(), Integer::sum);

        log.info(String.valueOf(result2));

    }

    static void test42() {
        List<Person> people = Arrays.asList(new Person("Person 1", 20), new Person("Person 2", 25));

        Integer result = people.stream().reduce(0, (n, p) -> n + p.age(), Integer::sum);
        log.info(String.valueOf(result));
    }

    static Collection<Integer> nombresPremiers = new LinkedList<>();

    private static boolean isDiviseur(Integer p, Integer n) {
        return Float.compare((n / p) * p, n) == 0;
    }

    static Predicate<? super Integer> myPredicate = n -> {
        ;
        boolean newOne = nombresPremiers.stream().anyMatch(p -> isDiviseur(p, n));
        if (!newOne) {
            nombresPremiers.add(n);
        }
        return !newOne;
    };

    private static void test5() {
        Stream<Integer> stream = Stream.iterate(2, (i) -> i + 1);
        Collection<Integer> centPremiers = stream.filter(myPredicate).limit(100000).collect(Collectors.toList());
        log.info(String.valueOf(nombresPremiers));
    }

    static Function<? super Integer, ? extends Stream<?>> test51Function = n -> {
        if ((nombresPremiers.parallelStream().anyMatch(p -> isDiviseur(p, n)))) {
            return Stream.empty();
        } else {
            nombresPremiers.add(n);
            return Stream.of(n);
        }
    };

    private static void test51() {
        Stream<Integer> stream = Stream.iterate(2, (i) -> i + 1);
        stream.flatMap(test51Function).limit(100000).forEach(System.out::println);
    }

    // . You have a BiFunction that takes two instances
    // of String as parameters and returns an int.
    // It is written in the following form:
    // (s, word) -> s.indexOf(word).
    //
    // What is the corresponding method reference?
    static BiFunction<String, String, Integer> test6Function = String::indexOf;

    private static void test6() {
        // You have a stream of strings of characters.
        // You map it to the length of the strings
        // and call the average() method on the resulting stream.
        //
        // What do you get as a result?
        Supplier<?> getString = () -> {
            return "ab";
        };
        OptionalDouble x = Stream.of("abc", "dvfr", "frqsfqf", "qsdqfvo", "fq").mapToInt(s -> ((String) s).length())
                .limit(100).average();
        log.info(String.valueOf(x));
    }

    private static void test7() {
        Integer x = Stream.of("abc", "dvfr", "frsfqf", "qsdqfvo", "fq").reduce(0, (s, l) -> s + l.length(),
                Integer::sum);
        log.info(String.valueOf(x));
    }

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        test7();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }
}
