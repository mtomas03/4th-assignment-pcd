package it.unibo.pcd.ttt.shared;

/**
 * Checked exception used to signal a violation of the Tic-Tac-Toe game
 * rules or of the matchmaking process.
 */
public class GameException extends Exception {

    /**
     * Creates a new Game exception with the given explanatory message.
     *
     * @param message a description of the rule violation
     */
    public GameException(final String message) {
        super(message);
    }
}
