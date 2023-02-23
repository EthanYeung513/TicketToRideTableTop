package games.loveletter.actions;

import core.AbstractGameState;
import core.interfaces.IPrintable;
import games.loveletter.cards.LoveLetterCard;

/**
 * The Countess needs to be discarded in case the player also hold a King or a Prince card.
 * Despite its high value, the Countess has no other effect.
 */
public class CountessAction extends PlayCard implements IPrintable {

    public CountessAction(int playerID, LoveLetterCard.CardType cardType) {
        super(LoveLetterCard.CardType.Countess, playerID, -1, null, cardType);
    }

    @Override
    public String toString(){
        if (forcedCountessCardType == null) return "Countess (no effect)";
        return "Countess (auto discard with " + forcedCountessCardType + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this);
    }

    @Override
    public CountessAction copy() {
        return new CountessAction(playerID, forcedCountessCardType);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CountessAction && super.equals(o);
    }
}
