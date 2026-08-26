---
name: database-safety
description: Use when inspecting SQL Server schema/data or changing SQL, migrations, JPA entities, DAOs, or queries.
---

# Database Safety

- Confirm `@@SERVERNAME`, `DB_NAME()`, state, compatibility level, and relevant catalog objects first.
- Confirm the exact database identity before every MCP write.
- MCP may execute DML and DDL only when `DB_NAME() = 'DemoDatabase'` and the user has approved the requested operation.
- `FastGuyDB` and every database not explicitly allowlisted remain read-only; use an account limited to `SELECT` and `VIEW DEFINITION` there.
- Stored procedures remain disabled through MCP.
- Compare runtime schema with canonical SQL, migrations, constraints, indexes, and JPA mappings.
- Never run `database/init.sql` against retained data.
- Writes outside `DemoDatabase` follow `sqlserver-migrations`; require a disposable target or explicit retained-data approval and recovery plan.
- Stop on target mismatch, missing approval, missing recovery evidence, destructive SQL outside `DemoDatabase`, or validator failure.
