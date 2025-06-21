package com.atraparalagato.impl.service;

import com.atraparalagato.base.service.GameService;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexGameBoard;
import com.atraparalagato.impl.repository.InMemoryHexGameRepository;
import com.atraparalagato.impl.strategy.AStarCatMovement;
import com.atraparalagato.impl.strategy.BFSCatMovement;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.UUID;

/**
 * Implementación esqueleto de GameService para el juego hexagonal.
 *
 * Conceptos a implementar:
 * - Orquestación de todos los componentes del juego
 * - Lógica de negocio compleja
 * - Manejo de eventos y callbacks
 * - Validaciones avanzadas
 * - Integración con repositorio y estrategias
 */
public class HexGameService extends GameService<HexPosition> {

    public HexGameService() {
        super(
            new HexGameBoard(9),
            new AStarCatMovement(new HexGameBoard(9)),
            new InMemoryHexGameRepository(),
            () -> UUID.randomUUID().toString(),
            HexGameBoard::new,
            HexGameState::new
        );
    }

    /**
     * Crear un nuevo juego con configuración personalizada.
     * Debe ser más sofisticado que ExampleGameService.
     */
    public HexGameState createGame(int boardSize, String difficulty, Map<String, Object> options) {
        if (boardSize <= 0) {
            throw new IllegalArgumentException("Board size must be positive");
        }
        return (HexGameState) startNewGame(boardSize);
    }

    /**
     * Ejecutar movimiento del jugador con validaciones avanzadas.
     */
    public Optional<HexGameState> executePlayerMove(String gameId, HexPosition position, String playerId) {
        Optional<GameState<HexPosition>> opt = super.executePlayerMove(gameId, position);
        return opt.map(state -> (HexGameState) state);
    }

    /**
     * Obtener estado del juego con información enriquecida.
     */
    public Optional<Map<String, Object>> getEnrichedGameState(String gameId) {
        return loadGameState(gameId).map(state -> {
            HexGameState gs = (HexGameState) state;
            Map<String, Object> map = new HashMap<>();
            map.put("gameId", gs.getGameId());
            map.put("status", gs.getStatus().toString());
            map.put("catPosition", Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
            map.put("blockedCells", gs.getGameBoard().getBlockedPositions());
            map.put("moves", gs.getMoveCount());
            map.put("statistics", gs.getAdvancedStatistics());
            return map;
        });
    }

    /**
     * Obtener sugerencia inteligente de movimiento.
     */
    public Optional<HexPosition> getIntelligentSuggestion(String gameId, String difficulty) {
        Optional<GameState<HexPosition>> opt = loadGameState(gameId);
        if (opt.isEmpty()) {
            return Optional.empty();
        }
        HexGameState gs = (HexGameState) opt.get();
        CatMovementStrategy<HexPosition> strategy = createMovementStrategy(difficulty, gs.getGameBoard());
        return strategy.findBestMove(gs.getCatPosition(), getTargetPosition(gs));
    }

    /**
     * Analizar la partida y generar reporte.
     */
    public Map<String, Object> analyzeGame(String gameId) {
        Optional<GameState<HexPosition>> opt = loadGameState(gameId);
        if (opt.isEmpty()) {
            return Map.of("error", "Game not found");
        }
        HexGameState gs = (HexGameState) opt.get();
        Map<String, Object> analysis = new HashMap<>(gs.getAdvancedStatistics());
        analysis.put("score", gs.calculateScore());
        analysis.put("catPosition", Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
        return analysis;
    }

    /**
     * Obtener estadísticas globales del jugador.
     */
    public Map<String, Object> getPlayerStatistics(String playerId) {
        long total = gameRepository.findAll().size();
        long won = gameRepository.findWhere(HexGameState::hasPlayerWon).size();
        double winRate = total > 0 ? (double) won / total * 100 : 0;
        return Map.of(
            "totalGames", total,
            "wonGames", won,
            "winRate", winRate
        );
    }

    /**
     * Configurar dificultad del juego.
     */
    public void setGameDifficulty(String gameId, String difficulty) {
        notifyGameEvent(gameId, "difficulty_changed", Map.of("difficulty", difficulty));
    }

    /**
     * Pausar/reanudar juego.
     */
    public boolean toggleGamePause(String gameId) {
        notifyGameEvent(gameId, "pause_toggled", Map.of());
        return true;
    }

    /**
     * Deshacer último movimiento.
     */
    public Optional<HexGameState> undoLastMove(String gameId) {
        return Optional.empty();
    }

    /**
     * Obtener ranking de mejores puntuaciones.
     */
    public List<Map<String, Object>> getLeaderboard(int limit) {
        return gameRepository.findAllSorted(gs -> gs.calculateScore(), false)
                .stream()
                .limit(limit)
                .map(s -> Map.<String, Object>of(
                        "gameId", s.getGameId(),
                        "score", s.calculateScore(),
                        "date", s.getCreatedAt()
                ))
                .toList();
    }

    // Métodos auxiliares que los estudiantes pueden implementar

    /**
     * Validar movimiento según reglas avanzadas.
     */
    private boolean isValidAdvancedMove(HexGameState gameState, HexPosition position, String playerId) {
        return !position.equals(gameState.getCatPosition()) &&
               gameState.getGameBoard().isValidMove(position);
    }

    /**
     * Ejecutar movimiento del gato usando estrategia apropiada.
     */
    private void executeCatMove(HexGameState gameState, String difficulty) {
        super.executeCatMove(gameState);
    }

    /**
     * Calcular puntuación avanzada.
     */
    private int calculateAdvancedScore(HexGameState gameState, Map<String, Object> factors) {
        return gameState.calculateScore();
    }

    /**
     * Notificar eventos del juego.
     */
    private void notifyGameEvent(String gameId, String eventType, Map<String, Object> eventData) {
        System.out.println("[EVENT] " + eventType + " -> " + gameId + " " + eventData);
    }

    /**
     * Crear factory de estrategias según dificultad.
     */
    private CatMovementStrategy<HexPosition> createMovementStrategy(String difficulty, HexGameBoard board) {
        if ("hard".equalsIgnoreCase(difficulty)) {
            return new AStarCatMovement(board);
        }
        return new BFSCatMovement(board);
    }

    // Métodos abstractos requeridos por GameService

    @Override
    protected void initializeGame(GameState<HexPosition> gameState, GameBoard<HexPosition> gameBoard) {
        HexGameState state = (HexGameState) gameState;
        state.setCatPosition(new HexPosition(0, 0));
        state.setGameBoard((HexGameBoard) gameBoard);
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
                    state.getGameBoard()
                         .getAdjacentPositions(state.getCatPosition()).stream()
                         .filter(pos -> !state.getGameBoard().isBlocked(pos))
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
