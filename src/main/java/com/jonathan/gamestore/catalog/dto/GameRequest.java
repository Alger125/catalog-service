package com.jonathan.gamestore.catalog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Payload para creacion o actualizacion de un videojuego en el catalogo")
public record GameRequest(
        @Schema(description = "Titulo oficial del videojuego", example = "Elden Ring")
        @NotBlank(message = "El titulo es obligatorio")
        String title,

        @Schema(description = "Sinopsis o descripcion detallada", example = "Juego de rol y accion en un vasto mundo abierto")
        String description,

        @Schema(description = "Genero o categoria principal", example = "RPG")
        @NotBlank(message = "El genero es obligatorio")
        String genre,

        @Schema(description = "Precio oficial en dolares (mayor a 0)", example = "59.99")
        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
        BigDecimal price,

        @Schema(description = "Cantidad de copias disponibles en stock", example = "50")
        @NotNull(message = "El stock es obligatorio")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stock,

        @Schema(description = "Lista de plataformas compatibles", example = "[\"PC\", \"PS5\", \"Xbox Series X\"]")
        List<String> platforms
) {
}
