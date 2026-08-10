package it.unibo.pcd.ttt.shared;

/**
 * Stateless utility class that decides whether a {@link Symbol}
 * has completed a winning line on a {@link Board}.
 */
public final class WinChecker {

    private WinChecker() {
    }

    /**
     * Tells whether {@code symbol} occupies a full row, column, or diagonal
     * of {@code board}.
     *
     * @param board  the board to inspect
     * @param symbol the symbol whose lines are being checked
     * @return {@code true} if {@code symbol} has a winning line on the board
     */
    public static boolean isWinner(final Board board, final Symbol symbol) {
        for (int i = 0; i < Board.SIZE; i++) {
            if (line(board, symbol, i, 0, i, 1, i, 2)) {
                return true;
            }
            if (line(board, symbol, 0, i, 1, i, 2, i)) {
                return true;
            }
        }
        return line(board, symbol, 0, 0, 1, 1, 2, 2) || line(board, symbol, 0, 2, 1, 1, 2, 0);
    }

    private static boolean line(final Board board, final Symbol symbol,
                                final int r1, final int c1,
                                final int r2, final int c2,
                                final int r3, final int c3) {
        return board.get(r1, c1) == symbol && board.get(r2, c2) == symbol && board.get(r3, c3) == symbol;
    }
}
