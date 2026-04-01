package org.manu.cards.bridge;

import java.util.List;
import java.util.stream.Collectors;

import org.manu.cards.CardPlay;
import org.manu.cards.bo.Card;
import org.manu.cards.bo.CardColor;
import org.manu.cards.bo.Player;

public class BridgePlay extends CardPlay {
    private volatile Player playerA1;
    private volatile Player playerB1;
    private volatile Player playerA2;
    private volatile Player playerB2;
    private volatile int lastGameWinner;
    List<Pli> plis;

    public BridgePlay(Long partyNumber) {
        super(partyNumber);
        playerA1 = Player.of("player A.1");
        playerB1 = Player.of("player B.1");
        playerA1 = Player.of("player A.2");
        playerB2 = Player.of("player B.2");
        List.of(playerA1, playerB1, playerA2, playerB2);
        lastGameWinner = 0;
    }

    @Override
    protected boolean isExistingWinner() {
        return playerA1.handEmpty();
    }

    @Override
    protected Player playRound() {
        Pli pli = new Pli();
        for (int i = 0; i < 4; i++) {
            lastGameWinner = calculateBestCardToPlay(i + lastGameWinner % 4, pli);
        }
        plis.add(pli);
        return null;
    }

    private int calculateBestCardToPlay(int playerIndex, Pli pli) {
        CardColor color = pli.getColor();
        Player currentPlayer = playerList.get(playerIndex);
        List<Card> potentielCards = currentPlayer.getCards(color);
        // pli.[playerIndex] = bestCard(potentielCards, pli);
        return 0;

    }

    private Card bestCard(List<Card> potentielCards, Pli pli) {
        return null;
    }

    @Override
    protected String getName() {
        return K_GAME_NAME_BRIDGE;
    }

    @Override
    protected Player getWinner() {
        Integer scoreA = calculateScore(playerA1, playerA2);
        Integer scoreB = calculateScore(playerB1, playerB2);
        return (scoreA > scoreB) ? playerA1 : playerB1;
    }

    private Integer calculateScore(Player player1, Player player2) {
        int score1 = player1.hand().getCards().stream().collect(Collectors.summingInt(Card::value));
        int score2 = player2.hand().getCards().stream().collect(Collectors.summingInt(Card::value));
        return score1 + score2;
    }
}
