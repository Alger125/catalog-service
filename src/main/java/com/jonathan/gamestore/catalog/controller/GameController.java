package com.jonathan.gamestore.catalog.controller;

import com.jonathan.gamestore.catalog.dto.GameRequest;
import com.jonathan.gamestore.catalog.model.Game;
import com.jonathan.gamestore.catalog.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CAPA DE CONTROLADOR REST: GameController
 *
 * Expone las operaciones comerciales de administracion del catalogo,
 * inventario y precios de videojuegos sobre MongoDB.
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
@Tag(name = "Catalogo de Videojuegos", description = "Endpoints para administracion de productos, precios e inventario NoSQL")
public class GameController {

    private final GameService gameService;

    @Operation(summary = "Crear nuevo videojuego",
            description = "Da de alta un nuevo videojuego en el catalogo con validacion estructural de precio y stock")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Videojuego creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos (precio <= 0, campos obligatorios vacios)")
    })
    @PostMapping
    public ResponseEntity<Game> createGame(@Valid @RequestBody GameRequest request) {
        Game createdGame = gameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
    }

    @Operation(summary = "Listar todos los videojuegos",
            description = "Retorna la lista completa de videojuegos registrados en MongoDB")
    @ApiResponse(responseCode = "200", description = "Catalogo de videojuegos obtenido exitosamente")
    @GetMapping
    public ResponseEntity<List<Game>> getAllGames() {
        return ResponseEntity.ok(gameService.getAllGames());
    }

    @Operation(summary = "Consultar videojuego por ID",
            description = "Busca los datos y precio oficial de un videojuego por su ObjectId (usado por sales-service)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Videojuego encontrado"),
            @ApiResponse(responseCode = "404", description = "Videojuego no encontrado con el ID proporcionado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Game> getGameById(
            @Parameter(description = "Identificador unico de MongoDB (ObjectId de 24 caracteres)", example = "650c1f1e9b1d8b2bad000001")
            @PathVariable String id) {
        return gameService.getGameById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Filtrar videojuegos por genero",
            description = "Obtiene todos los videojuegos asociados a una categoria o genero especifico")
    @ApiResponse(responseCode = "200", description = "Lista de videojuegos del genero solicitado")
    @GetMapping("/genre/{genre}")
    public ResponseEntity<List<Game>> getGamesByGenre(
            @Parameter(description = "Nombre del genero o categoria (case-insensitive)", example = "RPG")
            @PathVariable String genre) {
        return ResponseEntity.ok(gameService.getGamesByGenre(genre));
    }

    @Operation(summary = "Actualizar datos de videojuego",
            description = "Modifica los atributos, stock o precio oficial de un videojuego existente")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Videojuego actualizado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada invalidos"),
            @ApiResponse(responseCode = "404", description = "Videojuego no encontrado con el ID indicado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Game> updateGame(
            @Parameter(description = "Identificador unico de MongoDB del juego a actualizar", example = "650c1f1e9b1d8b2bad000001")
            @PathVariable String id,
            @Valid @RequestBody GameRequest request) {
        return gameService.updateGame(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Eliminar videojuego (Soft Delete)",
            description = "Desactiva el videojuego marcandolo como inactivo (active = false) preservando el historial para ordenes previas")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Videojuego desactivado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Videojuego no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(
            @Parameter(description = "Identificador unico de MongoDB del juego a desactivar", example = "650c1f1e9b1d8b2bad000001")
            @PathVariable String id) {
        if (gameService.deleteGame(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
