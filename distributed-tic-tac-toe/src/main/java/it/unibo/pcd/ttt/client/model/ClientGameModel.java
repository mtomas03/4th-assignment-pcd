package it.unibo.pcd.ttt.client.model;

import it.unibo.pcd.ttt.shared.GameSnapshot;
import it.unibo.pcd.ttt.shared.Symbol;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Client Model.
 */
public class ClientGameModel {

    private final List<GameModelListener> listeners = new CopyOnWriteArrayList<>();

    private String localPlayerName;
    private GameSnapshot snapshot;
    private String lastError;

    /**
     * Registers a listener to be notified of every state change.
     *
     * @param listener the listener to add
     */
    public void addListener(final GameModelListener listener) {
        listeners.add(listener);
    }

    /**
     * @return the local player's name, or {@code null} before matchmaking
     */
    public String getLocalPlayerName() {
        return localPlayerName;
    }

    /**
     * Sets the name of the local player.
     *
     * @param localPlayerName the local player's name
     */
    public void setLocalPlayerName(final String localPlayerName) {
        this.localPlayerName = localPlayerName;
        fireChanged();
    }

    /**
     * @return the latest known snapshot of the active match, or {@code null} if none
     */
    public GameSnapshot getSnapshot() {
        return snapshot;
    }

    /**
     * Sets the current match snapshot.
     *
     * @param snapshot the new snapshot
     */
    public void setSnapshot(final GameSnapshot snapshot) {
        this.snapshot = snapshot;
        fireChanged();
    }

    /**
     * @return the last error message recorded, or {@code null}
     */
    public String getLastError() {
        return lastError;
    }

    /**
     * Records an error message coming from a failed operation, to be
     * displayed by the view.
     *
     * @param message a human-readable error description
     */
    public void setLastError(final String message) {
        this.lastError = message;
        fireChanged();
    }

    /**
     * @return the {@link Symbol} owned by the local player in the current match, or {@code null}
     */
    public Symbol getLocalSymbol() {
        return snapshot == null ? null : snapshot.symbolOf(localPlayerName);
    }

    private void fireChanged() {
        for (final GameModelListener listener : listeners) {
            listener.onModelChanged(this);
        }
    }
}
