package com.jonathan.gamestore.catalog.controller;

import com.jonathan.gamestore.catalog.dto.GameRequest;
import com.jonathan.gamestore.catalog.model.Game;
import com.jonathan.gamestore.catalog.service.GameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ðŸŒ CAPA DE CONTROLADOR REST: GameController
 *
 * Â¿QuÃ© representa esta clase?
 * Es el "Mesero" del catÃ¡logo. Atiende todas las peticiones HTTP que llegan al puerto 8082
 * bajo la ruta "/api/games".
 *
 * Su Ãºnica funciÃ³n es:
 * 1. Recibir los datos de la peticiÃ³n HTTP.
 * 2. Validar que cumplan las anotaciones del DTO0mediante @Valid.
 * 3. Pasarle el trabajo al GameService (la cocina).
 * 4. Retornar el cÃ³digo HTTP estÃ¡ndar de la industria (201 Created, 200 OK, 204 No Content, 404 Not Found).
 */
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

 // InyecciÃ³n de dependencias de la capa de servicio
 private final GameService gameService;

 /**
 * ðŸŸ§ POST /api/games
 * Da de alta un nuevo videojuego en el catÃ¡lego.
 *
 * @Valid: Fuerza la validaciÃ³n de los campos de GameRequest antes de ejecutar el mÃ©todo.
 * @RequestBody: Deserializa el JSON enviado por el cliente a un objeto Java GameRequest.
 * @return HTTP 201 (Created) con el documento del juego reciÃ©n insertado.
 */
 @PostMapping
 public ResponseEntity<Game> createGame(@Valid @RequestBody GameRequest request) {
 Game createdGame = gameService.createGame(request);
 return ResponseEntity.status(HttpStatus.CREATED).body(createdGame);
 }

 /**
 * ðŸœµ GET /api/games
 * Lista todos los videojuegos registrados en la base de datos de MongoDB.
 * @return HTTP 200 (OK) con el arreglo JSON de videojuegos.
 */
 @GetMapping
 public ResponseEntity<List<Game>> getAllGames() {
 return ResponseEntity.ok(gameService.getAllGames());
 }

 /**
 * ðŸœµ GET /api/games/{id}
 * Busca un juego por su identificador Ãºnico de MongoDB (ej: /api/games/650c1f1e9b1d8b2bad000001).
 *
 * @PathVariable: Captura el valor dinÃ¡mico del ID en la URL.
 * @return HTTP 200 (OK) si existe, o HTTP 404 (Not Found) si no se encontrÃ³.
 */
 @GetMapping("/{id}")
 public ResponseEntity<Game> getGameById(@PathVariable String id) {
 return gameService.getGameById(id)
 .map(ResponseEntity::ok)
 .orElse(ResponseEntity.notFound().build());
 }

 /**
 * ðŸœµ GET /api/games/genre/{genre}
 * Filtra los juegos por su categorÃ­a (ej: /api/games/genre/RPG).
 * @return HTTP 200 (OK) con la lista de juegos de ese gÃ©nero.
 */
 @GetMapping("/genre/{genre}")
 public ResponseEntity<List<Game>> getGamesByGenre(@PathVariable String genre) {
 return ResponseEntity.ok(gameService.getGamesByGenre(genre));
 }

 /**
 * ðŸ“ PUT /api/games/{id}
 * Modifica los datos de un juego existente.
 * @return HTTP 200 (OK) con el juego actualizado, o HTTP 404 (Not Found) si no existÃ­a.
 */
 @PutMapping("/{id}")
 public ResponseEntity<Game> updateGame(@PathVariable String id, @Valid @RequestBody GameRequest request) {
 return gameService.updateGame(id, request)
 .map(ResponseEntity::ok)
 .orElse(ResponseEntity.notFound().build());
 }

 /**
 * ðŸœ DELETE /api/games/{id}
 * Desactiva un juego mediante Soft Delete (active = false).
 * @return HTTP 204 (No Content) indicando desactivaciÃ³n exitosa, o HTTP 404 (Not Found) si no existÃ­a.
 */
 @DeleteMapping("/{id}")
 public ResponseEntity<Void> deleteGame(@PathVariable String id) {
 if (gameService.deleteGame(id)) {
 return ResponseEntity.noContent().build();
 }
 return ResponseEntity.notFound().build();
 }
}
