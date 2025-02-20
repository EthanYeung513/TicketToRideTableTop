package games.tickettoride.gui;

import core.AbstractGameState;
import core.components.*;
import core.properties.*;
import games.tickettoride.TicketToRideConstants;
import games.tickettoride.TicketToRideGameState;
import games.tickettoride.TicketToRideParameters;
import utilities.Hash;
import utilities.ImageIO;
import utilities.Utils;
import utilities.Vector2D;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;

import static core.CoreConstants.*;
import static gui.views.DeckView.drawDeck;
import static games.tickettoride.TicketToRideConstants.*;
import static games.tickettoride.gui.TicketToRideCardView.drawCard;

public class TicketToRideBoardView extends JComponent {
    private final Image background;
    private final Image cardBackPD;
    private final Image cardBackInf;
    private final GraphBoardWithEdges graphBoard;
    private int width;
    private int height;

    TicketToRideGameState gameState;
    double scale = 0.4;

    int cardWidth = (int)(scale * TicketToRideCardView.cardWidth);
    int cardHeight = (int)(scale * TicketToRideCardView.cardHeight);
    int nodeSize = (int)(scale * 20);
    int strokeWidth = (int)(scale * 2);



    // Clickable locations

    Rectangle playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle plannerDeckLocation = new Rectangle((int)(scale * 1070), (int)(scale * 50), cardWidth, cardHeight);
    Rectangle playerDeckLocation = new Rectangle((int)(scale * 760), (int)(scale * 50), cardWidth, cardHeight);
    HashMap<String, Rectangle> boardNodeLocations;
    Rectangle[] playerLocations;

    HashMap<String, Rectangle> highlights;
    int maxHighlights = 3;

    ArrayList<Integer> bufferHighlights;
    HashSet<Integer> playerHighlights;
    ArrayList<Integer>[] handCardHighlights;

    int panX, panY;

    public TicketToRideBoardView(AbstractGameState gs) {
        System.out.println( "In board view  doing constructor");
        gameState = (TicketToRideGameState) gs;
        this.graphBoard = ((TicketToRideGameState) gs).getWorld();
        String dataPath = ((TicketToRideParameters)gs.getGameParameters()).getDataPath() + "img/";
        System.out.println( dataPath +  " data path");

        // Background and card backs
        background = ImageIO.GetInstance().getImage(dataPath + "ticketToRideBg.png");
        cardBackInf = ImageIO.GetInstance().getImage(dataPath + "trainCardBlueBg.png");
        cardBackPD = ImageIO.GetInstance().getImage(dataPath + "trainCardRedBg.png");


        System.out.println( background +  " background");

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        playerLocations = new Rectangle[gs.getNPlayers()];
        highlights = new HashMap<>();

        Collection<BoardNodeWithEdges> bList = graphBoard.getBoardNodes();
        for (BoardNodeWithEdges b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

        addMouseWheelListener(e -> {
            double amount = 0.2 * Math.abs(e.getPreciseWheelRotation());
            if (e.getPreciseWheelRotation() > 0) {
                updateScale(scale - amount);
            } else {
                updateScale(scale + amount);
            }
            highlights.clear();
        });
        addMouseListener(new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    start = e.getPoint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2 && start != null) {
                    // Middle (wheel) click, pan around
                    Point end = e.getPoint();
                    panX += (int)(scale * (end.x - start.x));
                    panY += (int)(scale * (end.y - start.y));
                    start = null;
                    highlights.clear();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 || highlights.size() >= maxHighlights) {
                    highlights.clear();
                    return;
                }
                Point p = new Point(e.getX() - panX, e.getY() - panY);
                if (playerDiscardDeckLocation.contains(p)) {
                    highlights.put("playerDiscard", playerDiscardDeckLocation);
                } else if (plannerDeckLocation.contains(p)) {
                    highlights.put("plannerDeck", plannerDeckLocation);
                } else {
                    for (int i = 0; i < playerLocations.length; i++) {
                        if (playerLocations[i] != null && playerLocations[i].contains(p)) {
                            highlights.put("player " + i, playerLocations[i]);
                            break;
                        }
                    }
                    for (Map.Entry<String, Rectangle> en: boardNodeLocations.entrySet()) {
                        if (en.getValue().contains(p)) {
                            highlights.put("BN " + en.getKey(), en.getValue());
                            break;
                        }
                    }
                }
            }
        });

    }

    private void updateScale(double scale) {
        this.scale = scale;


        cardWidth = (int)(scale * TicketToRideCardView.cardWidth);
        cardHeight = (int)(scale * TicketToRideCardView.cardHeight);
        nodeSize = (int)(scale * 20);


        playerDiscardDeckLocation = new Rectangle((int)(scale * 880), (int)(scale * 50), cardWidth, cardHeight);
        plannerDeckLocation = new Rectangle((int)(scale * 1070), (int)(scale * 50), cardWidth, cardHeight);
        playerDeckLocation = new Rectangle((int)(scale * 760), (int)(scale * 50), cardWidth, cardHeight);

        width = (int)(background.getWidth(null) * scale);
        height = (int)(background.getHeight(null) * scale);

        boardNodeLocations = new HashMap<>();
        Collection<BoardNodeWithEdges> bList = graphBoard.getBoardNodes();
        for (BoardNodeWithEdges b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int)(poss.getX()*scale), (int)(poss.getY()*scale));
            boardNodeLocations.put(((PropertyString) b.getProperty(nameHash)).value,
                    new Rectangle(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize));
        }

    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.black);
        System.out.println( "calling draw board in paintComponent");
        drawBoard(g2);

        if (highlights.size() > 0) {
            Stroke s = g2.getStroke();
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.setColor(Color.CYAN);
            for (Map.Entry<String, Rectangle> e: highlights.entrySet()) {
                Rectangle highlight = e.getValue();
                g2.drawRect(panX + highlight.x - strokeWidth / 2,
                        panY + highlight.y - strokeWidth / 2,
                        highlight.width + strokeWidth,
                        highlight.height + strokeWidth);
            }
            g2.setStroke(s);
        }
    }

    private void drawBoard(Graphics2D g) {
        int fSize = g.getFont().getSize();
        int fontSize = (int) (scale * fSize);
        g.setFont(new Font(g.getFont().getFontName(), Font.PLAIN, fontSize));
        int nPlayers = gameState.getNPlayers();

        // Draw board background
        drawImage(g, background, panX, panY);

        // Draw nodes
        Collection<BoardNodeWithEdges> bList = graphBoard.getBoardNodes();
        for (BoardNodeWithEdges b : bList) {
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int) (poss.getX() * scale) + panX, (int) (poss.getY() * scale) + panY);
            PropertyBoolean edge = ((PropertyBoolean) b.getProperty(edgeHash));

            HashSet<BoardNodeWithEdges> neighbours = (HashSet<BoardNodeWithEdges>) b.getNeighbours();
            for (BoardNodeWithEdges b2 : neighbours) {
                Vector2D poss2 = ((PropertyVector2D) b2.getProperty(coordinateHash)).values;
                Vector2D pos2 = new Vector2D((int) (poss2.getX() * scale) + panX, (int) (poss2.getY() * scale) + panY);
                PropertyBoolean edge2 = ((PropertyBoolean) b2.getProperty(edgeHash));

                if (edge != null && edge.value && edge2 != null && edge2.value) {
                    // Two edge nodes connected check if on opposite sides, draw connection as if b2 on the other side of map
                    if (pos2.getX() < width / 2 && pos.getX() > width / 2 || pos2.getX() > width / 2 && pos.getX() < width / 2) {
                        if (pos2.getX() > pos.getX()) pos2.setX(pos2.getX() - width);
                        else pos2.setX(width + pos2.getX());
                    }
                }
                g.setColor(Color.white);
                g.drawLine(pos.getX(), pos.getY(), pos2.getX(), pos2.getY());
            }

            g.setColor(Color.black);

        }

        for (BoardNodeWithEdges b : bList) {
            String name = ((PropertyString) b.getProperty(nameHash)).value;
            Vector2D poss = ((PropertyVector2D) b.getProperty(coordinateHash)).values;
            Vector2D pos = new Vector2D((int) (poss.getX() * scale) + panX, (int) (poss.getY() * scale) + panY);


            g.setColor(Utils.stringToColor(((PropertyColor) b.getProperty(colorHash)).valueStr));
            g.fillOval(pos.getX() - nodeSize / 2, pos.getY() - nodeSize / 2, nodeSize, nodeSize);



            Stroke s = g.getStroke();
            g.setStroke(s);
            g.setColor(Color.black);


            // Decks
            Deck<Card> playerDiscardDeck = (Deck<Card>) gameState.getComponent(TicketToRideConstants.playerDeckDiscardHash);
            if (playerDiscardDeck != null) {
                Card cP = playerDiscardDeck.peek();
                drawCard(g, cP, null, new Rectangle(playerDiscardDeckLocation.x + panX, playerDiscardDeckLocation.y + panY, playerDiscardDeckLocation.width, playerDiscardDeckLocation.height));
            }

            g.drawString("Player Discard Deck", (int) playerDiscardDeckLocation.getX() + panX, (int) playerDiscardDeckLocation.getY() - fontSize + panY);

            drawDeck(g, (Deck<Card>) gameState.getComponent(trainCardDeckHash), null, cardBackPD, new Rectangle(playerDeckLocation.x + panX, playerDeckLocation.y + panY, playerDeckLocation.width, playerDeckLocation.height), false);
            g.drawString("Player Train Card Deck", (int)playerDeckLocation.getX() + panX, panY + (int)playerDeckLocation.getY() - fontSize);


            System.out.println( "DRAW BOARD");
        }

    }
    private void drawImage(Graphics2D g, Image img, int x, int y) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        g.drawImage(img, x, y, (int) (w*scale), (int) (h*scale), null);
    }

    public HashMap<String, Rectangle> getHighlights() {
        return highlights;
    }

}
