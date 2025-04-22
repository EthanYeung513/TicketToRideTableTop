package games.tickettoride.stats;

import core.actions.AbstractAction;
import core.components.*;
import core.interfaces.IGameEvent;
import core.properties.*;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.tickettoride.TicketToRideConstants;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.actions.ClaimRoute;
import games.tickettoride.actions.DrawDestinationTicketCards;
import games.tickettoride.actions.DrawTrainCards;

import java.util.*;

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

            int[] longestContinuousRoutes = new int[gs.getNPlayers()];
            int greatestRouteLength = 0;
            for (int playerId = 0; playerId < gs.getNPlayers(); playerId++) {
                longestContinuousRoutes[playerId] = gs.getLongestRouteLength(playerId);
                greatestRouteLength = Math.max(greatestRouteLength, longestContinuousRoutes[playerId]);
            }

            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_TotalScore", gs.getScores()[i]);
                records.put(playerName + "_DestinationPoints", gs.calculateDestinationCardPoints(i));
                records.put(playerName + "_RemainingTrains", gs.getTrainCars(i));
                records.put(playerName + "_LongestContinuousRoute", longestContinuousRoutes[i]);
                records.put(playerName + "_LongestContinuousRouteBonus", (longestContinuousRoutes[i] == greatestRouteLength && greatestRouteLength > 0) ? 10 :0);
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
                columns.put(p + "_LongestContinuousRoute", Integer.class);
                columns.put(p + "_LongestContinuousRouteBonus", Integer.class);
            }
            return columns;
        }
    }

    public static class RoutesClaimed extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            HashSet<Edge> routes = (HashSet<Edge>) gs.getWorld().getBoardEdges();

            int[] totalRoutes = new int[gs.getNPlayers()];
            int[] totalLength = new int[gs.getNPlayers()];

            for (Edge currentRoute : routes) {
                boolean claimed = ((PropertyBoolean) currentRoute.getProperty(TicketToRideConstants.routeClaimedHash)).value;
                if (claimed) {
                    PropertyInt route1 = (PropertyInt) currentRoute.getProperty(TicketToRideConstants.claimedByPlayerRoute1Hash);
                    PropertyInt route2 = (PropertyInt) currentRoute.getProperty(TicketToRideConstants.claimedByPlayerRoute2Hash);
                    int lengthOfRoute = ((PropertyInt) currentRoute.getProperty(TicketToRideConstants.trainCardsRequiredHash)).value;

                    if (route1.value != -1) {
                        totalRoutes[route1.value]++;
                        totalLength[route1.value] += lengthOfRoute;
                    }
                    if (route2 != null && route2.value != -1) {
                        totalRoutes[route2.value]++;
                        totalLength[route2.value] += lengthOfRoute;
                    }
                }
            }

            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_RoutesClaimed", totalRoutes[i]);
                records.put(playerName + "_TotalRouteLength", totalLength[i]);
                if (totalRoutes[i] > 0) {
                    records.put(playerName + "_AvgRouteLength", (double) totalLength[i] / totalRoutes[i]);
                }
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
                columns.put(p + "_AvgRouteLength", Double.class);
            }
            return columns;
        }
    }

    public static class DestinationCards extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            TicketToRideGameState gs = (TicketToRideGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                Deck<Card> destinationHand = (Deck<Card>) gs.getComponentActingPlayer(i, TicketToRideConstants.playerDestinationHandHash);
                int iForLambda = i;
                long completed = destinationHand.getComponents().stream().filter(card -> gs.checkIfConnectedCity(gs.getSearchGraph(gs.getWorld().getBoardEdges(), iForLambda), String.valueOf(card.getProperty(TicketToRideConstants.location1Hash)), String.valueOf(card.getProperty(TicketToRideConstants.location2Hash)))).count();

                String playerName = listener.getGame().getPlayers().get(i).toString();
                int totalDestinationCardsInHand = destinationHand.getSize();

                records.put(playerName + "_CompletedDestinations", (int) completed);
                records.put(playerName + "_UncompletedDestinations", totalDestinationCardsInHand - (int) completed);

                double completionPercentage = 0.0f;
                if (totalDestinationCardsInHand > 0) {
                    completionPercentage = ((double) completed / totalDestinationCardsInHand) * 100;
                }
                records.put(playerName + "_DestCompletionPercentage", completionPercentage);
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
                columns.put(p + "_DestCompletionPercentage", Double.class);
            }
            return columns;
        }
    }
    public static class ActionsChosen extends AbstractMetric {
        private Map<String, Integer> trainCounts = new HashMap<>();
        private Map<String, Integer> destCounts = new HashMap<>();
        private Map<String, Integer> claimCounts = new HashMap<>();

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.ACTION_CHOSEN) {
                AbstractAction action = e.action;
                String playerName = listener.getGame().getPlayers().get(e.playerID).toString();

                if (action instanceof DrawTrainCards) {
                    trainCounts.put(playerName, trainCounts.getOrDefault(playerName, 0) + 1);
                }
                else if (action instanceof DrawDestinationTicketCards) {
                    destCounts.put(playerName, destCounts.getOrDefault(playerName, 0) + 1);
                }
                else if (action instanceof ClaimRoute) {
                    claimCounts.put(playerName, claimCounts.getOrDefault(playerName, 0) + 1);
                }
                return false;

            } else if (e.type == Event.GameEvent.GAME_OVER) {
                for (String playerName : listener.getGame().getPlayers().stream().map(p -> p.toString()).toList()) {
                    int train = trainCounts.getOrDefault(playerName, 0);
                    int dest = destCounts.getOrDefault(playerName, 0);
                    int claim = claimCounts.getOrDefault(playerName, 0);
                    int totalActions = train + dest + claim;

                    records.put(playerName + "_TrainCardsDrawn", train);
                    records.put(playerName + "_DestinationCardsDrawn", dest);
                    records.put(playerName + "_RoutesClaimed", claim);

                    if (totalActions > 0) {
                        records.put(playerName + "_%TrainCards", (double) Math.round((train * 100.0) / totalActions));
                        records.put(playerName + "_%DestCards", (double) Math.round((dest * 100.0) / totalActions));
                        records.put(playerName + "_%RoutesClaimed", (double) Math.round((claim * 100.0) / totalActions));
                    } else {
                        records.put(playerName + "_%TrainCards", 0.0);
                        records.put(playerName + "_%DestCards", 0.0);
                        records.put(playerName + "_%RoutesClaimed", 0.0);
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            trainCounts.clear();
            destCounts.clear();
            claimCounts.clear();

            Map<String, Class<?>> columns = new HashMap<>();
            for (String p : playerNames) {
                columns.put(p + "_TrainCardsDrawn", Integer.class);
                columns.put(p + "_DestinationCardsDrawn", Integer.class);
                columns.put(p + "_RoutesClaimed", Integer.class);

                columns.put(p + "_%TrainCards", Double.class);
                columns.put(p + "_%DestCards", Double.class);
                columns.put(p + "_%RoutesClaimed", Double.class);
            }
            return columns;
        }
    }



}