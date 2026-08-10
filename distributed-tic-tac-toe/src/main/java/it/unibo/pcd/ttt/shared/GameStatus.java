package it.unibo.pcd.ttt.shared;

/**
 * The lifecycle states of a single Tic-Tac-Toe match.
 */
public enum GameStatus {

    /**
     * The match was created and is waiting for a second player to join.
     */
    WAITING_FOR_OPPONENT,

    /**
     * Both players joined and the match is being played.
     */
    IN_PROGRESS,

    /**
     * Player X completed a winning line.
     */
    X_WON,

    /**
     * Player O completed a winning line.
     */
    O_WON,

    /**
     * The board is full and no player completed a winning line.
     */
    DRAW,

    /**
     * One of the two players left before the match reached a natural end.
     */
    ABANDONED
}
