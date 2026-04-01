package org.manu.learning.analyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Builder;

public class ReadLinesAndValidate {
    private static final Logger log = LoggerFactory.getLogger(ReadLinesAndValidate.class);

    record CsvLine(String[] elt) {

        static CsvLine of(String s) {
            return new CsvLine(s.split(";"));
        }

        public boolean isNotAComment() {
            return !elt[0].startsWith("#");
        }

        public Revenue getPort() {
            return Revenue.of(elt[11]);
        }

        public String getHost() {
            return elt[8];
        }

        public String gelLogin() {
            return elt[6];
        }
    }

    record Revenue(Integer value) implements Comparable<Revenue> {

        public static Revenue of(String s) {
            return new Revenue(Integer.valueOf(s));
        }

        @Override
        public int compareTo(Revenue o) {
            return this.value.compareTo(o.value);
        }
    }

    // Name;Id;Parent;NodeType;Icon;Panel;Username;Password;Hostname;Protocol;PuttySession;Port
    // 16:02:49.437 [main] INFO org.manu.analyzer.ReadLinesAndValidate --
    // AET;AET;root;Container;Linux;Antai;;;;SSH2;Default Settings;22
    @Builder
    public record Data(String Name, String Id, String Parent, String NodeType, String Icon, String Panel,
            MyLogin Username, String Password, String Hostname, String Protocol, String PuttySession, Revenue Port) {

        public static final Comparator<? super Data> comparingByPort = Comparator.comparing(Data::Port);

        public static Stream<Data> of(CsvLine line) {
            try {
                return Stream.of(Data.builder().Name(line.elt[0]).Id(line.elt[1]).Parent(line.elt[2])
                        .NodeType(line.elt[3]).Icon(line.elt[4]).Panel(line.elt[5])
                        .Username(MyLogin.of(line.gelLogin())).Password(line.elt[7]).Hostname(line.getHost())
                        .Protocol(line.elt[9]).PuttySession(line.elt[10]).Port(line.getPort()).build());
            } catch (Exception e) {
                return Stream.empty();
            }
        }

        public static Collector<? super Data, ?, TotalRevenue> summingRevenues() {
            return Collectors.collectingAndThen(Collectors.summingDouble(data -> data.Port.value), TotalRevenue::new);
        }
    }

    public record MyLogin(String login) {

        public static MyLogin of(String s) {
            return new MyLogin(s);
        }
    }

    public record TotalRevenue(Double value) implements Comparable<TotalRevenue> {
        TotalRevenue of(Double v) {
            return new TotalRevenue(v);
        }

        public int compareTo(TotalRevenue o) {
            return this.value.compareTo(o.value);
        }

    }

    record LoginTotalRevenue(MyLogin login, TotalRevenue revenue) implements Comparable<LoginTotalRevenue> {

        public LoginTotalRevenue(Map.Entry<MyLogin, TotalRevenue> entry) {
            this(entry.getKey(), new TotalRevenue(Double.valueOf(entry.getValue().value)));
        }

        @Override
        public int compareTo(LoginTotalRevenue o) {
            return this.revenue.compareTo(o.revenue);
        }
    }

    static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        exo6();
        long endTime = System.currentTimeMillis();
        log.info("Total elapsed time in execution of method callMethod() is :" + (endTime - startTime));

    }

    public static void exo6() {

        var lines = readLines(Path.of("src/main/resources/MyCsvFile.csv"));
        log.info("number of lines: {}", lines.size());

        // lines.stream().limit(5).forEach(log::info);

        Predicate<? super String> filterLines = (t -> {
            return !t.isEmpty();
        });
        // List<Data> datas =
        // lines.stream().peek(log::info).map(Data::of).toList();
        List<Data> datas = lines.stream().map(CsvLine::of).filter(l -> l.isNotAComment()).flatMap(Data::of).toList();
        log.info("#data: {} ", datas.size());

        // max port
        Data maxPortValue = datas.stream().max(Data.comparingByPort).orElseThrow();
        log.info("#max port: {} ", maxPortValue);

        // lines with maximum same login
        // plutot la some des port avec le meme login

        Map<MyLogin, TotalRevenue> totalPortPerLogin = datas.stream()
                .collect(Collectors.groupingBy(Data::Username, Data.summingRevenues()));

        log.info("#sum port per login: {} ", totalPortPerLogin);

        // recuperer le max
        LoginTotalRevenue maxData = totalPortPerLogin.entrySet().stream().map(LoginTotalRevenue::new)
                .max(LoginTotalRevenue::compareTo).orElseThrow();
        log.info("#max sum port: {} ", maxData);
    }

    /**
     * open a file and read it line by line
     *
     * @param csvFile
     * @return
     */
    private static List<String> readLines(Path csvFile) {
        try (var is = Files.newInputStream(csvFile)) {
            var isr = new InputStreamReader(is);
            var br = new BufferedReader(isr);
            var stream = br.lines();
            return stream.toList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
