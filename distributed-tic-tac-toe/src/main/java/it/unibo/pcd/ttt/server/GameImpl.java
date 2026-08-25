package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.GameException;
import it.unibo.pcd.ttt.shared.GameSnapshot;
import it.unibo.pcd.ttt.shared.PlayerCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server RMI remote object for a single Tic-Tac-Toe match.
 */
public class GameImpl extends UnicastRemoteObject implements Game {

    private final GameState state;
    private final GameEventNotifier notifier;
    private final Runnable onEndedCleanup;

    /**
     * Creates and exports a new match.
     *
     * @param gameName       the unique name identifying this match
     * @param onEndedCleanup action executed to remove this match from the GameManager upon completion
     * @throws RemoteException if exporting the remote object to the RMI runtime fails
     */
    public GameImpl(final String gameName, final Runnable onEndedCleanup) throws RemoteException {
        super();
        this.state = new GameState(gameName);
        this.notifier = new GameEventNotifier(gameName);
        this.onEndedCleanup = onEndedCleanup;
    }

    /**
     * Adds a new player to this match and broadcasts the updated state snapshot.
     *
     * @param playerName the name of the joining player
     * @param callback   the remote callback handle used for state update push notifications
     * @throws GameException if the name is invalid, already taken, or if the game is full
     */
    void addPlayer(final String playerName, final PlayerCallback callback) throws GameException {
        final GameSnapshot newSnapshot = state.addPlayer(playerName, callback);
        broadcast(newSnapshot);
    }

    /**
     * Makes a move on behalf of the specified player.
     *
     * @param playerName the name of the player attempting the move
     * @param row        the target cell's row index
     * @param col        the target cell's column index
     * @throws GameException if the move violates game rules
     */
    @Override
    public void makeMove(final String playerName, final int row, final int col) throws GameException {
        final GameSnapshot newSnapshot = state.makeMove(playerName, row, col);
        broadcast(newSnapshot);
    }

    /**
     * Handles a player leaving the match and broadcasts the updated state.
     *
     * @param playerName the name of the player leaving
     * @throws GameException if the player is not part of this match
     */
    @Override
    public void leaveGame(final String playerName) throws GameException {
        final GameSnapshot newSnapshot = state.leaveGame(playerName);
        broadcast(newSnapshot);
    }

    /**
     * Tells whether this match has ended.
     *
     * @return {@code true} if the match is ended/abandoned
     */
    public boolean isEnded() {
        return state.isEnded();
    }

    private void broadcast(final GameSnapshot snapshot) {
        if (state.isEnded()) {
            notifier.notifyUpdateAndShutdown(snapshot, state.getPlayerXCallback(), state.getPlayerOCallback(), () -> {
                if (onEndedCleanup != null) {
                    onEndedCleanup.run();
                }
                try {
                    UnicastRemoteObject.unexportObject(this, true);
                } catch (final Exception ignored) {}
            });
        } else {
            notifier.notifyUpdate(snapshot, state.getPlayerXCallback(), state.getPlayerOCallback());
        }
    }
}