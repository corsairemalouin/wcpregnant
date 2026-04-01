package org.manu.cards.bo;

import java.util.ArrayList;
import java.util.List;

public class Hand {
    private List<Card> cards = new ArrayList<>();

    public void addCard(Card card) {
        cards.addLast(card);
    }

    public Card removeFirst() {
        return cards.removeFirst();
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public List<Card> getCards() {
        return List.copyOf(cards);
    }
}
