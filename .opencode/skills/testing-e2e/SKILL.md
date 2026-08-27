---
name: testing-e2e
description: Use when verifying important FastGuy UI flows with Playwright on a real browser.
---

# FastGuy E2E

- Cover important auth, checkout, payment, account, staff, shipper, admin mutation, and critical guest flows.
- Run Chromium desktop and a mobile viewport.
- Assert user-visible behavior, no uncaught page errors, no console errors, and successful critical API requests.
- Use role/label/test-id locators and condition-based waits; never use arbitrary sleep.
- Run against a known test environment. A mocked UI test is not an integration result.
- Keep traces and screenshots on failure only. Do not commit browser profiles or reports.
