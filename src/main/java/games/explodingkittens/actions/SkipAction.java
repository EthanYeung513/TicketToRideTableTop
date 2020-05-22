package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import core.turnorder.TurnOrder;

public class SkipAction extends DrawCard implements IsNopeable, IPrintable {

    public SkipAction(int deckFrom, int deckTo, int index) {
        super(deckFrom, deckTo, index);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard the card played
        super.execute(gs);
        // Execute action
        gs.setMainGamePhase();
        ((ExplodingKittenTurnOrder)gs.getTurnOrder()).endPlayerTurnStep(gs);
        return true;
    }

    @Override
    public String toString(){
        return "Player skips its draw";
    }

    @Override
    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}
