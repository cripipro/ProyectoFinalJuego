package com.atraparalagato.impl.repository;

import com.atraparalagato.base.model.GameState;
import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.model.HexPosition;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repositorio en memoria para HexGameState.
 * Se inspira en la implementaci\u00f3n de ejemplo pero adaptado para la versi\u00f3n
 * de estudiantes. No usa una base de datos real pero cumple con la interfaz
 * DataRepository.
 */
public class InMemoryHexGameRepository extends DataRepository<GameState<HexPosition>, String> {

    private final Map<String, HexGameState> storage = new ConcurrentHashMap<>();

    @Override
    public GameState<HexPosition> save(GameState<HexPosition> entity) {
        HexGameState hexState = (HexGameState) entity;
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }

        beforeSave(entity);
        storage.put(hexState.getGameId(), hexState);
        afterSave(entity);
        return hexState;
    }

    @Override
    public Optional<GameState<HexPosition>> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<GameState<HexPosition>> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<GameState<HexPosition>> findWhere(Predicate<GameState<HexPosition>> condition) {
        return storage.values().stream()
                .map(gs -> (GameState<HexPosition>) gs)
                .filter(condition)
                .collect(Collectors.toList());
    }

    @Override
    public <R> List<R> findAndTransform(Predicate<GameState<HexPosition>> condition,
                                        Function<GameState<HexPosition>, R> transformer) {
        return storage.values().stream()
                .map(gs -> (GameState<HexPosition>) gs)
                .filter(condition)
                .map(transformer)
                .collect(Collectors.toList());
    }

    @Override
    public long countWhere(Predicate<GameState<HexPosition>> condition) {
        return storage.values().stream()
                .map(gs -> (GameState<HexPosition>) gs)
                .filter(condition)
                .count();
    }

    @Override
    public boolean deleteById(String id) {
        if (id == null) {
            return false;
        }
        return storage.remove(id) != null;
    }

    @Override
    public long deleteWhere(Predicate<GameState<HexPosition>> condition) {
        List<String> toDelete = storage.values().stream()
                .map(gs -> (GameState<HexPosition>) gs)
                .filter(condition)
                .map(gs -> ((HexGameState) gs).getGameId())
                .collect(Collectors.toList());

        long deleted = 0;
        for (String id : toDelete) {
            if (storage.remove(id) != null) {
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public boolean existsById(String id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public <R> R executeInTransaction(Function<DataRepository<GameState<HexPosition>, String>, R> operation) {
        try {
            return operation.apply(this);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    @Override
    public List<GameState<HexPosition>> findWithPagination(int page, int size) {
        if (page < 0 || size <= 0) {
            return Collections.emptyList();
        }
        List<GameState<HexPosition>> all = findAll();
        int start = page * size;
        int end = Math.min(start + size, all.size());
        if (start >= all.size()) {
            return Collections.emptyList();
        }
        return all.subList(start, end);
    }

    @Override
    public List<GameState<HexPosition>> findAllSorted(Function<GameState<HexPosition>, ? extends Comparable<?>> sortKeyExtractor,
                                            boolean ascending) {
        @SuppressWarnings("unchecked")
        Comparator<GameState<HexPosition>> comparator = (Comparator<GameState<HexPosition>>) Comparator.comparing(
                (Function<GameState<HexPosition>, Comparable<Object>>) sortKeyExtractor
        );
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return storage.values().stream()
                .map(gs -> (GameState<HexPosition>) gs)
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
        // Consultas personalizadas sencillas para pruebas
        if ("finished_games".equals(query)) {
            return storage.values().stream()
                    .map(gs -> (GameState<HexPosition>) gs)
                    .filter(g -> ((HexGameState) g).isGameFinished())
                    .map(resultMapper)
                    .collect(Collectors.toList());
        }
        if ("won_games".equals(query)) {
            return storage.values().stream()
                    .map(gs -> (GameState<HexPosition>) gs)
                    .filter(g -> ((HexGameState) g).hasPlayerWon())
                    .map(resultMapper)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected void initialize() {
        System.out.println("Inicializando repositorio Hex en memoria...");
    }

    @Override
    protected void cleanup() {
        storage.clear();
    }

    @Override
    protected boolean validateEntity(GameState<HexPosition> entity) {
        if (!(entity instanceof HexGameState hex)) {
            return false;
        }
        return hex.getGameId() != null && !hex.getGameId().isBlank();
    }

    @Override
    protected void beforeSave(GameState<HexPosition> entity) {
        if (!validateEntity(entity)) {
            throw new IllegalArgumentException("Invalid game state entity");
        }
    }

    @Override
    protected void afterSave(GameState<HexPosition> entity) {
        // no-op
    }

    /**
     * Estad\u00edsticas b\u00e1sicas del repositorio.
     */
    public Map<String, Object> getRepositoryStatistics() {
        long total = storage.size();
        long finished = countWhere(gs -> ((HexGameState) gs).isGameFinished());
        long won = countWhere(gs -> ((HexGameState) gs).hasPlayerWon());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", total);
        stats.put("finishedGames", finished);
        stats.put("wonGames", won);
        stats.put("inProgressGames", total - finished);
        stats.put("winRate", total > 0 ? (double) won / total * 100 : 0);
        return stats;
    }

    /**
     * Limpia juegos antiguos seg\u00fan antig\u00fcedad en horas.
     */
    public long cleanupOldGames(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        return deleteWhere(game -> game.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() < now - maxAgeMillis);
    }
}
