package com.jonathan.gamestore.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CONFIGURACION DE SWAGGER / OPENAPI 3
 *
 * Esta clase personaliza los metadatos globales que se muestran
 * en la cabecera de la interfaz grafica interactiva de Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Catalog Service API - GameStore")
                        .description("Microservicio encargado del catalogo de videojuegos, inventario NoSQL (MongoDB) " +
                                "y definicion de precios oficiales dentro del ecosistema GameStore. " +
                                "Consumido por sales-service mediante OpenFeign.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Alger125")
                                .url("https://github.com/Alger125/catalog-service"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://spring.io")));
    }
}
