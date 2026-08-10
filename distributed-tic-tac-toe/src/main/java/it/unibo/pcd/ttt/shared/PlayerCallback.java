package it.unibo.pcd.ttt.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote listener interface for receiving push notifications from the game server.
 */
public interface PlayerCallback extends Remote {

    /**
     * Notifies the client that the state of the match has changed.
     *
     * @param snapshot the updated and immutable state of the match
     * @throws RemoteException if the communication with the client fails
     */
    void onSnapshotUpdate(GameSnapshot snapshot) throws RemoteException;
}
