package games.loveletter.actions;

import core.AbstractGameState;
import core.components.Deck;
import core.observations.IPrintable;
import games.loveletter.LoveLetterGameState;
import games.loveletter.cards.LoveLetterCard;

import java.util.Objects;


public class KingAction extends DrawCard implements IPrintable {

    private final int opponentID;

    public KingAction(int deckFrom, int deckTo, int fromIndex, int opponentID) {
        super(deckFrom, deckTo, fromIndex);
        this.opponentID = opponentID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        int playerID = gs.getTurnOrder().getCurrentPlayer(gs);
        Deck<LoveLetterCard> playerDeck = llgs.getPlayerHandCards().get(playerID);
        Deck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(opponentID);

        if (((LoveLetterGameState) gs).isNotProtected(opponentID)){
            Deck<LoveLetterCard> tmpDeck = new Deck<>("tmp");
            while (opponentDeck.getSize() > 0)
                tmpDeck.add(opponentDeck.draw());
            while (playerDeck.getSize() > 0)
                opponentDeck.add(playerDeck.draw());
            while (tmpDeck.getSize() > 0)
                playerDeck.add(tmpDeck.draw());
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "King - trade hands with player "+ opponentID;
    }

    @Override
    public void printToConsole() {
        System.out.println(toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        KingAction that = (KingAction) o;
        return opponentID == that.opponentID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentID);
    }
}
