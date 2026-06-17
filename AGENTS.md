# FastGuy — AGENTS.md

## Project state

Simple food ordering web app using Jakarta Servlet 6 + JPA/Hibernate. Building step by step from auth → dashboard → products → cart → orders.

## Backend package map

```
entity/     — JPA entities (17 tables)
dao/        — Data access using Hibernate EntityManager
service/    — Business logic
servlet/    — REST endpoints (@WebServlet, sends JSON via Jackson)
utils/      — DatabaseUtil, JwtUtil, PasswordUtil, JsonUtil
```

## Technology

| Layer | Stack |
|---|---|
| Runtime | Java JDK 17, Tomcat 11, Jakarta Servlet 6 |
| Build | Maven, war packaging |
| ORM | JPA/Hibernate 6 + SQL Server JDBC |
| Auth | JWT (jjwt 0.12+) + BCrypt |
| JSON | Jackson 2.18+ |
| Database | SQL Server |

## Database tables (17)

Role, Users, Address, DeliveryZone, Category, Product, ProductOption, Cart, CartItem, Orders, OrderItem, Payment, Ingredient, ProductIngredient, WorkShift, Schedule, Review

## Git

- Branch flow: `feature/*` → `develop` → `main`
- Commit only when asked. No force-push, no amend.
