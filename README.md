# ðŸ›’ Catalog Service (`catalog-service`)

> **Microservicio de GestiÃ³n de Ventas, Ã“rdenes y GeneraciÃ³n de Claves Digitales de Videojuegos.**  
> Desarrollado con **Java 17**, **Spring Boot 4**, **Spring Data JPA**, **H2 Database** y **Spring Cloud Netflix Eureka**.

---

## ðŸ›ï¸ 1. Â¿Por quÃ© es un Microservicio Independiente? (AutonomÃ­a y Desacoplamiento)

Una de las preguntas mÃ¡s importantes para un programador que da el salto a la arquitectura empresarial es:  
**Â¿Por quÃ© no metimos todo esto en un solo proyecto gigante (monolito)?**

### ðŸŽ¯ Principio de Base de Datos Propia (Database-per-Service Pattern)
En un sistema monolÃ­tico tradicional, si la base de datos se satura o se cae, **toda la tienda muere**.  
En nuestra arquitectura de microservicios:
* `catalog-service` **posee su propia base de datos relacional (H2 / SQL)**. NingÃºn otro microservicio tiene permitido conectarse directamente por JDBC a las tablas de ventas.
* Â¿Por quÃ© SQL para Ventas? Porque las ventas involucran dinero y requieren garantÃ­as **ACID** (transacciones bancarias estrictas donde nada se puede perder ni duplicar).
* Si maÃ±ana `catalog-service` se apaga o sufre una caÃ­da por mantenimiento, `catalog-service` **sigue vivo**; puede consultar cachÃ©s locales o devolver un mensaje de reintento controlado sin tumbar el sistema completo.

### ðŸš€ Despliegue y Escalabilidad Independiente
Si se acerca el *Black Friday*, la cantidad de personas pagando Ã³rdenes se dispara por 100x, pero la cantidad de administradores subiendo nuevos juegos al catÃ¡logo sigue siendo baja.  
Al ser independiente, podemos levantar **5 instancias de `catalog-service` en servidores distintos** sin tener que gastar memoria RAM levantando copias innecesarias del catÃ¡logo.

---

## ðŸ¤ 2. Mapa de RelaciÃ³n: Â¿CÃ³mo se comunica con los demÃ¡s Microservicios?

Los microservicios **NUNCA** deben compartir bases de datos. Se comunican exclusivamente a travÃ©s de la red usando protocolos ligeros:

```
                                  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                                  â”‚        EUREKA SERVER        â”‚
                                  â”‚   (Directorio TelefÃ³nico)   â”‚
                                  â”‚         Puerto 8761         â”‚
                                  â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                                                 â”‚
                          â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
                          â”‚ 1. Heartbeat ("Estoy vivo en 8082")         â”‚ 1. Heartbeat ("Estoy vivo en 8082")
                          â–¼                                             â–¼
            â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”                 â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
            â”‚       SALES-SERVICE       â”‚                 â”‚      CATALOG-SERVICE      â”‚
            â”‚        Puerto 8082        â”‚                 â”‚        Puerto 8082        â”‚
            â”‚     Base de Datos H2      â”‚                 â”‚    Base de Datos Mongo    â”‚
            â”‚   (Ventas y CD-Keys)      â”‚                 â”‚    (Fichas de Juegos)     â”‚
            â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜                 â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                          â”‚                                             â–²
                          â”‚ 2. Pregunta por HTTP vÃ­a OpenFeign          â”‚
                          â”‚    "Â¿El juego 1 existe y cuÃ¡nto cuesta?"    â”‚
                          â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

### 1. RelaciÃ³n con `eureka-server` (Puerto 8761 - Discovery Server)
* **Â¿QuÃ© problema resuelve?**: En la nube o en contenedores Docker, las IPs y puertos cambian todo el tiempo. Si ponemos URLs fijas (*hardcoded*) como `http://192.168.1.50:8082`, el dÃ­a que esa mÃ¡quina cambie de IP, todo se rompe.
* **Â¿CÃ³mo interactÃºan?**:
  1. Al arrancar `catalog-service`, lee su archivo `application.properties` y busca a `eureka-server`.
  2. Le dice: *"Hola Eureka, me llamo `SALES-SERVICE` y estoy escuchando en el puerto 8082"*.
  3. Cada 30 segundos le manda un latido (*heartbeat*). Si `catalog-service` se apaga, Eureka lo tacha de la lista para que nadie le envÃ­e trÃ¡fico.

### 2. RelaciÃ³n con `catalog-service` (Puerto 8082 - Inventario NoSQL)
* **Â¿QuÃ© problema resuelve?**: **PrevenciÃ³n de Fraude y ValidaciÃ³n de Inventario**.  
  Si permitiÃ©ramos que el usuario o el frontend nos diga: *"Cobrame el Elden Ring a $0.01 centavos"*, cualquiera podrÃ­a hackear la tienda modificando el JSON en el navegador.
* **Â¿CÃ³mo interactÃºan (Paso a Paso con OpenFeign)?**:
  1. El cliente web manda a `catalog-service`: *Quiero comprar el juego ID: 1*.
  2. `catalog-service` no confÃ­a en nadie: acude a Eureka y le pide la direcciÃ³n de `CATALOG-SERVICE`.
  3. Hace una llamada HTTP interna `GET /api/games/1` a `catalog-service`.
  4. `catalog-service` responde con el precio oficial de la base de datos de MongoDB ($59.99) y confirma que hay stock disponible.
  5. `catalog-service` calcula el total real, cobra, descuenta stock y genera la clave digital (CD-Key).

---

## ðŸ§­ 3. GuÃ­a PedagÃ³gica: Â¿CÃ³mo viaja una peticiÃ³n dentro de `catalog-service`?

Imagina que este microservicio funciona exactamente igual a un **restaurante de alta cocina**:

```
[Cliente HTTP / Postman / Frontend]
                 â”‚ 1. EnvÃ­a JSON de compra al puerto 8082
                 â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 1. CAPA WEB: SaleOrderController                            â”‚
â”‚    El "Mesero": Recibe la comanda del cliente.              â”‚
â”‚    Verifica con @Valid que no venga vacÃ­a o con letras rarasâ”‚
â”‚    y de inmediato se la entrega a la cocina (Service).      â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                               â”‚ 2. Llama al mÃ©todo createOrder()
                               â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 2. CAPA NEGOCIO: SaleOrderService                           â”‚
â”‚    El "Chef": Tiene las recetas y las reglas del negocio.   â”‚
â”‚    Multiplica precios x cantidades con BigDecimal, genera   â”‚
â”‚    claves digitales Ãºnicas estilo STEAM-XXXX y calcula el   â”‚
â”‚    total blindado.                                          â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                               â”‚ 3. Llama a orderRepository.save()
                               â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 3. CAPA DATOS: SaleOrderRepository                          â”‚
â”‚    La "Despensa": Es la Ãºnica autorizada para tocar la base â”‚
â”‚    de datos. Genera las instrucciones SQL automÃ¡ticamente.  â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                               â”‚ 4. Sentencias SQL Hibernate
                               â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ 4. BASE DE DATOS: H2 SQL (Tablas sale_orders y order_items) â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

---

## ðŸ“‚ 4. AnatomÃ­a Detallada de Clases (Clase por Clase)

### Paquete: `com.jonathan.gamestore.sales`

#### ðŸŒŸ `SalesServiceApplication.java`
* **Â¿QuÃ© es?**: La puerta principal de entrada y punto de arranque del microservicio.
* **Anotaciones clave**:
  * `@SpringBootApplication`: Enciende todo el ecosistema de Spring (inyecciÃ³n de dependencias, configuraciÃ³n automÃ¡tica, servidor web interno Tomcat).
  * `@EnableDiscoveryClient`: Le da la orden al microservicio de buscar a `eureka-server` en el puerto 8761 y registrarse con su tarjeta de presentaciÃ³n.
* **Â¿QuiÃ©n la manda a llamar?**: El comando de ejecuciÃ³n de consola (`java -jar`) o el botÃ³n Play de IntelliJ.

---

### Paquete: `com.jonathan.gamestore.sales.config`

#### âš™ï¸ `H2Config.java`
* **Â¿QuÃ© es?**: ConfiguraciÃ³n del panel visual de la base de datos H2 en memoria.
* **Â¿Por quÃ© existe?**: En versiones modernas de Spring Boot (Jakarta EE), la consola `/h2-console` requiere registrar manualmente el servlet `JakartaWebServlet` para poder ver las tablas desde el navegador en `http://localhost:8082/h2-console`.

---

### Paquete: `com.jonathan.gamestore.sales.model` (Entidades de Base de Datos)

#### ðŸ—ƒï¸ `OrderStatus.java` (Enum)
* **Â¿QuÃ© es?**: Un catÃ¡logo fijo de opciones vÃ¡lidas para el estado de una compra.
* **Valores**:
  * `PENDING`: La orden fue registrada pero estÃ¡ en espera de pago.
  * `COMPLETED`: Pago confirmado exitosamente y claves digitales liberadas.
  * `CANCELLED`: Pago rechazado o compra anulada.

#### ðŸ—ƒï¸ `SaleOrder.java` (Entidad Padre)
* **Â¿QuÃ© es?**: La tabla principal en SQL (`sale_orders`). Representa la factura o ticket de compra general.
* **Campos clave**:
  * `@Id @GeneratedValue`: Clave primaria autoincremental (1, 2, 3...).
  * `userId`: Identificador del usuario que comprÃ³.
  * `totalAmount`: Importe total a pagar calculado en servidor con `BigDecimal`.
  * `status`: Estado actual (`PENDING`, etc.).
  * `createdAt`: Fecha y hora de creaciÃ³n automÃ¡tica (`@PrePersist`).
  * `items`: Lista de productos contenidos en esta orden (`List<OrderItem>`).
* **RelaciÃ³n `@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)`**:
  * Significa: *"Una orden tiene muchos Ã­tems"*.
  * `CascadeType.ALL`: Al guardar la orden en Java, Hibernate automÃ¡ticamente guarda todos sus Ã­tems en la tabla hija sin tener que hacer dos llamadas separadas. Si borras la orden, se borran sus Ã­tems en cascada.
* **MÃ©todo `addItem(OrderItem item)`**: MÃ©todo ayudante que asegura la relaciÃ³n bidireccional asignando este objeto orden como padre del Ã­tem.

#### ðŸ—ƒï¸ `OrderItem.java` (Entidad Hija)
* **Â¿QuÃ© es?**: La tabla detalle en SQL (`order_items`). Representa cada juego individual dentro del carrito de compra.
* **Campos clave**:
  * `gameId`: Identificador del juego en el catÃ¡logo.
  * `gameTitle`: Nombre del juego para conservar el historial histÃ³rico (ej: *"Elden Ring"*).
  * `unitPrice`: Precio al que se vendiÃ³ en ese momento exacto.
  * `quantity`: NÃºmero de copias compradas.
  * `digitalKey`: La clave secreta de activaciÃ³n generada.
  * `order`: Referencia a la orden padre (`@ManyToOne` con clave forÃ¡nea `order_id`).
  * `@JsonIgnore`: AnotaciÃ³n crucial para evitar que el conversor a JSON entre en un ciclo infinito de recursiÃ³n (Orden -> Ãtem -> Orden -> Ãtem...).

---

### Paquete: `com.jonathan.gamestore.sales.dto` (Data Transfer Objects)
*Los DTOs son cajas de encomienda: solo transportan datos limpios entre el cliente y el backend. Usamos `record` de Java 17 por ser inmutables y compactos.*

#### ðŸ“¦ `OrderItemRequest.java`
* **Â¿QuÃ© es?**: Molde de datos que define quÃ© informaciÃ³n debe enviar el cliente por cada juego que quiere comprar.
* **Validaciones**:
  * `@NotNull gameId`: Es obligatorio especificar quÃ© juego se quiere.
  * `@NotBlank gameTitle`: El tÃ­tulo no puede venir vacÃ­o o con puros espacios.
  * `@NotNull @DecimalMin("0.01") unitPrice`: El precio debe ser un nÃºmero positivo mayor a cero.
  * `@NotNull @Positive quantity`: La cantidad debe ser al menos 1 copia.

#### ðŸ“¦ `SaleOrderRequest.java`
* **Â¿QuÃ© es?**: Molde de datos del cuerpo de la peticiÃ³n de compra.
* **Campos**:
  * `@NotNull @Positive Long userId`: ID del usuario que realiza la compra.
  * `@NotEmpty @Valid List<OrderItemRequest> items`: La lista no puede venir vacÃ­a, y `@Valid` fuerza a Spring a revisar tambiÃ©n los campos de cada Ã­tem de la lista.
* **Regla de oro de seguridad**: Â¡Este DTO **NO** contiene el campo `totalAmount`! El cliente jamÃ¡s puede decidir cuÃ¡nto va a pagar.

---

### Paquete: `com.jonathan.gamestore.sales.repository` (Persistencia)

#### ðŸ—„ï¸ `SaleOrderRepository.java`
* **Â¿QuÃ© es?**: Interfaz que extiende de `JpaRepository<SaleOrder, Long>`.
* **Â¿CÃ³mo funciona?**: No requiere escribir sentencias SQL a mano. Spring Data JPA las genera automÃ¡ticamente:
  * `save(orden)`: Inserta o actualiza en la tabla `sale_orders`.
  * `findById(id)`: Busca una orden por su clave primaria.
  * `findAll()`: Trae todas las Ã³rdenes existentes.
  * `findByUserId(userId)`: Spring lee el nombre del mÃ©todo y genera: `SELECT * FROM sale_orders WHERE user_id = ?`.

---

### Paquete: `com.jonathan.gamestore.sales.service` (LÃ³gica de Negocio)

#### ðŸ§  `SaleOrderService.java`
* **Â¿QuÃ© es?**: El cerebro de la aplicaciÃ³n donde residen las reglas comerciales.
* **AnotaciÃ³n `@Transactional`**: Garantiza integridad financiera. Si se produce un error a mitad de camino, hace **Rollback** automÃ¡tico y la base de datos queda limpia, sin compras a medias.
* **MÃ©todos detallados**:
  1. `createOrder(SaleOrderRequest request)`:
     * Inicializa una nueva `SaleOrder` con estado `PENDING`.
     * Recorre cada Ã­tem del pedido.
     * **Genera una clave digital Ãºnica** para cada producto:
       `UUID.randomUUID().toString().toUpperCase()` -> `F5DBA670-B98E-406E-...`
     * **Calcula el total real**: Multiplica `unitPrice` por `quantity` usando `BigDecimal` y acumula el total.
     * Guarda la orden completa y sus Ã­tems llamando a `orderRepository.save(order)`.
  2. `getAllOrders()`: Retorna todas las ventas registradas.
  3. `getOrderById(Long id)`: Busca una orden por ID.
  4. `getOrdersByUserId(Long userId)`: Lista las compras de un cliente en particular.
  5. `updateOrder(Long id, SaleOrderRequest request)`: Permite actualizar datos de una orden existente.
  6. `deleteOrder(Long id)`: Elimina la orden y sus Ã­tems de la base de datos.

---

### Paquete: `com.jonathan.gamestore.sales.controller` (Capa Web REST)

#### ðŸŒ `SaleOrderController.java`
* **Â¿QuÃ© es?**: La cara visible del microservicio al mundo exterior (HTTP).
* **Anotaciones clave**:
  * `@RestController`: Configura la clase para responder siempre en formato JSON.
  * `@RequestMapping("/api/orders")`: Prefijo de todas las rutas de este recurso.
  * `@RequiredArgsConstructor`: Inyecta automÃ¡ticamente el servicio mediante constructor.
* **Endpoints y cÃ³mo llaman al servicio**:
  * `POST /api/orders` -> `orderService.createOrder(request)`: Devuelve `HTTP 201 Created`.
  * `GET /api/orders` -> `orderService.getAllOrders()`: Devuelve `HTTP 200 OK`.
  * `GET /api/orders/{id}` -> `orderService.getOrderById(id)`: Devuelve `HTTP 200 OK` o `HTTP 404 Not Found`.
  * `GET /api/orders/user/{userId}` -> `orderService.getOrdersByUserId(userId)`: Devuelve `HTTP 200 OK`.
  * `PUT /api/orders/{id}` -> `orderService.updateOrder(id, request)`: Devuelve `HTTP 200 OK` o `HTTP 404 Not Found`.
  * `DELETE /api/orders/{id}` -> `orderService.deleteOrder(id)`: Devuelve `HTTP 204 No Content` o `HTTP 404 Not Found`.

---

### Paquete: `com.jonathan.gamestore.sales.exception` (Manejo de Errores)

#### ðŸš¨ `ErrorResponse.java` (Record)
* **Â¿QuÃ© es?**: Estructura estÃ¡ndar y profesional para reportar errores en formato JSON.
* **Campos**:
  * `status`: CÃ³digo numÃ©rico HTTP (ej: 400).
  * `error`: Nombre descriptivo (ej: `"Validation Error"`).
  * `message`: ExplicaciÃ³n en espaÃ±ol amigable.
  * `validationErrors`: Mapa clave-valor con los campos que fallaron y la razÃ³n exacta (ej: `{"items[0].quantity": "La cantidad debe ser al menos 1"}`).
  * `timestamp`: Fecha y hora exacta del error.

#### ðŸ›¡ï¸ `GlobalExceptionHandler.java`
* **Â¿QuÃ© es?**: Interceptor global de fallos anotado con `@RestControllerAdvice`.
* **Â¿CÃ³mo funciona?**: Atrapa excepciones como `MethodArgumentNotValidException` antes de que provoquen una pantalla de error fea de Spring Boot, traduciÃ©ndolas a un JSON limpio con cÃ³digo `HTTP 400 Bad Request`.

---

## ðŸ“¡ 5. CatÃ¡logo de Endpoints RESTful (`/api/orders`)

| MÃ©todo | Endpoint | DescripciÃ³n | CÃ³digo Ã‰xito | CÃ³digos Error |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/orders` | Crea una nueva orden de venta con mÃºltiples Ã­tems y genera sus claves digitales | `201 Created` | `400 Bad Request` |
| `GET` | `/api/orders` | Lista todas las Ã³rdenes registradas en el sistema | `200 OK` | `500 Internal Error` |
| `GET` | `/api/orders/{id}` | Obtiene el detalle completo de una orden por su identificador | `200 OK` | `404 Not Found` |
| `GET` | `/api/orders/user/{userId}` | Filtra todas las Ã³rdenes de compra realizadas por un usuario | `200 OK` | - |
| `PUT` | `/api/orders/{id}` | Actualiza el estado o datos de una orden existente | `200 OK` | `404 Not Found`, `400 Bad Request` |
| `DELETE` | `/api/orders/{id}` | Elimina una orden y sus Ã­tems en cascada | `204 No Content` | `404 Not Found` |

---

## âš¡ 6. EjecuciÃ³n Optimizada en Memoria (Bajo Consumo de RAM)

```powershell
cd C:\Users\ErickJimz\IdeaProjects\catalog-service
.\mvnw.cmd clean package -DskipTests
java -Xmx300m -jar .\target\catalog-service-0.0.1-SNAPSHOT.jar
```
* **Puerto**: `8082`
* **Consola H2**: `http://localhost:8082/h2-console` (JDBC URL: `jdbc:h2:mem:salesdb`)

