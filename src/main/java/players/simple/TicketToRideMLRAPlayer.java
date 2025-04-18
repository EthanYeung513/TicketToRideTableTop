package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.actions.ClaimRoute;
import games.tickettoride.actions.DrawDestinationTicketCards;
import games.tickettoride.actions.DrawTrainCards;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TicketToRideMLRAPlayer extends AbstractPlayer {

    private final Random rnd;


    int lowerBound = 3;
    int upperBound = 4;

    public TicketToRideMLRAPlayer() {
        super(null, "MLRA");
        this.rnd = new Random();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> actions) {
        TicketToRideGameState state = (TicketToRideGameState) observation;

        List<ClaimRoute> claimRouteActions = new ArrayList<>();
        DrawTrainCards drawTrainCardAction = null;
        DrawDestinationTicketCards drawDestinationTicketCardAction = null;

        for (AbstractAction action : actions) {
            if (action instanceof ClaimRoute) {
                claimRouteActions.add((ClaimRoute) action);
            }
            if (action instanceof DrawTrainCards){
                drawTrainCardAction = (DrawTrainCards) action;
            }
            if (action instanceof DrawDestinationTicketCards){
                drawDestinationTicketCardAction = (DrawDestinationTicketCards) action;
            }
        }

        // if no ClaimRouteActions, draw train card
        if (claimRouteActions.isEmpty() && drawTrainCardAction != null) {
            return drawTrainCardAction;
        }

        ClaimRoute claimRoutebestAction = null;

        for (ClaimRoute action : claimRouteActions) {

            int routeLength = action.costOfRoute;

            if (routeLength >= lowerBound && routeLength <= upperBound) {
                if (claimRoutebestAction == null) {
                    claimRoutebestAction = action;
                }
            }
        }

        // no short/medium route found, go for highest route size
        if (claimRoutebestAction == null) {
            for (ClaimRoute action : claimRouteActions) {
                int routeLength = action.costOfRoute;
                if (claimRoutebestAction == null && routeLength > upperBound) {
                    claimRoutebestAction = action;
                }
            }
        }

        //draw train card, if unlikely event they cant, draw destination card
        if (claimRoutebestAction == null) {
            if (drawTrainCardAction != null){
                return  drawTrainCardAction;
            }
            else if (drawDestinationTicketCardAction != null){
                return  drawDestinationTicketCardAction;
            }
        }

        return claimRoutebestAction;
    }

    @Override
    public String toString() {
        return "MLRA";
    }

    @Override
    public AbstractPlayer copy() {
        return new TicketToRideMLRAPlayer();
    }
}