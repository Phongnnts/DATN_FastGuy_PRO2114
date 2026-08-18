# FastGuy OpenAPI

`fastguy.yaml` is authoritative for endpoints already listed in it.

- Add the smallest complete operation before changing a legacy endpoint.
- Define observable requests, responses, required fields, nullability, enums, status codes, and errors.
- Verify backend serialization and frontend consumption against the same contract.
- Do not copy database schemas into the public API contract.
- Do not add remote `$ref` values.

Run `npm run contract:lint` from `frontend/` before completing an API change.
