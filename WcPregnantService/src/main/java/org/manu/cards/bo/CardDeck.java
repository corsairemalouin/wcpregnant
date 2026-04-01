package org.manu.cards.bo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

public class CardDeck {
    private List<Card> cards;

    public CardDeck() {
        cards = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 13; j++) {
                cards.add(new Card(CardColor.valueOf(String.valueOf(i)), j));
            }
        }
    }

    public void distribute(List<Player> playerList) {
        Lists.partition(cards, playerList.size()).stream().forEach(c -> affect(c, playerList));
    }

    private void affect(List<Card> cards, List<Player> playerList) {
        if (cards.size() != playerList.size())
            throw new IllegalStateException("unexpected card count");
        var myCards = new ArrayList<Card>();
        myCards.addAll(cards);
        playerList.stream().forEach(player -> player.addCard(Optional.ofNullable(myCards.removeFirst())));
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
}
