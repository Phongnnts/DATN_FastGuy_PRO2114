# FastGuy — AGENTS.md

## Project state

Early scaffolding. **Full restructuring required.** Backend has ~70 stub Java classes (all empty) in the wrong package layout. Frontend has `dist/` (pre-built from deleted source) but no `package.json` or source files. Code is written bottom-up per feature: **Entity → Repository → Service → Controller** (never skip layers).

## Key constraints

- **Not Spring Boot.** Jakarta Servlet 6 + Tomcat 11. No DI container. Controllers instantiate services manually.
- **No interfaces for single implementations.** Only create one when a second impl exists.
- **No Clean Architecture / Hexagonal / CQRS / Event Driven.**
- **Flat packages** — `controller`, `service`, `repository`, `entity`, `dto`, `filter`, `config`, `util`, `exception`. No `com.example` prefix. No `impl` sub-packages.
- **SQL Server** — schema in `Document/DBFastGuy.sql` (must run before backend starts).

## Backend package map

```
controller/     — REST endpoints (@WebServlet, sends JSON via Jackson)
service/        — Business logic (plain classes, no interfaces)
repository/     — Data access (JPA/Hibernate EntityManager)
entity/         — JPA entities matching SQL tables
dto/            — Request/Response objects
filter/         — JwtAuthenticationFilter, CorsFilter
config/         — HibernateConfig, JwtConfig
util/           — JwtUtil, PasswordUtil (BCrypt), JsonUtil, ValidationUtil
exception/      — AppException, GlobalExceptionHandler (returns JSON error)
```

## Technology

| Layer | Stack |
|---|---|
| Runtime | Java JDK 24, Tomcat 11, Jakarta Servlet 6 |
| Build | Maven, war packaging (`mvn clean package`) |
| ORM | JPA/Hibernate 6 + SQL Server JDBC |
| Auth | JWT (jjwt 0.12+) + BCrypt |
| JSON | Jackson 2.18+ |
| Frontend | Vue 3 + Pinia + Vue Router + Axios + Bootstrap 5 |
| API test | Bruno (not Postman) |
| Database | SQL Server |

## Role & Permission flow

```
GUEST → USER → STAFF / SHIPPER / ADMIN
```
JWT carries `role`; `filter/JwtAuthenticationFilter` validates. Permission check via `Role` ↔ `Permission` mapping.

## Database tables

`Role`, `Permission`, `RolePermission`, `Users`, `Address`, `DeliveryZone`, `Category`, `Product`, `ProductOption`, `Cart`, `CartItem`, `Orders`, `OrderItem`, `Payment`, `Ingredient`, `ProductIngredient`, `InventoryTransaction`, `WorkShift`, `Schedule`, `Review`

## Guest cart mechanism

Browser `localStorage` → `X-Session-Id` header on every API call. Guest → User migration merges cart on login.

## Git

- 3 commits exist: `init` → `init` → `docx` (bulk stubs).
- Commit only when asked. No force-push, no amend.
