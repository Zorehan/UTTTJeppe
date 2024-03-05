package dk.easv.bll.bot;

import dk.easv.bll.game.GameManager;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;

public class MinimaxBot implements IBot {

    private static final String BOTNAME = "Minimax Bot";

    @Override
    public IMove doMove(IGameState state) {
        List<IMove> moves = state.getField().getAvailableMoves();
        if (moves.size() > 0) {
            return minimax(state, 4); // Adjust depth as needed
        }
        return null;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }

    // Minimax algorithm with alpha-beta pruning
    private IMove minimax(IGameState state, int depth) {
        List<IMove> moves = state.getField().getAvailableMoves();
        long score = Integer.MIN_VALUE;
        IMove bestMove = null;
        for (IMove move : moves) {
            IGameState clonedState = new GameState(state);
            clonedState.getField().getBoard()[move.getX()][move.getY()] = "X"; // Assuming player is "X"
            long currentScore = alphaBeta(clonedState, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, false, move);
            if (currentScore > score) {
                score = currentScore;
                bestMove = move;
            }
            if (score == Long.MAX_VALUE || score == Long.MIN_VALUE) {
                break; // Terminal state reached
            }
        }
        return bestMove;
    }

    // Alpha-beta pruning
    private long alphaBeta(IGameState state, int depth, long alpha, long beta, boolean maximizingPlayer, IMove lastMove) {
        if (depth == 0) {
            return evaluateGameState(state, lastMove);
        }

        List<IMove> moves = state.getField().getAvailableMoves();
        if (maximizingPlayer) {
            long value = Long.MIN_VALUE;
            for (IMove move : moves) {
                IGameState clonedState = new GameState(state);
                clonedState.getField().getBoard()[move.getX()][move.getY()] = "X"; // Assuming player is "X"
                value = Math.max(value, alphaBeta(clonedState, depth - 1, alpha, beta, false, move));
                alpha = Math.max(alpha, value);
                if (alpha >= beta) {
                    break;
                }
            }
            return value;
        } else {
            long value = Long.MAX_VALUE;
            for (IMove move : moves) {
                IGameState clonedState = new GameState(state);
                clonedState.getField().getBoard()[move.getX()][move.getY()] = "O"; // Assuming opponent is "O"
                value = Math.min(value, alphaBeta(clonedState, depth - 1, alpha, beta, true, move));
                beta = Math.min(beta, value);
                if (alpha >= beta) {
                    break;
                }
            }
            return value;
        }
    }

    private long evaluateGameState(IGameState state, IMove lastMove) {
        String[][] board = state.getField().getBoard();
        String currentPlayer = "X"; // Assuming the current player is "X"

        // Check if the current player has won
        if (GameManager.isWin(board, lastMove, currentPlayer)) {
            return Long.MAX_VALUE; // Return a maximum value indicating a winning position
        }

        // Check if the opponent has won
        String opponentPlayer = "O"; // Assuming the opponent player is "O"
        if (GameManager.isWin(board, lastMove, opponentPlayer)) {
            return Long.MIN_VALUE; // Return a minimum value indicating a losing position
        }

        // Evaluate based on control of center, corners, and edges
        long score = 0;
        score += evaluatePosition(board, 1, 1, currentPlayer); // Center
        score += evaluatePosition(board, 0, 0, currentPlayer); // Top-left corner
        score += evaluatePosition(board, 0, 2, currentPlayer); // Top-right corner
        score += evaluatePosition(board, 2, 0, currentPlayer); // Bottom-left corner
        score += evaluatePosition(board, 2, 2, currentPlayer); // Bottom-right corner

        // Evaluate edges
        score += evaluatePosition(board, 1, 0, currentPlayer); // Top edge
        score += evaluatePosition(board, 0, 1, currentPlayer); // Left edge
        score += evaluatePosition(board, 1, 2, currentPlayer); // Bottom edge
        score += evaluatePosition(board, 2, 1, currentPlayer); // Right edge

        return score;
    }

    private long evaluatePosition(String[][] board, int x, int y, String player) {
        long value = 0;
        if (board[x][y].equals(player)) {
            // Assign higher values to center positions
            if ((x == 1 && y == 1)) {
                value += 6;
            }
            // Assign medium values to corner positions
            else if ((x == 0 && y == 0) || (x == 0 && y == 2) || (x == 2 && y == 0) || (x == 2 && y == 2)) {
                value += 5;
            } else {
                // Assign lower values to edge positions
                value += 4;
            }
        }
        return value;
    }
}