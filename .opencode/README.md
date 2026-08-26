# OpenCode Project Setup

The tracked config enables CodeGraph from the global config, Playwright, and ECC Memory Vault. Restart OpenCode after changing MCP configuration.

## GitHub MCP

Set `GITHUB_PERSONAL_ACCESS_TOKEN`, then change `github.enabled` to `true` in `opencode.json`. Keep the token outside this repository.

## SQL Server MCP

The writable SQL Server MCP must target only `DemoDatabase`. Give its dedicated login DML/DDL permissions only inside that database; keep `FastGuyDB` read-only. Set these environment variables, then restart OpenCode:

- `FASTGUY_MSSQL_PASSWORD`

The global `mssql` entry supplies `localhost:1433`, `DemoDatabase`, user `JavaDuAn`, encryption, and a 30-second timeout. Password stays in the environment. The tracked legacy `sqlserver` MCP remains disabled to avoid duplicate tools.

Verify with `opencode mcp list`. Before every write, verify `DB_NAME() = 'DemoDatabase'`. Stored procedures remain prohibited.
