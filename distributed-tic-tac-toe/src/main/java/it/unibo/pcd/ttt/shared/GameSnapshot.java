package it.unibo.pcd.ttt.shared;

import java.io.Serializable;

/**
 * An immutable snapshot of a match, sent by value from the server to the clients.
 *
 * @param gameName    the name of the match
 * @param board       the current board
 * @param status      the current status of the match
 * @param turn        the symbol allowed to move next
 * @param playerXName the name of the player holding {@link Symbol#X}
 * @param playerOName the name of the player holding {@link Symbol#O}
 */
public record GameSnapshot(
        String gameName,
        Board board,
        GameStatus status,
        Symbol turn,
        String playerXName,
        String playerOName
) implements Serializable {

    /**
     * Maps a player's name to their assigned symbol in this snapshot.
     *
     * @param playerName the name of the player to look up
     * @return the {@link Symbol} assigned to the player, or {@code null}
     *         if the player is not in this match
     */
    public Symbol symbolOf(final String playerName) {
        if (playerName != null && playerName.equals(playerXName)) {
            return Symbol.X;
        }
        if (playerName != null && playerName.equals(playerOName)) {
            return Symbol.O;
        }
        return null;
    }
}