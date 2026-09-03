# FastGuy Warehouse macOS Floating Workspace Design

## Objective

Redesign the warehouse Admin UI as a cohesive macOS-inspired floating workspace while preserving all existing routes, APIs, payloads, business rules, and mutation flows.

Primary scope:

- `/admin/ingredients`
- `/admin/inventory`
- `/admin/inventory/stock-counts`

Secondary visual alignment only:

- `/admin/inventory/receipts`
- `/admin/recipes`

## Shared visual system

- App background: `#F5F5F7` or the closest existing Admin token.
- White or subtly translucent floating surfaces.
- Radius: 16–18px for primary cards, 10–12px for controls and nested rows.
- Border: one low-contrast pixel.
- Shadow: soft, shallow, and used only to establish floating hierarchy.
- Spacing scale: 8 / 12 / 16 / 24 / 32px.
- FastGuy orange remains the accent for primary CTA, warnings, and selected state.
- Repeated table-row actions remain neutral; orange is reserved for the page CTA and rows needing attention.
- Table rows target 56–64px height.
- Text, not color alone, communicates state.
- Controls remain at least 40px high, preferably 44px for primary touch targets.
- No `transition: all`; reduced-motion is respected.

## Ingredients

### Structure

```text
Header + Add ingredient
Three floating KPI cards
Floating data workspace
  Search + status filter + result count
  Compact low-stock alert
  Six-column table
  Pagination
```

### Table

Columns remain:

- Ingredient
- Base unit
- Warning threshold
- Current cost
- Status
- Actions

`Receive stock` is a neutral secondary row action by default and becomes accented only when the row needs stock or cost data. `Edit` remains secondary.

### Pagination

- Client-side.
- Exactly 10 filtered ingredients per page.
- Pagination operates after search and status filtering.
- Search/filter changes reset to page 1.
- Current page clamps when the filtered result count shrinks.
- Footer displays the visible one-based range, total filtered count, and page controls.

## Inventory

### Structure

```text
Header + existing actions
Three compact metric cards
White floating priority card
Compact quick-action chips
Floating inventory data workspace
  Search + status filter + detail context
  Six-column table
  Pagination
```

Remove the large dark priority block and oversized workflow cards. Preserve the existing actions and route behavior, but present them as compact controls.

Metrics:

- shortage risk;
- low/out stock;
- current inventory value.

The priority card names low-stock items from the current response and links to Goods Receipts. Quick actions preserve existing destinations without competing with the main action.

Table columns remain:

- Item
- Current stock
- Minimum
- Current average cost
- Status
- Actions

Available and reserved quantities become supporting text under current stock when useful.

### Pagination

- Client-side.
- Exactly 10 filtered items per page.
- Runs after search/status filtering.
- Search/filter changes reset to page 1.
- Current page clamps after result changes.
- The current/history tab and URL query behavior remain unchanged.

## Stock Counts

### Stepper

- Three lightweight steps: select ingredients, enter counts, review variance and approve.
- Active step uses a raised white surface and orange accent.
- Complete step uses a check mark.
- Future steps use muted text.
- Avoid a full orange border around a wide tab.

### Step 1 selection workspace

```text
Floating selection card
  Heading + selected count
  Search + group/status filter + Select all + Clear
  2 columns × 5 rows ingredient card grid
  Range + pagination
  Count date | selected count + Create count
```

Ingredient cards:

- entire card is clickable;
- show name, inventory code, and system on-hand quantity;
- selected state uses a subtle tint, accent border, and small check indicator;
- native checkbox remains available to assistive technology but is not the oversized visual control;
- 10 filtered ingredients per page;
- search/filter reset page to 1;
- selection persists across pages;
- `Select all` and `Clear` operate on the currently filtered collection, not only the visible page.

### Count document workspace

- 30/70 desktop split.
- Left panel shows at most five most recent counts.
- Footer action says `Xem tất cả phiếu`.
- No second pagination is placed in the recent-count panel.
- Until a dedicated all-count route/contract is approved, `Xem tất cả phiếu` reveals the existing full list in place rather than creating an unsupported route.
- Right panel uses a compact directed empty state when no count is selected.
- Selected-count editor preserves snapshots, variance filters, required reasons, note fields, approval confirmation, conflict handling, and immutability.

### Responsive behavior

- Ingredient selection grid becomes one column on narrow screens.
- The 30/70 split stacks.
- No horizontal overflow at 390px.
- Selection and count editing remain keyboard accessible.

## Goods Receipts

No major structural redesign beyond the already approved guided workflow. Apply the shared floating surface, radius, border, shadow, spacing, and restrained accent system. Preserve conversion, cost warning, review, draft, approval, and history behavior.

## Recipes

No major structural redesign. Apply shared floating surfaces and reduce redundant borders. Make capacity and cost summaries visually consistent with the warehouse KPI cards. Preserve separate recipe/settings save boundaries and all conflict behavior.

## Data and behavior boundaries

- No database, OpenAPI, backend, API-client, or route-contract changes.
- Pagination is client-side because current item endpoints return arrays.
- No server pagination fields are invented.
- No new all-count route is created.
- Existing mutations, conflict recovery, lazy loading, query synchronization, and approved-document immutability remain unchanged.

## Verification

- Unit tests for pagination range, page reset, and page clamping.
- Source-policy tests for macOS floating hierarchy, reduced row-action emphasis, count grid, recent-five list, and no second count pagination.
- Full `npm test` and `npm run build`.
- Existing Playwright Chromium desktop/mobile warehouse flow.
- Firefox desktop/mobile route smoke when the browser harness is available.
- No console/page errors and no horizontal overflow at 390px.
- `git diff --check`.
