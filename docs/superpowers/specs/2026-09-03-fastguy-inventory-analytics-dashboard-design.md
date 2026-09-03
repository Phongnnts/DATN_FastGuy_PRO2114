# FastGuy Inventory Analytics Dashboard Design

## Objective

Upgrade `/admin/inventory` from a status-oriented page into a manager-facing warehouse dashboard with the hierarchy:

```text
KPI → trends → action → detailed inventory → evidence
```

The dashboard must answer within seconds:

- What is the warehouse worth now?
- Is inventory value increasing or decreasing?
- How much value entered, was consumed, or was lost each day?
- What percentage of items is healthy?
- Which items require action today?

## Scope

This design extends the current warehouse macOS floating redesign. It changes:

- OpenAPI contract;
- backend inventory reporting aggregation;
- one Admin analytics endpoint;
- frontend API client;
- Inventory dashboard presentation and charts;
- tests and browser fixtures.

It does not require a database migration. Existing `InventoryTransaction`, item balances, cost snapshots, and minimum thresholds are the source data.

## API contract

Add:

```http
GET /admin/inventory/analytics?fromDate=YYYY-MM-DD&toDate=YYYY-MM-DD&granularity=DAY
```

Constraints:

- Admin authorization required.
- `fromDate` and `toDate` are required inclusive local dates.
- `fromDate <= toDate`.
- Maximum range: 366 days.
- `granularity` currently accepts only `DAY`.
- Response schemas are closed and use decimal values consistently with existing inventory reports.

### Response

```text
period
  fromDate
  toDate
  granularity
comparisonPeriod
  fromDate
  toDate
kpis
  itemCount
  lowStockCount
  outOfStockCount
  inventoryValue
previousKpis
  itemCount
  lowStockCount
  outOfStockCount
  inventoryValue
series[]
  date
  inventoryValue
  receiptValue
  consumptionValue
  wasteValue
  adjustmentLossValue
  adjustmentGainValue
health
  healthyCount
  attentionCount
  lowStockCount
  outOfStockCount
attentionItems[]
  inventoryItemId
  inventoryCode
  name
  baseUnit
  onHandQuantity
  availableQuantity
  minimumQuantity
  healthRatio
  healthState
```

`healthState` is one of:

- `OUT`: available quantity is zero or below;
- `LOW`: health ratio below `1.0`;
- `ATTENTION`: ratio from `1.0` through `1.25` inclusive;
- `HEALTHY`: ratio above `1.25` through `2.0`;
- `EXCESS`: ratio above `2.0`.

Items with nonpositive minimum quantity have no meaningful ratio and are excluded from `attentionItems`; they count as `HEALTHY` unless out of stock.

## Historical valuation semantics

Daily inventory value must be reconstructed from transaction history through the end of each day, using quantity-after snapshots and cost snapshots according to existing weighted-average rules.

For each day:

- `inventoryValue` is the ending on-hand inventory value;
- `receiptValue` sums `RECEIPT.totalCost`;
- `consumptionValue` sums `CONSUME.totalCost`;
- `wasteValue` sums `WASTE.totalCost`;
- `adjustmentLossValue` sums negative stock-count `ADJUSTMENT.totalCost`;
- `adjustmentGainValue` sums positive stock-count `ADJUSTMENT.totalCost`.

Missing cost evidence remains missing from monetary aggregation rather than being invented as zero-cost stock. The existing report convention may expose a partial-data indicator if needed; UI copy must not claim complete valuation when cost data is incomplete.

The comparison period has the same inclusive day count immediately preceding `fromDate`.

## Dashboard layout

### Header

- Title: `Vận hành kho hôm nay`.
- Description on the left.
- Existing actions on the right: Add item, Goods Receipts, History.
- Period controls: 7 days, 30 days, 90 days, custom date range.
- Period selection is URL-backed so refresh/share preserves the view.

### KPI strip

One floating horizontal card with four cells:

- Total items;
- Need replenishment;
- Out of stock;
- Inventory value.

Each cell shows current value and previous-period delta. For count deltas, show signed absolute change. For inventory value, show signed percentage only when previous value is positive; otherwise show truthful fallback copy.

Do not duplicate these metrics in separate cards below.

### Primary chart row

#### Inventory value over time

Large line chart:

- one point per day;
- ending inventory value;
- tooltip shows date and ending value;
- comparison text above chart.

#### Inventory movement value

Stacked/grouped daily bars using VND:

- receipts;
- consumption;
- waste and count loss;
- adjustment gain may be a separate positive series.

No quantity/value switch is provided because quantities use incompatible base units (`g`, `ml`, `cái`).

### Secondary row

#### Inventory health

Use a minimal segmented horizontal bar, not a donut. Show counts for healthy/excess, attention, low, and out states. State remains textual and color is supplementary.

#### Action today

Compact floating card:

- names the highest-priority low/out items;
- shows available amount relative to minimum;
- links to Goods Receipts;
- shows a healthy empty state when no item needs action.

### Detailed inventory

Keep the approved minimal six-column inventory table and client-side 10-item pagination. Search, status filtering, current/history tabs, adjustments, waste, and URL query behavior remain unchanged.

### Bottom evidence row

- Attention-item horizontal ratio visualization (`available / minimum`).
- Recent inventory transactions.

The ratio visualization is decision-oriented, not a largest-stock chart. Values over 200% may be visually capped while the text preserves the true ratio.

## Charts implementation

Reuse the chart library already present in the Admin dashboard. Do not add a dependency. Charts must:

- have semantic headings and text summaries;
- use tooltips for pointer users;
- expose equivalent accessible text/table summaries;
- resize without horizontal overflow;
- avoid animation under `prefers-reduced-motion`;
- distinguish series with labels and patterns/shape where practical, not color alone.

## Error and partial states

- Inventory item list and analytics load independently.
- Initial analytics failure shows a retryable chart-area error while the inventory table remains usable.
- Refresh retains prior analytics and displays a nonblocking warning.
- Empty period shows zero activity and a flat/empty value chart without fabricating points.
- Missing monetary cost evidence is labeled `Chưa đủ dữ liệu giá vốn`.
- Stale analytics responses cannot replace newer period requests.

## Verification

- OpenAPI exact path/query/schema tests.
- Backend unit tests for date range, comparison period, daily buckets, valuation reconstruction, movement categories, health thresholds, and missing costs.
- Servlet contract/auth/error tests.
- Disposable/local integration test using real transaction rows.
- Frontend helper tests for period query, KPI deltas, chart normalization, health presentation, and stale request handling.
- Full `mvn test`, `npm test`, and `npm run build`.
- Chromium desktop/mobile and Firefox smoke tests.
- Zero console/page errors, successful analytics request, no 390px overflow.

## Safety and non-goals

- No database migration or seed change.
- No fabricated chart data.
- No summing incompatible physical quantities.
- No demand forecasting, reorder recommendation engine, supplier analytics, or multi-warehouse comparison.
- No commit, merge, push, or deployment without explicit instruction.
