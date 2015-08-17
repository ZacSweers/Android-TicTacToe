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
    private int[] winningIndices = {-1, -1, -1};
    private char currentPlayer;
    private boolean isOver = false;
    private int nextCpuMove;
    @GameState private int gameState = CONTINUE;
    private OnGameOverListener onGameOverListener;
    private final Random random = new Random();

    // Mapping of grid indices to their x,y coordinates
    private static final int[][] COORDINATES_MAPPING = {
            new int[] {0, 0},
            new int[] {1, 0},
            new int[] {2, 0},
            new int[] {0, 1},
            new int[] {1, 1},
            new int[] {2, 1},
            new int[] {0, 2},
            new int[] {1, 2},
            new int[] {2, 2}
    };

    // Mapping of coordinates to indices
    private static final int[][] INDEX_MAPPING = {
            new int[] {0, 1, 2},
            new int[] {3, 4, 5},
            new int[] {6, 7, 8}
    };

    private static final boolean[] DIAGONALS = {
            true, false, true,
            false, true, false,
            true, false, true
    };

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
        grid[position] = currentPlayer;

        currentPlayer = currentPlayer == PLAYER_ONE ? PLAYER_TWO : PLAYER_ONE;

        if (checkForWinner(position) != CONTINUE) {
            endGame();
        }
    }

    private void makeDummyMove(int position, char player) {
        grid[position] = player;
    }

    @GameState
    private int checkForWinner(int newIndex) {
        boolean winnerFound = true;
        int[] coordinates = COORDINATES_MAPPING[newIndex];
        char player = grid[newIndex];
        int x = coordinates[0];
        int y = coordinates[1];

        // Check columns
        for (int i = 0; i < 3; ++i) {
            int index = INDEX_MAPPING[i][x];
            if (grid[index] != player) {
                winnerFound = false;
                break;
            }
            winningIndices[i] = index;
        }

        // Check rows
        if (!winnerFound) {
            winnerFound = true;
            for (int i = 0; i < 3; ++i) {
                int index = INDEX_MAPPING[y][i];
                if (grid[index] != player) {
                    winnerFound = false;
                    break;
                }
                winningIndices[i] = index;
            }
        }

        // Check diagonals
        if (!winnerFound && DIAGONALS[newIndex]) {
            winnerFound = true;
            boolean checkBoth = newIndex == 4;
            if (checkBoth || newIndex == 0 || newIndex == 8) {
                // Left to right
                for (int i = 0; i < 3; ++i) {
                    int index = INDEX_MAPPING[i][i];
                    if (grid[index] != player) {
                        winnerFound = false;
                        break;
                    }
                    winningIndices[i] = index;
                }
            } else {
                winnerFound = false;
            }
            if (!winnerFound && (checkBoth || newIndex == 2 || newIndex == 6)) {
                winnerFound = true;
                // Right to left
                for (int i = 2; i < 7; i += 2) {
                    if (grid[i] != player) {
                        winnerFound = false;
                        break;
                    }
                    winningIndices[(i / 2) - 1] = i;
                }
            }
        }


        @GameState int result = TIE;
        if (!winnerFound) {
            // No winner yet, continue if there are any open spaces left
            // Clear winning indices
            winningIndices[0] = -1;
            winningIndices[1] = -1;
            winningIndices[2] = -1;
            if (!getAvailableStates().isEmpty()) {
                result = CONTINUE;
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
            onGameOverListener.onGameOver(gameState, winningIndices[0] == -1 ? null : winningIndices);
        }
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
                            minimax(0, PLAYER_TWO, -1);
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
    private int minimax(int depth, char player, int newIndex) {
        List<Integer> pointsAvailable = getAvailableStates();
        if (depth != 0) {
            @GameState int currentResult = checkForWinner(newIndex);
            if (currentResult == TWO_WINS) {
                return 10 - depth;
            } else if (currentResult == ONE_WINS) {
                return depth - 10;
            } else if (pointsAvailable.isEmpty()) {
                return 0;   // Ties help no one
            }
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
                int score = minimax(depth, PLAYER_ONE, index);
                if (score > max
                        || (score == max && random.nextBoolean())) {    // Equally good options, so randomly choose one for added flavor
                    max = score;
                    maxIndex = index;
                }
            } else if (player == PLAYER_ONE) {
                makeDummyMove(index, PLAYER_ONE);
                int score = minimax(depth, PLAYER_TWO, index);
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
