# Catalog Service (catalog-service)

> **Microservicio de Gestion de Catalogo de Videojuegos, Fichas Tecnicas, Inventario y Precios Oficiales.**  
> Desarrollado con **Java 17**, **Spring Boot 4**, **Spring Data MongoDB (NoSQL)** y **Spring Cloud Netflix Eureka**.

---

## 1. Introduccion y Justificacion de Persistencia NoSQL

Una de las decisiones arquitectonicas primordiales en el ecosistema es:  
**¿Por que catalog-service utiliza MongoDB (NoSQL) mientras sales-service utiliza SQL relacional?**

### Flexibilidad de Esquema (Schema-less)
* Los videojuegos presentan estructuras heterogeneas: compatibilidad de hardware, clasificaciones por edades, soporte multilenguaje, listas dinamicas de plataformas (`List<String>`) y complementos descargables (DLCs).
* **MongoDB** almacena la informacion como documentos BSON (formato JSON binario). Esto permite agregar o modificar propiedades tecnicas de un juego sin necesidad de ejecutar sentencias bloqueantes de migracion (`ALTER TABLE`) como ocurre en bases relacionales tradicionales.

### Fuente Unica de la Verdad (Source of Truth)
* `catalog-service` es el propietario exclusivo de las fichas tecnicas, existencias en almacen y **precios oficiales**.
* Ningun microservicio externo tiene permitido alterar de forma directa la coleccion de MongoDB; todas las consultas y modificaciones deben canalizarse a traves de su interfaz RESTful.

---

## 2. Diagrama de Comunicacion en la Arquitectura

```
                                  +-----------------------------+
                                  |        EUREKA SERVER        |
                                  |   (Directorio Central)      |
                                  |         Puerto 8761         |
                                  +--------------+--------------+
                                                 |
                          +----------------------+----------------------+
                          | 1. Heartbeat ("Estoy vivo en 8081")         | 1. Heartbeat ("Estoy vivo en 8082")
                          v                                             v
            +---------------------------+                 +---------------------------+
            |       SALES-SERVICE       |                 |      CATALOG-SERVICE      |
            |        Puerto 8081        |                 |        Puerto 8082        |
            |     Base de Datos H2      |                 |    Base de Datos Mongo    |
            |   (Transacciones SQL)     |                 |     (Catalogo NoSQL)      |
            +-------------+-------------+                 +-------------+-------------+
                          |                                             ^
                          | 2. Consulta de Precios y Stock (OpenFeign)  |
                          |    GET /api/games/{id}                      |
                          +---------------------------------------------+
```

---

## 3. Arquitectura Interna por Capas (Clean Architecture)

El microservicio desacopla estrictamente sus responsabilidades siguiendo el patron arquitectonico por capas:

```
[Peticion HTTP Externa o desde Sales-Service]
                       | 1. Llega al endpoint /api/games
                       v
+-------------------------------------------------------------+
| 1. CAPA CONTROLADOR: GameController                         |
|    - Expone los endpoints REST                              |
|    - Valida los datos entrantes mediante @Valid y Bean Valid.|
|    - Retorna codigos HTTP estandar (200, 201, 204, 404)     |
+-----------------------------+-------------------------------+
                               | 2. Invoca metodos del servicio
                               v
+-------------------------------------------------------------+
| 2. CAPA SERVICIO: GameService                               |
|    - Contiene las reglas de negocio del catalogo            |
|    - Aplica Soft Delete (desactivacion logica active=false) |
|    - Gestiona conversiones de DTO a entidad de dominio      |
+-----------------------------+-------------------------------+
                               | 3. Solicita persistencia
                               v
+-------------------------------------------------------------+
| 3. CAPA REPOSITORIO: GameRepository (MongoRepository)       |
|    - Interfaz de persistencia NoSQL                         |
|    - Genera consultas derivadas automaticas                 |
+-----------------------------+-------------------------------+
                               | 4. Lectura/Escritura BSON
                               v
+-------------------------------------------------------------+
| 4. BASE DE DATOS: MongoDB (Coleccion "games")               |
+-------------------------------------------------------------+
```

---

## 4. Anatomia Detallada de Clases y Componentes

### Paquete: `com.jonathan.gamestore.catalog`

#### `CatalogServiceApplication.java`
* Punto de entrada del microservicio.
* Anotaciones clave:
  * `@SpringBootApplication`: Arranca la autoconfiguracion, escaneo de componentes y servidor embebido.
  * `@EnableDiscoveryClient`: Activa el registro en Eureka Server (`http://localhost:8761`).

---

### Paquete: `com.jonathan.gamestore.catalog.model`

#### `Game.java` (Entidad / Documento NoSQL)
* Modela el documento persistido dentro de la coleccion `games` de MongoDB.
* Atributos:
  * `@Id String id`: Clave primaria administrada como un ObjectId hexadecimal de 24 caracteres autogenerado por MongoDB.
  * `String title`: Nombre oficial del videojuego.
  * `String description`: Sinopsis o resumen descriptivo.
  * `String genre`: Genero de clasificacion (RPG, Accion, Estrategia, etc.).
  * `BigDecimal price`: Precio oficial de venta (utiliza `BigDecimal` para precision monetaria).
  * `Integer stock`: Unidades disponibles para compra.
  * `List<String> platforms`: Plataformas compatibles (`["PC", "PS5", "Xbox Series X"]`).
  * `Boolean active`: Bandera booleana para borrado suave (`true` = activo, `false` = descontinuado).
  * `LocalDateTime createdAt`: Marca de tiempo de registro en base de datos.

---

### Paquete: `com.jonathan.gamestore.catalog.dto`

#### `GameRequest.java` (Data Transfer Object)
* Java `record` inmutable que transporta los datos enviados por los clientes para operaciones de creacion y actualizacion.
* Reglas de validacion integradas:
  * `@NotBlank(message = "El titulo es obligatorio") String title`
  * `String description` (campo opcional)
  * `@NotBlank(message = "El genero es obligatorio") String genre`
  * `@NotNull(message = "El precio es obligatorio") @DecimalMin(value = "0.01", message = "El precio debe ser mayor a 0") BigDecimal price`
  * `@NotNull(message = "El stock es obligatorio") @PositiveOrZero(message = "El stock no puede ser negativo") Integer stock`
  * `List<String> platforms`

---

### Paquete: `com.jonathan.gamestore.catalog.repository`

#### `GameRepository.java`
* Interfaz que hereda de `MongoRepository<Game, String>`.
* Consultas derivadas provistas por Spring Data MongoDB:
  * `List<Game> findByGenreIgnoreCase(String genre)`: Filtrado dinamico por categoria sin distincion de mayusculas/minusculas.
  * `List<Game> findByActiveTrue()`: Consulta orientada a retornar exclusivamente juegos activos para publicacion en catalogo.

---

### Paquete: `com.jonathan.gamestore.catalog.service`

#### `GameService.java`
* Implementa la logica de negocio:
  * `createGame(GameRequest request)`: Inicializa la entidad con `active = true` y `createdAt = now()`, persistiendo el documento en MongoDB.
  * `getAllGames()`: Retorna la coleccion completa.
  * `getGameById(String id)`: Busca por ID devolviendo `Optional<Game>` para prevenir `NullPointerException`.
  * `getGamesByGenre(String genre)`: Filtra por clasificacion.
  * `updateGame(String id, GameRequest request)`: Actualiza selectivamente los atributos comerciales manteniendo el ID y fecha originales.
  * `deleteGame(String id)`: **Implementacion de Soft Delete (Borrado Logico)**. En lugar de eliminar el documento fisico, marca `active = false`. Esto previene la perdida de integridad referencial con las ordenes de compra emitidas en `sales-service`.

---

### Paquete: `com.jonathan.gamestore.catalog.controller`

#### `GameController.java`
* Controlador REST expuesto bajo el path `/api/games`.
* Coordina la entrada y salida de datos HTTP traduciendo los resultados del servicio a codigos de estado estandar.

---

## 5. Catalogo Completo de Endpoints RESTful (CRUD)

| Operacion CRUD | Metodo HTTP | Ruta Endpoint | Descripcion del Recurso | Codigo Exito | Codigos Falla |
| :--- | :---: | :--- | :--- | :---: | :---: |
| **CREATE** | `POST` | `/api/games` | Da de alta un nuevo videojuego con validacion Bean Validation | `201 Created` | `400 Bad Request` |
| **READ (All)** | `GET` | `/api/games` | Recupera el listado completo de videojuegos | `200 OK` | `500 Internal Error` |
| **READ (ById)** | `GET` | `/api/games/{id}` | Obtiene los detalles de un juego por su ObjectId (usado por OpenFeign) | `200 OK` | `404 Not Found` |
| **READ (Filter)**| `GET` | `/api/games/genre/{genre}` | Recupera los juegos clasificados por un genero especifico | `200 OK` | - |
| **UPDATE** | `PUT` | `/api/games/{id}` | Actualiza atributos comerciales de un juego existente | `200 OK` | `404 Not Found`, `400 Bad Request` |
| **DELETE** | `DELETE` | `/api/games/{id}` | Desactiva el juego del catalogo aplicando Soft Delete (`active=false`) | `204 No Content`| `404 Not Found` |

---

## 6. Guia Exhaustiva de Pruebas CRUD (cURL y PowerShell)

### 1. CREATE: Crear un nuevo videojuego
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games" -Method Post -ContentType "application/json" -Body '{
  "title": "Elden Ring",
  "description": "Juego de rol y accion en mundo abierto de FromSoftware",
  "genre": "RPG",
  "price": 59.99,
  "stock": 50,
  "platforms": ["PC", "PS5", "Xbox Series X"]
}' | ConvertTo-Json -Depth 5
```
*Respuesta esperada:* `HTTP 201 Created` con el campo `"id": "674a123f8b1c4e..."`.

---

### 2. READ: Listar todos los videojuegos
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games" -Method Get | ConvertTo-Json -Depth 5
```
*Respuesta esperada:* `HTTP 200 OK` conteniendo el arreglo JSON de juegos registrados.

---

### 3. READ: Buscar un videojuego por su identificador unico
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games/TU_ID_AQUI" -Method Get | ConvertTo-Json -Depth 5
```
*Respuesta esperada:* `HTTP 200 OK` con la informacion del juego, o `HTTP 404 Not Found` si el ID no existe.

---

### 4. READ: Filtrar videojuegos por genero
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games/genre/RPG" -Method Get | ConvertTo-Json -Depth 5
```
*Respuesta esperada:* `HTTP 200 OK` con los videojuegos asociados a dicho genero.

---

### 5. UPDATE: Actualizar datos de un videojuego existente
```powershell
Invoke-RestMethod -Uri "http://localhost:8082/api/games/TU_ID_AQUI" -Method Put -ContentType "application/json" -Body '{
  "title": "Elden Ring: Shadow of the Erdtree Edition",
  "description": "Edicion que incluye la expansion oficial",
  "genre": "RPG",
  "price": 79.99,
  "stock": 35,
  "platforms": ["PC", "PS5", "Xbox Series X"]
}' | ConvertTo-Json -Depth 5
```
*Respuesta esperada:* `HTTP 200 OK` con los campos actualizados.

---

### 6. DELETE: Desactivar videojuego (Soft Delete)
```powershell
Invoke-WebRequest -Uri "http://localhost:8082/api/games/TU_ID_AQUI" -Method Delete
```
*Respuesta esperada:* `HTTP 204 No Content`. El documento permanecera en MongoDB con el atributo `"active": false`.

---

## 7. Instrucciones de Ejecucion Optimizada (8 GB RAM Setup)

```powershell
cd C:\Users\ErickJimz\IdeaProjects\catalog-service
.\mvnw.cmd clean package -DskipTests
java -Xmx300m -jar .\target\catalog-service-0.0.1-SNAPSHOT.jar
```
* **Puerto configurado:** `8082`
* **Base de datos:** `mongodb://localhost:27017/gamestore_catalog`
