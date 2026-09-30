package com.jonathan.gamestore.catalog.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 📄 DOCUMENTO NoSQL: Game (Mapea a la colección MongoDB: 'games')
 *
 * ¿Qué representa?
 * Representa la ficha técnica y comercial de un videojuego en la tienda.
 *
 * ¿Por qué usamos MongoDB (NoSQL) y no una base de datos relacional (SQL) aquí?
 * - En SQL, las entidades tienen columnas rígidas fijas. Si un juego tiene 3 plataformas
 *   y otro tiene 8, en SQL tendríamos que crear tablas intermedias ( game_platforms)
 *   con claves foráneas y hacer JOINs costosos.
 * - En MongoDB, los datos se guardan como Documentos BSON/JSON flexibles. Las plataformas
 *   se guardan como una lista nativa: [PC, PS5, Xbox] dentro del mismo documento.
 *
 * Anotaciones clave:
 * - @Document(collection = "games"): Es el equivalente NoSQL al @Table de JPA. Le dice a Spring
 *   que guarde estos objetos dentro de la colección llamada games en MongoDB.
 * - @Data: Lombok genera automáticamente getters, setters, toString, equals y hashCode.
 * - @Builder: Permite instanciar juegos con un patrón fluido y elegante:
 *   Game.builder().title(Elden Ring).price(new BigDecimal(59.99)).build()
 */
@Document(collection = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {

    /**
     * @Id: Clave primaria del documento en MongoDB.
     * En MongoDB no se usan enteros autoincrementales (1, 2, 3...) como en SQL.
     * MongoDB genera automáticamente un ObjectId, que es un hash hexadecimal
     * único de 24 caracteres (por ejemplo: 650c1f1e9b1d8b2bad000001).
     * Por esa razón, su tipo de dato en Java es String.
     */
    @Id
    private String id;

    /** Título oficial del videojuego (ej: Elden Ring) */
    private String title;

    /** Sinopsis o descripción del juego */
    private String description;

    /** Género o categoría principal (ej: RPG, Acción, Metroidvania) */
    private String genre;

    /**
     * Precio de venta oficial.
     * Se usa BigDecimal por exactitud monetaria en centavos, evitando imprecisiones de coma flotante.
     */
    private BigDecimal price;

    /** Cantidad de copias o licencias digitales disponibles en inventario */
    private Integer stock;

    /**
     * Lista de plataformas en las que corre el juego (ej: [PC, Steam, PS5]).
     * En NoSQL esto se guarda como un arreglo JSON nativo sin necesidad de tablas foráneas.
     */
    private List<String> platforms;

    /**
     * Estado activo del videojuego.
     * Se usa para Soft Delete (Borrado Lógico):
     * true = Disponible para compra en tienda.
     * false = Descontinuado u oculto, pero preservado en base de datos para no romper
     *         el historial de compras antiguas en sales-service.
     */
    private Boolean active;

    /** Fecha y hora en la que se dio de alta el juego en el catálogo */
    private LocalDateTime createdAt;
}
