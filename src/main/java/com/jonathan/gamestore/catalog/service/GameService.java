package com.jonathan.gamestore.catalog.service;

import com.jonathan.gamestore.catalog.dto.GameRequest;
import com.jonathan.gamestore.catalog.model.Game;
import com.jonathan.gamestore.catalog.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 🧠 CAPA DE NEGOCIO: GameService
 *
 * ¿Qué representa esta clase?
 * Es el cerebro del catálogo de videojuegos. Aquí se procesan todas las decisiones,
 * reglas comerciales, transformaciones de DTOs a Entidades de MongoDB y borrados lógicos.
 *
 * Principio de Responsabilidad Única (SRP):
 * Ni el Controller ni el Repository deben contener reglas de negocio.
 * El Controller solo atiende peticiones HTTP y el Repository solo ejecuta consultas.
 * Toda la lógica vive aquí.
 */
@Service
@RequiredArgsConstructor
public class GameService {

    // Dependencia al repositorio NoSQL inyectada por constructor (Lombok @RequiredArgsConstructor)
    private final GameRepository gameRepository;

    /**
     * 🎮 CREAR UN NUEVO JUEGO
     * Toma los datos validados del DTO, construye la entidad Game, asigna metadatos
     * obligatorios del sistema (activo = true, fecha actual) y guarda en MongoDB.
     *
     * @param request Datos del juego enviados por el cliente.
     * @return El juego persistido con su ID autogenerado de MongoDB.
     */
    public Game createGame(GameRequest request) {
        Game game = Game.builder()
                .title(request.title())
                .description(request.description())
                .genre(request.genre())
                .price(request.price())
                .stock(request.stock())
                .platforms(request.platforms())
                .active(true)                   // Regla de negocio: Todo juego nuevo nace activo para venta
                .createdAt(LocalDateTime.now()) // Regla de negocio: Asignamos la marca de tiempo exacta de alta
                .build();

        return gameRepository.save(game);
    }

    /**
     * 📋 LISTAR TODOS LOS JUEGOS
     * Retorna todos los documentos almacenados en la colección 'games'.
     */
    public List<Game> getAllGames() {
        return gameRepository.findAll();
    }

    /**
     * 🔍 BUSCAR UN JUEGO POR SU ID
     * Devuelve Optional<Game> para manejar de forma elegante y segura el caso en que el ID no exista,
     * evitando NullPointerException.
     */
    public Optional<Game> getGameById(String id) {
        return gameRepository.findById(id);
    }

    /**
     * 🎯 FILTRAR JUEGOS POR GÉNERO
     * Busca todos los títulos que pertenezcan a una categoría (ej:  RPG, Acción).
     */
    public List<Game> getGamesByGenre(String genre) {
        return gameRepository.findByGenreIgnoreCase(genre);
    }

    /**
     * ✏️ ACTUALIZAR DATOS DE UN JUEGO
     * Si el juego existe, actualiza sus campos comerciales (título, precio, stock, etc.)
     * conservando su ID original y su fecha de creación.
     */
    public Optional<Game> updateGame(String id, GameRequest request) {
        return gameRepository.findById(id).map(existingGame -> {
            existingGame.setTitle(request.title());
            existingGame.setDescription(request.description());
            existingGame.setGenre(request.genre());
            existingGame.setPrice(request.price());
            existingGame.setStock(request.stock());
            existingGame.setPlatforms(request.platforms());
            return gameRepository.save(existingGame);
        });
    }

    /**
     * 🗑️ SOFT DELETE (BORRADO LÓGICO)
     * ¡Concepto Senior clave para Juniors!
     * En sistemas de comercio electrónico reales, NUNCA debes borrar un producto físicamente de la base de datos
     * (DELETE FROM games). ¿Por qué? Porque si un usuario compró ese juego hace 3 meses,
     * la orden en 'sales-service' hace referencia a ese gameId. Si destruyes el juego, romperías el historial de ventas.
     *
     * En su lugar, aplicamos borrado suave:
     * Cambiamos 'active = false'. El juego deja de verse en la tienda, pero sus datos se conservan intactos.
     *
     * @param id Identificador del juego a desactivar.
     * @return true si el juego existía y fue desactivado; false si no se encontró.
     */
    public boolean deleteGame(String id) {
        return gameRepository.findById(id).map(existingGame -> {
            existingGame.setActive(false);
            gameRepository.save(existingGame);
            return true;
        }).orElse(false);
    }
}
