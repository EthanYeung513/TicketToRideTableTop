package games.tickettoride.gui;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;

import games.tickettoride.TicketToRideGameState;
import gui.AbstractGUIManager;
import gui.GamePanel;
import players.human.ActionController;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;



/**
 * <p>This class allows the visualisation of the game. The game components (accessible through {@link Game#getGameState()}
 * should be added into {@link javax.swing.JComponent} subclasses (e.g. {@link javax.swing.JLabel},
 * {@link javax.swing.JPanel}, {@link javax.swing.JScrollPane}; or custom subclasses such as those in {@link gui} package).
 * These JComponents should then be added to the <code>`parent`</code> object received in the class constructor.</p>
 *
 * <p>An appropriate layout should be set for the parent GamePanel as well, e.g. {@link javax.swing.BoxLayout} or
 * {@link java.awt.BorderLayout} or {@link java.awt.GridBagLayout}.</p>
 *
 * <p>Check the super class for methods that can be overwritten for a more custom look, or
 * {@link games.terraformingmars.gui.TMGUI} for an advanced game visualisation example.</p>
 *
 * <p>A simple implementation example can be found in {@link games.tictactoe.gui.TicTacToeGUIManager}.</p>
 */
public class TicketToRideGUIManager extends AbstractGUIManager {

    TicketToRideCardView[] playerCards;
    JLabel[][] playerHandCardCounts;
    ArrayList<TicketToRideCardView>[] playerHands;
    ArrayList<TicketToRideCardView> bufferDeck;
    TicketToRideBoardView boardView;


    TicketToRideGameState gameState;
    int nPlayers;
    int maxCards;
    int maxBufferCards = 50;
    int panelWidth;

    ArrayList<Integer>[] handCardHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer> bufferHighlights;


    JLabel gameTurnStep;

    public TicketToRideGUIManager(GamePanel parent, Game game, ActionController ac, Set<Integer> human) {
        super(parent, game, ac, human);
        if (game == null) return;

        this.game = game;
        gameState = (TicketToRideGameState) game.getGameState();

        this.gameState = (TicketToRideGameState) game.getGameState();

        boardView = new TicketToRideBoardView(gameState);

        parent.add(boardView, BorderLayout.CENTER);


        parent.revalidate();
        parent.setVisible(true);
        parent.repaint();

        // TODO: set up GUI components and add to `parent`
    }

    /**
     * Defines how many action button objects will be created and cached for usage if needed. Less is better, but
     * should not be smaller than the number of actions available to players in any game state.
     *
     * @return maximum size of the action space (maximum actions available to a player for any decision point in the game)
     */
    @Override
    public int getMaxActionSpace() {
        // TODO
        return 10;
    }

    /**
     * Updates all GUI elements given current game state and player that is currently acting.
     *
     * @param player    - current player acting.
     * @param gameState - current game state to be used in updating visuals.
     */
    @Override
    protected void _update(AbstractPlayer player, AbstractGameState gameState) {
        // TODO
    }
}
