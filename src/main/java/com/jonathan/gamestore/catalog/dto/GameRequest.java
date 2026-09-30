package com.jonathan.gamestore.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record GameRequest(
 @NotBlank(message = "El titulo es obligatorio")
 String title,

 String description,

 @NotBlank(message = "El genero es obligatorio")
 String genre,

 @NotNull(message = "El precio es obligatorio")
 @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0")
 BigDecimal price,

 @NotNull(message = "El stock es obligatorio")
 @PositiveOrZero(message = "El stock no puede ser negativo")
 Integer stock,

 List<String> platforms
) {
}
