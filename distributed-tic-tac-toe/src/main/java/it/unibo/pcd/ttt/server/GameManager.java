package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.GameException;
import it.unibo.pcd.ttt.shared.PlayerCallback;
import it.unibo.pcd.ttt.shared.Symbol;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface for the entry point of the Tic-Tac-Toe matchmaking service.
 */
public interface GameManager extends Remote {

    String REGISTRY_NAME = "TTT_GAME_MANAGER";

    /**
     * Creates a new match with the given name and immediately registers the
     * caller as its first player (symbol {@link Symbol#X}), waiting for an
     * opponent.
     *
     * @param gameName   the unique name of the new match
     * @param playerName the name of the creating player
     * @param callback   the remote callback the server will use to push
     *                   state updates to this client for this match
     * @return a remote reference to the newly created match
     * @throws RemoteException if the remote call fails
     * @throws GameException   if {@code gameName} is blank or already taken
     */
    Game createGame(String gameName, String playerName, PlayerCallback callback) throws RemoteException, GameException;

    /**
     * Joins an existing match, given its name, as its second player
     * (symbol {@link Symbol#O}), which also starts the match.
     *
     * @param gameName   the unique name of the match to join
     * @param playerName the name of the joining player
     * @param callback   the remote callback the server will use to push
     *                   state updates to this client for this match
     * @return a remote reference to the joined match
     * @throws RemoteException if the remote call fails
     * @throws GameException   if the match does not exist, is already full,
     *                         or {@code playerName} is blank or already used
     *                         in that match
     */
    Game joinGame(String gameName, String playerName, PlayerCallback callback) throws RemoteException, GameException;
}
