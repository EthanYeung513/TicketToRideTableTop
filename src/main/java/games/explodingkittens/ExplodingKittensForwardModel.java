package games.explodingkittens;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.components.PartialObservableDeck;
import games.explodingkittens.actions.Favor;
import games.explodingkittens.actions.Pass;
import games.explodingkittens.actions.PlayInterruptibleCard;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.*;

import static games.explodingkittens.cards.ExplodingKittensCard.CardType.EXPLODING_KITTEN;

public class ExplodingKittensForwardModel extends StandardForwardModel {


    @Override
    protected void _setup(AbstractGameState firstState) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) firstState;
        ExplodingKittensParameters ekp = (ExplodingKittensParameters) firstState.getGameParameters();
        ekgs.playerHandCards = new ArrayList<>();
        // Set up draw pile deck
        PartialObservableDeck<ExplodingKittensCard> drawPile = new PartialObservableDeck<>("Draw Pile", -1, firstState.getNPlayers(), CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        ekgs.drawPile = drawPile;
        ekgs.inPlay = new Deck<>("In Play", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Add all cards but defuse and exploding kittens
        for (HashMap.Entry<ExplodingKittensCard.CardType, Integer> entry : ekp.cardCounts.entrySet()) {
            if (entry.getKey() == ExplodingKittensCard.CardType.DEFUSE || entry.getKey() == EXPLODING_KITTEN)
                continue;
            for (int i = 0; i < entry.getValue(); i++) {
                ExplodingKittensCard card = new ExplodingKittensCard(entry.getKey());
                drawPile.add(card);
            }
        }
        ekgs.drawPile.shuffle(ekgs.getRnd());

        // Set up player hands
        List<PartialObservableDeck<ExplodingKittensCard>> playerHandCards = new ArrayList<>(firstState.getNPlayers());
        for (int i = 0; i < firstState.getNPlayers(); i++) {
            boolean[] visible = new boolean[firstState.getNPlayers()];
            visible[i] = true;
            PartialObservableDeck<ExplodingKittensCard> playerCards = new PartialObservableDeck<>("Player Cards", i, visible);
            playerHandCards.add(playerCards);

            // Add defuse card
            ExplodingKittensCard defuse = new ExplodingKittensCard(ExplodingKittensCard.CardType.DEFUSE);
            defuse.setOwnerId(i);
            playerCards.add(defuse);

            // Add N random cards from the deck
            for (int j = 0; j < ekp.nCardsPerPlayer; j++) {
                ExplodingKittensCard c = ekgs.drawPile.draw();
                c.setOwnerId(i);
                playerCards.add(c);
            }
        }
        ekgs.playerHandCards = playerHandCards;
        ekgs.discardPile = new Deck<>("Discard Pile", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);

        // Add remaining defuse cards and exploding kitten cards to the deck and shuffle again
        for (int i = ekgs.getNPlayers(); i < ekp.nDefuseCards; i++) {
            ExplodingKittensCard defuse = new ExplodingKittensCard(ExplodingKittensCard.CardType.DEFUSE);
            drawPile.add(defuse);
        }
        for (int i = 0; i < ekgs.getNPlayers() + ekp.cardCounts.get(EXPLODING_KITTEN); i++) {
            ExplodingKittensCard explodingKitten = new ExplodingKittensCard(EXPLODING_KITTEN);
            drawPile.add(explodingKitten);
        }
        drawPile.shuffle(ekgs.getRnd());

        ekgs.orderOfPlayerDeath = new int[ekgs.getNPlayers()];
        ekgs.setGamePhase(CoreConstants.DefaultGamePhase.Main);
    }

    @Override
    protected void _afterAction(AbstractGameState state, AbstractAction action) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) state;
        if (ekgs.isActionInProgress())
            return;

        if (ekgs.isNotTerminal()) {
            // Draw a card for the current player
            int currentPlayer = ekgs.getCurrentPlayer();
            ExplodingKittensCard card = ekgs.drawPile.draw();
            if (card.cardType == EXPLODING_KITTEN) {
                if (ekgs.playerHandCards.get(currentPlayer).stream().anyMatch(c -> c.cardType == ExplodingKittensCard.CardType.DEFUSE)) {
                    // Exploding kitten drawn, player has defuse
                    ExplodingKittensCard defuseCard = ekgs.playerHandCards.get(currentPlayer).stream().filter(c -> c.cardType == ExplodingKittensCard.CardType.DEFUSE).findFirst().get();
                    ekgs.playerHandCards.get(currentPlayer).remove(defuseCard);
                    // Add to a random location in the draw pile (that we then know)
                    // TODO: This should formally be a player decision
                    int position = ekgs.getRnd().nextInt(ekgs.drawPile.getSize()+1);
                    ekgs.drawPile.add(card, position);
                    ekgs.drawPile.setVisibilityOfComponent(position, currentPlayer, true);
                    ekgs.discardPile.add(defuseCard);
                } else {
                    // Exploding kitten drawn, player is dead
                    ekgs.discardPile.add(card);
                    killPlayer(ekgs, currentPlayer);
                }
            } else {
                card.setOwnerId(currentPlayer);
                ekgs.playerHandCards.get(currentPlayer).add(card);
            }
            endPlayerTurn(ekgs);
        }
    }


    public void killPlayer(ExplodingKittensGameState ekgs, int playerID) {
        ekgs.setPlayerResult(CoreConstants.GameResult.LOSE_GAME, playerID);
        int players = ekgs.getNPlayers();
        int nPlayersActive = 0;
        for (int i = 0; i < players; i++) {
            if (ekgs.getPlayerResults()[i] == CoreConstants.GameResult.GAME_ONGOING) nPlayersActive++;
        }
        ekgs.orderOfPlayerDeath[playerID] = players - nPlayersActive;
        if (nPlayersActive == 1) {
            endGame(ekgs);
        }
    }


    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     *
     * @return - List of AbstractAction objects.
     */
    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {

        // This is called when it is a player's main turn; not during the Nope interrupts
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gameState;
        int playerID = ekgs.getCurrentPlayer();
        List<AbstractAction> actions = new ArrayList<>();
        Deck<ExplodingKittensCard> playerDeck = ekgs.playerHandCards.get(playerID);

        // We can pass, or play any card in our hand
        actions.add(new Pass());
        // get all unique playable types in hand
        Set<ExplodingKittensCard.CardType> playableTypes = new HashSet<>();
        for (int i = 0; i < playerDeck.getSize(); i++) {
            playableTypes.add(playerDeck.get(i).cardType);
        }
        // remove defuse and exploding kittens from playable types
        playableTypes.remove(ExplodingKittensCard.CardType.DEFUSE);
        playableTypes.remove(EXPLODING_KITTEN);

        for (ExplodingKittensCard.CardType type : playableTypes) {
            switch(type) {
                case FAVOR:
                    for (int i = 0; i < ekgs.getNPlayers(); i++) {
                        if (i != playerID) {
                            actions.add(new Favor(playerID, i));
                        }
                    }
                    break;
                default :
                    actions.add(new PlayInterruptibleCard(type, playerID));
            }
        }
        // TODO: Special case for a pair of identical Cat cards (not implemented in OLD version)
        return actions;
    }


}
