package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameState;

import java.util.Map;

/**
 * Implementación esqueleto de GameState para tableros hexagonales.
 *
 * Conceptos a implementar:
 * - Estado del juego más sofisticado que ExampleGameState
 * - Sistema de puntuación avanzado
 * - Lógica de victoria/derrota más compleja
 * - Serialización eficiente
 * - Manejo de eventos y callbacks
 */
public class HexGameState extends GameState<HexPosition> {

    private HexPosition catPosition;
    private HexGameBoard gameBoard;
    private int boardSize;

    // Campos adicionales como tiempo de juego o dificultad pueden agregarse aquí

    public HexGameState(String gameId) {
        super(gameId);
        this.boardSize = 0;
        this.gameBoard = null;
        this.catPosition = new HexPosition(0, 0);
    }

    @Override
    protected boolean canExecuteMove(HexPosition position) {
        if (isGameFinished()) {
            return false;
        }
        if (position.equals(catPosition)) {
            return false;
        }
        return gameBoard != null && gameBoard.isValidMove(position);
    }

    @Override
    protected boolean performMove(HexPosition position) {
        return gameBoard.makeMove(position);
    }

    @Override
    protected void updateGameStatus() {
        if (isCatAtBorder()) {
            setStatus(GameStatus.PLAYER_LOST);
        } else if (isCatTrapped()) {
            setStatus(GameStatus.PLAYER_WON);
        } else {
            setStatus(GameStatus.IN_PROGRESS);
        }
    }

    @Override
    public HexPosition getCatPosition() {
        return catPosition;
    }

    @Override
    public void setCatPosition(HexPosition position) {
        this.catPosition = position;
        updateGameStatus();
    }

    @Override
    public boolean isGameFinished() {
        return getStatus() != GameStatus.IN_PROGRESS;
    }

    @Override
    public boolean hasPlayerWon() {
        return getStatus() == GameStatus.PLAYER_WON;
    }

    @Override
    public int calculateScore() {
        if (hasPlayerWon()) {
            return Math.max(0, 1000 - getMoveCount() * 10 + boardSize * 50);
        }
        return Math.max(0, 100 - getMoveCount() * 5);
    }

    @Override
    public Object getSerializableState() {
        return Map.of(
            "gameId", getGameId(),
            "catPosition", Map.of("q", catPosition.getQ(), "r", catPosition.getR()),
            "blockedCells", gameBoard.getBlockedPositions(),
            "status", getStatus().toString(),
            "moveCount", getMoveCount(),
            "boardSize", boardSize
        );
    }

    @Override
    public void restoreFromSerializable(Object serializedState) {
        if (!(serializedState instanceof Map<?, ?> map)) {
            return;
        }

        var posObj = map.get("catPosition");
        if (posObj instanceof Map<?, ?> posMap) {
            var qObj = posMap.get("q");
            var rObj = posMap.get("r");
            if (qObj instanceof Number qNum && rObj instanceof Number rNum) {
                this.catPosition = new HexPosition(qNum.intValue(), rNum.intValue());
            }
        }

        var statusStr = map.get("status");
        if (statusStr instanceof String s) {
            setStatus(GameStatus.valueOf(s));
        }

        var blocked = map.get("blockedCells");
        if (blocked instanceof Iterable<?> iterable) {
            for (var obj : iterable) {
                if (obj instanceof HexPosition pos) {
                    gameBoard.makeMove(pos);
                }
            }
        }

        var moves = map.get("moveCount");
        if (moves instanceof Number n) {
            this.moveCount = n.intValue();
        }

        var sizeObj = map.get("boardSize");
        if (sizeObj instanceof Number sizeNum) {
            this.boardSize = sizeNum.intValue();
        }
    }

    // Métodos auxiliares que los estudiantes pueden implementar

    /** Verifica si el gato está en el borde del tablero. */
    private boolean isCatAtBorder() {
        return gameBoard != null && gameBoard.isAtBorder(catPosition);
    }

    /** Verifica si el gato está completamente atrapado. */
    private boolean isCatTrapped() {
        return gameBoard.getAdjacentPositions(catPosition).stream()
            .allMatch(gameBoard::isBlocked);
    }

    /** Calcula estadísticas básicas del juego. */
    public Map<String, Object> getAdvancedStatistics() {
        return Map.of(
            "boardSize", boardSize,
            "moves", getMoveCount(),
            "status", getStatus().toString(),
            "blockedCells", gameBoard.getBlockedPositions().size()
        );
    }

    // Getters / Setters

    public HexGameBoard getGameBoard() {
        return gameBoard;
    }

    public void setGameBoard(HexGameBoard board) {
        this.gameBoard = board;
        this.boardSize = board.getSize();
    }

    public int getBoardSize() {
        return boardSize;
    }

    // Métodos adicionales pueden agregarse según sea necesario,
    // por ejemplo getDifficulty(), getTimeElapsed(), etc.
}
