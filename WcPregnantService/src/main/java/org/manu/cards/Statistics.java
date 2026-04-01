package org.manu.cards;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Statistics {

    private record StatRecord(String game, String winner, Long roundCount) {

    }

    private static List<StatRecord> records = new LinkedList<>();

    public static synchronized void registerGameWin(String game, String winner, Long roundCount) {
        records.add(new StatRecord(game, winner, roundCount));
    }

    public record UserStats(String winner, Long winNumber, Float roundAverage, Long maxRound) {

        public static Stream<UserStats> of(String winner, List<StatRecord> values) {
            Long victories = values.stream().count();
            Long totalRound = values.stream().collect(Collectors.summingLong(StatRecord::roundCount));
            Long maxRound = values.stream().max(Comparator.comparingLong(StatRecord::roundCount)).orElseThrow()
                    .roundCount();
            UserStats data = new UserStats(winner, victories, Float.valueOf(totalRound) / Float.valueOf(victories),
                    maxRound);
            return Stream.of(data);
        }

    }

    public static List<UserStats> getStats(String name) {
        return records.stream().filter(r -> matchGame(name, r)).collect(Collectors.groupingBy(StatRecord::winner))
                .entrySet().stream().sorted(Comparator.comparing(e -> e.getKey()))
                .flatMap(e -> UserStats.of(e.getKey(), e.getValue())).collect(Collectors.toList());
    }

    private static boolean matchGame(String name, StatRecord record) {
        return name.equals(record.game);
    }

}
