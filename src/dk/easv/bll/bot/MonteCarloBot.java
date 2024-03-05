package dk.easv.bll.bot;

import dk.easv.bll.field.IField;
import dk.easv.bll.game.GameState;
import dk.easv.bll.game.IGameState;
import dk.easv.bll.move.IMove;
import dk.easv.bll.move.Move;

import java.util.List;
import java.util.Random;

public class MonteCarloBot implements IBot {

    private static final String BOTNAME = "Monte Carlo Bot";
    private Random rand = new Random();
    private static final int SIMULATION_COUNT = 1000;

    @Override
    public IMove doMove(IGameState state) {
        List<IMove> moves = state.getField().getAvailableMoves();

        System.out.println("Available Moves: " + moves.size());

        if (!moves.isEmpty()) {
            // Perform Monte Carlo Tree Search
            IMove bestMove = null;
            int maxScore = Integer.MIN_VALUE;

            for (IMove move : moves) {
                int score = simulateMove(state, move);
                System.out.println("Move " + move.getX() + ", " + move.getY() + ": Score " + score);
                if (score > maxScore) {
                    maxScore = score;
                    bestMove = move;
                }
            }

            System.out.println("Best Move: " + bestMove.getX() + ", " + bestMove.getY());

            return bestMove;
        }

        return null;
    }

    private int simulateMove(IGameState state, IMove move) {
        String currentPlayerId = getCurrentPlayerId(state);
        int totalScore = 0;
        System.out.println("Simulating move: " + move.getX() + ", " + move.getY());

        if (isWinningMove(state, move)) {
            // Winning move found, return a high score
            return Integer.MAX_VALUE;
        }

        for (int i = 0; i < SIMULATION_COUNT; i++) {
            IGameState simulatedState = new GameState(state); // Create a copy of the current game state
            int macroX = move.getX() / 3; // Adjust the x-coordinate for the macroboard
            int macroY = move.getY() / 3; // Adjust the y-coordinate for the macroboard
            simulatedState.getField().getMacroboard()[macroX][macroY] = currentPlayerId;
            // Simulate random moves until the game ends
            while (!simulatedState.getField().isFull()) {
                List<IMove> availableMoves = simulatedState.getField().getAvailableMoves();
                if (availableMoves.isEmpty()) {
                    System.out.println("No more available moves. Game ends.");
                    break;
                }
                IMove randomMove = availableMoves.get(rand.nextInt(availableMoves.size()));
                int randomMacroX = randomMove.getX() / 3; // Adjust the x-coordinate for the macroboard
                int randomMacroY = randomMove.getY() / 3; // Adjust the y-coordinate for the macroboard
                simulatedState.getField().getMacroboard()[randomMacroX][randomMacroY] = currentPlayerId;
                // Update currentPlayerId for the next move
                currentPlayerId = currentPlayerId.equals("X") ? "O" : "X";
            }
            if (simulatedState.getField().isFull()) {
                // Tie
                totalScore += 0;
                System.out.println("Game ended in a tie.");
            } else {
                // Assuming our bot is always "X"
                if (simulatedState.getField().getPlayerId(0, 0).equals("0")) { // Check if currentPlayer represents the bot
                    totalScore += 1;
                    System.out.println("Bot wins.");
                } else {
                    totalScore -= 1;
                    System.out.println("Bot loses.");
                }
            }
        }

        return totalScore;
    }

    @Override
    public String getBotName() {
        return BOTNAME;
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
        return isMacroboardWonByPlayer(macroboard, currentPlayerId(state), macroX, macroY);
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

    private String currentPlayerId(IGameState state) {
        // Assuming players are represented as "X" and "O" in the board
        if (state.getMoveNumber() % 2 == 0) {
            return "X";
        } else {
            return "O";
        }
    }
}