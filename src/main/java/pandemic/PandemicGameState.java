package pandemic;

import actions.*;
import components.*;
import content.*;
import core.Area;
import core.Game;
import core.GameParameters;
import core.GameState;
import pandemic.actions.*;
import utilities.Hash;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;

import static pandemic.Constants.*;

public class PandemicGameState extends GameState {

    public Board world;
    private int numAvailableActions = 0;
    private boolean quietNight;

    public void setComponents()
    {
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        // For each player, initialize their own areas: they get a player hand and a player card
        for (int i = 0; i < nPlayers; i++) {
            Area playerArea = new Area();
            playerArea.setOwner(i);
            playerArea.addComponent(Constants.playerHandHash, new Deck(pp.max_cards_per_player));
            playerArea.addComponent(Constants.playerCardHash, new Card());
            areas.put(i, playerArea);
        }

        // Initialize the game area: board, player deck, player discard deck, infection deck, infection discard
        // infection rate counter, outbreak counter, diseases x 4
        Area gameArea = new Area();
        gameArea.setOwner(-1);
        gameArea.addComponent(Constants.pandemicBoardHash, world);
        areas.put(-1, gameArea);

        // load the board
        world = findBoard("cities"); //world.getNode("name","Valencia");

        // Set up the counters
        Counter infection_rate = findCounter("Infection Rate");
        Counter outbreaks = findCounter("Outbreaks");
        gameArea.addComponent(Constants.infectionRateHash, infection_rate);
        gameArea.addComponent(Constants.outbreaksHash, outbreaks);

        for (String color : Constants.colors) {
            int hash = Hash.GetInstance().hash("Disease " + color);
            Counter diseaseC = findCounter("Disease " + color);
            diseaseC.setValue(0);  // 0 - cure not discovered; 1 - cure discovered; 2 - eradicated
            gameArea.addComponent(hash, diseaseC);

            hash = Hash.GetInstance().hash("Disease Cube " + color);
            Counter diseaseCubeCounter = findCounter("Disease Cube " + color);
            gameArea.addComponent(hash, diseaseCubeCounter);
        }

        // Set up decks
        Deck playerDeck = new Deck("Player Deck"); // contains city & event cards
        playerDeck.add(findDeck("Cities"));
        playerDeck.add(findDeck("Events"));
        Deck playerDiscard = new Deck("Player Deck Discard");
        Deck infDiscard = new Deck("Infection Discard");

        gameArea.addComponent(Constants.playerDeckHash, playerDeck);
        gameArea.addComponent(Constants.playerDeckDiscardHash, playerDiscard);
        gameArea.addComponent(Constants.infectionDiscardHash, infDiscard);
        gameArea.addComponent(Constants.infectionHash, findDeck("Infections"));
        gameArea.addComponent(Constants.playerRolesHash, findDeck("Player Roles"));

        // add them to the list of decks, so they are accessible by the findDeck() function
        addDeckToList(playerDeck);
        addDeckToList(infDiscard);
        addDeckToList(playerDiscard);
    }

    @Override
    public GameState copy() {
        //TODO: copy pandemic game state
        return this;
    }

    @Override
    public int nPossibleActions() {
        return this.numAvailableActions;
    }

    @Override
    public List<Action> possibleActions(List<Action> preDetermined) {
        if (preDetermined != null && preDetermined.size() > 0) {
            numAvailableActions = preDetermined.size();
            return preDetermined;
        }

        // Create a list for possible actions
        ArrayList<Action> actions = new ArrayList<>();
        PandemicParameters pp = (PandemicParameters) this.gameParameters;

        Deck playerHand = ((Deck)this.areas.get(activePlayer).getComponent(Constants.playerHandHash));
        if (playerHand.isOverCapacity()){
            // need to discard a card
            for (int i = 0; i < playerHand.getCards().size(); i++){
                actions.add(new DiscardCard(playerHand, i));
            }
            this.numAvailableActions = actions.size();
            return actions;
        }

        // add do nothing action
        actions.add(new DoNothing());

        // Drive / Ferry add actions for travelling immediate cities
        PropertyString playerLocationName = (PropertyString) this.areas.get(activePlayer).getComponent(Constants.playerCardHash).getProperty(Constants.playerLocationHash);
        BoardNode playerLocationNode = world.getNodeByProperty(nameHash, playerLocationName);
        for (BoardNode otherCity : playerLocationNode.getNeighbours()){
            actions.add(new MovePlayer(activePlayer, ((PropertyString)otherCity.getProperty(nameHash)).value));
        }

        // Direct Flight, discard city card and travel to that city
        for (Card card: playerHand.getCards()){
            //  check if card has country to determine if it is city card or not
            if ((card.getProperty(Constants.countryHash)) != null){
                actions.add(new MovePlayerWithCard(activePlayer, ((PropertyString)card.getProperty(nameHash)).value, card));
            }
        }

        // charter flight, discard card that matches your city and travel to any city
        for (Card card: playerHand.getCards()){
            // get the city from the card
            if (playerLocationName.equals(card.getProperty(nameHash))){
                // add all the cities
                // iterate over all the cities in the world
                for (BoardNode bn: this.world.getBoardNodes()) {
                    PropertyString destination = (PropertyString) bn.getProperty(nameHash);

                    // only add the ones that are different from the current location
                    if (!destination.equals(playerLocationName)) {
                        actions.add(new MovePlayerWithCard(activePlayer, destination.value, card));
                    }
                }
            }
        }

        // shuttle flight, move from city with research station to any other research station
        // get research stations from board
        ArrayList<PropertyString> researchStations = new ArrayList<>();
        boolean currentHasStation = false;
        for (BoardNode bn: this.world.getBoardNodes()){
            if (((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value){
                if (bn.getProperty(nameHash).equals(playerLocationName)){
                    currentHasStation = true;
                } else {
                    // researchStations do not contain the current station
                    researchStations.add((PropertyString)bn.getProperty(nameHash));
                }
            }
        }
        // if current city has research station, add every city that has research stations
        if (currentHasStation) {
            for (PropertyString station: researchStations){
                actions.add(new MovePlayer(activePlayer, station.value));
            }
        }

        // Build research station, discard card with that city to build one,
        // Check if there is not already a research station there
        if (!((PropertyBoolean) playerLocationNode.getProperty(Constants.researchStationHash)).value) {
            // Check player has card in hand
            Card card_in_hand = null;
            for (Card card: playerHand.getCards()){
                Property cardName = card.getProperty(nameHash);
                if (cardName.equals(playerLocationName)){
                    card_in_hand = card;
                    break;
                }
            }
            if (card_in_hand != null) {
                // Check if any research station tokens left
                if (findCounter("Research Stations").getValue() == 0) {
                    // If all research stations are used, then take one from board
                    for (PropertyString ps : researchStations) {
                        actions.add(new AddResearchStationWithCardFrom(ps.value, playerLocationName.value, card_in_hand));
                    }
                } else {
                    // Otherwise can just build here
                    actions.add(new AddResearchStationWithCard(playerLocationName.value, card_in_hand));
                }
            }
        }

        // Treat disease
        PropertyIntArray cityInfections = (PropertyIntArray)playerLocationNode.getProperty(Constants.infectionHash);
        for (int i = 0; i < cityInfections.getValues().length; i++){
            if (cityInfections.getValues()[i] > 0){
                actions.add(new TreatDisease(gameParameters, Constants.colors[i], playerLocationName.value));
            }
        }

        // Share knowledge, give or take card, player can only have 7 cards
        // can give any card to anyone
        for (Card card: playerHand.getCards()){
            for (int i = 0; i < nPlayers; i++){
                if (i != activePlayer){
                    actions.add(new GiveCard(card, i));
                }
            }
        }
        // can take any card from anyone
        for (int i = 0; i < nPlayers; i++){
            if (i != activePlayer){
                Deck otherDeck = (Deck)this.areas.get(activePlayer).getComponent(Constants.playerHandHash);
                for (Card card: otherDeck.getCards()){
                    actions.add(new TakeCard(card, i));
                }
            }
        }

        // Discover a cure, cards of the same colour at a research station
        ArrayList<Card>[] colourCounter = new ArrayList[Constants.colors.length];
        for (int i = 0; i < colourCounter.length; i++) {
            colourCounter[i] = new ArrayList<>();
        }
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p != null){
                // Only city cards have colours, events don't
                String color = ((PropertyColor)p).valueStr;
                colourCounter[Utils.indexOf(Constants.colors, color)].add(card);
            }
        }
        for (int i = 0 ; i < colourCounter.length; i++){
            if (colourCounter[i].size() >= pp.n_cards_for_cure){
                actions.add(new CureDisease(Constants.colors[i], colourCounter[i]));
            }
        }

        // TODO event cards don't count as action and can be played anytime
        for (Card card: playerHand.getCards()){
            Property p  = card.getProperty(Constants.colorHash);
            if (p == null){
                // Event cards don't have colour
                actions.addAll(actionsFromEventCard(card, researchStations));
            }
        }

        this.numAvailableActions = actions.size();

        return actions;
    }

    void setActivePlayer(int activePlayer) {
        this.activePlayer = activePlayer;
    }

    private List<Action> actionsFromEventCard(Card card, ArrayList<PropertyString> researchStations){
        ArrayList<Action> actions = new ArrayList<>();
        String cardString = ((PropertyString)card.getProperty(nameHash)).value;

        switch (cardString) {
            case "Airlift":
//                System.out.println("Airlift");
//            System.out.println("Move any 1 pawn to any city. Get permission before moving another player's pawn.");
                for (BoardNode bn: world.getBoardNodes()) {
                    String cityName = ((PropertyString)bn.getProperty(nameHash)).value;
                    for (int i = 0; i < nPlayers; i++) {
                        // Check if player is already there
                        String pLocation = ((PropertyString)areas.get(i).getComponent(playerCardHash).getProperty(playerLocationHash)).value;
                        if (pLocation.equals(cityName)) continue;
                        actions.add(new MovePlayerWithCard(i, cityName, card));
                    }
                }
                break;
            case "Government Grant":
//                System.out.println("Government Grant");
//            System.out.println("Add 1 research station to any city (no City card needed).");
                for (BoardNode bn: world.getBoardNodes()) {
                    if (!((PropertyBoolean) bn.getProperty(Constants.researchStationHash)).value) {
                        String cityName = ((PropertyString) bn.getProperty(nameHash)).value;
                        if (findCounter("Research Stations").getValue() == 0) {
                            // If all research stations are used, then take one from board
                            for (PropertyString ps : researchStations) {
                                actions.add(new AddResearchStationWithCardFrom(ps.value, cityName, card));
                            }
                        } else {
                            // Otherwise can just build here
                            actions.add(new AddResearchStationWithCard(cityName, card));
                        }
                    }
                }
                break;
            case "One quiet night":
//                System.out.println("One quiet night");
//            System.out.println("Skip the next Infect Cities step (do not flip over any Infection cards).");
                actions.add(new QuietNight(card));
                break;
            case "Forecast":
//                System.out.println("Forecast");
//            System.out.println("Draw, look at, and rearrange the top 6 cards of the Infection Deck. Put them back on top.");
                // TODO partial observability: leave the top 6 cards as in the real game to allow player to see them
                // generate all permutations
                Deck infectionDiscard = findDeck("Infection Discard");
                int nInfectDiscards = infectionDiscard.getCards().size();
                int n = Math.max(nInfectDiscards, gp.n_forecast_cards);
                ArrayList<int[]> permutations = new ArrayList<>();
                int[] order = new int[n];
                for (int i = 0; i < n; i++) {
                    order[i] = i;
                }
                generatePermutations(n, order, permutations);
                for (int[] perm: permutations) {
                    actions.add(new RearrangeCardsWithCard("Infection Discard", perm, card));
                }
                break;
        }

        return actions;
    }
    
    protected void setQuietNight(boolean qn) {
        quietNight = qn;
    }

    public boolean isQuietNight() {
        return quietNight;
    }


    private static void generatePermutations(int n, int[] elements, ArrayList<int[]> all) {
        if (n == 1) {
            all.add(elements.clone());
        } else {
            for(int i = 0; i < n-1; i++) {
                generatePermutations(n - 1, elements, all);
                if(n % 2 == 0) {
                    swap(elements, i, n-1);
                } else {
                    swap(elements, 0, n-1);
                }
            }
            generatePermutations(n - 1, elements, all);
        }
    }

    private static void swap(int[] input, int a, int b) {
        int tmp = input[a];
        input[a] = input[b];
        input[b] = tmp;
    }
}
