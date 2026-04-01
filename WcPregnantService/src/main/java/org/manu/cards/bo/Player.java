package org.manu.cards.bo;

import java.util.List;
import java.util.Optional;

public record Player(String name, Hand hand) {

    public static Player of(String name) {
        return new Player(name, new Hand());
    }

    public void addCard(Optional<Card> card) {
        if (card.isPresent())
            hand.addCard(card.get());
    }

    public Optional<Card> playFirstCard() {
        return (handEmpty()) ? Optional.empty() : Optional.ofNullable(hand.removeFirst());
    }

    public int handSize() {
        return hand.size();
    }

    public boolean handEmpty() {
        return hand.isEmpty();
    }

    public List<Card> getCards(CardColor color) {
        if (color == CardColor.EMPTY)
            return List.copyOf(hand.getCards());
        return hand.getCards().stream().filter(c -> c.color() == color).toList();
    }
}
