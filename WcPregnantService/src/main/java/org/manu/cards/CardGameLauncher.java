package org.manu.cards;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

import org.manu.cards.bataille.BataillePlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CardGameLauncher {
    protected static final Logger log = LoggerFactory.getLogger(CardGameLauncher.class);

    public static void main(String[] args) {

        String gameToPlay = BataillePlay.K_GAME_NAME_BATAILLE;

        var game = new CardGameLauncher();
        var start = Instant.now();
        game.play(gameToPlay, 1_000L);
        var end = Instant.now();
        log.info(" stats: \n{}\nexecution in {}", Statistics.getStats(gameToPlay),
                Duration.between(start, end).toMillis());
    }

    public void play(String gameName, Long count) {
        // try (var scope = new BatailleScope()) {
        try (var scope = new StructuredTaskScope<BataillePlay, >()) {
            Stream.iterate(1, i -> i + 1).limit(count).forEach(i -> this.launchPlay(gameName, scope, Long.valueOf(i)));
            scope.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private StructuredTaskScope.Subtask<CardGameLauncher> launchPlay(String gameName, StructuredTaskScope scope,
            Long partyNumber) {
        return scope.fork(CardPlay.of(gameName, partyNumber));
    }

}
