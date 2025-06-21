package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia BFS (Breadth-First Search) para el gato.
 * Conceptos a implementar:
 * - Algoritmo BFS para pathfinding
 * - Exploración exhaustiva de caminos
 * - Garantía de encontrar el camino más corto
 * - Uso de colas para exploración por niveles
 */
public class BFSCatMovement extends CatMovementStrategy<HexPosition> {
    
    public BFSCatMovement(GameBoard<HexPosition> board) {
        super(board);
    }
    
    @Override
    protected List<HexPosition> getPossibleMoves(HexPosition currentPosition) {
        return board.getAdjacentPositions(currentPosition).stream()
                .filter(pos -> !board.isBlocked(pos))
                .toList();
    }
    
    @Override
    protected Optional<HexPosition> selectBestMove(List<HexPosition> possibleMoves, 
                                                  HexPosition currentPosition, 
                                                  HexPosition targetPosition) {
        return possibleMoves.stream()
                .map(move -> bfsToGoal(move).orElse(List.of()))
                .filter(path -> !path.isEmpty())
                .min(Comparator.comparingInt(List::size))
                .map(path -> path.get(0));
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        return position -> position.distanceTo(targetPosition);
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        int size = board.getSize();
        return pos -> Math.abs(pos.getQ()) == size
                || Math.abs(pos.getR()) == size
                || Math.abs(pos.getS()) == size;
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        return 1.0;
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        return bfsToGoal(currentPosition).isPresent();
    }
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        return bfsToGoal(currentPosition).orElse(List.of());
    }
    
    // Métodos auxiliares que los estudiantes pueden implementar
    
    /**
     * Ejecuta BFS desde una posición hasta encontrar un objetivo.
     */
    private Optional<List<HexPosition>> bfsToGoal(HexPosition start) {
        Set<HexPosition> visited = new HashSet<>();
        Queue<HexPosition> queue = new LinkedList<>();
        Map<HexPosition, HexPosition> parent = new HashMap<>();

        queue.offer(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            HexPosition current = queue.poll();
            if (getGoalPredicate().test(current)) {
                return Optional.of(reconstructPath(parent, start, current));
            }
            for (HexPosition neighbor : getPossibleMoves(current)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return Optional.empty();
    }
    
    /**
     * Reconstruye el camino desde el mapa de padres.
     */
    private List<HexPosition> reconstructPath(Map<HexPosition, HexPosition> parentMap,
                                            HexPosition start, HexPosition goal) {
        List<HexPosition> path = new ArrayList<>();
        HexPosition current = goal;
        while (current != null) {
            path.add(current);
            current = parentMap.get(current);
        }
        Collections.reverse(path);
        return path;
    }
    
    /**
     * Evalúa la calidad de un camino encontrado.
     */
    private double evaluatePathQuality(List<HexPosition> path) {
        return path.isEmpty() ? Double.MAX_VALUE : path.size();
    }
}
