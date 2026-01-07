package com.example.chess.game;

import java.util.List;

public final class ChessRules {
    private ChessRules() {
    }

    public static BoardState buildBoard(List<MoveEntity> moves) {
        BoardState state = BoardState.initial();
        for (MoveEntity move : moves) {
            state.applyMove(move.getFromSquare(), move.getToSquare(), move.getPiece());
        }
        return state;
    }

    public static boolean isLegalMove(BoardState state, String from, String to) {
        Square fromSq = Square.parse(from);
        Square toSq = Square.parse(to);
        if (fromSq == null || toSq == null) {
            return false;
        }
        if (fromSq.row() == toSq.row() && fromSq.col() == toSq.col()) {
            return false;
        }
        String piece = state.getPiece(fromSq);
        if (piece == null || piece.isEmpty()) {
            return false;
        }
        String target = state.getPiece(toSq);
        if (target != null && !target.isEmpty() && sameColor(piece, target)) {
            return false;
        }

        char type = piece.charAt(1);
        int dr = toSq.row() - fromSq.row();
        int dc = toSq.col() - fromSq.col();

        return switch (type) {
            case 'P' -> pawnMove(state, piece, fromSq, toSq, dr, dc, target);
            case 'R' -> rookMove(state, fromSq, toSq, dr, dc);
            case 'N' -> knightMove(dr, dc);
            case 'B' -> bishopMove(state, fromSq, toSq, dr, dc);
            case 'Q' -> queenMove(state, fromSq, toSq, dr, dc);
            case 'K' -> kingMove(dr, dc);
            default -> false;
        };
    }

    private static boolean pawnMove(BoardState state, String piece, Square from, Square to, int dr, int dc, String target) {
        int direction = piece.startsWith("w") ? -1 : 1;
        int startRow = piece.startsWith("w") ? 6 : 1;

        if (dc == 0) {
            if (dr == direction && isEmpty(target)) {
                return true;
            }
            if (dr == 2 * direction && from.row() == startRow) {
                Square mid = new Square(from.row() + direction, from.col());
                return isEmpty(state.getPiece(mid)) && isEmpty(target);
            }
            return false;
        }
        if (Math.abs(dc) == 1 && dr == direction) {
            return !isEmpty(target) && !sameColor(piece, target);
        }
        return false;
    }

    private static boolean rookMove(BoardState state, Square from, Square to, int dr, int dc) {
        if (dr != 0 && dc != 0) {
            return false;
        }
        return state.isPathClear(from, to);
    }

    private static boolean bishopMove(BoardState state, Square from, Square to, int dr, int dc) {
        if (Math.abs(dr) != Math.abs(dc)) {
            return false;
        }
        return state.isPathClear(from, to);
    }

    private static boolean queenMove(BoardState state, Square from, Square to, int dr, int dc) {
        if (dr == 0 || dc == 0) {
            return state.isPathClear(from, to);
        }
        if (Math.abs(dr) == Math.abs(dc)) {
            return state.isPathClear(from, to);
        }
        return false;
    }

    private static boolean knightMove(int dr, int dc) {
        int adr = Math.abs(dr);
        int adc = Math.abs(dc);
        return (adr == 2 && adc == 1) || (adr == 1 && adc == 2);
    }

    private static boolean kingMove(int dr, int dc) {
        return Math.abs(dr) <= 1 && Math.abs(dc) <= 1;
    }

    private static boolean sameColor(String a, String b) {
        return a.charAt(0) == b.charAt(0);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    public record Square(int row, int col) {
        public static Square parse(String square) {
            if (square == null || square.length() != 2) {
                return null;
            }
            char file = square.charAt(0);
            char rankChar = square.charAt(1);
            if (file < 'a' || file > 'h' || rankChar < '1' || rankChar > '8') {
                return null;
            }
            int col = file - 'a';
            int rank = rankChar - '0';
            int row = 8 - rank;
            return new Square(row, col);
        }

        public String toNotation() {
            char file = (char) ('a' + col);
            int rank = 8 - row;
            return "" + file + rank;
        }
    }

    public static final class BoardState {
        private final String[][] board;

        private BoardState(String[][] board) {
            this.board = board;
        }

        public static BoardState initial() {
            String[][] board = new String[8][8];
            for (int row = 0; row < 8; row++) {
                for (int col = 0; col < 8; col++) {
                    board[row][col] = "";
                }
            }
            board[0] = new String[] { "bR", "bN", "bB", "bQ", "bK", "bB", "bN", "bR" };
            board[1] = new String[] { "bP", "bP", "bP", "bP", "bP", "bP", "bP", "bP" };
            board[6] = new String[] { "wP", "wP", "wP", "wP", "wP", "wP", "wP", "wP" };
            board[7] = new String[] { "wR", "wN", "wB", "wQ", "wK", "wB", "wN", "wR" };
            return new BoardState(board);
        }

        public String getPiece(Square square) {
            return board[square.row()][square.col()];
        }

        public void applyMove(String from, String to, String piece) {
            Square fromSq = Square.parse(from);
            Square toSq = Square.parse(to);
            if (fromSq == null || toSq == null) {
                return;
            }
            String moving = piece;
            if (moving == null || moving.isEmpty()) {
                moving = getPiece(fromSq);
            }
            board[fromSq.row()][fromSq.col()] = "";
            board[toSq.row()][toSq.col()] = moving;
        }

        public boolean isPathClear(Square from, Square to) {
            int dr = Integer.signum(to.row() - from.row());
            int dc = Integer.signum(to.col() - from.col());
            int r = from.row() + dr;
            int c = from.col() + dc;
            while (r != to.row() || c != to.col()) {
                if (!board[r][c].isEmpty()) {
                    return false;
                }
                r += dr;
                c += dc;
            }
            return true;
        }
    }
}
