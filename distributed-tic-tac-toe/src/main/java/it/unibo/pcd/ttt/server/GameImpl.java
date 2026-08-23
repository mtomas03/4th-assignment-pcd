package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.Game;
import it.unibo.pcd.ttt.shared.GameException;
import it.unibo.pcd.ttt.shared.GameSnapshot;
import it.unibo.pcd.ttt.shared.PlayerCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Server RMI remote object for a single Tic-Tac-Toe match.
 */
public class GameImpl extends UnicastRemoteObject implements Game {

    private final String gameName;
    private final GameState state;
    private final GameEventNotifier notifier;

    /**
     * Creates and exports a new match.
     *
     * @param gameName the unique name identifying this match
     * @throws RemoteException if exporting the remote object to the RMI runtime fails
     */
    public GameImpl(final String gameName) throws RemoteException {
        super();
        this.gameName = gameName;
        this.state = new GameState(gameName);
        this.notifier = new GameEventNotifier(gameName);
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

    /**
     * Retrieves current player callbacks from the state and passes
     * the new snapshot to the asynchronous notification pipeline.
     *
     * @param snapshot the updated game state snapshot to broadcast
     */
    private void broadcast(final GameSnapshot snapshot) {
        notifier.notifyUpdate(snapshot, state.getPlayerXCallback(), state.getPlayerOCallback());
    }
}