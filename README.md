# Spring Boot Microservices

Repositorio de microservicios independientes con Spring Boot. La arquitectura objetivo contempla un gateway, descubrimiento de servicios, configuración centralizada, autenticación, bases de datos por servicio y comunicación por eventos. El código actual implementa solo una parte de ese esquema; el estado real se detalla más abajo.

Consulta [Comunicaciones.md](./Comunicaciones.md) para ver los flujos previstos entre servicios y el estado actual de RabbitMQ.

## Módulos y puertos

| Módulo | Puerto | Responsabilidad / estado actual |
|---|---:|---|
| `discover` | `8761` | Servicio de descubrimiento Eureka. La dependencia está incluida, pero falta activar el servidor con `@EnableEurekaServer`; los demás servicios todavía no están configurados como clientes Eureka. |
| `product-service` | `8081` | API REST CRUD de productos en `/api/products`, con MongoDB. |
| `order-service` | `8082` | API REST CRUD de pedidos en `/api/orders`, con PostgreSQL y reserva de stock vía WebClient. |
| `inventory-service` | `8083` | API REST de inventario y reserva/liberación de stock en MySQL. |
| `notification-service` | `8084` | Consume eventos `order.created` de RabbitMQ y envía correos mediante SMTP. |
| API Gateway (del esquema) | `9000` | Puerto reservado para el gateway previsto en el diagrama. No existe aún un módulo de gateway que lo utilice. |

Los puertos `8081`–`8084` permiten ejecutar los cuatro servicios en la misma máquina sin competir por el puerto predeterminado `8080`. Eureka usa `8761`. Los puertos se pueden sobrescribir con la variable de entorno `SERVER_PORT`.

### Puertos de infraestructura

Estos son los puertos locales usados o previstos para las dependencias externas:

| Dependencia | Puerto | Estado |
|---|---:|---|
| MongoDB | `27017` | Base `product-service`, inicializada con la colección `product`. |
| MySQL | `3306` | Base `inventory_service`. |
| PostgreSQL | `5432` | Base `order_service`. |
| RabbitMQ (AMQP) | `5672` | Broker local para eventos de pedidos; exchanges y colas se declaran al iniciar los servicios. |
| RabbitMQ Management UI | `15672` | Panel local de administración. |

## Estructura

```text
discover/             Aplicación de descubrimiento Eureka
product-service/      API de productos y persistencia MongoDB
order-service/        API de pedidos y cliente WebClient de Inventory
inventory-service/    API de inventario, reservas y persistencia MySQL
notification-service/ Consumidor de eventos y envío de correo (AMQP/SMTP)
resources/RabbitMQ/   Script para iniciar RabbitMQ localmente
resources/            Script para iniciar todas las dependencias Docker
```

Cada módulo tiene su propio `pom.xml` y Maven Wrapper. No hay un POM agregador en la raíz.

## Requisitos

- JDK 21
- Docker con el daemon en ejecución, para iniciar las dependencias

## Iniciar las dependencias en Docker

Desde la raíz del repositorio:

```bash
./resources/start-dependencies.sh
```

El script inicia MongoDB, MySQL, PostgreSQL y RabbitMQ en puertos locales y crea las bases de datos necesarias. Los contenedores que crea usan volúmenes Docker persistentes, por lo que reiniciarlos no borra los datos. Si ya existe un contenedor con el nombre esperado, lo reutiliza sin cambiar su configuración.

Las credenciales incluidas son solo para desarrollo local; no las uses en un entorno compartido o de producción. Para las aplicaciones ejecutadas directamente con Maven, los servicios se conectan mediante `localhost`.

Para iniciar solo RabbitMQ también está disponible `resources/RabbitMQ/install.sh`.

## Ejecutar localmente

Ejecuta cada comando desde la raíz en una terminal independiente:

```bash
(cd discover && ./mvnw spring-boot:run)
(cd product-service && ./mvnw spring-boot:run)
(cd order-service && ./mvnw spring-boot:run)
(cd inventory-service && ./mvnw spring-boot:run)
(cd notification-service && ./mvnw spring-boot:run)
```

Inicia primero las dependencias Docker. Las propiedades de conexión están en el `application.yaml` de cada módulo.

Para crear pedidos, inicia también Inventory Service antes de Order Service: cada pedido reserva stock mediante HTTP. Order Service usa `INVENTORY_SERVICE_URL` para localizar Inventory (por defecto `http://localhost:8083`).

Order Service expone CRUD en `/api/orders`, reserva stock y publica el evento `order.created` después de guardar el pedido. Notification Service consume ese evento. `NOTIFICATION_EMAIL_ENABLED=false` (valor predeterminado) simula el envío y lo indica en el log; con `true` envía realmente por SMTP. Para el envío real configura `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS` y `NOTIFICATION_EMAIL_FROM`; inyecta las credenciales por variables de entorno, no las guardes en el repositorio. La configuración actual publica después del commit: si RabbitMQ falla, el pedido queda guardado y la API devuelve un error; consulta el pedido antes de reintentar. Para garantizar la publicación ante caídas, implementa Transactional Outbox. Asimismo, Eureka, Gateway, Config Server y Keycloak requieren completar la configuración e implementación indicada en el esquema.

> **Nota sobre Docker:** `product-service/Dockerfile` contiene actualmente un manifiesto de Docker Compose, no instrucciones de construcción Docker. La ejecución en contenedores requiere corregir esa configuración; los comandos Maven anteriores ejecutan los servicios directamente.

## Pruebas de la API de productos

La colección de Postman está en `product-service/Product-Service.postman_collection.json`. Su URL base local es `http://localhost:8081`.

## Configuración y seguridad

Las URLs y credenciales de bases de datos que aparecen en los archivos de configuración son valores locales de desarrollo. Antes de desplegar, deben externalizarse mediante variables de entorno o un gestor de secretos. HTTPS, Keycloak y Config Server están en el esquema, pero todavía no están implementados en este repositorio.
