package com.jonathan.gamestore.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 🌟 CLASE PRINCIPAL: CatalogServiceApplication
 *
 * ¿Qué hace esta clase?
 * Es el punto de arranque de todo el microservicio de Catálogo de Videojuegos.
 * Levanta el servidor web Tomcat interno en el puerto 8082 y conecta con MongoDB.
 *
 * Anotaciones clave explicadas para un Junior:
 * 1. @SpringBootApplication: Inicializa Spring Boot, el contenedor de dependencias
 *    y la autoconfiguración de Spring Data MongoDB.
 *
 * 2. @EnableDiscoveryClient: Le da la orden a este microservicio de buscar a Eureka Server
 *    (puerto 8761) y registrarse automáticamente con el nombre lógico  CATALOG-SERVICE.
 *    Gracias a esto, sales-service podrá descubrirlo a través de Eureka sin conocer su IP física.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class CatalogServiceApplication {

    public static void main(String[] args) {
        // Arranca el microservicio de catálogo
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
