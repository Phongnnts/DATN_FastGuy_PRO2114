# Task 4 Report

## Status

Completed.

## Diagnostics and Root Causes

1. `friendly queue keeps compact filters, tabs, and responsive order presentation`
   - Exact failure: `getByRole('tab', { name: 'Tất cả' })` and later `getByRole('tab', { name: /Khác/ })` were not focused after Arrow navigation.
   - Artifact: `test-results/admin-orders-r4-friendly-q-01f78-sponsive-order-presentation-desktop-chrome/error-context.md`; trace generated beside it.
   - Root cause: moving `ATTENTION` ahead of `Tất cả` changed index-zero keyboard wrap semantics while handlers still treated index zero as the first lifecycle tab.
   - Fix: keep `Tất cả` first and expose `Cần xử lý` immediately after it; preserve existing wrap and roving-tabindex behavior.

2. `drawer confirms an allowed order with exact expected status and refreshes canonical data`
   - Exact failure: focused `tr.order-row-trigger` issued `GET /api/admin/orders/9`, but no `.order-drawer` or dialog named `FG-0009` appeared.
   - Artifact: `test-results/admin-orders-r4-drawer-con-9cace-nd-refreshes-canonical-data-desktop-chrome/error-context.md`; trace generated beside it.
   - Root cause: Enter activation did not prevent the browser default, allowing duplicate activation timing between row keydown and synthetic click.
   - Fix: use `@keydown.enter.prevent`, matching Space activation; retain row focus restoration through `event.currentTarget`.

3. `failed canonical detail reload removes stale drawer actions`
   - Exact failure: code-button click timed out because `<th>Đơn hàng</th>` from the sticky `<thead>` intercepted pointer events.
   - Artifact: `test-results/admin-orders-r4-failed-can-d85fe-emoves-stale-drawer-actions-desktop-chrome/error-context.md`; trace generated beside it.
   - Root cause: sticky table header overlapped the scrolled first row and captured clicks despite being noninteractive.
   - Fix: disable pointer events on the noninteractive sticky `<thead>` and add row scroll clearance.

4. `drawer conflict reloads canonical order and preserves cancellation reason`
   - Exact failure: same `<thead>/<th>` pointer interception at the explicit order-code button.
   - Artifact: `test-results/admin-orders-r4-drawer-con-bc78a-eserves-cancellation-reason-desktop-chrome/error-context.md`; trace generated beside it.
   - Root cause/fix: same sticky-header geometry correction as failure 3.

## Changes

- Removed low-value shortcut cards and `ORDER_SHORTCUTS`.
- Preserved all status tabs, exact URL/API parameters, stale-request guards, refund links, and mobile cards.
- Added compact sticky filter toolbar, advanced date panel, removable chips, and “Xóa tất cả”.
- Added focusable whole-row mouse/Enter/Space interaction without hijacking nested controls.
- Added sticky table header, row focus/hover hierarchy, warning/brand rails, and tabular numeric figures.
- Updated focused policy and desktop E2E coverage.

## Tests

- `node --test tests/admin-order-workspace.test.mjs tests/admin-orders-r4.test.mjs`: RED confirmed before implementation.
- `node --test tests/admin-order-workspace.test.mjs tests/admin-orders-r4.test.mjs tests/admin-order-drawer.test.mjs`: PASS, 24/24.
- `$env:PLAYWRIGHT_API_TARGET='http://127.0.0.1:1'; npx playwright test tests/e2e/admin-orders-r4.spec.js --project=desktop-chrome --config=playwright.config.js --reporter=list`: PASS, 6/6.
- `npm test`: PASS, 698/698.
- `npm run build`: PASS, Vite built 351 modules in 2.03s.
- `git diff --check`: PASS.

## Concerns

None.
