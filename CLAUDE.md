# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Kanban task management app: Spring Boot 4 backend (Java 21) + React 19 / TypeScript frontend. Real-time updates via WebSocket (STOMP), Redis caching, RabbitMQ event bus, PostgreSQL database.

---

## Commands

### Backend (`/backend`)

```bash
# Start infrastructure (PostgreSQL, Redis, RabbitMQ)
docker-compose up -d

# Run application
./mvnw spring-boot:run          # Linux/Mac
mvnw.cmd spring-boot:run        # Windows

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=BoardServiceTest

# Build
./mvnw clean package
```

Tests use H2 in-memory DB with the `test` Spring profile (`application-test.yml`). Redis and RabbitMQ are disabled in tests; Spring caching is set to `none`.

### Frontend (`/frontend`)

```bash
npm install
npm run dev         # Dev server on http://localhost:5173
npm run build       # TypeScript compile + Vite build
npm run lint        # ESLint (max-warnings 0)
npm run preview     # Preview production build
```

---

## Architecture

### Backend layers

```
controller/     REST endpoints — thin, delegates to service
service/        Business logic; owns caching (@Cacheable / @CacheEvict)
repository/     Spring Data JPA interfaces
model/
  entity/       JPA entities (Serializable for Redis)
  dto/          Request/response objects
  event/        RabbitMQ event payloads
security/       JWT filter, UserDetailsService, AuthorizationService
config/         SecurityConfig, RedisConfig, RabbitMQConfig, WebSocketConfig
messaging/
  producer/     EventPublisher — sends card/board events to RabbitMQ
  consumer/     AnalyticsConsumer, NotificationConsumer
exception/      Global exception handling
```

**Cache TTLs** (Spring Cache / Redis): boards 30 min, cards 15 min, lists 20 min.

**RabbitMQ topology**: two `DirectExchange`s (`taskboard.card.events`, `taskboard.board.events`) fan out to `taskboard.notifications` and `taskboard.analytics` queues, with a dead-letter exchange (`taskboard.dlx` → `taskboard.dlq`).

**WebSocket**: STOMP over SockJS at `/ws`. Clients subscribe per-board; the backend broadcasts via `WebSocketController` after mutations.

**Security**: stateless JWT. Public routes: `/api/v1/auth/**`, `/ws/**`, `/actuator/health`. Admin-only: `/api/v1/admin/**`, `/actuator/**`. Method-level security is enabled (`@EnableMethodSecurity`). `AuthorizationService` is the central helper for ownership/permission checks.

**Database migrations**: Flyway, `classpath:db/migration`, `V1__` through `V8__`. Add new migrations as `V9__...sql`, etc.

### Frontend layers

```
src/
  api/          Axios instance + interceptors (JWT attach, token refresh, logout on 401)
  services/     websocket.ts — STOMP client wrapper
  store/        authStore.ts, boardStore.ts (Zustand)
  components/   Feature folders: auth, boards, cards, lists, labels, analytics, admin, layout, common
  types/        Centralized TypeScript type definitions
```

All API calls go through the single Axios instance in `api/axios.ts`. Base URL is `/api/v1` (proxied by Vite in dev). JWTs are stored in `localStorage` (`accessToken` / `refreshToken`); the interceptor handles silent refresh and redirects to `/login` on failure.

WebSocket connects to `http://localhost:8080/ws` (hardcoded in `services/websocket.ts`). Subscriptions are keyed by `boardId`; the `boardStore` wires up callbacks.

---

## Key conventions

- **MapStruct** for entity ↔ DTO mapping; generated code lives in `target/`.
- **Lombok** is used heavily (`@Builder`, `@Getter/@Setter`, `@RequiredArgsConstructor`, `@Slf4j`).
- All JPA entities implement `Serializable` (required for Redis serialization).
- Spring profile `test` activates `application-test.yml` which swaps to H2 and disables external dependencies.
- Frontend Vite proxy is configured in `vite.config.ts` to forward `/api` and `/ws` to `localhost:8080`.