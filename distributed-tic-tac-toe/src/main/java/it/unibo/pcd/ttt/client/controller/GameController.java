package it.unibo.pcd.ttt.client.controller;

import it.unibo.pcd.ttt.client.model.ClientGameModel;
import it.unibo.pcd.ttt.shared.*;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The Client Controller.
 *
 * <p> The {@code GameController} is an active component that plays two roles:
 * <ol>
 *   <li>Outbound direction: {@link #createGame}, {@link #joinGame}, {@link #makeMove}
 *       are invoked by the view in reaction to a user action.
 *       Because RMI calls are blocking, none of them run on the EDT:
 *       they are submitted to a private single-thread
 *       {@link ExecutorService}, so the GUI never freezes while
 *       waiting for the server.</li>
 *   <li>Inbound direction: this class implements {@link PlayerCallback}
 *       and exports itself as a remote object ({@link UnicastRemoteObject}),
 *       so the server can call {@link #onSnapshotUpdate(GameSnapshot)} directly
 *       whenever the match state changes.</li>
 * </ol>
 */
public class GameController extends UnicastRemoteObject implements PlayerCallback {

    private final GameManager gameManager;
    private final ClientGameModel model;
    private final ExecutorService networkExecutor;

    private volatile Game currentGame;
    private volatile String playerName;

    /**
     * Creates and exports the controller.
     *
     * @param gameManager the remote stub of the matchmaking entry point
     * @param model       the client-side model this controller feeds
     * @throws RemoteException if this object cannot be exported
     */
    public GameController(final GameManager gameManager, final ClientGameModel model) throws RemoteException {
        super();
        this.gameManager = gameManager;
        this.model = model;
        this.networkExecutor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "ttt-client-network");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Asks the server to create a new match and registers the local player
     * as its first participant.
     *
     * @param gameName     the name of the match to create
     * @param playerName   the local player's display name
     * @param onCompletion invoked on the EDT once the operation completes:
     *                     with {@code null} on success, or an error message
     *                     on failure
     */
    public void createGame(final String gameName, final String playerName, final Consumer<String> onCompletion) {
        networkExecutor.execute(() -> {
            try {
                this.playerName = playerName;
                SwingUtilities.invokeLater(() -> model.setLocalPlayerName(playerName));
                currentGame = gameManager.createGame(gameName, playerName, this);
                completeOnEdt(onCompletion, null);
            } catch (final RemoteException | GameException e) {
                completeOnEdt(onCompletion, e.getMessage());
            }
        });
    }

    /**
     * Asks the server to join an existing match as its second participant.
     *
     * @param gameName     the name of the match to join
     * @param playerName   the local player's display name
     * @param onCompletion invoked on the EDT once the operation completes:
     *                     with {@code null} on success, or an error message
     *                     on failure
     */
    public void joinGame(final String gameName, final String playerName, final Consumer<String> onCompletion) {
        networkExecutor.execute(() -> {
            try {
                this.playerName = playerName;
                SwingUtilities.invokeLater(() -> model.setLocalPlayerName(playerName));
                currentGame = gameManager.joinGame(gameName, playerName, this);
                completeOnEdt(onCompletion, null);
            } catch (final RemoteException | GameException e) {
                completeOnEdt(onCompletion, e.getMessage());
            }
        });
    }

    /**
     * Attempts to play a move in the current match.
     *
     * @param row the row index of the target cell
     * @param col the column index of the target cell
     */
    public void makeMove(final int row, final int col) {
        final Game game = currentGame;
        if (game == null) {
            return;
        }
        networkExecutor.execute(() -> {
            try {
                game.makeMove(playerName, row, col);
            } catch (final RemoteException | GameException e) {
                SwingUtilities.invokeLater(() -> model.setLastError(e.getMessage()));
            }
        });
    }

    /**
     * Releases the network resources owned by this controller and
     * unexports it from the RMI runtime. Should be called once, when the
     * client application is shutting down.
     */
    public void shutdown() {
        networkExecutor.shutdown();
        try {
            UnicastRemoteObject.unexportObject(this, true);
        } catch (final java.rmi.NoSuchObjectException ignored) {
            // Already unexported.
        }
    }

    @Override
    public void onSnapshotUpdate(final GameSnapshot snapshot) {
        SwingUtilities.invokeLater(() -> model.setSnapshot(snapshot));
    }

    private void completeOnEdt(final Consumer<String> onCompletion, final String errorOrNull) {
        if (onCompletion != null) {
            SwingUtilities.invokeLater(() -> onCompletion.accept(errorOrNull));
        }
    }
}
