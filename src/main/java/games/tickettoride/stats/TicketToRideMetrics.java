package games.tickettoride.stats;


import core.AbstractGameState;
import core.components.*;
import core.interfaces.IGameEvent;
import core.properties.*;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.tickettoride.TicketToRideConstants;
import games.tickettoride.TicketToRideGameState;
import utilities.Hash;

import java.util.*;
import java.util.stream.IntStream;

@SuppressWarnings("unused")
public class TicketToRideMetrics implements IMetricsCollection {

    public static class Win extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Set<Integer> winners = e.state.getWinners();
            if (winners.size() != 1) return false;
            int winnerId = winners.iterator().next();
            records.put("PlayerType", listener.getGame().getPlayers().get(winnerId).toString());
            records.put("PlayerType-StartingPos", listener.getGame().getPlayers().get(winnerId).toString() + "-" + winnerId);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("PlayerType", String.class);
            columns.put("PlayerType-StartingPos", String.class);
            return columns;
        }
    }

    public static class FinalScores extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_TotalScore", gs.getScores()[i]);
                records.put(playerName + "_DestinationPoints", gs.calculateDestinationCardPoints(i));
                records.put(playerName + "_RemainingTrains", gs.getTrainCars(i));
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p : playerNames) {
                columns.put(p + "_TotalScore", Integer.class);
                columns.put(p + "_DestinationPoints", Integer.class);
                columns.put(p + "_RemainingTrains", Integer.class);
            }
            return columns;
        }
    }

    public static class RoutesClaimed extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            HashSet<Edge> edges = (HashSet<Edge>) gs.getWorld().getBoardEdges();

            int[] totalRoutes = new int[gs.getNPlayers()];
            int[] totalLength = new int[gs.getNPlayers()];

            for (Edge edge : edges) {
                boolean claimed = ((PropertyBoolean) edge.getProperty(TicketToRideConstants.routeClaimedHash)).value;
                if (claimed) {
                    PropertyInt route1 = (PropertyInt) edge.getProperty(TicketToRideConstants.claimedByPlayerRoute1Hash);
                    PropertyInt route2 = (PropertyInt) edge.getProperty(TicketToRideConstants.claimedByPlayerRoute2Hash);
                    int length = ((PropertyInt) edge.getProperty(TicketToRideConstants.trainCardsRequiredHash)).value;

                    IntStream.of(route1.value, route2.value)
                            .filter(p -> p != -1)
                            .forEach(p -> {
                                totalRoutes[p]++;
                                totalLength[p] += length;
                            });
                }
            }

            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_RoutesClaimed", totalRoutes[i]);
                records.put(playerName + "_TotalRouteLength", totalLength[i]);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p : playerNames) {
                columns.put(p + "_RoutesClaimed", Integer.class);
                columns.put(p + "_TotalRouteLength", Integer.class);
            }
            return columns;
        }
    }

    public static class DestinationCards extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                Deck<Card> destDeck = (Deck<Card>) gs.getComponentActingPlayer(i, TicketToRideConstants.playerDestinationHandHash);
                int finalI = i;
                long completed = destDeck.getComponents().stream()
                        .filter(card -> gs.checkIfConnectedCity(
                                gs.getSearchGraph(gs.getWorld().getBoardEdges(), finalI),
                                String.valueOf(card.getProperty(TicketToRideConstants.location1Hash)),
                                String.valueOf(card.getProperty(TicketToRideConstants.location2Hash))
                        )).count();

                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_CompletedDestinations", (int) completed);
                records.put(playerName + "_UncompletedDestinations", destDeck.getSize() - (int) completed);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p : playerNames) {
                columns.put(p + "_CompletedDestinations", Integer.class);
                columns.put(p + "_UncompletedDestinations", Integer.class);
            }
            return columns;
        }
    }

    public static class LongestRoute extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            int maxLength = Arrays.stream(gs.getScores()).max().orElse(0); // Simplified example

            // Actual longest route calculation would need graph analysis
            // This is a placeholder implementation
            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_LongestRoute", maxLength == gs.getScores()[i] ? 1 : 0);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p : playerNames) {
                columns.put(p + "_LongestRoute", Integer.class);
            }
            return columns;
        }
    }
}
