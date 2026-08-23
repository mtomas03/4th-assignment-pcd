package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.*;

/**
 * A monitor representing a single Tic-Tac-Toe match.
 */
public class GameState {

    private final String gameName;
    private Board board = Board.empty();
    private GameStatus status = GameStatus.WAITING_FOR_OPPONENT;
    private Symbol turn = Symbol.X;
    private String playerXName;
    private String playerOName;
    private PlayerCallback playerXCallback;
    private PlayerCallback playerOCallback;

    /**
     * Constructs a new {@code GameState} for a match with the specified name.
     *
     * @param gameName the unique name of the match
     */
    public GameState(final String gameName) {
        this.gameName = gameName;
    }

    /**
     * Registers a new player and their remote callback in this match.
     *
     * @param playerName the display name of the joining player
     * @param callback   the remote callback to notify for state updates
     * @return an updated, immutable {@link GameSnapshot} capturing the new state
     * @throws GameException if the player name is invalid, already taken, or if the match is full/abandoned
     */
    public synchronized GameSnapshot addPlayer(final String playerName, final PlayerCallback callback) throws GameException {
        if (playerName == null || playerName.isBlank()) {
            throw new GameException("Player name must not be empty.");
        }
        if (status == GameStatus.ABANDONED) {
            throw new GameException("Match '" + gameName + "' was abandoned.");
        }
        if (status == GameStatus.X_WON || status == GameStatus.O_WON || status == GameStatus.DRAW) {
            throw new GameException("Match '" + gameName + "' has already finished.");
        }
        if (status != GameStatus.WAITING_FOR_OPPONENT) {
            throw new GameException("Match '" + gameName + "' is already full or in progress.");
        }
        if (playerName.equals(playerXName) || playerName.equals(playerOName)) {
            throw new GameException("Name '" + playerName + "' is already taken in match '" + gameName + "'.");
        }

        if (playerXName == null) {
            playerXName = playerName;
            playerXCallback = callback;
        } else if (playerOName == null) {
            playerOName = playerName;
            playerOCallback = callback;
            status = GameStatus.IN_PROGRESS;
            turn = Symbol.X;
        } else {
            throw new GameException("Match '" + gameName + "' already has two players.");
        }
        return buildSnapshot();
    }

    /**
     * Validates and applies a move played by the specified player and
     * updates match status and turns accordingly.
     *
     * @param playerName the name of the player making the move
     * @param row        the row index of the target cell
     * @param col        the column index of the target cell
     * @return an updated, immutable {@link GameSnapshot} representing the new state
     * @throws GameException if the match is not in progress, it is not the player's turn,
     *                       the target cell is invalid/occupied, or the player is unknown
     */
    public synchronized GameSnapshot makeMove(final String playerName, final int row, final int col) throws GameException {
        if (status != GameStatus.IN_PROGRESS) {
            throw new GameException("Match '" + gameName + "' is not currently in progress.");
        }
        final Symbol mover = symbolOf(playerName);
        if (mover == null) {
            throw new GameException("'" + playerName + "' is not a player of match '" + gameName + "'.");
        }
        if (mover != turn) {
            throw new GameException("It is not " + playerName + "'s turn.");
        }
        if (row < 0 || row >= Board.SIZE || col < 0 || col >= Board.SIZE || !board.isEmpty(row, col)) {
            throw new GameException("Cell (" + row + ", " + col + ") is not a legal move.");
        }

        board = board.withMove(row, col, mover);

        if (WinChecker.isWinner(board, mover)) {
            status = mover == Symbol.X ? GameStatus.X_WON : GameStatus.O_WON;
            turn = null;
        } else if (board.isFull()) {
            status = GameStatus.DRAW;
            turn = null;
        } else {
            turn = mover.opponent();
        }
        return buildSnapshot();
    }

    /**
     * Handles a player leaving the match. If the match is in progress, the remaining player
     * wins by default. If the match is waiting for an opponent, it is marked as abandoned.
     *
     * @param playerName the name of the player leaving
     * @return an updated, immutable {@link GameSnapshot} representing the new state
     * @throws GameException if the player is not part of this match
     */
    public synchronized GameSnapshot leaveGame(final String playerName) throws GameException {
        final Symbol leavingSymbol = symbolOf(playerName);
        if (leavingSymbol == null) {
            throw new GameException("'" + playerName + "' is not a player of match '" + gameName + "'.");
        }

        if (leavingSymbol == Symbol.X) {
            playerXCallback = null;
        } else {
            playerOCallback = null;
        }

        if (status == GameStatus.IN_PROGRESS) {
            status = leavingSymbol == Symbol.X ? GameStatus.O_WON : GameStatus.X_WON;
            turn = null;
        } else if (status == GameStatus.WAITING_FOR_OPPONENT) {
            status = GameStatus.ABANDONED;
            turn = null;
        }
        return buildSnapshot();
    }

    /**
     * Tells whether the match has ended (either abandoned or finished).
     *
     * @return {@code true} if the match is no longer active
     */
    public synchronized boolean isEnded() {
        return status == GameStatus.ABANDONED || status == GameStatus.X_WON || status == GameStatus.O_WON || status == GameStatus.DRAW;
    }

    /**
     * Builds and returns an immutable snapshot of the current match state.
     *
     * @return a new {@link GameSnapshot} capturing the current match details
     */
    public synchronized GameSnapshot buildSnapshot() {
        return new GameSnapshot(gameName, board, status, turn, playerXName, playerOName);
    }

    /**
     * Retrieves the remote callback handle for player X.
     *
     * @return the {@link PlayerCallback} for player X, or {@code null} if not yet registered
     */
    public synchronized PlayerCallback getPlayerXCallback() {
        return playerXCallback;
    }

    /**
     * Retrieves the remote callback handle for player O.
     *
     * @return the {@link PlayerCallback} for player O, or {@code null} if not yet registered
     */
    public synchronized PlayerCallback getPlayerOCallback() {
        return playerOCallback;
    }

    /**
     * Maps a player's name to their assigned {@link Symbol} in this match.
     *
     * @param playerName the name of the player
     * @return the assigned {@link Symbol}, or {@code null} if the player is not part of this match
     */
    private Symbol symbolOf(final String playerName) {
        if (playerName != null && playerName.equals(playerXName)) {
            return Symbol.X;
        }
        if (playerName != null && playerName.equals(playerOName)) {
            return Symbol.O;
        }
        return null;
    }
}