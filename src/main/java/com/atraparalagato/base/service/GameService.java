package com.atraparalagato.base.service;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.model.Position;
import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.base.strategy.CatMovementStrategy;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Servicio abstracto del juego que orquesta todas las operaciones.
 * 
 * Design Patterns:
 * - Facade Pattern: Simplifica la interacción con el sistema complejo
 * - Command Pattern: Encapsula operaciones del juego
 * - Observer Pattern: Notifica eventos del juego
 * 
 * SOLID Principles:
 * - Single Responsibility: Coordina la lógica del juego
 * - Dependency Inversion: Depende de abstracciones
 * - Open/Closed: Extensible para nuevas funcionalidades
 * 
 * Conceptos a implementar:
 * - Programación Funcional: Function, Consumer, Supplier
 * - Modularización: Coordinación de diferentes componentes
 * - OOP: Composición y delegación
 */
public abstract class GameService<T extends Position> {
    
    protected final GameBoard<T> gameBoard;
    protected final CatMovementStrategy<T> movementStrategy;
    protected final DataRepository<GameState<T>, String> gameRepository;
    
    // Functional Programming: Factory functions
    protected final Supplier<String> gameIdGenerator;
    protected final Function<Integer, GameBoard<T>> boardFactory;
    protected final Function<String, GameState<T>> gameStateFactory;
    
    protected GameService(GameBoard<T> gameBoard,
                         CatMovementStrategy<T> movementStrategy,
                         DataRepository<GameState<T>, String> gameRepository,
                         Supplier<String> gameIdGenerator,
                         Function<Integer, GameBoard<T>> boardFactory,
                         Function<String, GameState<T>> gameStateFactory) {
        this.gameBoard = gameBoard;
        this.movementStrategy = movementStrategy;
        this.gameRepository = gameRepository;
        this.gameIdGenerator = gameIdGenerator;
        this.boardFactory = boardFactory;
        this.gameStateFactory = gameStateFactory;
    }
    
    /**
     * Inicia un nuevo juego con el tamaño de tablero especificado.
     * Template Method que define el flujo de creación.
     */
    public final GameState<T> startNewGame(int boardSize) {
        String gameId = generateGameId();
        GameBoard<T> board = createGameBoard(boardSize);
        GameState<T> gameState = createGameState(gameId);
        
        initializeGame(gameState, board);
        configureGameCallbacks(gameState);
        
        GameState<T> savedState = persistGameState(gameState);
        onGameStarted(savedState);
        
        return savedState;
    }
    
    /**
     * Ejecuta un movimiento del jugador.
     * Coordina la actualización del tablero y el movimiento del gato.
     */
    public final Optional<GameState<T>> executePlayerMove(String gameId, T position) {
        Optional<GameState<T>> gameStateOpt = loadGameState(gameId);
        
        if (gameStateOpt.isEmpty()) {
            return Optional.empty();
        }
        
        GameState<T> gameState = gameStateOpt.get();
        
        if (!gameState.executeMove(position)) {
            return Optional.of(gameState);
        }
        
        // Mover el gato después del movimiento del jugador
        executeCatMove(gameState);
        
        GameState<T> updatedState = persistGameState(gameState);
        onMoveExecuted(updatedState, position);
        
        return Optional.of(updatedState);
    }
    
    /**
     * Genera un identificador único para el juego.
     * Los estudiantes pueden implementar diferentes estrategias.
     */
    protected String generateGameId() {
        return gameIdGenerator.get();
    }
    
    /**
     * Crea un tablero de juego con el tamaño especificado.
     * Factory method que permite diferentes tipos de tablero.
     */
    protected GameBoard<T> createGameBoard(int size) {
        return boardFactory.apply(size);
    }
    
    /**
     * Crea un estado de juego inicial.
     * Factory method para diferentes implementaciones de estado.
     */
    protected GameState<T> createGameState(String gameId) {
        return gameStateFactory.apply(gameId);
    }
    
    /**
     * Inicializa el juego con valores por defecto.
     * Los estudiantes definen la configuración inicial.
     */
    protected abstract void initializeGame(GameState<T> gameState, GameBoard<T> board);
    
    /**
     * Configura los callbacks del estado del juego.
     * Ejemplo de programación funcional con Consumer.
     */
    protected void configureGameCallbacks(GameState<T> gameState) {
        gameState.setOnStateChanged(this::onGameStateChanged);
        gameState.setOnGameEnded(this::onGameEnded);
    }
    
    /**
     * Ejecuta el movimiento del gato usando la estrategia configurada.
     */
    protected void executeCatMove(GameState<T> gameState) {
        T currentPosition = gameState.getCatPosition();
        T targetPosition = getTargetPosition(gameState);
        
        Optional<T> nextMove = movementStrategy.findBestMove(currentPosition, targetPosition);
        
        if (nextMove.isPresent()) {
            gameState.setCatPosition(nextMove.get());
            onCatMoved(gameState, nextMove.get());
        }
    }
    
    /**
     * Determina la posición objetivo para el gato.
     * Los estudiantes definen hacia dónde debe moverse el gato.
     */
    protected abstract T getTargetPosition(GameState<T> gameState);
    
    /**
     * Carga el estado del juego desde el repositorio.
     */
    public Optional<GameState<T>> loadGameState(String gameId) {
        return gameRepository.findById(gameId);
    }
    
    /**
     * Persiste el estado del juego en el repositorio.
     */
    protected GameState<T> persistGameState(GameState<T> gameState) {
        return gameRepository.save(gameState);
    }
    
    /**
     * Obtiene estadísticas del juego.
     * Los estudiantes implementan métricas específicas.
     */
    public abstract Object getGameStatistics(String gameId);
    
    /**
     * Valida si un movimiento es legal según las reglas del juego.
     */
    public abstract boolean isValidMove(String gameId, T position);
    
    /**
     * Obtiene sugerencias de movimiento para el jugador.
     * Funcionalidad opcional para ayudar a los jugadores.
     */
    public abstract Optional<T> getSuggestedMove(String gameId);
    
    // Event handlers - Hook methods para extensibilidad
    protected void onGameStarted(GameState<T> gameState) {
        // Default: no operation
    }
    
    protected void onMoveExecuted(GameState<T> gameState, T position) {
        // Default: no operation
    }
    
    protected void onCatMoved(GameState<T> gameState, T newPosition) {
        // Default: no operation
    }
    
    protected void onGameStateChanged(GameState<T> gameState) {
        // Default: no operation
    }
    
    protected void onGameEnded(GameState<T> gameState) {
        // Default: no operation
    }
} 