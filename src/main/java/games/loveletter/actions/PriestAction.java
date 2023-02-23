package games.loveletter.actions;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IPrintable;
import games.loveletter.LoveLetterGameState;
import core.components.PartialObservableDeck;
import games.loveletter.cards.LoveLetterCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The Priest allows a player to see another player's hand cards.
 * This has no effect in case the game is fully observable.
 */
public class PriestAction extends PlayCard implements IPrintable {

    private LoveLetterCard.CardType opponentCard;

    public PriestAction(int playerID, int opponentID) {
        super(LoveLetterCard.CardType.Priest, playerID, opponentID, null, null);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        LoveLetterGameState llgs = (LoveLetterGameState)gs;
        PartialObservableDeck<LoveLetterCard> opponentDeck = llgs.getPlayerHandCards().get(targetPlayer);

        // Set all cards to be visible by the current player
        for (int i = 0; i < opponentDeck.getComponents().size(); i++)
            opponentDeck.setVisibilityOfComponent(i, playerID, true);

        opponentCard = opponentDeck.get(0).cardType;
        if (llgs.getCoreGameParameters().recordEventHistory) {
            llgs.recordHistory("Priest sees " + opponentCard);
        }

        return super.execute(gs);
    }

    @Override
    public String toString(){
        return "Priest (" + playerID + " sees " + (opponentCard != null? opponentCard : "card") + " of " + targetPlayer + ")";
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PriestAction)) return false;
        if (!super.equals(o)) return false;
        PriestAction that = (PriestAction) o;
        return opponentCard == that.opponentCard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), opponentCard);
    }

    @Override
    public PriestAction copy() {
        PriestAction copy = new PriestAction(playerID, targetPlayer);
        copy.opponentCard = opponentCard;
        return copy;
    }

    public static List<? extends PlayCard> generateActions(LoveLetterGameState gs, int playerID) {
        List<PlayCard> cardActions = new ArrayList<>();
        for (int targetPlayer = 0; targetPlayer < gs.getNPlayers(); targetPlayer++) {
            if (targetPlayer == playerID || gs.getPlayerResults()[targetPlayer] == CoreConstants.GameResult.LOSE || gs.isProtected(targetPlayer))
                continue;
            cardActions.add(new PriestAction(playerID, targetPlayer));
        }
        return cardActions;
    }
}
