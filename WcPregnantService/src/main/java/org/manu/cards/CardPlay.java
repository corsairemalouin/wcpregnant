package org.manu.cards;

import java.util.List;
import java.util.concurrent.Callable;

import org.manu.cards.bataille.BataillePlay;
import org.manu.cards.bo.CardDeck;
import org.manu.cards.bo.Player;
import org.manu.cards.bridge.BridgePlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract public class CardPlay implements Callable<CardPlay> {
    public static final String K_GAME_NAME_BATAILLE = "BATAILLE";
    public static final String K_GAME_NAME_BRIDGE = "BRIDGE";
    protected static final Logger log = LoggerFactory.getLogger(CardGameLauncher.class);
    private Long partyNumber;
    protected List<Player> playerList;
    protected Long roundNumber = 0L;

    protected CardPlay(Long partyNumber) {
        this.partyNumber = partyNumber;
    }

    public static CardPlay of(String gameName, Long partyNumber) {
        if (gameName.equals(BataillePlay.K_GAME_NAME_BATAILLE))
            return new BataillePlay(partyNumber);
        if (gameName.equals(BataillePlay.K_GAME_NAME_BRIDGE))
            return new BridgePlay(partyNumber);
        throw new IllegalStateException("unkown game");
    }

    @Override
    public CardPlay call() throws Exception {
        return this.launchPlay();
    }

    protected CardPlay launchPlay() {
        initializeGame();
        while (!isExistingWinner()) {
            playRound();
        }
        registerGameResult();
        return this;
    }

    protected void initializeGame() {
        CardDeck deck = new CardDeck();
        deck.shuffle();
        deck.distribute(playerList);
    }

    protected Long partyNumber() {
        return partyNumber;
    }

    protected void registerGameResult() {
        log.info("Game {} win by {} in {} round", this.partyNumber(), getWinner().name(), roundNumber);
        Statistics.registerGameWin(getName(), getWinner().name(), roundNumber);
    }

    protected abstract boolean isExistingWinner();

    protected abstract Player playRound();

    protected abstract String getName();

    protected abstract Player getWinner();

}
