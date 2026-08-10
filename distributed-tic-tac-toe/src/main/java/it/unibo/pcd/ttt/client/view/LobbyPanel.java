package it.unibo.pcd.ttt.client.view;

import it.unibo.pcd.ttt.client.controller.GameController;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

/**
 * Swing panel shown before a match starts: the user can enter a player name and a match name,
 * then either creates a new match with that name or joins an existing one with that name.
 *
 * <p> This panel never talks to the RMI layer directly:
 * every user action is forwarded to the {@link GameController}.
 */
public class LobbyPanel extends JPanel {

    private final JTextField playerNameField = new JTextField(12);
    private final JTextField gameNameField = new JTextField(12);

    /**
     * Builds the lobby panel.
     *
     * @param controller the controller to forward user actions to
     */
    public LobbyPanel(final GameController controller) {
        super(new GridLayout(3, 1, 8, 8));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        final JPanel form = new JPanel(new GridLayout(2, 2, 6, 6));
        form.add(new JLabel("Player name:"));
        form.add(playerNameField);
        form.add(new JLabel("Game name:"));
        form.add(gameNameField);
        add(form);

        final JButton createButton = new JButton("Create game");
        final JButton joinButton = new JButton("Join game");
        final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(createButton);
        buttons.add(joinButton);
        add(buttons);

        createButton.addActionListener(e -> submit(controller::createGame));
        joinButton.addActionListener(e -> submit(controller::joinGame));
    }

    private void submit(final MatchAction action) {
        final String playerName = playerNameField.getText().trim();
        final String gameName = gameNameField.getText().trim();
        if (playerName.isEmpty() || gameName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both player name and game name.");
            return;
        }
        action.run(gameName, playerName, error -> {
            if (error != null) {
                JOptionPane.showMessageDialog(this, error, "Operation failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    @FunctionalInterface
    private interface MatchAction {
        void run(String gameName, String playerName, Consumer<String> onCompletion);
    }
}
