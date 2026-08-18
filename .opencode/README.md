# OpenCode Project Setup

The tracked config enables CodeGraph from the global config, Playwright, and ECC Memory Vault. Restart OpenCode after changing MCP configuration.

## GitHub MCP

Set `GITHUB_PERSONAL_ACCESS_TOKEN`, then change `github.enabled` to `true` in `opencode.json`. Keep the token outside this repository.

## SQL Server MCP

Use a dedicated login limited to `SELECT` and `VIEW DEFINITION`. Set these environment variables, then change `sqlserver.enabled` to `true`:

- `FASTGUY_DB_AUTHENTICATION_TYPE`
- `FASTGUY_DB_SERVER`
- `FASTGUY_DB_NAME`
- `FASTGUY_DB_USER`
- `FASTGUY_DB_PASSWORD`
- `FASTGUY_DB_PORT`
- `FASTGUY_DB_ENCRYPT`
- `FASTGUY_DB_TRUST_SERVER_CERTIFICATE`

`DB_ALLOW_MODIFICATIONS` and `DB_ALLOW_STORED_PROCEDURES` remain hard-disabled in the tracked config.

Verify with `opencode mcp list`. Database writes never use MCP.
