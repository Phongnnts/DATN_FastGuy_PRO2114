---
name: sqlserver-migrations
description: Use when reviewing, validating, or executing Microsoft SQL Server migrations, T-SQL schema changes, sqlcmd workflows, migration validators, schema parity, or disposable database checks.
---

# SQL Server Migrations

## Core Rule

Never apply an unverified migration to a retained database. Review source first, validate on a disposable database, then require explicit user approval before any retained-database write.

## Required Inputs

- Exact server and database names
- Authentication mode supplied through environment or interactive credential handling
- Migration and validator paths
- Confirmation database is disposable, or explicit retained-database approval
- Recovery method for retained data

Never print credentials or place passwords in command history.

## Workflow

1. Read `database/migrations/RUNBOOK.md`, migration, validator, and canonical schema files.
2. Confirm target identity with read-only queries: `@@SERVERNAME`, `DB_NAME()`, database state, compatibility level.
3. Inspect migration for destructive DDL, unbounded DML, lock duration, transaction behavior, `GO`, idempotency, and deployed-file mutation.
4. Compare migration result expectations against `database/init.sql`, `database/DB_FastGuy.sql`, JPA mappings, constraints, defaults, indexes, and seed behavior.
5. Use `scripts/Invoke-SqlServerMigrationCheck.ps1 -Mode Preflight` before any execution.
6. Apply migration only to a disposable database using direct `sqlcmd -b -V 16 -i ...` after explicit confirmation.
7. Use wrapper `-Mode Validate` for validator scripts after migration.
8. Report command, target identity, exit code, validator result, and remaining runtime gaps.

## Safety Gates

| Target | Allowed action |
|---|---|
| Unknown target | Read source only |
| Retained/local development DB | Read-only inspection until explicit approval and recovery plan |
| Disposable DB | Preflight and validator allowed; migration execution still requires explicit confirmation |
| Production | Plan/review only unless user explicitly authorizes change window and recovery procedure |

Stop on `DROP DATABASE`, `TRUNCATE TABLE`, database-wide destructive initialization, credential literals, target mismatch, missing recovery plan, or validator failure. Never run `database/init.sql` against retained data.

## SQL Server Patterns

- Use `SET XACT_ABORT ON` for transactional failure semantics.
- Use `TRY...CATCH` with `THROW`; never swallow errors.
- Use `IF OBJECT_ID(...) IS NULL`, catalog checks, or equivalent guards for idempotent DDL.
- Keep transactions short and lock order deterministic.
- Use `sp_getapplock` when only one migration runner may execute.
- Use `decimal(18,2)` plus explicit scale/range validation for money.
- Validate constraint, default, index, FK, nullability, and data parity through catalog queries.
- Treat `GO` as a `sqlcmd` batch separator, not T-SQL transaction syntax.

## Wrapper

```powershell
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 `
  -Mode Preflight `
  -Server $env:FASTGUY_DB_SERVER `
  -Database FastGuyDB
```

Validator execution:

```powershell
& .\.opencode\skills\sqlserver-migrations\scripts\Invoke-SqlServerMigrationCheck.ps1 `
  -Mode Validate `
  -Server $env:FASTGUY_DB_SERVER `
  -Database FastGuyDB_Disposable `
  -ScriptPath .\database\migrations\047_validate.sql `
  -Disposable
```

Wrapper never executes migration files and rejects destructive scripts. Execute an approved migration directly with `sqlcmd` only after gates pass.

## Common Mistakes

- PostgreSQL syntax such as `CREATE INDEX CONCURRENTLY`, `LIMIT`, `RETURNING`, or RLS guidance in SQL Server work
- Assuming local means disposable
- Running `init.sql` to test migration behavior
- Editing migration already deployed
- Reporting source tests as runtime SQL validation
- Using `-Q` with interpolated untrusted values
