package games.tickettoride;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import core.components.Deck;
import core.components.Card;
import core.properties.Property;
import core.properties.PropertyInt;
import core.properties.PropertyString;

import static games.tickettoride.TicketToRideConstants.*;

public class TicketToRideHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        TicketToRideGameState state = (TicketToRideGameState) gs;
        CoreConstants.GameResult playerResult = state.getPlayerResults()[playerId];

        if (playerResult == CoreConstants.GameResult.LOSE_GAME) return -1.0;
        if (playerResult == CoreConstants.GameResult.WIN_GAME) return 1.0;

        int baseScore = state.getScores()[playerId]; //i.e. just from claiming routes
        int destinationPoints = calculateDestinationCardRewardPoints(state, playerId); //reward system
        boolean hasLongestRoute = isLongestRoute(state, playerId);


        int totalScore = baseScore + destinationPoints + (hasLongestRoute ? 10 : 0);

        return Math.min(1.0, Math.max(-1.0, totalScore / 250.0)); //250 being roughly the max score a player can have
    }

    private int calculateDestinationCardRewardPoints(TicketToRideGameState state, int playerId) {
        Deck<Card> destinationHand = (Deck<Card>) state.getComponentActingPlayer(playerId, playerDestinationHandHash);
        int rewardPoints = 0;

        for (Card currentDestinationCard : destinationHand.getComponents()) {
            int points = ((PropertyInt) currentDestinationCard.getProperty(pointsHash)).value;
            Property location1Prop = currentDestinationCard.getProperty(location1Hash);
            Property location2Prop = currentDestinationCard.getProperty(location2Hash);

            if (location1Prop != null && location2Prop != null) {
                String location1 = ((PropertyString) location1Prop).value;
                String location2 = ((PropertyString) location2Prop).value;
                boolean completed = state.checkIfConnectedCity(state.getSearchGraph(state.getWorld().getBoardEdges(), playerId), location1, location2);
                rewardPoints += completed ? 2 * points : -points;
            }
        }
        return rewardPoints;
    }

    private boolean isLongestRoute(TicketToRideGameState state, int playerId) {
        int playerRoute = state.getLongestRouteLength(playerId);
        for (int i = 0; i < state.getNPlayers(); i++) {
            if (i != playerId && state.getLongestRouteLength(i) >= playerRoute) {
                return false;
            }
        }
        return true;
    }
}