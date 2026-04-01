package org.manu.cards.bo;

import java.util.Optional;

public record Card(CardColor color, int value) implements Comparable<Card> {
    public static int compare(Optional<Card> card1, Optional<Card> card2) {
        if (card1.isPresent() && card1.isPresent()) {
            return Integer.compare(card1.get().value(), card2.get().value());
        } else {
            return 0;
        }
    }

    public int compareTo(Card other) {
        return Integer.compare(this.value(), other.value());
    }
}
