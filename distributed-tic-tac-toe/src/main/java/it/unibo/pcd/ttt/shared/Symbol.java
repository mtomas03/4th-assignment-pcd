package it.unibo.pcd.ttt.shared;

/**
 * The two marks that can occupy a cell of a Tic-Tac-Toe {@link Board}.
 */
public enum Symbol {

    X,
    O;

    /**
     * Returns the symbol of the opposing player.
     *
     * @return {@link #O} if this is {@link #X}, {@link #X} otherwise
     */
    public Symbol opponent() {
        return this == X ? O : X;
    }
}
