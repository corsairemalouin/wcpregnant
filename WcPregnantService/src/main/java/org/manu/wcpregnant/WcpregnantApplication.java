package org.manu.wcpregnant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cglib.core.internal.Function;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WcpregnantApplication {

    public static void main(String[] args) {
        SpringApplication.run(WcpregnantApplication.class, args);
    }

    @Bean
    public Function<String, String> enrichLogMessage() {
        return value -> "[%s] - %s".formatted("Baeldung", value);
    }

}
