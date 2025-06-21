package com.atraparalagato.impl.strategy;

import com.atraparalagato.base.model.GameBoard;
import com.atraparalagato.base.strategy.CatMovementStrategy;
import com.atraparalagato.impl.model.HexPosition;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de estrategia de movimiento usando algoritmo A*.
 * Conceptos a implementar:
 * - Algoritmos: A* pathfinding
 * - Programación Funcional: Function, Predicate
 * - Estructuras de Datos: PriorityQueue, Map, Set
 */
public class AStarCatMovement extends CatMovementStrategy<HexPosition> {
    
    public AStarCatMovement(GameBoard<HexPosition> board) {
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
                .map(move -> getFullPath(move, targetPosition))
                .filter(path -> !path.isEmpty())
                .min(Comparator.comparingDouble(this::evaluatePathCost))
                .map(path -> path.get(0));
    }
    
    @Override
    protected Function<HexPosition, Double> getHeuristicFunction(HexPosition targetPosition) {
        return position -> position.distanceTo(targetPosition);
    }
    
    @Override
    protected Predicate<HexPosition> getGoalPredicate() {
        int size = board.getSize();
        return position -> Math.abs(position.getQ()) == size
                || Math.abs(position.getR()) == size
                || Math.abs(position.getS()) == size;
    }
    
    @Override
    protected double getMoveCost(HexPosition from, HexPosition to) {
        return 1.0;
    }
    
    @Override
    public boolean hasPathToGoal(HexPosition currentPosition) {
        return !getFullPath(currentPosition, null).isEmpty();
    }
    
    @Override
    public List<HexPosition> getFullPath(HexPosition currentPosition, HexPosition targetPosition) {
        Predicate<HexPosition> goal = targetPosition != null
                ? pos -> pos.equals(targetPosition)
                : getGoalPredicate();

        Function<HexPosition, Double> heuristic =
                targetPosition != null ? getHeuristicFunction(targetPosition)
                        : pos -> 0.0;

        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<HexPosition, Double> gScore = new HashMap<>();
        Map<HexPosition, AStarNode> nodeMap = new HashMap<>();
        Set<HexPosition> closed = new HashSet<>();

        AStarNode startNode = new AStarNode(currentPosition, 0, heuristic.apply(currentPosition), null);
        openSet.add(startNode);
        gScore.put(currentPosition, 0.0);
        nodeMap.put(currentPosition, startNode);

        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            if (goal.test(current.position)) {
                return reconstructPath(current);
            }

            if (!closed.add(current.position)) {
                continue;
            }

            for (HexPosition neighbor : getPossibleMoves(current.position)) {
                double tentativeG = current.gScore + getMoveCost(current.position, neighbor);
                if (tentativeG < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    double fScore = tentativeG + heuristic.apply(neighbor);
                    AStarNode node = new AStarNode(neighbor, tentativeG, fScore, current);
                    gScore.put(neighbor, tentativeG);
                    nodeMap.put(neighbor, node);
                    openSet.add(node);
                }
            }
        }

        return List.of();
    }
    
    // Clase auxiliar para nodos del algoritmo A*
    private static class AStarNode {
        public final HexPosition position;
        public final double gScore; // Costo desde inicio
        public final double fScore; // gScore + heurística
        public final AStarNode parent;
        
        public AStarNode(HexPosition position, double gScore, double fScore, AStarNode parent) {
            this.position = position;
            this.gScore = gScore;
            this.fScore = fScore;
            this.parent = parent;
        }
    }
    
    // Método auxiliar para reconstruir el camino
    private List<HexPosition> reconstructPath(AStarNode goalNode) {
        List<HexPosition> path = new ArrayList<>();
        AStarNode current = goalNode;
        while (current != null) {
            path.add(current.position);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }
    
    // Hook methods - los estudiantes pueden override para debugging
    @Override
    protected void beforeMovementCalculation(HexPosition currentPosition) {
        super.beforeMovementCalculation(currentPosition);
    }
    
    @Override
    protected void afterMovementCalculation(Optional<HexPosition> selectedMove) {
        super.afterMovementCalculation(selectedMove);
    }

    private double evaluatePathCost(List<HexPosition> path) {
        return path.isEmpty() ? Double.POSITIVE_INFINITY : path.size();
    }
}
