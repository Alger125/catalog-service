package com.jonathan.gamestore.catalog.repository;

import com.jonathan.gamestore.catalog.model.Game;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * CAPA DE PERSISTENCIA NoSQL: GameRepository
 *
 * ¿Qué es y qué hace?
 * Es la interfaz que conecta directamente nuestra aplicación con la base de datos MongoDB.
 *
 * ¿Cómo funciona sin código adentro?
 * Al heredar de MongoRepository<Game, String>, Spring Data MongoDB genera automáticamente
 * las operaciones CRUD en formato BSON sin que tengamos que escribir consultas a mano:
 * - save(game): Inserta un nuevo documento o actualiza uno existente en la colección 'games'.
 * - findById(String id): Busca un juego por su identificador hexadecimal de 24 caracteres.
 * - findAll(): Recupera todos los documentos de la colección.
 * - deleteById(String id): Elimina físicamente un documento.
 *
 * Consultas Derivadas (Query Methods):
 * Spring Data lee el nombre en inglés del método y construye la consulta NoSQL por ti:
 */
@Repository
public interface GameRepository extends MongoRepository<Game, String> {

 /**
 * Busca todos los videojuegos que pertenezcan a un género específico.
 * 'IgnoreCase' hace que la búsqueda no distinga mayúsculas de minúsculas
 * (ej: rpg, RPG o Rpg devolverán los mismos resultados).
 */
 List<Game> findByGenreIgnoreCase(String genre);

 /**
 * Recupera únicamente los videojuegos que tengan active = true (en venta).
 * Ignora los juegos que fueron dados de baja por borrado lógico.
 */
 List<Game> findByActiveTrue();
}
