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
    private static int TIME_LIMIT = 111; // 1000 milliseconds
    private static final double EXPLORATION_PARAMETER = 6; // UCT exploration parameter
    private String whatPlayerAmI;

    @Override
    public IMove doMove(IGameState state) {
        List<IMove> moves = state.getField().getAvailableMoves();
        int originalTimeLimit = TIME_LIMIT;

        // Adjust TIME_LIMIT based on the size of the available moves list
        if (moves.size() > 9) {
            TIME_LIMIT = 10;
        } else {
            TIME_LIMIT = 111;
        }

        System.out.println("Available Moves: " + moves.size());

        if (!moves.isEmpty()) {
            // Check if any available moves are winning moves
            for (IMove move : moves) {
                if (isWin(state.getField().getBoard(), move, "1")) {
                    System.out.println("Winning Move: " + move.getX() + ", " + move.getY());
                    return move;
                }
            }

            for (IMove move : moves) {
                if (isWin(state.getField().getBoard(), move, "1")) {
                    System.out.println("Winning Move: " + move.getX() + ", " + move.getY());
                    return move;
                }
            }

            IMove bestMove = null;
            double bestUCBValue = Double.NEGATIVE_INFINITY;

            // Perform Monte Carlo Tree Search for non-winning moves
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

            }

            // Reset TIME_LIMIT to its original value
            TIME_LIMIT = originalTimeLimit;

            return bestMove;
        }

        // Reset TIME_LIMIT to its original value
        TIME_LIMIT = originalTimeLimit;

        return null;
    }

    private int uctSimulateMove(IGameState state, IMove move, int simulations) {
        // Simulate moves using UCT strategy
        int totalScore = 0;
        for (int i = 0; i < simulations; i++) {
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
            currentPlayerId = currentPlayerId.equals("0") ? "1" : "0";
        }
    }

    private int evaluateState(IGameState state) {
        // Evaluate the game state here, returns a score

        // Check if the current player wins
        if (isWinningPlayer(state, getCurrentPlayerId(state))) {
            return Integer.MAX_VALUE;
        }

        // Check if the opponent wins
        String opponentPlayerId = getCurrentPlayerId(state).equals("0") ? "1" : "0";
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
        // Assuming players are represented as "0" and "1" in the board
        if (state.getMoveNumber() % 2 == 0) {
            return "0";
        } else {
            return "1";
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
        if (isWin(macroboard, new Move(macroX * 3, macroY * 3), playerId)) {
            return true;
        }

        // Check columns
        if (isWin(macroboard, new Move(macroX * 3, macroY * 3), playerId)) {
            return true;
        }

        // Check diagonals
        if ((isWin(macroboard, new Move(macroX * 3, macroY * 3), playerId)) ||
                (isWin(macroboard, new Move(macroX * 3, macroY * 3), playerId))) {
            return true;
        }

        return false;
    }

    public static boolean isWin(String[][] board, IMove move, String currentPlayer){
        int localX = move.getX() % 3;
        int localY = move.getY() % 3;
        int startX = move.getX() - (localX);
        int startY = move.getY() - (localY);

        //check col
        for (int i = startY; i < startY + 3; i++) {
            if (!board[move.getX()][i].equals(currentPlayer))
                break;
            if (i == startY + 3 - 1) return true;
        }

        //check row
        for (int i = startX; i < startX + 3; i++) {
            if (!board[i][move.getY()].equals(currentPlayer))
                break;
            if (i == startX + 3 - 1) return true;
        }

        //check diagonal
        if (localX == localY) {
            //we're on a diagonal
            int y = startY;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][y++].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }
        }

        //check anti diagonal
        if (localX + localY == 3 - 1) {
            int less = 0;
            for (int i = startX; i < startX + 3; i++) {
                if (!board[i][(startY + 2)-less++].equals(currentPlayer))
                    break;
                if (i == startX + 3 - 1) return true;
            }
        }
        return false;
    }

    /*private List<IMove> getWinningMovesWithinMacroboard(IGameState state, int macroX, int macroY) {
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
    }*/

    @Override
    public String getBotName() {
        return BOTNAME;
    }
}