---
name: fastguy-backend
description: Use when editing FastGuy Java Servlets, services, DAOs, JPA entities, API mappings, or transactions.
---

# FastGuy Backend

- Stack: Java 17 WAR, Jakarta Servlet 6.1, JPA 3.1, Hibernate 6.6, Tomcat 11, SQL Server.
- Plan first. Use CodeGraph to trace `Servlet → Service → DAO → Entity/DTO` plus frontend consumers.
- Never introduce Spring MVC, Spring Data, or Spring `@Transactional` unless migration is explicitly requested.
- Verify runtime SQL Server schema before DB-dependent edits. Stop rather than guess when the target is unavailable.
- Keep transactions short. Roll back active transactions on failure. Always close owned `EntityManager` instances.
- For API changes, update OpenAPI first, then test serialized output and frontend consumers.
- Run focused tests, `mvn test`, and integration tests for DB/API behavior.
