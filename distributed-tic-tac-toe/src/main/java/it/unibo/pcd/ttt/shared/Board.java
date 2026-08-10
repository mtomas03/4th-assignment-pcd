package it.unibo.pcd.ttt.shared;

import java.io.Serializable;
import java.util.Arrays;

/**
 * An immutable Tic-Tac-Toe grid.
 */
public final class Board implements Serializable {

    public static final int SIZE = 3;

    private final Symbol[][] cells;

    private Board(final Symbol[][] cells) {
        this.cells = cells;
    }

    /**
     * Creates a new empty board.
     *
     * @return a board with every cell unmarked
     */
    public static Board empty() {
        return new Board(new Symbol[SIZE][SIZE]);
    }

    private static void checkBounds(final int row, final int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) {
            throw new IllegalArgumentException("Cell (" + row + ", " + col + ") is out of bounds.");
        }
    }

    /**
     * Returns the symbol occupying the given cell.
     *
     * @param row the row index, from 0 to {@link #SIZE} - 1
     * @param col the column index, from 0 to {@link #SIZE} - 1
     * @return the symbol in the cell, or {@code null} if the cell is empty
     */
    public Symbol get(final int row, final int col) {
        checkBounds(row, col);
        return cells[row][col];
    }

    /**
     * Tells whether the given cell has not been played yet.
     *
     * @param row the row index
     * @param col the column index
     * @return {@code true} if the cell is empty
     */
    public boolean isEmpty(final int row, final int col) {
        return get(row, col) == null;
    }

    /**
     * Tells whether every cell of the board has been played.
     *
     * @return {@code true} if there is no empty cell left
     */
    public boolean isFull() {
        for (final Symbol[] row : cells) {
            for (final Symbol cell : row) {
                if (cell == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Returns a new immutable board equal to the current one, but with the
     * specified move applied to the target cell.
     *
     * @param row    the row index of the move
     * @param col    the column index of the move
     * @param symbol the symbol to place on the board
     * @return a new immutable {@code Board} with the move applied
     * @throws IndexOutOfBoundsException if row or column indices are out of bounds
     * @throws IllegalStateException     if the target cell is already occupied
     */
    public Board withMove(final int row, final int col, final Symbol symbol) {
        checkBounds(row, col);
        if (cells[row][col] != null) {
            throw new IllegalStateException("Cell (" + row + ", " + col + ") is already occupied.");
        }
        final Symbol[][] copy = new Symbol[SIZE][];
        for (int r = 0; r < SIZE; r++) {
            copy[r] = Arrays.copyOf(cells[r], SIZE);
        }
        copy[row][col] = symbol;
        return new Board(copy);
    }
}
