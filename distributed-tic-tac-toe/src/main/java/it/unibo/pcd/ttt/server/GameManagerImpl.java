package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.GameException;
import it.unibo.pcd.ttt.shared.PlayerCallback;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Server implementation of the {@link GameManager} remote interface.
 *
 * <p> This class serves as the single entry-point object registered in the RMI registry.
 * It manages the registry of active matches and handles the initial matchmaking logic
 * (creating matches with unique names and registering up to two players per match).
 * Actual match execution and state transitions are fully delegated to individual {@link GameImpl}
 * instances.
 */
public class GameManagerImpl extends UnicastRemoteObject implements GameManager {

    private final ConcurrentMap<String, GameImpl> games = new ConcurrentHashMap<>();

    /**
     * Creates and exports a new {@code GameManagerImpl} instance.
     *
     * @throws RemoteException if exporting the remote object to the RMI runtime fails
     */
    public GameManagerImpl() throws RemoteException {
        super();
    }

    /**
     * Creates a new match with the given name and registers its first player.
     * If a match with the given name exists but was abandoned or finished, it is replaced.
     *
     * @param gameName   the unique name for the match to create
     * @param playerName the name of the player creating the match
     * @param callback   the remote callback handle for push updates
     * @return the remote {@link Game} reference for the created match
     * @throws RemoteException if an RMI communication failure occurs
     * @throws GameException   if the game name is invalid, an active match with that name exists,
     *                         or the player cannot be added
     */
    @Override
    public Game createGame(final String gameName, final String playerName, final PlayerCallback callback)
            throws RemoteException, GameException {
        if (gameName == null || gameName.isBlank()) {
            throw new GameException("Game name must not be empty.");
        }

        final GameImpl existing = games.get(gameName);
        if (existing != null) {
            if (existing.isEnded()) {
                games.remove(gameName, existing);
            } else {
                throw new GameException("A match named '" + gameName + "' already exists.");
            }
        }

        final GameImpl newGame = new GameImpl(gameName, () -> games.remove(gameName));
        final GameImpl previous = games.putIfAbsent(gameName, newGame);
        if (previous != null && !previous.isEnded()) {
            UnicastRemoteObject.unexportObject(newGame, true);
            throw new GameException("A match named '" + gameName + "' already exists.");
        }

        try {
            newGame.addPlayer(playerName, callback);
        } catch (final GameException e) {
            games.remove(gameName, newGame);
            UnicastRemoteObject.unexportObject(newGame, true);
            throw e;
        }
        return newGame;
    }

    /**
     * Joins an existing match as the second player.
     *
     * @param gameName   the unique name of the match to join
     * @param playerName the name of the player joining the match
     * @param callback   the remote callback handle for push updates
     * @return the remote {@link Game} reference for the joined match
     * @throws RemoteException if an RMI communication failure occurs
     * @throws GameException   if no match with the specified name exists, or if player registration fails
     */
    @Override
    public Game joinGame(final String gameName, final String playerName, final PlayerCallback callback)
            throws RemoteException, GameException {
        final GameImpl game = games.get(gameName);
        if (game == null) {
            throw new GameException("No match named '" + gameName + "' exists.");
        }
        game.addPlayer(playerName, callback);
        return game;
    }
}