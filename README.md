# Spring Boot Microservices

Repositorio de microservicios independientes con Spring Boot. La arquitectura objetivo contempla un gateway, descubrimiento de servicios, configuración centralizada, autenticación, bases de datos por servicio y comunicación por eventos. El código actual implementa solo una parte de ese esquema; el estado real se detalla más abajo.

Consulta [Comunicaciones.md](./Comunicaciones.md) para ver los flujos previstos entre servicios y el estado actual de RabbitMQ.

## Módulos y puertos

| Módulo | Puerto | Responsabilidad / estado actual |
|---|---:|---|
| `discover` | `8761` | Servicio de descubrimiento Eureka. La dependencia está incluida, pero falta activar el servidor con `@EnableEurekaServer`; los demás servicios todavía no están configurados como clientes Eureka. |
| `product-service` | `8081` | API REST CRUD de productos en `/api/products`, con MongoDB. |
| `order-service` | `8082` | Esqueleto del servicio de pedidos; tiene configurada la conexión a PostgreSQL, pero aún no tiene endpoints ni entidades de pedidos. |
| `inventory-service` | `8083` | Base de persistencia con JPA y MySQL; faltan los endpoints y la lógica del servicio. |
| `notification-service` | `8084` | Esqueleto con dependencias de RabbitMQ y correo; todavía no hay consumidor de eventos ni envío de emails implementados. |
| API Gateway (del esquema) | `9000` | Puerto reservado para el gateway previsto en el diagrama. No existe aún un módulo de gateway que lo utilice. |

Los puertos `8081`–`8084` permiten ejecutar los cuatro servicios en la misma máquina sin competir por el puerto predeterminado `8080`. Eureka usa `8761`. Los puertos se pueden sobrescribir con la variable de entorno `SERVER_PORT`.

### Puertos de infraestructura

Estos son los puertos locales usados o previstos para las dependencias externas:

| Dependencia | Puerto | Estado |
|---|---:|---|
| MongoDB | `27017` | Base `product-service`, inicializada con la colección `product`. |
| MySQL | `3306` | Base `inventory_service`. |
| PostgreSQL | `5432` | Base `order_service`. |
| RabbitMQ (AMQP) | `5672` | Broker local; Notification Service tiene configurada la conexión, pero aún no consume eventos. |
| RabbitMQ Management UI | `15672` | Panel local de administración. |

## Estructura

```text
discover/             Aplicación de descubrimiento Eureka
product-service/      API de productos y persistencia MongoDB
order-service/        Base del servicio de pedidos
inventory-service/    Base del servicio de inventario y persistencia MySQL
notification-service/ Base del servicio de notificaciones (AMQP y correo)
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

Order Service tiene configurada la conexión a PostgreSQL, aunque todavía no implementa endpoints ni entidades de pedidos. Notification Service apunta a RabbitMQ, pero todavía no consume eventos ni envía correo. Asimismo, Eureka, Gateway, Config Server y Keycloak requieren completar la configuración e implementación indicada en el esquema.

> **Nota sobre Docker:** `product-service/Dockerfile` contiene actualmente un manifiesto de Docker Compose, no instrucciones de construcción Docker. La ejecución en contenedores requiere corregir esa configuración; los comandos Maven anteriores ejecutan los servicios directamente.

## Pruebas de la API de productos

La colección de Postman está en `product-service/Product-Service.postman_collection.json`. Su URL base local es `http://localhost:8081`.

## Configuración y seguridad

Las URLs y credenciales de bases de datos que aparecen en los archivos de configuración son valores locales de desarrollo. Antes de desplegar, deben externalizarse mediante variables de entorno o un gestor de secretos. HTTPS, Keycloak y Config Server están en el esquema, pero todavía no están implementados en este repositorio.
