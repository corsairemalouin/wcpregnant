package org.manu.cards.bataille;

import java.util.List;
import java.util.Optional;

import org.manu.cards.CardGameLauncher;
import org.manu.cards.CardPlay;
import org.manu.cards.bo.Card;
import org.manu.cards.bo.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BataillePlay extends CardPlay {
    protected static final Logger log = LoggerFactory.getLogger(CardGameLauncher.class);
    private static final Long K_MAX_ROUND = 25_000L;
    private volatile Player player1;
    private volatile Player player2;
    private volatile Player playerPAT;

    public BataillePlay(Long partyNumber) {
        super(partyNumber);
        player1 = Player.of("player 1");
        player2 = Player.of("player 2");
        playerPAT = Player.of("PAT");
        playerList = List.of(player1, player2);
    }

    protected String getName() {
        return K_GAME_NAME_BATAILLE;
    }

    // return round winner
    protected Player playRound() {
        roundNumber++;

        Player winner = playerPAT;
        Optional<Card> card1 = player1.playFirstCard();
        Optional<Card> card2 = player2.playFirstCard();
        int delta = Card.compare(card1, card2);
        if (delta == 0) {
            // bataille !
            log.debug("bataille !");
            Optional<Card> card12 = player1.playFirstCard();
            Optional<Card> card22 = player2.playFirstCard();
            if (!isExistingWinner()) {
                winner = playRound();
                winner.addCard(card12);
                winner.addCard(card22);
            }
        } else {
            winner = (delta > 0) ? player1 : player2;
        }
        winner.addCard(card1);
        winner.addCard(card2);
        log.debug("round {} win by {} having {} cards", roundNumber, winner.name(), winner.handSize());
        return winner;
    }

    protected boolean isExistingWinner() {
        return (roundNumber > K_MAX_ROUND) || (player1.handSize() < 1 || player2.handSize() < 1);
    }

    public Player getWinner() {
        if (player1.handEmpty())
            return player2;
        else if (player2.handEmpty())
            return player1;
        else
            return playerPAT;
    }

}
