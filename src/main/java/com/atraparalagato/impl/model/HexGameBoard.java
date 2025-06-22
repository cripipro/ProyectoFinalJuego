package com.atraparalagato.impl.model;

import com.atraparalagato.base.model.GameBoard;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Implementación esqueleto de GameBoard para tableros hexagonales.
 * 
 * Los estudiantes deben completar los métodos marcados con TO DO.
 * 
 * Conceptos a implementar:
 * - Modularización: Separación de lógica de tablero hexagonal
 * - OOP: Herencia y polimorfismo
 * - Programación Funcional: Uso de Predicate y streams
 */
public class HexGameBoard extends GameBoard<HexPosition> {
    
    public HexGameBoard(int size) {
        super(size);
    }
    
    @Override
    protected Set<HexPosition> initializeBlockedPositions() {
        // Usar HashSet por su eficiencia O(1) en operaciones básicas y
        // porque el orden de las celdas bloqueadas no afecta la lógica
        // del juego. Esta estructura evita duplicados y ofrece buen
        // rendimiento para búsquedas.
        return new HashSet<>();
    }
    
    @Override
    protected boolean isPositionInBounds(HexPosition position) {
        // Una posición es válida si sus coordenadas axiales se encuentran
        // dentro de los límites del tablero. La coordenada 's' se calcula
        // como -q - r.
        return Math.abs(position.getQ()) <= size
                && Math.abs(position.getR()) <= size
                && Math.abs(position.getS()) <= size;
    }
    
    @Override
    protected boolean isValidMove(HexPosition position) {
        // Movimiento válido si la posición está dentro del tablero (sin contar
        // celdas de borde) y no se encuentra bloqueada.
        return isPositionInBounds(position)
                && !isAtBorder(position)
                && !isBlocked(position);
    }
    
    @Override
    protected void executeMove(HexPosition position) {
        blockedPositions.add(position);
    }
    
    @Override
    public List<HexPosition> getPositionsWhere(Predicate<HexPosition> condition) {
        return getAllPossiblePositions().stream()
                .filter(condition)
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<HexPosition> getAdjacentPositions(HexPosition position) {
        HexPosition[] directions = {
            new HexPosition(1, 0),
            new HexPosition(1, -1),
            new HexPosition(0, -1),
            new HexPosition(-1, 0),
            new HexPosition(-1, 1),
            new HexPosition(0, 1)
        };

        return java.util.Arrays.stream(directions)
                .map(dir -> (HexPosition) position.add(dir))
                .filter(this::isPositionInBounds)
                .filter(pos -> !isBlocked(pos))
                .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public boolean isBlocked(HexPosition position) {
        return blockedPositions.contains(position);
    }
    
    // Método auxiliar que los estudiantes pueden implementar
    private List<HexPosition> getAllPossiblePositions() {
        List<HexPosition> positions = new java.util.ArrayList<>();
        for (int q = -size + 1; q < size; q++) {
            for (int r = -size + 1; r < size; r++) {
                HexPosition pos = new HexPosition(q, r);
                if (isPositionInBounds(pos) && !isAtBorder(pos)) {
                    positions.add(pos);
                }
            }
        }
        return positions;
    }
    
    // Hook method override - ejemplo de extensibilidad
    @Override
    protected void onMoveExecuted(HexPosition position) {
        super.onMoveExecuted(position);
    }

    /**
     * Verifica si la posición se encuentra en el borde del tablero
     * (donde el gato podría escapar).
     */
    public boolean isAtBorder(HexPosition position) {
        return Math.abs(position.getQ()) == size
                || Math.abs(position.getR()) == size
                || Math.abs(position.getS()) == size;
    }
} 