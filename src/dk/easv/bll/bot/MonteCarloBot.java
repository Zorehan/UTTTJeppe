package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MonteCarloBot implements IBot {

    private static final String BOTNAME = "Monte Carlo Bot";
    private Random rand = new Random();
    private static final int SIMULATION_COUNT = 100;
    private static final int TIME_LIMIT = 1000; // 1000 milliseconds
    private static final double EXPLORATION_PARAMETER = 1.414; // UCT exploration parameter

    @Override
    public IMove doMove(IGameState state) {
        List<IMove> moves = state.getField().getAvailableMoves();

        System.out.println("Available Moves: " + moves.size());

        if (!moves.isEmpty()) {
            IMove bestMove = null;
            double bestUCBValue = Double.NEGATIVE_INFINITY;

            // Perform Monte Carlo Tree Search
            for (IMove move : moves) {
                long startTime = System.currentTimeMillis(); // Reset startTime for each move
                int simulations = 0;
                double moveUCBValue = 0; // Initialize move-specific UCB value

                // Perform simulations until time limit is reached
                while (System.currentTimeMillis() - startTime < TIME_LIMIT) {
                    simulations++;
                    IGameState simulatedState = new GameState(state);
                    int score = uctSimulateMove(simulatedState, move, simulations);
                    double ucbValue = calculateUCBValue(simulations, score);

                    // Update move-specific UCB value
                    moveUCBValue += ucbValue; // Accumulate UCB values for this move
                }

                // Calculate average UCB value for the move
                moveUCBValue /= simulations;

                // Update best move if current move has higher average UCB value
                if (moveUCBValue > bestUCBValue) {
                    bestUCBValue = moveUCBValue;
                    bestMove = move;
                }

                System.out.println("Move: " + move.getX() + ", " + move.getY() + " - Average UCB Value: " + moveUCBValue);
            }

            System.out.println("Best Move: " + bestMove.getX() + ", " + bestMove.getY() + " with UCB value " + bestUCBValue);

            return bestMove;
        }

        return null;
    }

    private int uctSimulateMove(IGameState state, IMove move, int simulations) {
        // Simulate moves using UCT strategy
        int totalScore = 0;
        for (int i = 0; i < simulations; i++) {
            System.out.println("Simulating move: " + move.getX() + ", " + move.getY() + " - Simulation: " + (i + 1) + "/" + simulations);
            IGameState simulatedState = new GameState(state);
            simulateRandomMoves(simulatedState, move);
            int score = evaluateState(simulatedState);
            totalScore += score;
        }
        return totalScore;
    }

    private void simulateRandomMoves(IGameState state, IMove initialMove) {
        String currentPlayerId = getCurrentPlayerId(state);

        IGameState currentState = new GameState(state); // Create a copy of the initial state

        // Apply the initial move
        currentState.getField().getMacroboard()[initialMove.getX() / 3][initialMove.getY() / 3] = currentPlayerId;

        while (!state.getField().isFull()) {
            List<IMove> availableMoves = currentState.getField().getAvailableMoves();
            if (availableMoves.isEmpty()) {
                break;
            }
            IMove randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
            int randomMacroX = randomMove.getX() / 3;
            int randomMacroY = randomMove.getY() / 3;
            currentState.getField().getMacroboard()[randomMacroX][randomMacroY] = currentPlayerId;
            currentPlayerId = currentPlayerId.equals("X") ? "O" : "X";
            System.out.println("Simulating move: " + randomMove.getX() + ", " + randomMove.getY());
        }
    }

    private int evaluateState(IGameState state) {
        // Evaluate the game state here, returns a score

        // Check if the current player wins
        if (isWinningPlayer(state, getCurrentPlayerId(state))) {
            return Integer.MAX_VALUE;
        }

        // Check if the opponent wins
        String opponentPlayerId = getCurrentPlayerId(state).equals("X") ? "O" : "X";
        if (isWinningPlayer(state, opponentPlayerId)) {
            return Integer.MIN_VALUE;
        }

        // Default score if no one wins
        return 0;
    }

    private boolean isWinningPlayer(IGameState state, String playerId) {
        // Check rows
        for (int i = 0; i < state.getField().getBoard().length; i++) {
            if (isWinningRow(state, i, playerId)) {
                return true;
            }
        }

        // Check columns
        for (int j = 0; j < state.getField().getBoard()[0].length; j++) {
            if (isWinningColumn(state, j, playerId)) {
                return true;
            }
        }

        // Check diagonals
        if (isWinningDiagonal(state, playerId)) {
            return true;
        }

        return false;
    }

    private boolean isWinningRow(IGameState state, int row, String playerId) {
        String[][] board = state.getField().getBoard();
        for (int j = 0; j < board[0].length; j++) {
            if (!board[row][j].equals(playerId)) {
                return false;
            }
        }
        return true;
    }

    private boolean isWinningColumn(IGameState state, int column, String playerId) {
        String[][] board = state.getField().getBoard();
        for (int i = 0; i < board.length; i++) {
            if (!board[i][column].equals(playerId)) {
                return false;
            }
        }
        return true;
    }

    private boolean isWinningDiagonal(IGameState state, String playerId) {
        String[][] board = state.getField().getBoard();

        // Check main diagonal
        for (int i = 0; i < board.length; i++) {
            if (!board[i][i].equals(playerId)) {
                break;
            }
            if (i == board.length - 1) {
                return true; // Main diagonal is all the same playerId
            }
        }

        // Check anti-diagonal
        for (int i = 0; i < board.length; i++) {
            if (!board[i][board.length - 1 - i].equals(playerId)) {
                break;
            }
            if (i == board.length - 1) {
                return true; // Anti-diagonal is all the same playerId
            }
        }

        return false;
    }

    private double calculateUCBValue(int totalSimulations, int score) {
        if (totalSimulations == 0) {
            return Double.MAX_VALUE; // To ensure unexplored nodes are prioritized
        }
        return (double) score / totalSimulations + EXPLORATION_PARAMETER * Math.sqrt(Math.log(totalSimulations));
    }

    private String getCurrentPlayerId(IGameState state) {
        // Assuming players are represented as "X" and "O" in the board
        if (state.getMoveNumber() % 2 == 0) {
            return "X";
        } else {
            return "O";
        }
    }

    private boolean isWinningMove(IGameState state, IMove move) {
        int macroX = move.getX() / 3; // Adjust the x-coordinate for the macroboard
        int macroY = move.getY() / 3; // Adjust the y-coordinate for the macroboard
        String[][] macroboard = state.getField().getMacroboard();

        // Check if placing the move leads to winning the corresponding macroboard
        return isMacroboardWonByPlayer(macroboard, getCurrentPlayerId(state), macroX, macroY);
    }

    private boolean isMacroboardWonByPlayer(String[][] macroboard, String playerId, int macroX, int macroY) {
        // Check rows
        if (isWin(macroboard[macroX][0], macroboard[macroX][1], macroboard[macroX][2])) {
            return true;
        }

        // Check columns
        if (isWin(macroboard[0][macroY], macroboard[1][macroY], macroboard[2][macroY])) {
            return true;
        }

        // Check diagonals
        if ((isWin(macroboard[0][0], macroboard[1][1], macroboard[2][2]) && (macroX == macroY)) ||
                (isWin(macroboard[0][2], macroboard[1][1], macroboard[2][0]) && (macroX + macroY == 2))) {
            return true;
        }

        return false;
    }

    private boolean isWin(String cell1, String cell2, String cell3) {
        return !cell1.equals(IField.AVAILABLE_FIELD) &&
                cell1.equals(cell2) &&
                cell2.equals(cell3);
    }

    private List<IMove> getWinningMovesWithinMacroboard(IGameState state, int macroX, int macroY) {
        List<IMove> winningMoves = new ArrayList<>();
        String[][] macroboard = state.getField().getMacroboard();

        // Check rows
        if (isWin(macroboard[macroX][0], macroboard[macroX][1], macroboard[macroX][2])) {
            // Add winning moves in the row
            for (int j = 0; j < 3; j++) {
                winningMoves.add(new Move(macroX * 3, macroY * 3 + j));
            }
        }

        // Check columns
        if (isWin(macroboard[0][macroY], macroboard[1][macroY], macroboard[2][macroY])) {
            // Add winning moves in the column
            for (int i = 0; i < 3; i++) {
                winningMoves.add(new Move(macroX * 3 + i, macroY * 3));
            }
        }

        // Check diagonals
        if ((isWin(macroboard[0][0], macroboard[1][1], macroboard[2][2]) && (macroX == macroY))) {
            // Add winning moves in the main diagonal
            for (int i = 0; i < 3; i++) {
                winningMoves.add(new Move(macroX * 3 + i, macroY * 3 + i));
            }
        }
        if ((isWin(macroboard[0][2], macroboard[1][1], macroboard[2][0]) && (macroX + macroY == 2))) {
            // Add winning moves in the anti-diagonal
            for (int i = 0; i < 3; i++) {
                winningMoves.add(new Move(macroX * 3 + i, macroY * 3 + 2 - i));
            }
        }

        return winningMoves;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
    }
}