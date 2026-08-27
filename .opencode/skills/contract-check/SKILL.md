---
name: contract-check
description: Use when changing HTTP endpoints, DTOs, JSON responses, request bodies, or frontend API consumers.
---

# Contract Check

- `openapi/fastguy.yaml` is authoritative for contractized endpoints.
- Change the contract before provider or consumer implementation.
- Define request/response bodies, required fields, nullability, enums, status codes, and errors.
- Validate actual serialized backend output and frontend fixtures against the same schema.
- Do not expose DB implementation details or maintain duplicate handwritten contract sources.
- Reject remote `$ref` values. Lint the contract and run backend/frontend contract tests before completion.
