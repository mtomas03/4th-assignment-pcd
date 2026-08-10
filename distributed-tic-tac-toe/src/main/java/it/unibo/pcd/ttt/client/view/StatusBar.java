package it.unibo.pcd.ttt.client.view;

import it.unibo.pcd.ttt.client.model.ClientGameModel;
import it.unibo.pcd.ttt.client.model.GameModelListener;
import it.unibo.pcd.ttt.shared.GameSnapshot;

import javax.swing.*;
import java.awt.*;

/**
 * Swing panel showing the current match status.
 */
public class StatusBar extends JPanel implements GameModelListener {

    private final JLabel label = new JLabel(" ");

    /**
     * Builds an empty status bar.
     */
    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        add(label, BorderLayout.CENTER);
    }

    @Override
    public void onModelChanged(final ClientGameModel model) {
        final GameSnapshot snapshot = model.getSnapshot();
        if (snapshot == null) {
            label.setText(" ");
            return;
        }
        label.setText(describe(snapshot, model.getLocalPlayerName()));
    }

    private String describe(final GameSnapshot snapshot, final String localPlayerName) {
        return switch (snapshot.status()) {
            case WAITING_FOR_OPPONENT -> "Match '" + snapshot.gameName() + "': waiting for an opponent to join...";
            case IN_PROGRESS -> {
                final boolean myTurn = snapshot.turn() == snapshot.symbolOf(localPlayerName);
                yield "Match '" + snapshot.gameName() + "': " + (myTurn ? "your turn" : "opponent's turn")
                        + " (playing as " + snapshot.symbolOf(localPlayerName) + ")";
            }
            case X_WON -> describeOutcome(snapshot, snapshot.playerXName() + " (X) won!");
            case O_WON -> describeOutcome(snapshot, snapshot.playerOName() + " (O) won!");
            case DRAW -> "Match '" + snapshot.gameName() + "' ended in a draw.";
            case ABANDONED -> "Match '" + snapshot.gameName() + "' was abandoned by the other player.";
        };
    }

    private String describeOutcome(final GameSnapshot snapshot, final String announcement) {
        return "Match '" + snapshot.gameName() + "': " + announcement;
    }
}
