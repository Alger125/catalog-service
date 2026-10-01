package com.jonathan.gamestore.catalog.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DOCUMENTO NoSQL: Game (Mapea a la coleccion MongoDB: 'games')
 *
 * Representa la ficha tecnica y comercial de un videojuego en la tienda.
 */
@Document(collection = "games")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Entidad que representa un videojuego en el catalogo de MongoDB")
public class Game {

    /**
     * Clave primaria del documento en MongoDB (ObjectId de 24 caracteres).
     */
    @Id
    @Schema(description = "Identificador unico de MongoDB (ObjectId)", example = "650c1f1e9b1d8b2bad000001")
    private String id;

    @Schema(description = "Titulo oficial del videojuego", example = "Elden Ring")
    private String title;

    @Schema(description = "Sinopsis o descripcion del juego", example = "Juego de rol y accion en mundo abierto")
    private String description;

    @Schema(description = "Genero o categoria principal", example = "RPG")
    private String genre;

    @Schema(description = "Precio de venta oficial", example = "59.99")
    private BigDecimal price;

    @Schema(description = "Copias o licencias digitales disponibles en inventario", example = "50")
    private Integer stock;

    @Schema(description = "Lista de plataformas compatibles", example = "[\"PC\", \"PS5\", \"Xbox Series X\"]")
    private List<String> platforms;

    @Schema(description = "Estado activo del videojuego (false si fue desactivado via Soft Delete)", example = "true")
    private Boolean active;

    @Schema(description = "Fecha y hora de registro en el catalogo", example = "2026-10-01T10:00:00")
    private LocalDateTime createdAt;
}
