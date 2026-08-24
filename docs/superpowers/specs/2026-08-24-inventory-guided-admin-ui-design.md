# Guided Inventory Admin UI Design

## Goal

Make inventory understandable for an owner-manager without requiring warehouse or accounting terminology. Extend the admin recipe contract only where capacity and inventory settings need authoritative backend behavior.

## Direction

Use a guided operations center. Present the flow `Receive → Recipe → Sell → Count → Review` and today's actionable work before detailed records. New workflows use short staged sections; experienced users retain searchable detail views.

## Navigation

Group inventory links visually under `Quản lý kho`: `Tổng quan`, `Nhập hàng`, `Công thức món`, `Kiểm kê`, `Báo cáo & lịch sử`.

## Inventory Overview

- Lead with `Hôm nay cần làm gì?` and workflow orientation.
- Reduce the default table to ingredient, usable quantity, status, and next action.
- Reveal on-hand, reserved, cost, value, count schedule, and last count in an expandable detail panel.
- Replace technical labels with plain Vietnamese.

## Goods Receipts

- Split the workspace into `Thông tin giao hàng`, `Nguyên liệu nhận được`, and `Kiểm tra & duyệt`.
- Phrase conversion as a sentence: purchased quantity/unit becomes base quantity/unit.
- Preview stock increase, line total, base-unit cost, and document total.
- Keep drafts safe. Approval uses an accessible confirmation dialog explaining stock and average-cost impact.
- Approved receipts render as locked receipts.

## Recipes

- Start with dish/size and an outcome summary: sellable servings, limiting ingredient, cost per serving, food-cost ratio.
- Rename inventory modes in operational language.
- Hide yield under advanced settings with the question `Một mẻ làm được bao nhiêu phần?`.
- Each ingredient row shows amount per serving, current usable stock, estimated servings, and ingredient cost.
- Recipe and inventory settings are separate resources and separate user actions. Sticky `Lưu công thức` sends only recipe `PUT`. The settings panel has `Lưu cách quản lý tồn`, its own confirmation, and sends only settings `PUT`. No UI flow presents these writes as atomic.
- Settings `409` means the requested mode is not ready. Show the backend's operational readiness message exactly; do not claim stale data, concurrent editing, or automatic conflict resolution.
- `INGREDIENT` is ready only with an active, nonempty recipe whose items are active. `FINISHED_GOOD` is ready only with one valid active finished-good mapping/item. `SUSPENDED` and `UNTRACKED` need no source readiness.
- Capacity is a short read-only transaction. It takes `PESSIMISTIC_READ` locks in deterministic order on the variant, relevant recipe and recipe items, then inventory items; finished-good mode locks its mapping and item. Writers use conflicting write locks, preventing a mixed snapshot.
- Capacity treats average unit cost `0` as missing: `costAvailable=false`, `averageUnitCost=0` remains truthful, `costPerServing=null`, aggregate cost remains incomplete. The general OpenAPI `Money` schema still permits zero.
- Unknown persisted inventory mode is a server invariant failure and returns generic `500`, never client `400`.
- OpenAPI documents `400`, `401`, `403`, `404`, `409`, and `500` for the new settings/capacity endpoints at the exact servlet paths.

## Stock Counts

- Guide through `Chọn nhóm`, `Nhập số đếm`, `Xem chênh lệch & duyệt`.
- Show progress and filters for uncounted, variance, matched.
- Show each item as a count card on mobile and a readable row on desktop.
- Require a reason only when variance exists in UI validation.
- Approval dialog summarizes total shortage, surplus, and loss cost.

## Accessibility

- Native buttons, links, inputs, fieldsets, and dialogs.
- 40px minimum project hit areas, visible focus, descriptive labels, text plus color status.
- Dialog focus containment, Escape close, trigger restoration.
- Recipe/settings confirmations use shared `ConfirmDialog`. The custom ingredient picker additionally redirects programmatic outside focus, locks body scroll, closes through one helper on every path, and restores its trigger.
- Live regions for progress, previews, errors, and successful state changes.
- Reflow without horizontal data-entry tables on mobile.

## Scope

OpenAPI, existing Java servlet/service behavior, Vue consumers, tests, and this spec/plan. No database schema/data, dependency, or route-path changes. Existing feature work remains intact.
