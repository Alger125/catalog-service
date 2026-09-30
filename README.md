# 🎮 Catalog Service (`catalog-service`)

> **Microservicio de Gestión de Catálogo de Videojuegos, Fichas Técnicas, Inventario y Precios Oficiales.**  
> Desarrollado con **Java 17**, **Spring Boot 4**, **Spring Data MongoDB (NoSQL)** y **Spring Cloud Netflix Eureka**.

---

## 🏛️ 1. ¿Por qué es un Microservicio Independiente? (Autonomía y NoSQL)

Una de las decisiones arquitectónicas clave del proyecto es:  
**¿Por qué el catálogo usa MongoDB (NoSQL) mientras que ventas usa SQL relacional?**

### 🍃 Flexibilidad de Esquema (Schema-less)
* Los videojuegos no tienen una estructura rígida: unos tienen requisitos de sistema para PC, otros son exclusivos de PlayStation 5, otros tienen múltiples DLCs, géneros o plataformas (`List<String>`).
* **MongoDB** almacena la información como documentos BSON (JSON binario), lo que permite agregar nuevos atributos a un juego (como enlaces a trailers, puntuación de Metacritic o idiomas) **sin tener que alterar tablas ni hacer migraciones costosas (`ALTER TABLE`)** en la base de datos.

### 🛡️ Fuente Única de la Verdad (Source of Truth)
* `catalog-service` es el **dueño absoluto** de los productos: títulos, descripción, géneros, stock disponible y **precios oficiales**.
* Ningún otro microservicio (ni siquiera `sales-service`) puede modificar directamente los precios o fichas en la colección de Mongo; están obligados a comunicarse por HTTP REST.

---

## 🤝 2. Mapa de Relación: ¿Cómo se comunica con los demás Microservicios?

```
                                  ┌─────────────────────────────┐
                                  │        EUREKA SERVER        │
                                  │   (Directorio Telefónico)   │
                                  │         Puerto 8761         │
                                  └──────────────┬──────────────┘
                                                 │
                          ┌──────────────────────┴──────────────────────┐
                          │ 1. Heartbeat ("Estoy vivo en 8081")         │ 1. Heartbeat ("Estoy vivo en 8082")
                          ▼                                             ▼
            ┌───────────────────────────┐                 ┌───────────────────────────┐
            │       SALES-SERVICE       │                 │      CATALOG-SERVICE      │
            │        Puerto 8081        │                 │        Puerto 8082        │
            │     Base de Datos H2      │                 │    Base de Datos Mongo    │
            │   (Ventas y CD-Keys)      │                 │    (Fichas de Juegos)     │
            └─────────────┬─────────────┘                 └─────────────┬─────────────┘
                          │                                             ▲
                          │ 2. Consulta de Precios y Stock              │
                          │    GET /api/games/{id}                      │
                          └─────────────────────────────────────────────┘
```

### 1. Relación con `eureka-server` (Puerto 8761)
* Al iniciar, `catalog-service` se registra automáticamente con el nombre lógico `CATALOG-SERVICE` en el puerto `8082`.
* Envía *heartbeats* periódicos para avisar que está disponible para recibir consultas.

### 2. Relación con `sales-service` (Puerto 8081)
* Cuando un usuario compra en la tienda, `sales-service` llama mediante **OpenFeign** a este microservicio:
  `GET http://catalog-service/api/games/{id}`
* `catalog-service` busca el juego en MongoDB y le responde con su precio real y existencias en stock, blindando la transacción contra fraudes.

---

## 🧭 3. Guía Pedagógica: Arquitectura por Capas

El flujo interno de una petición en `catalog-service` sigue la separación estricta de responsabilidades:

```
[Petición Externa / OpenFeign desde Sales-Service]
                       │ 1. GET /api/games/{id}
                       ▼
┌─────────────────────────────────────────────────────────────┐
│ 1. CONTROLADOR: GameController                              │
│    - Expone los endpoints REST en /api/games                │
│    - Valida los datos entrantes con @Valid                  │
│    - Retorna códigos HTTP estándar (200, 201, 204, 404)     │
└─────────────────────────────┬───────────────────────────────┘
                               │ 2. Delega al servicio
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 2. SERVICIO: GameService                                    │
│    - Contiene las reglas de negocio                         │
│    - Aplica Soft Delete (desactivación lógica active=false) │
│    - Gestiona actualizaciones de stock y precios            │
└─────────────────────────────┬───────────────────────────────┘
                               │ 3. Consulta la base de datos
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 3. REPOSITORIO: GameRepository (MongoRepository)            │
│    - Habla directamente con MongoDB                         │
│    - Métodos automáticos: findById, findAll, save,          │
│      findByGenre, findByActiveTrue                          │
└─────────────────────────────┬───────────────────────────────┘
                               │ 4. Persistencia NoSQL
                               ▼
┌─────────────────────────────────────────────────────────────┐
│ 4. BASE DE DATOS: MongoDB (Colección "games")               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 4. Anatomía Detallada de Clases

### Paquete: `com.jonathan.gamestore.catalog`

#### 🌟 `CatalogServiceApplication.java`
* **¿Qué es?**: Punto de entrada de la aplicación Spring Boot.
* **Anotaciones**:
  * `@SpringBootApplication`: Arranca el servidor embebido y la inyección de dependencias.
  * `@EnableDiscoveryClient`: Permite que Eureka Server descubra este microservicio.

---

### Paquete: `com.jonathan.gamestore.catalog.model`

#### 🗃️ `Game.java` (Documento MongoDB)
* **¿Qué es?**: Representa un documento dentro de la colección `games` de MongoDB.
* **Anotaciones clave**:
  * `@Document(collection = "games")`: Indica a Spring Data MongoDB que esta clase se guarda en la colección `games`.
  * `@Id String id`: Identificador único (ObjectId de 24 caracteres hexadecimales generado automáticamente por MongoDB).
  * `title`: Título del juego.
  * `description`: Sinopsis.
  * `genre`: Categoría (RPG, Acción, etc.).
  * `price`: Precio oficial de venta (`BigDecimal`).
  * `stock`: Cantidad de licencias disponibles.
  * `platforms`: Lista de plataformas compatibles (PC, PS5, Xbox).
  * `active`: Booleano para el borrado lógico (`true` = activo, `false` = descontinuado).

---

### Paquete: `com.jonathan.gamestore.catalog.dto`

#### 📦 `GameRequest.java` (Record)
* **¿Qué es?**: DTO inmutable utilizado para recibir los datos al crear o editar un juego.
* **Validaciones**:
  * `@NotBlank(message = "El titulo es obligatorio") String title`
  * `@NotBlank(message = "El genero es obligatorio") String genre`
  * `@NotNull @DecimalMin("0.01") BigDecimal price`: El precio debe ser mayor a 0.
  * `@NotNull @PositiveOrZero Integer stock`: El stock no puede ser negativo.

---

### Paquete: `com.jonathan.gamestore.catalog.repository`

#### 🗄️ `GameRepository.java`
* **¿Qué es?**: Interfaz que extiende de `MongoRepository<Game, String>`.
* **Consultas personalizadas**:
  * `findByGenre(String genre)`: Retorna juegos filtrados por género.
  * `findByActiveTrue()`: Retorna solo los juegos activos en tienda.

---

### Paquete: `com.jonathan.gamestore.catalog.service`

#### 🧠 `GameService.java`
* **¿Qué es?**: Capa de negocio del catálogo.
* **Características especiales**:
  * **Soft Delete (Borrado Lógico)**: En lugar de borrar el documento de la base de datos (`repository.delete`), cambia el estado a `active = false`. Esto protege la integridad histórica: si alguien compró ese juego en el pasado, la orden de venta no se romperá por una referencia nula.

---

### Paquete: `com.jonathan.gamestore.catalog.controller`

#### 🌐 `GameController.java`
* **¿Qué es?**: Controlador REST expuesto en `/api/games`.

---

## 📡 5. Catálogo de Endpoints RESTful (`/api/games`)

| Método | Endpoint | Descripción | Código Éxito | Códigos Error |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/games` | Registra un nuevo videojuego en el catálogo | `201 Created` | `400 Bad Request` |
| `GET` | `/api/games` | Lista todos los videojuegos registrados | `200 OK` | `500 Internal Error` |
| `GET` | `/api/games/{id}` | Obtiene la ficha técnica y precio de un juego por su ObjectId | `200 OK` | `404 Not Found` |
| `GET` | `/api/games/genre/{genre}` | Filtra videojuegos por género | `200 OK` | - |
| `PUT` | `/api/games/{id}` | Actualiza información o stock de un videojuego | `200 OK` | `404 Not Found`, `400 Bad Request` |
| `DELETE` | `/api/games/{id}` | Desactiva un juego del catálogo (Soft Delete) | `204 No Content` | `404 Not Found` |

---

## 🧪 6. Guía de Pruebas Rápidas con PowerShell

### Crear un Videojuego:
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games" -Method Post -ContentType "application/json" -Body '{
  "title": "Elden Ring",
  "description": "Juego de rol y accion en mundo abierto",
  "genre": "RPG",
  "price": 59.99,
  "stock": 50,
  "platforms": ["PC", "PS5", "Xbox Series X"]
}' | ConvertTo-Json -Depth 5
```

### Consultar todos los Videojuegos:
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games" -Method Get | ConvertTo-Json -Depth 5
```

### Consultar un Juego por ID (el que llama OpenFeign):
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games/TU_ID_DE_MONGO" -Method Get | ConvertTo-Json -Depth 5
```
