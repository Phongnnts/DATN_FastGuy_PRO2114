# FastGuy — AGENTS.md

## Project state

Functional codebase with core flows working. **Current focus**: complete ordering flow (Guest → Staff → Ready handoff), Cloudinary upload, product options, address CRUD.

## Technology

| Layer | Stack |
|---|---|
| Runtime | Java 17, Tomcat 11, Jakarta Servlet 6 |
| Build | Maven `war` packaging (`mvn clean compile`) |
| ORM | JPA 3.1 / Hibernate 6.6 + SQL Server JDBC |
| Auth | JWT (jjwt 0.12, HMAC-SHA, 24h expiry) |
| JSON | Jackson 2.18 |
| Frontend | Vue 3.5 + Pinia 3 + Vue Router 4.6 + Axios 1.18 + Chart.js 4.5 |
| Build | Vite 8 |
| Database | SQL Server (`FastGuyDB`) |
| Image upload | Cloudinary (unsigned, from frontend) |

## Backend packages

```
dao/     Data Access Objects (10 files)
entity/  JPA entities (17 files)
service/ Business logic (7 files)
servlet/ HTTP controllers (15 files)
utils/   Utilities (5 files)
```

## Frontend packages

```
api/       Axios API clients
assets/    CSS styles
components/ Shared components
layouts/   4 layouts
router/    Vue Router + guards
stores/    Pinia stores
views/     guest/, user/, staff/, admin/
utils/     Constants, format, helpers
```

## Roles

```
GUEST ──login──→ USER ──(admin set)──→ STAFF / ADMIN
```

## Order status

```
PENDING ──→ CONFIRMED ──→ PREPARING ──→ READY
PENDING ──→ CANCELLED
```

## Constraints

- Jakarta Servlet 6 + Tomcat 11, NOT Spring Boot
- Flat packages (no com.example prefix)
- Code order per feature: Entity → DAO → Service → Servlet
- No interfaces for single implementations

## Module assignment

| Person | Module | Scope |
|--------|--------|-------|
| 1 | Auth | Login, Register, Profile API |
| 2 | Guest | Menu, ProductDetail, Cart, Checkout |
| 3 | User | Address CRUD, order history |
| 4 | Staff | Order processing PENDING→READY |
| 5 | Admin | CRUD + Cloudinary upload + chart |
| 6 | Common | Layouts, config, Chart.js, support |
