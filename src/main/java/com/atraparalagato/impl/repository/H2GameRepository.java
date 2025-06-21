package com.atraparalagato.impl.repository;

import com.atraparalagato.base.repository.DataRepository;
import com.atraparalagato.impl.model.HexGameState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementaci\u00f3n simplificada de DataRepository que simula una base
 * de datos H2 usando almacenamiento en memoria. Esto permite que el resto
 * de la aplicaci\u00f3n funcione sin depender de una base de datos real.
 */
public class H2GameRepository extends DataRepository<HexGameState, String> {

    private final Map<String, HexGameState> storage = new ConcurrentHashMap<>();

    public H2GameRepository() {
        initialize();
    }

    @Override
    public HexGameState save(HexGameState entity) {
        beforeSave(entity);
        storage.put(entity.getGameId(), entity);
        afterSave(entity);
        return entity;
    }

    @Override
    public Optional<HexGameState> findById(String id) {
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
        return storage.values().stream().filter(condition).count();
    }

    @Override
    public boolean deleteById(String id) {
        return storage.remove(id) != null;
    }

    @Override
    public long deleteWhere(Predicate<HexGameState> condition) {
        List<String> ids = storage.values().stream()
                .filter(condition)
                .map(HexGameState::getGameId)
                .collect(Collectors.toList());
        long deleted = 0;
        for (String id : ids) {
            if (storage.remove(id) != null) {
                deleted++;
            }
        }
        return deleted;
    }

    @Override
    public boolean existsById(String id) {
        return storage.containsKey(id);
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
        int end = Math.min(start + size, all.size());
        if (start >= all.size()) {
            return Collections.emptyList();
        }
        return all.subList(start, end);
    }

    @Override
    public List<HexGameState> findAllSorted(Function<HexGameState, ? extends Comparable<?>> sortKeyExtractor,
                                           boolean ascending) {
        @SuppressWarnings("unchecked")
        Comparator<HexGameState> comparator = (Comparator<HexGameState>) Comparator.comparing(
                (Function<HexGameState, Comparable<Object>>) sortKeyExtractor
        );
        if (!ascending) {
            comparator = comparator.reversed();
        }
        return storage.values().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    @Override
    public <R> List<R> executeCustomQuery(String query, Function<Object, R> resultMapper) {
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
        // Nada que inicializar en la versi\u00f3n en memoria
    }

    @Override
    protected void cleanup() {
        storage.clear();
    }
}
