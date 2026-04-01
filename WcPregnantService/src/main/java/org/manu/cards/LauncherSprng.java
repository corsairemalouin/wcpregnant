package org.manu.cards;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.manu")
public class LauncherSprng {
    protected static final Logger log = LoggerFactory.getLogger(LauncherSprng.class);

    public static void main(String[] args) {
        SpringApplication.run(LauncherSprng.class, args);
    }

    @Bean
    public String launchTest(@NotNull CardGameLauncher launcher) {
        launcher.play(CardPlay.K_GAME_NAME_BATAILLE, 1L);
        log.info("DONE");
        return "DONE";
    }
}
