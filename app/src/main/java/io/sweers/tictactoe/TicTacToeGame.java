package io.sweers.tictactoe;

import android.support.annotation.IntDef;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import rx.Observable;
import rx.functions.Action1;

/**
 * Class representing a game of Tic Tac Toe
 */
public final class TicTacToeGame {

    public interface OnGameOverListener {
        void onGameOver(@GameState int state, int[] winningIndices);
    }

    public static final int CONTINUE = 0;
    public static final int TIE = 1;
    public static final int ONE_WINS = 2;
    public static final int TWO_WINS = 3;
    @IntDef({CONTINUE, TIE, ONE_WINS, TWO_WINS})
    public @interface GameState {
    }

    // No CharDef :(
    public static final char NONE = '-';
    public static final char PLAYER_ONE = 'X';
    public static final char PLAYER_TWO = 'O';

    private static final int GRID_SIZE = 9;

    private char[] grid = {NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE, NONE};
    private int[] winningIndices = null;
    private char currentPlayer;
    private boolean isOver = false;
    private int nextCpuMove;
    @GameState private int gameState = CONTINUE;
    private OnGameOverListener onGameOverListener;
    private final Random random = new Random();

    public TicTacToeGame() {
        boolean playerOneGoesFirst = random.nextBoolean();
        currentPlayer = playerOneGoesFirst ? PLAYER_ONE : PLAYER_TWO;
    }

    public char currentPlayer() {
        return this.currentPlayer;
    }

    public void setOnGameOverListener(OnGameOverListener listener) {
        this.onGameOverListener = listener;
    }

    public void makeMove(int position) {
        char prevPlayer = currentPlayer;
        grid[position] = currentPlayer;

        currentPlayer = currentPlayer == PLAYER_ONE ? PLAYER_TWO : PLAYER_ONE;

        if (checkForWinner(prevPlayer) != CONTINUE) {
            endGame();
        }
    }

    private void makeDummyMove(int position, char player) {
        grid[position] = player;
    }

    @GameState
    private int checkForWinner(char player) {
        boolean matched = false;

        // Check rows
        for (int i = 0; i <= 6; i += 3) {
            winningIndices = new int[]{i, i + 1, i + 2};
            matched = checkMatches(winningIndices, player);
            if (matched) {
                break;
            }
        }

        // Check columns
        if (!matched) {
            for (int i = 0; i <= 2; i++) {
                winningIndices = new int[]{i, i + 3, i + 6};
                matched = checkMatches(winningIndices, player);
                if (matched) {
                    break;
                }
            }
        }

        // Check diagonals
        if (!matched) {
            winningIndices = new int[]{0, 4, 8};
            matched = checkMatches(winningIndices, player);
        }
        if (!matched) {
            winningIndices = new int[]{2, 4, 6};
            matched = checkMatches(winningIndices, player);
        }


        @GameState int result = TIE;
        if (!matched) {
            // No winner yet, continue if there are any open spaces left
            winningIndices = null;
            for (int i = 0; i < GRID_SIZE; ++i) {
                if (grid[i] == NONE) {
                    result = CONTINUE;
                    break;
                }
            }
        } else {
            result = player == PLAYER_ONE ? ONE_WINS : TWO_WINS;
        }

        gameState = result;
        return gameState;
    }

    public void endGame() {
        isOver = true;
        if (onGameOverListener != null) {
            onGameOverListener.onGameOver(gameState, winningIndices);
        }
    }

    private boolean checkMatches(int[] indices, char player) {
        boolean matches = true;
        for (int index : indices) {
            matches = matches && grid[index] == player;
        }

        return matches;
    }

    public Observable<Object> getCpuMove() {
        return Observable.just(null)
                .doOnNext(new Action1<Object>() {
                    @Override
                    public void call(Object o) {
                        if (getAvailableStates().size() == grid.length) {
                            // minimax will spend a lot of time calculating every permutation of this, but always ends on 0. Let's spice it up
                            nextCpuMove = new Random().nextInt(grid.length);
                        } else {
                            minimax(0, PLAYER_TWO);
                        }
                    }
                });
    }

    public boolean isOver() {
        return isOver;
    }

    public void restart() {
        isOver = false;
        for (int i = 0; i < GRID_SIZE; ++i) {
            grid[i] = NONE;
        }
        boolean playerOneGoesFirst = new Random().nextBoolean();
        currentPlayer = playerOneGoesFirst ? PLAYER_ONE : PLAYER_TWO;
    }

    @Override
    public String toString() {
        return "TicTacToeGame{currentPlayer="
                + currentPlayer
                + ", current grid="
                + pprintGrid()
                + "\n}";
    }

    private String pprintGrid() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 9; ++i) {
            if (i % 3 == 0) {
                builder.append(" | ");
            }
            builder.append(grid[i]);
        }
        return builder.toString();
    }

    public char[] getGridState() {
        return this.grid;
    }

    public void setGridState(char[] gridState) {
        this.grid = gridState;
    }

    public int[] getWinningIndices() {
        return this.winningIndices;
    }

    public void setWinningIndices(int[] indices) {
        this.winningIndices = indices;
    }

    @GameState
    public int getGameState() {
        return gameState;
    }

    public void setGameState(int gameState) {
        this.gameState = gameState;
    }

    public int getNextCpuMove() {
        return nextCpuMove;
    }

    public void setIsOver(boolean isOver) {
        this.isOver = isOver;
    }

    public void setCurrentPlayer(char currentPlayer) {
        this.currentPlayer = currentPlayer;
    }

    private List<Integer> getAvailableStates() {
        List<Integer> availableSpaces = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            char c = grid[i];
            if (c == NONE) {
                availableSpaces.add(i);
            }
        }

        return availableSpaces;
    }

    /**
     * Implementation of the minimax algorithm for this game implementation
     *
     * @param depth current depth of the minimax recursion
     * @param player player to check
     * @return the maximized score if this is the computer (PLAYER_TWO)
     *         or minimized score if human (PLAYER_ONE)
     */
    private int minimax(int depth, char player) {

        if (checkForWinner(PLAYER_TWO) == TWO_WINS) {
            return 10 - depth;
        } else if (checkForWinner(PLAYER_ONE) == ONE_WINS) {
            return depth - 10;
        }

        List<Integer> pointsAvailable = getAvailableStates();
        if (pointsAvailable.isEmpty()) {
            return 0;   // Ties help no one
        }

        ++depth;
        int max = Integer.MIN_VALUE;
        int min = Integer.MAX_VALUE;
        int maxIndex = 0;
        int minIndex = 0;

        for (int i = 0; i < pointsAvailable.size(); ++i) {
            int index = pointsAvailable.get(i);
            if (player == PLAYER_TWO) {
                makeDummyMove(index, PLAYER_TWO);
                int score = minimax(depth, PLAYER_ONE);
                if (score > max
                        || (score == max && random.nextBoolean())) {    // Equally good options, so randomly choose one for added flavor
                    max = score;
                    maxIndex = index;
                }
            } else if (player == PLAYER_ONE) {
                makeDummyMove(index, PLAYER_ONE);
                int score = minimax(depth, PLAYER_TWO);
                if (score < min
                        || (score == min && random.nextBoolean())) {    // Equally good options, so randomly choose one for added flavor
                    min = score;
                    minIndex = index;
                }
            }
            grid[index] = NONE; // Clean up when we're done
        }

        if (player == PLAYER_TWO) {
            // Max calc
            nextCpuMove = maxIndex;
            return max;
        } else {
            // Min calc
            nextCpuMove = minIndex;
            return min;
        }
    }
}
