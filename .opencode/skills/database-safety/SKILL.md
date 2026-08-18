---
name: database-safety
description: Use when inspecting SQL Server schema/data or changing SQL, migrations, JPA entities, DAOs, or queries.
---

# Database Safety

- Confirm `@@SERVERNAME`, `DB_NAME()`, state, compatibility level, and relevant catalog objects first.
- SQL Server MCP is read-only. Use an account limited to `SELECT` and `VIEW DEFINITION`.
- Never execute DML, DDL, or stored procedures through MCP.
- Compare runtime schema with canonical SQL, migrations, constraints, indexes, and JPA mappings.
- Never run `database/init.sql` against retained data.
- All writes follow `sqlserver-migrations`; require a disposable target or explicit retained-data approval and recovery plan.
- Stop on target mismatch, missing recovery evidence, destructive SQL, or validator failure.
