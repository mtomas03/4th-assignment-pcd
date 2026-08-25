package it.unibo.pcd.ttt.client.view;

import it.unibo.pcd.ttt.client.controller.GameController;
import it.unibo.pcd.ttt.client.model.ClientGameModel;
import it.unibo.pcd.ttt.client.model.GameModelListener;
import it.unibo.pcd.ttt.shared.Board;
import it.unibo.pcd.ttt.shared.GameSnapshot;
import it.unibo.pcd.ttt.shared.GameStatus;
import it.unibo.pcd.ttt.shared.Symbol;

import javax.swing.*;
import java.awt.*;

/**
 * Swing panel rendering the Tic-Tac-Toe grid.
 *
 * <p> Every click only asks the {@link GameController} to attempt a move; the
 * grid itself is redrawn only once the server has confirmed the resulting state.
 */
public class BoardPanel extends JPanel implements GameModelListener {

    private final JButton[][] cells = new JButton[Board.SIZE][Board.SIZE];

    /**
     * Builds the board panel.
     *
     * @param controller the controller to forward move attempts to
     */
    public BoardPanel(final GameController controller) {
        super(new GridLayout(Board.SIZE, Board.SIZE, 4, 4));
        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                final int r = row;
                final int c = col;
                final JButton button = new JButton();
                button.setFont(button.getFont().deriveFont(Font.BOLD, 32f));
                button.addActionListener(e -> controller.makeMove(r, c));
                cells[row][col] = button;
                add(button);
            }
        }
        render(null, null);
    }

    @Override
    public void onModelChanged(final ClientGameModel model) {
        final GameSnapshot snapshot = model.getSnapshot();
        final Symbol localSymbol = model.getLocalSymbol();
        SwingUtilities.invokeLater(() -> render(snapshot, localSymbol));
    }

    private void render(final GameSnapshot snapshot, final Symbol localSymbol) {
        final boolean myTurn = snapshot != null
                && snapshot.status() == GameStatus.IN_PROGRESS
                && localSymbol != null
                && snapshot.turn() == localSymbol;

        for (int row = 0; row < Board.SIZE; row++) {
            for (int col = 0; col < Board.SIZE; col++) {
                final JButton button = cells[row][col];
                final Symbol mark = snapshot == null ? null : snapshot.board().get(row, col);
                button.setText(mark == null ? "" : mark.name());
                button.setEnabled(myTurn && mark == null);
            }
        }
    }
}
