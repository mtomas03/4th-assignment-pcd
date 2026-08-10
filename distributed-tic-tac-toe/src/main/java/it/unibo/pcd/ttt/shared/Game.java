package it.unibo.pcd.ttt.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Remote interface of a single Tic-Tac-Toe match.
 */
public interface Game extends Remote {

    /**
     * Makes a move on behalf of {@code playerName}.
     *
     * @param playerName the name of the player making the move; must be
     *                   one of the two players of this match
     * @param row        the row index of the target cell
     * @param col        the column index of the target cell
     * @throws RemoteException if the remote call fails
     * @throws GameException   if the move is against the game rules
     */
    void makeMove(String playerName, int row, int col) throws RemoteException, GameException;
}
