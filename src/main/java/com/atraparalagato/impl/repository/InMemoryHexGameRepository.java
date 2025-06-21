package com.atraparalagato.impl.repository;

import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Repositorio en memoria para HexGameState.
 * Se inspira en la implementación de ejemplo pero adaptado para la versión
 * de estudiantes. No usa una base de datos real pero cumple con la interfaz
 * DataRepository.
 */
public class InMemoryHexGameRepository extends DataRepository<HexGameState, String> {

    private final Map<String, HexGameState> storage = new ConcurrentHashMap<>();

    @Override
    public HexGameState save(HexGameState entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Entity cannot be null");
        }
        beforeSave(entity);
        storage.put(entity.getGameId(), entity);
        afterSave(entity);
        return entity;
    }

    @Override
    public Optional<HexGameState> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<HexGameState> findAll() {
        return new ArrayList<>(storage.values());
    }

    @Override
    public List<HexGameState> findWhere(Predicate<HexGameState> condition) {
        return storage.values().stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    @Override
    public <R> List<R> findAndTransform(Predicate<HexGameState> condition,
                                        Function<HexGameState, R> transformer) {
        return storage.values().stream()
                .filter(condition)
                .map(transformer)
                .collect(Collectors.toList());
    }

    @Override
    public long countWhere(Predicate<HexGameState> condition) {
        return storage.values().stream()
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
    public long deleteWhere(Predicate<HexGameState> condition) {
        List<String> toDelete = storage.values().stream()
                .filter(condition)
                .map(HexGameState::getGameId)
                .collect(Collectors.toList());

        long deleted = 0;
        for (String gameId : toDelete) {
            if (storage.remove(gameId) != null) {
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
    public <R> R executeInTransaction(Function<DataRepository<HexGameState, String>, R> operation) {
        try {
            return operation.apply(this);
        } catch (Exception e) {
            throw new RuntimeException("Transaction failed", e);
        }
    }

    @Override
    public List<HexGameState> findWithPagination(int page, int size) {
        if (page < 0 || size <= 0) {
            return Collections.emptyList();
        }
        List<HexGameState> all = findAll();
        int start = page * size;
        if (start >= all.size()) {
            return Collections.emptyList();
        }
        int end = Math.min(start + size, all.size());
        return all.subList(start, end);
    }

    @Override
    public List<HexGameState> findAllSorted(Function<HexGameState, ? extends Comparable<?>> sortKeyExtractor,
                                            boolean ascending) {
        @SuppressWarnings("unchecked")
        Comparator<HexGameState> comparator = (Comparator<HexGameState>)
                Comparator.comparing((Function<HexGameState, Comparable<Object>>) sortKeyExtractor);
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return storage.values().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
        // Consultas personalizadas sencillas para pruebas
        if ("finished_games".equals(query)) {
            return storage.values().stream()
                    .filter(HexGameState::isGameFinished)
                    .map(resultMapper)
                    .collect(Collectors.toList());
        }
        if ("won_games".equals(query)) {
            return storage.values().stream()
                    .filter(HexGameState::hasPlayerWon)
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
    protected boolean validateEntity(HexGameState entity) {
        return entity != null && entity.getGameId() != null && !entity.getGameId().isBlank();
    }

    @Override
    protected void beforeSave(HexGameState entity) {
        if (!validateEntity(entity)) {
            throw new IllegalArgumentException("Invalid game state entity");
        }
    }

    @Override
    protected void afterSave(HexGameState entity) {
        // no-op
    }

    /**
     * Estadísticas básicas del repositorio.
     */
    public Map<String, Object> getRepositoryStatistics() {
        long total = storage.size();
        long finished = countWhere(HexGameState::isGameFinished);
        long won = countWhere(HexGameState::hasPlayerWon);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGames", total);
        stats.put("finishedGames", finished);
        stats.put("wonGames", won);
        stats.put("inProgressGames", total - finished);
        stats.put("winRate", total > 0 ? (double) won / total * 100 : 0);
        return stats;
    }

    /**
     * Limpia juegos antiguos según antigüedad en milisegundos.
     */
    public long cleanupOldGames(long maxAgeMillis) {
        long now = System.currentTimeMillis();
        return deleteWhere(game ->
            game.getCreatedAt()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() < now - maxAgeMillis
        );
    }
}
