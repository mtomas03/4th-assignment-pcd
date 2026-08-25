package it.unibo.pcd.ttt.client.view;

import it.unibo.pcd.ttt.client.controller.GameController;
import it.unibo.pcd.ttt.client.model.ClientGameModel;
import it.unibo.pcd.ttt.client.model.GameModelListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Swing window of the client application.
 */
public class MainWindow extends JFrame implements GameModelListener {

    private static final String CARD_LOBBY = "lobby";
    private static final String CARD_GAME = "game";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    /**
     * Builds the main window, lobby and board panel.
     *
     * @param controller the shared controller
     * @param model      the shared model
     */
    public MainWindow(final GameController controller, final ClientGameModel model) {
        super("Distributed Tic-Tac-Toe");
        model.addListener(this);

        final LobbyPanel lobbyPanel = new LobbyPanel(controller);

        final BoardPanel boardPanel = new BoardPanel(controller);
        final StatusBar statusBar = new StatusBar();

        model.addListener(boardPanel);
        model.addListener(statusBar);

        final JPanel gamePanel = new JPanel(new BorderLayout());
        gamePanel.add(boardPanel, BorderLayout.CENTER);
        gamePanel.add(statusBar, BorderLayout.NORTH);

        cards.add(lobbyPanel, CARD_LOBBY);
        cards.add(gamePanel, CARD_GAME);
        add(cards);

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                controller.shutdown();
                dispose();
                System.exit(0);
            }
        });

        setSize(420, 480);
        setLocationRelativeTo(null);
    }

    @Override
    public void onModelChanged(final ClientGameModel model) {
        final boolean showLobby = model.getSnapshot() == null;
        SwingUtilities.invokeLater(() -> cardLayout.show(cards, showLobby ? CARD_LOBBY : CARD_GAME));
    }
}
