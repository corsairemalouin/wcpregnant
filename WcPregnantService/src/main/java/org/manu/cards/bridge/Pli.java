package org.manu.cards.bridge;

import java.util.ArrayList;
import java.util.List;

import org.manu.cards.bo.Card;
import org.manu.cards.bo.CardColor;

public class Pli {
    List<Card> cards = new ArrayList<>();

    public CardColor getColor() {
        if (cards.isEmpty())
            return CardColor.EMPTY;
        return cards.getFirst().color();
    }
}
