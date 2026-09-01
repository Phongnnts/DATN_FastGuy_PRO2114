---
name: vue-frontend
description: Use when editing FastGuy Vue components, Pinia stores, Vue Router, or frontend API clients.
---

# FastGuy Vue Frontend

- Plan first. Follow Vue 3 Composition API and existing `<script setup>` conventions.
- Before editing `src/api`, locate the OpenAPI operation and schemas. Add the smallest contract first if absent.
- Never infer response fields from screenshots, stale examples, or component assumptions.
- Preserve loading, error, empty, stale-request, keyboard, focus, and mobile states where relevant.
- Use existing Pinia, Router, Axios, and CSS patterns; add no dependency for a few lines of code.
- Run focused tests, `npm test`, and `npm run build`. Important UI flows also require Playwright desktop checks.
