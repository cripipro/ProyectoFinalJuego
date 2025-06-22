package com.atraparalagato.controller;

import com.atraparalagato.base.model.GameState;
import com.atraparalagato.example.service.ExampleGameService;
import com.atraparalagato.impl.model.HexPosition;
import com.atraparalagato.impl.model.HexGameState;
import com.atraparalagato.impl.service.HexGameService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controlador REST para Atrapar al Gato:
 * - Inicio y bloqueo de celdas
 * - Persistencia en memoria de puntuaciones
 * - Endpoints de ranking: top, victorias y recientes
 */
@RestController
@RequestMapping("/api/game")
@CrossOrigin(origins = "*")
public class GameController {

    @Value("${game.use-example-implementation:true}")
    private boolean useExampleImplementation;

    private final ExampleGameService exampleGameService;
    private final HexGameService hexGameService;

    /** Lista en memoria de todas las puntuaciones guardadas */
    private final List<ScoreEntry> savedScores = new ArrayList<>();

    /** Registro de cada puntuación */
    private static record ScoreEntry(
        String gameId,
        String playerName,
        int movesCount,
        int boardSize,
        Instant timestamp,
        boolean playerWon
    ) {}

    public GameController() {
        this.exampleGameService = new ExampleGameService();
        this.hexGameService     = new HexGameService();
    }

    /** +++++++++++++++ Iniciar juego +++++++++++++++ */
    @GetMapping("/start")
    public ResponseEntity<Map<String,Object>> startGame(
            @RequestParam(defaultValue="5") int boardSize,
            @RequestParam(defaultValue="5") int difficulty
    ) {
        HexGameState gs = hexGameService.createGame(boardSize, difficulty, Map.of());
        Map<String,Object> out = new HashMap<>();
        out.put("gameId",      gs.getGameId());
        out.put("catPosition", Map.of("q", gs.getCatPosition().getQ(),
                                      "r", gs.getCatPosition().getR()));
        out.put("blockedCells", gs.getGameBoard().getBlockedPositions());
        out.put("movesCount",   gs.getMoveCount());
        return ResponseEntity.ok(out);
    }

    /** +++++++++++++++ Bloquear celda +++++++++++++++ */
    @PostMapping("/block")
    public ResponseEntity<Map<String, Object>> blockPosition(
            @RequestParam String gameId,
            @RequestParam int q,
            @RequestParam int r) {
        try {
            HexPosition pos = new HexPosition(q, r);
            if (useExampleImplementation) {
                return blockPositionWithExample(gameId, pos);
            } else {
                return blockPositionWithStudentImplementation(gameId, pos);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al ejecutar movimiento: " + e.getMessage()));
        }
    }

    /** +++++++++++++++ Estado actual +++++++++++++++ */
    @GetMapping("/state/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameState(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                return getGameStateWithExample(gameId);
            } else {
                return getGameStateWithStudentImplementation(gameId);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estado del juego: " + e.getMessage()));
        }
    }

    /** +++++++++++++++ Estadísticas de partida +++++++++++++++ */
    @GetMapping("/statistics/{gameId}")
    public ResponseEntity<Map<String, Object>> getGameStatistics(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                return ResponseEntity.ok(exampleGameService.getGameStatistics(gameId));
            } else {
                return ResponseEntity.ok(Map.of("error", "Student implementation not available yet"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    /** +++++++++++++++ Sugerencia de movimiento +++++++++++++++ */
    @GetMapping("/suggestion/{gameId}")
    public ResponseEntity<Map<String, Object>> getSuggestion(@PathVariable String gameId) {
        try {
            if (useExampleImplementation) {
                Optional<HexPosition> suggestion = exampleGameService.getSuggestedMove(gameId);
                if (suggestion.isPresent()) {
                    HexPosition p = suggestion.get();
                    return ResponseEntity.ok(Map.of(
                        "suggestion", Map.of("q", p.getQ(), "r", p.getR()),
                        "message", "Sugerencia: bloquear posición adyacente al gato"
                    ));
                } else {
                    return ResponseEntity.ok(Map.of("message", "No hay sugerencias disponibles"));
                }
            } else {
                return ResponseEntity.ok(Map.of("error", "Student implementation not available yet"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al obtener sugerencia: " + e.getMessage()));
        }
    }

    /** +++++++++++++++ Información de implementación +++++++++++++++ */
    @GetMapping("/implementation-info")
    public ResponseEntity<Map<String,Object>> getImplementationInfo() {
        Map<String,Object> info = new HashMap<>();
        info.put("useExampleImplementation", useExampleImplementation);
        info.put("currentImplementation",
                 useExampleImplementation ? "example" : "impl");
        info.put("description",
                 useExampleImplementation ?
                 "Usando implementaciones de ejemplo (básicas)" :
                 "Usando implementaciones de estudiantes");
        return ResponseEntity.ok(info);
    }

    /** +++++++++++++++ Guardar puntuación +++++++++++++++ */
    @PostMapping("/save-score")
    public ResponseEntity<Map<String,Object>> saveScore(
            @RequestParam String gameId,
            @RequestParam String playerName) {

        Optional<GameState<HexPosition>> opt = hexGameService.loadGameState(gameId);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HexGameState gs = (HexGameState) opt.get();
        int moves = gs.getMoveCount();
        int size  = gs.getBoardSize();
        boolean won = gs.hasPlayerWon();

        // Persistir en memoria
        savedScores.add(new ScoreEntry(
            gameId, playerName, moves, size, Instant.now(), won
        ));

        Map<String,Object> resp = new HashMap<>();
        resp.put("gameId",     gameId);
        resp.put("playerName", playerName);
        resp.put("movesCount", moves);
        resp.put("boardSize",  size);
        resp.put("playerWon",  won);
        return ResponseEntity.ok(resp);
    }

    /** +++++++++++++++ Top N por menos movimientos +++++++++++++++ */
    @GetMapping("/high-scores")
    public ResponseEntity<List<Map<String,Object>>> highScores(
            @RequestParam(defaultValue="10") int limit) {
        var list = savedScores.stream()
            .sorted(Comparator.comparingInt(ScoreEntry::movesCount))
            .limit(limit)
            .map(this::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** +++++++++++++++ Top N de victorias (solo ganadas) +++++++++++++++ */
    @GetMapping("/winning-scores")
    public ResponseEntity<List<Map<String,Object>>> winningScores(
            @RequestParam(defaultValue="10") int limit) {
        var list = savedScores.stream()
            .filter(ScoreEntry::playerWon)
            .sorted(Comparator.comparingInt(ScoreEntry::movesCount))
            .limit(limit)
            .map(this::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** +++++++++++++++ N más recientes por fecha +++++++++++++++ */
    @GetMapping("/recent-scores")
    public ResponseEntity<List<Map<String,Object>>> recentScores(
            @RequestParam(defaultValue="10") int limit) {
        var list = savedScores.stream()
            .sorted(Comparator.comparing(ScoreEntry::timestamp).reversed())
            .limit(limit)
            .map(this::toMap)
            .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    /** Helper para convertir cada ScoreEntry a JSON */
    private Map<String,Object> toMap(ScoreEntry e) {
        Map<String,Object> m = new HashMap<>();
        m.put("gameId",     e.gameId());
        m.put("playerName", e.playerName());
        m.put("movesCount", e.movesCount());
        m.put("boardSize",  e.boardSize());
        m.put("playerWon",  e.playerWon());
        m.put("timestamp",  e.timestamp().toString());
        return m;
    }

    // … Métodos privados de ejemplo y estudiante que ya tenías …
    // blockPositionWithExample, blockPositionWithStudentImplementation, etc.
    // … después de todos tus endpoints de ranking …

    // Métodos privados para implementación de ejemplo
    private ResponseEntity<Map<String, Object>> blockPositionWithExample(String gameId, HexPosition position) {
        var gameStateOpt = exampleGameService.executePlayerMove(gameId, position);
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var gs = gameStateOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("gameId",      gs.getGameId());
        response.put("status",      gs.getStatus().toString());
        response.put("catPosition", Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
        response.put("blockedCells", gs.getGameBoard().getBlockedPositions());
        response.put("movesCount",   gs.getMoveCount());
        response.put("implementation", "example");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> getGameStateWithExample(String gameId) {
        var gameStateOpt = exampleGameService.getGameState(gameId);
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var gs = gameStateOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("gameId",       gs.getGameId());
        response.put("status",       gs.getStatus().toString());
        response.put("catPosition",  Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
        response.put("blockedCells", gs.getGameBoard().getBlockedPositions());
        response.put("movesCount",   gs.getMoveCount());
        response.put("implementation", "example");
        return ResponseEntity.ok(response);
    }

    // Métodos privados para implementación de estudiantes
    private ResponseEntity<Map<String, Object>> blockPositionWithStudentImplementation(String gameId, HexPosition position) {
        var gameStateOpt = hexGameService.executePlayerMove(gameId, position);
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        HexGameState gs = (HexGameState) gameStateOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("gameId",       gs.getGameId());
        response.put("status",       gs.getStatus().toString());
        response.put("catPosition",  Map.of("q", gs.getCatPosition().getQ(), "r", gs.getCatPosition().getR()));
        response.put("blockedCells", gs.getGameBoard().getBlockedPositions());
        response.put("movesCount",   gs.getMoveCount());
        response.put("implementation", "impl");
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> getGameStateWithStudentImplementation(String gameId) {
        var gameStateOpt = hexGameService.getEnrichedGameState(gameId);
        if (gameStateOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = gameStateOpt.get();
        response.put("implementation", "impl");
        return ResponseEntity.ok(response);
    }
}



