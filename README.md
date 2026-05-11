# Ecomera Order Service

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.11-brightgreen?logo=springboot&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.1-6DB33F?logo=spring&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![OpenFeign](https://img.shields.io/badge/OpenFeign-Integrated-6DB33F)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-yellow?logo=open-source-initiative&logoColor=white)

Order management microservice for the Ecomera ecosystem. Handles order lifecycle from checkout through delivery with product and cart service integration via OpenFeign.

---

## Overview

Provides a complete order management API. Orders are created either directly or via a **checkout** flow that fetches the user's cart from the **Cart Service**, validates stock against the **Product Service**, creates the order, and clears the cart. Order status tracks the full lifecycle: PENDING → PROCESSING → CONFIRMED → SHIPPED → DELIVERED / CANCELED.

---

## Tech Stack

- **Spring Boot** 3.5.11
- **Spring Data JPA** - Database persistence
- **Spring Cloud OpenFeign** - Inter-service communication
- **PostgreSQL** - Order data storage
- **Redis** - Distributed caching
- **Liquibase** - Database migrations
- **MapStruct** - DTO mapping
- **Spring Cloud Config** - Centralized configuration
- **Eureka Client** - Service registration
- **Springdoc OpenAPI** - API documentation

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.6+
- PostgreSQL 16+ (database: `ecomera_order`)
- Redis 7+
- Config Server running on port 8888
- Eureka Server running on port 8761
- Cart Service running on port 8083
- Product Service running on port 8082

### Start the Service
```bash
mvn spring-boot:run
```

**Service available at:** `http://localhost:8084`

---

## API Endpoints

### User Endpoints (requires JWT — gateway injects `X-User-Id`)

| Method | Endpoint | Description | Body / Params |
|--------|----------|-------------|---------------|
| POST | `/api/v1/orders` | Create order manually | `{ "items": [ { "productId": "uuid", "quantity": 2 } ] }` |
| POST | `/api/v1/orders/checkout` | Checkout — convert cart to order | — |
| GET | `/api/v1/orders/{id}` | Get order by ID (owner or admin/manager) | — |

### Admin/Manager Endpoints (requires `ADMIN` or `MANAGER` role in JWT)

| Method | Endpoint | Description | Params |
|--------|----------|-------------|--------|
| GET | `/api/v1/orders` | Get all orders (paginated) | `?page=0&size=10` |
| GET | `/api/v1/orders/user/{userId}` | Get orders by user ID | `?page=0&size=10` |
| GET | `/api/v1/orders/status` | Get orders by status | `?status=CONFIRMED&page=0&size=10` |
| PATCH | `/api/v1/orders/{id}` | Update order status | `{ "status": "SHIPPED" }` |

### Admin Only Endpoint (requires `ADMIN` role)

| Method | Endpoint | Description |
|--------|----------|-------------|
| DELETE | `/api/v1/orders/{id}` | Delete an order |

### Health & Docs

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/actuator/health` | Health check |
| GET | `/swagger-ui.html` | OpenAPI documentation |

---

## Database Schema

```
orders
├── id (UUID, PK)
├── user_id (UUID, NOT NULL)
├── status (VARCHAR, NOT NULL)
├── total_price (DECIMAL, NOT NULL)
├── created_at
├── created_by
├── updated_at
└── updated_by

order_item
├── id (UUID, PK)
├── order_id (UUID, FK → orders.id)
├── product_id (UUID, NOT NULL)
├── product_title (VARCHAR, NOT NULL)
├── unit_price (DECIMAL, NOT NULL)
├── quantity (INTEGER, NOT NULL)
├── created_at
├── created_by
├── updated_at
└── updated_by
```

---

## Architecture

```
Client → API Gateway (port 8080)
              ↓
     Order Service (port 8084)
       ↓       ↓         ↓
  PostgreSQL  Redis    Cart Service (via Feign)
                         ↓
                    Product Service (via Feign)
                         ↓
                   Config Server (configs)
                         ↓
                   Eureka Server (registration)
```

### Checkout Flow
When a user checks out:
1. **Order Service** fetches the user's cart from **Cart Service** via Feign
2. Validates each item's stock against **Product Service** via Feign
3. Creates the order with a snapshot of product data
4. Saves order and order items to PostgreSQL
5. Clears the user's cart via Cart Service

---

## Features

- **Manual Order Creation** — Create orders directly with product IDs and quantities
- **Cart Checkout** — Convert entire cart into an order in one call
- **Status Lifecycle** — Full order status tracking (PENDING → DELIVERED / CANCELED)
- **Stock Validation** — Validates stock against Product Service during checkout
- **User Order History** — Users can view their own orders
- **Admin Management** — Admins/Managers can view, update status, and manage all orders
- **Admin Deletion** — Only ADMIN role can delete orders
- **Redis Caching** — Order data cached with configurable TTL
- **Liquibase Migrations** — Version-controlled database schema
- **Audit Fields** — Automatic created/updated timestamps
- **OpenFeign Integration** — Communicates with Cart and Product services

---

## Configuration

Configuration fetched from **Config Server** (`order-service.yml`):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/ecomera_order
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  data:
    redis:
      host: localhost
      port: 6379

server:
  port: 8084
```

---

## Docker Support

### Build Image
```bash
docker build -t ecomera-order-service .
```

### Run Container
```bash
docker run -p 8084:8084 \
  -e CONFIG_SERVER_URL=http://config-server:8888 \
  -e EUREKA_SERVER_URL=http://eureka:8761/eureka/ \
  ecomera-order-service
```

---

## Testing

```bash
# Unit tests
mvn test
```

---

## Related Services

**Infrastructure:**
- [Config Server](https://github.com/ecomera-ecosystem/ecomera-config-server) - Centralized configuration
- [Eureka Server](https://github.com/ecomera-ecosystem/ecomera-eureka-service-registry) - Service discovery
- [API Gateway](https://github.com/ecomera-ecosystem/ecomera-api-gateway) - Entry point

**Business Services:**
- [Auth Service](https://github.com/ecomera-ecosystem/ecomera-auth-service) - Authentication & authorization
- [Product Service](https://github.com/ecomera-ecosystem/ecomera-product-service) - Product catalog (order fetches stock via Feign)
- [Cart Service](https://github.com/ecomera-ecosystem/ecomera-cart-service) - Shopping cart (order fetches cart via Feign)

---

## License

MIT License — see [LICENSE](LICENSE) file for details

---
