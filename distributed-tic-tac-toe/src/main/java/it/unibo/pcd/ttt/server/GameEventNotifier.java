package it.unibo.pcd.ttt.server;

import it.unibo.pcd.ttt.shared.GameSnapshot;
import it.unibo.pcd.ttt.shared.PlayerCallback;

import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@code GameEventNotifier} is an active component dedicated exclusively in
 * delivering game state updates asynchronously to players' remote callbacks.
 *
 * <p> It encapsulates a single-threaded {@link ExecutorService} to ensure that
 * remote RMI callbacks never block the server's state transitions and that
 * state snapshots are delivered sequentially in the exact order they were produced.
 */
public class GameEventNotifier {

    private static final Logger LOGGER = Logger.getLogger(GameEventNotifier.class.getName());
    private final ExecutorService executor;

    /**
     * Creates a new event notifier for a specific match.
     *
     * @param gameName the unique name of the match, used to name the underlying daemon thread
     */
    public GameEventNotifier(final String gameName) {
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            final Thread thread = new Thread(runnable, "ttt-notifier-" + gameName);
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Sends state snapshot to both player callbacks asynchronously.
     *
     * @param snapshot the immutable game state snapshot to deliver
     * @param playerX  the remote callback registered for player X, or {@code null} if not assigned
     * @param playerO  the remote callback registered for player O, or {@code null} if not assigned
     */
    public void notifyUpdate(final GameSnapshot snapshot, final PlayerCallback playerX, final PlayerCallback playerO) {
        if (snapshot == null) {
            return;
        }
        executor.execute(() -> {
            deliver(playerX, snapshot, "X");
            deliver(playerO, snapshot, "O");
        });
    }

    /**
     * Shuts down the background notification thread pool.
     */
    public void shutdown() {
        executor.shutdown();
    }

    /**
     * Executes a single remote callback invocation safely.
     *
     * <p> Any {@link RemoteException} thrown during delivery is logged, preventing network failure on one
     * client from disrupting the other participant or the server.
     *
     * @param callback the remote callback to notify, if non-null
     * @param snapshot the snapshot to deliver
     * @param label    a descriptive label ("X" or "O") used in logging messages
     */
    private void deliver(final PlayerCallback callback, final GameSnapshot snapshot, final String label) {
        if (callback == null) {
            return;
        }
        try {
            callback.onSnapshotUpdate(snapshot);
        } catch (final RemoteException e) {
            LOGGER.log(Level.WARNING, "Failed to notify player " + label + " of match '" + snapshot.gameName() + "'.", e);
        }
    }
}