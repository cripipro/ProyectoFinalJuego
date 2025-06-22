package com.atraparalagato.impl.service;

import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.repository.InMemoryHexGameRepository;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.strategy.BFSCatMovement;

import java.util.UUID;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.Random;
import java.util.HashMap;

/**
 * Servicio de juego hexagonal, con control de dificultad 1–10.
 * Requiere que HexGameState tenga un campo 'difficulty' con sus getters/setters.
 */
public class HexGameService extends GameService<HexPosition> {

    @SuppressWarnings("unchecked")
    public HexGameService() {
        super(
            new HexGameBoard(9),
            new AStarCatMovement(new HexGameBoard(9)),
            (DataRepository<GameState<HexPosition>, String>)
                (DataRepository<?, ?>) new InMemoryHexGameRepository(),
            () -> UUID.randomUUID().toString(),
            HexGameBoard::new,
            HexGameState::new
        );
    }

    /**
     * Crea una nueva partida con tamaño y dificultad.
     */
    public HexGameState createGame(int boardSize, int difficulty, Map<String, Object> options) {
        HexGameState state = (HexGameState) startNewGame(boardSize);
        state.setDifficulty(difficulty);
        return state;
    }

    /**
     * Movimiento del jugador.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
        Optional<GameState<HexPosition>> opt = super.executePlayerMove(gameId, position);
        return opt.map(s -> (HexGameState) s);
    }

    /**
     * Estado enriquecido para el cliente.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        return loadGameState(gameId).map(s -> {
            HexGameState gs = (HexGameState) s;
            Map<String, Object> map = new HashMap<>();
            map.put("gameId", gs.getGameId());
            map.put("status", gs.getStatus().toString());
            map.put("catPosition",
                Map.of("q", gs.getCatPosition().getQ(),
                       "r", gs.getCatPosition().getR())
            );
            map.put("blockedCells", gs.getGameBoard().getBlockedPositions());
            map.put("moves", gs.getMoveCount());
            map.put("statistics", gs.getAdvancedStatistics());
            map.put("difficulty", gs.getDifficulty());
            return map;
        });
    }

    /**
     * Sugerencia inteligente (no usada para el movimiento real).
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        Optional<GameState<HexPosition>> opt = loadGameState(gameId);
        if (opt.isEmpty()) return Optional.empty();
        HexGameState gs = (HexGameState) opt.get();
        CatMovementStrategy<HexPosition> strat =
            createMovementStrategy(difficulty, gs.getGameBoard());
        return strat.findBestMove(gs.getCatPosition(), getTargetPosition(gs));
    }

    /**
     * Análisis y reporte de la partida.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        Optional<GameState<HexPosition>> opt = loadGameState(gameId);
        if (opt.isEmpty()) return Map.of("error", "Game not found");
        HexGameState gs = (HexGameState) opt.get();
        Map<String, Object> analysis = new HashMap<>(gs.getAdvancedStatistics());
        analysis.put("score", gs.calculateScore());
        analysis.put("catPosition",
            Map.of("q", gs.getCatPosition().getQ(),
                   "r", gs.getCatPosition().getR())
        );
        return analysis;
    }

    /**
     * Estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        long total = gameRepository.findAll().size();
        long won   = gameRepository.findWhere(gs -> ((HexGameState) gs).hasPlayerWon()).size();
        double winRate = total > 0 ? (double) won / total * 100 : 0;
        return Map.of(
            "totalGames", total,
            "wonGames",   won,
            "winRate",    winRate
        );
    }

    /**
     * (Solo notificación) cambia dificultad de la partida.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        notifyGameEvent(gameId, "difficulty_changed", Map.of("difficulty", difficulty));
    }

    /**
     * Pausa o reanuda partida.
     */
    public boolean toggleGamePause(String gameId) {
        notifyGameEvent(gameId, "pause_toggled", Map.of());
        return true;
    }

    /**
     * Undo no implementado aún.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        return Optional.empty();
    }

    /**
     * Top N de puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        return gameRepository.findAllSorted(gs -> gs.calculateScore(), false)
            .stream()
            .limit(limit)
            .map(s -> Map.<String, Object>of(
                "gameId", s.getGameId(),
                "score",  s.calculateScore(),
                "date",   s.getCreatedAt()
            ))
            .toList();
    }

    // ------------------------------------------------------------
    // Validaciones y utilidades internas
    // ------------------------------------------------------------

    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        HexGameBoard board = gameState.getGameBoard();
        int size = board.getSize();
        boolean inBounds =
            Math.abs(position.getQ()) <= size &&
            Math.abs(position.getR()) <= size &&
            Math.abs(position.getS()) <= size;
        return !position.equals(gameState.getCatPosition())
            && inBounds
            && !board.isAtBorder(position)
            && !board.isBlocked(position);
    }

    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> data) {
        System.out.println("[EVENT] " + eventType + " -> " + gameId + " " + data);
    }

    private CatMovementStrategy<HexPosition> createMovementStrategy(
            String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        }
        return new BFSCatMovement(board);
    }

    // ------------------------------------------------------------
    // Implementación de métodos abstractos heredados
    // ------------------------------------------------------------

    @Override
    protected void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> board) {
        HexGameState state = (HexGameState) gameState;
        state.setGameBoard((HexGameBoard) board);
        state.setCatPosition(new HexPosition(0, 0));
    }

    @Override
    protected void executeCatMove(GameState<HexPosition> gameState) {
        HexGameState state = (HexGameState) gameState;
        int diff = state.getDifficulty();
        HexGameBoard board   = state.getGameBoard();
        HexPosition current  = state.getCatPosition();
        HexPosition target   = getTargetPosition(state);
        Optional<HexPosition> next;
        Random rnd = new Random();

        if (diff <= 4) {
            // Fácil: se mueve aleatoriamente a una casilla no bloqueada
            List<HexPosition> moves = board.getAdjacentPositions(current).stream()
                .filter(p -> !board.isBlocked(p))
                .toList();
            next = moves.isEmpty()
                ? Optional.empty()
                : Optional.of(moves.get(rnd.nextInt(moves.size())));
        }
        else if (diff <= 7) {
            // Medio: estrategia BFS
            BFSCatMovement bfs = new BFSCatMovement(board);
            next = bfs.findBestMove(current, target);
        }
        else {
            // Difícil: estrategia A*
            AStarCatMovement astar = new AStarCatMovement(board);
            next = astar.findBestMove(current, target);
        }

        next.ifPresent(pos -> {
            state.setCatPosition(pos);
            onCatMoved(state, pos);
        });
    }

    @Override
    public boolean isValidMove(String gameId, HexPosition position) {
        return loadGameState(gameId)
            .map(gs -> isValidAdvancedMove((HexGameState) gs, position, null))
            .orElse(false);
    }

    @Override
    public Optional<HexPosition> getSuggestedMove(String gameId) {
        return loadGameState(gameId)
            .map(gs -> (HexGameState) gs)
            .flatMap(state ->
                state.getGameBoard().getAdjacentPositions(state.getCatPosition())
                    .stream()
                    .filter(p -> !state.getGameBoard().isBlocked(p))
                    .findFirst()
            );
    }

    @Override
    protected HexPosition getTargetPosition(GameState<HexPosition> gameState) {
        HexGameState gs = (HexGameState) gameState;
        return new HexPosition(gs.getBoardSize(), 0);
    }

    @Override
    public Object getGameStatistics(String gameId) {
        return loadGameState(gameId)
            .map(gs -> ((HexGameState) gs).getAdvancedStatistics())
            .orElse(Map.of("error", "Game not found"));
    }
}
