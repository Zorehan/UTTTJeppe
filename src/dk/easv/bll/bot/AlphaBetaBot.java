package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameManager;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;

import java.util.List;
import java.util.Random;

public class AlphaBetaBot implements IBot {
    private static final int DEPTH = 3; // Search depth
    private static final int[][] smallBoardScores = {
            {10_000, -10_000}, // Win/Lose
            {5, 3, 3, 3, 3}, // Heuristic 2 scores
            {2, 4} // Heuristic 3 scores
    };

    private static final Random random = new Random();

    @Override
    public IMove doMove(IGameState state) {
        IField field = state.getField();
        List<IMove> availableMoves = field.getAvailableMoves();

        if (!availableMoves.isEmpty()) {
            IMove bestMove = null;
            double bestScore = Double.NEGATIVE_INFINITY;

            int currentPlayer = getCurrentPlayer(state);

            //System.out.println("Current player: " + currentPlayer);

            for (IMove move : availableMoves) {
               // System.out.println("Considering move: " + move.getX() + ", " + move.getY());
                double score = alphaBeta(field, move, DEPTH, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, true, currentPlayer);
                //System.out.println("Move score: " + score);
                if (score > bestScore) {
                    bestScore = score;
                    bestMove = move;
                }
            }

            //System.out.println("Best move: " + bestMove.getX() + ", " + bestMove.getY());
            return bestMove;
        }

        return null; // No moves available
    }

    private double alphaBeta(IField field, IMove move, int depth, double alpha, double beta, boolean maximizingPlayer, int currentPlayer) {
        IGameState clonedState = cloneState(field);
        simulateMove(clonedState, move);

        if (depth == 0 || clonedState.getField().getAvailableMoves().isEmpty()) {
            return evaluateState(clonedState);
        }

        List<IMove> availableMoves = clonedState.getField().getAvailableMoves();

        if (maximizingPlayer) {
            double maxEval = Double.NEGATIVE_INFINITY;
            for (IMove nextMove : availableMoves) {
                double eval = alphaBeta(clonedState.getField(), nextMove, depth - 1, alpha, beta, false, (currentPlayer + 1) % 2);
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            double minEval = Double.POSITIVE_INFINITY;
            for (IMove nextMove : availableMoves) {
                double eval = alphaBeta(clonedState.getField(), nextMove, depth - 1, alpha, beta, true, (currentPlayer + 1) % 2);
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private IGameState cloneState(IField field) {
        IGameState clonedState = new GameState();
        clonedState.getField().setBoard(field.getBoard());
        clonedState.getField().setMacroboard(field.getMacroboard());
        return clonedState;
    }

    private void simulateMove(IGameState state, IMove move) {
        int currentPlayer = getCurrentPlayer(state);
        state.getField().getBoard()[move.getX()][move.getY()] = currentPlayer == 0 ? "0" : "1";
    }

    private int getCurrentPlayer(IGameState state) {
        IField field = state.getField();
        int totalMoves = field.getAvailableMoves().size();
        return totalMoves % 2; // If totalMoves is even, it's Player 0's turn; otherwise, it's Player 1's turn
    }

    private double evaluateState(IGameState state) {
        // Use heuristic 3 for evaluation
        int[] scores = calculateHeuristic3(state.getField());
        System.out.println("Heuristic 3 scores: Player 0: " + scores[0] + ", Player 1: " + scores[1]);
        return scores[0] - scores[1];
    }

    private int[] calculateHeuristic3(IField field) {
        int[] scores = new int[2]; // Player 0 and Player 1 scores

        for (int i = 0; i < 9; i += 3) {
            for (int j = 0; j < 9; j += 3) {
                int score = smallBoardScores[2][getSmallBoardIndex(field, i, j)];
                System.out.println("Small board score at (" + i + ", " + j + "): " + score);
                if (field.getMacroboard()[i / 3][j / 3].equals("0")) {
                    scores[0] += score;
                } else if (field.getMacroboard()[i / 3][j / 3].equals("1")) {
                    scores[1] += score;
                }
            }
        }

        return scores;
    }

    private int getSmallBoardIndex(IField field, int row, int col) {
        int index = 0;
        int power = 1;

        for (int i = row; i < row + 3; i++) {
            for (int j = col; j < col + 3; j++) {
                if (field.getBoard()[i][j].equals("0")) {
                    index += power;
                } else if (field.getBoard()[i][j].equals("1")) {
                    index += 2 * power;
                }
                power *= 3;
            }
        }

        int result = index % 2;
        System.out.println("Small board index at (" + row + ", " + col + "): " + result);
        return result; // Ensure the index is in the range of 0 to 1
    }

    @Override
    public String getBotName() {
        return "Alpha Beta Bot";
    }
}