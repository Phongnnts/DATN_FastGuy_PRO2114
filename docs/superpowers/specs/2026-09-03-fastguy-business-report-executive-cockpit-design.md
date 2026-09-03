# FastGuy Business Report Executive Cockpit Redesign

## Objective

Redesign the Admin business report so managers can scan revenue, order completion, product performance, refunds, and operating result in one coherent screen.

The page must:

- use the approved Executive Cockpit direction;
- follow the balanced two-column card rhythm of the supplied reference image;
- show all nine charts simultaneously without tabs or carousels;
- place the top-products table at the bottom across the full content width;
- give the table a restrained macOS-style treatment;
- preserve current report data, API contracts, date filtering, and financial semantics.

## Non-goals

- No database, API, OpenAPI, or backend changes.
- No new report metrics or fabricated data.
- No chart-library replacement or new dependency.
- No tabs, carousels, drill-down routes, exports, or additional filters.
- No redesign of Inventory Reports or unrelated Admin screens.

## Primary user and task

The primary user is a restaurant Admin reviewing business performance. The primary task is to answer, in order:

1. What net revenue and gross profit did the selected period produce?
2. Are completion, refunds, or operating costs a concern?
3. What changed over time?
4. Which order, product, category, payment, hour, weekday, refund, or exception dimensions explain the result?
5. Which products contributed most?

## Information hierarchy

### Header and period controls

The page header contains:

- eyebrow and title `Báo cáo kinh doanh`;
- one-line purpose statement;
- quick-range selector, from date, and to date controls aligned as one compact control group on desktop;
- the existing validation message directly below the controls.

Controls retain current behavior and use visible labels, keyboard focus, and minimum 40px targets.

### Primary KPI strip

Replace the visually uniform KPI wall with one compact strip:

- `Doanh thu thuần` is the dominant KPI and occupies more width;
- `Lợi nhuận gộp`, `Biên lợi nhuận gộp`, and `Tỷ lệ hoàn tất` are primary supporting KPIs;
- remaining values are grouped into quieter secondary facts beneath or beside the primary strip: item revenue, delivery fees, discounts, gross revenue, refunded amount/count, completed cohort orders, cost, cost ratio, average order value, store expenses, and estimated operating result;
- negative operating result uses text, sign, and restrained danger color rather than color alone;
- the management-estimate disclaimer remains adjacent to operating result.

No metric is removed; hierarchy changes only its visual emphasis.

### Period summary

The current natural-language six-month summary remains between KPI information and charts. It becomes a compact insight line highlighting order volume, completed cohort orders, leading product, and quantity.

## Chart layout

All nine charts remain visible at the same time in a two-column desktop grid. Each chart is a white panel with a quiet border, 12px radius, compact heading, short explanatory text, and a fixed chart region. Chart titles and descriptions remain visible outside the canvas.

Order:

1. `Xu hướng doanh thu` — left, first row.
2. `Doanh thu theo tháng` — right, first row.
3. `Trạng thái đơn hàng` — left, second row.
4. `Sản phẩm bán chạy` — right, second row.
5. `Doanh thu theo danh mục` — left, third row.
6. `Phương thức thanh toán` — right, third row.
7. `Doanh thu theo giờ` — left, fourth row.
8. `Hiệu suất theo thứ` — right, fourth row.
9. `Xu hướng hoàn tiền` — left, fifth row.
10. `Lý do ngoại lệ` — right, fifth row.

The supplied content lists ten named chart sections while referring to nine charts. The implementation preserves every currently rendered Chart.js canvas in `ReportsPage.vue`; the exact current canvas count is the source of truth. No current chart is hidden or removed to force an arbitrary count.

### Chart visual language

- Use the existing Chart.js dependency and current datasets.
- Use a restrained FastGuy palette: deep blue for primary revenue, teal for positive/net outcomes, coral-red for refunds, amber for exceptions, violet for order comparison, and cool gray for neutral context.
- Use light horizontal grid lines and remove unnecessary vertical grid lines where readability permits.
- Keep monetary axes compact with Vietnamese formatting.
- Keep legends at the bottom for multi-series charts and avoid legends where direct labels/tooltips suffice.
- Horizontal bars remain for long product/category/reason labels.
- Tooltips retain exact currency, quantities, percentages, order counts, and current semantic explanations.
- Doughnut/status presentation must include text labels so status is not communicated by color alone.
- Empty datasets retain an explicit empty state instead of an empty canvas.

## macOS-style top-products table

The top-products table spans the full grid width below all charts.

Visual treatment:

- white surface with a subtle cool-gray border;
- 12px outer radius and clipped corners;
- compact toolbar-style header containing title, supporting period label, and `10 sản phẩm` count;
- light gray column header band;
- thin row separators instead of boxed cells;
- restrained row hover and keyboard-focus treatment;
- product names left aligned;
- rank centered;
- quantity, revenue, and share right aligned with tabular numerals;
- no heavy shadow, gradient, oversized radius, or decorative traffic-light controls;
- sticky table header only if it does not conflict with the existing Admin shell.

The table keeps the current columns and ranking semantics. Desktop shows the full table. Narrow screens use horizontal scrolling rather than changing financial columns into ambiguous cards.

## Responsive behavior

- Desktop: two chart columns and a full-width table.
- Medium widths: two columns remain while chart heights and gaps tighten where labels stay readable.
- Mobile: charts become one column in the same decision order; KPI strip reflows without horizontal overflow; date controls stack; table scrolls horizontally.
- Primary actions and filters remain reachable without being covered by navigation.
- The page supports 400% zoom/reflow without loss of controls or data access.

## States and accessibility

Preserve existing loading, error, date-validation, and empty states.

- Loading state uses `role="status"` and avoids flashing partially initialized charts.
- Errors use `role="alert"` and keep the retry action.
- Canvas charts retain or gain concise accessible labels and nearby textual context.
- Interactive controls use semantic buttons, labels, visible focus, and at least 40×40px targets.
- Color is never the sole indicator of positive, negative, order, or refund state.
- Motion is limited to Chart.js rendering and respects `prefers-reduced-motion` by disabling or minimizing chart animation.

## Data flow and architecture

- `frontend/src/views/admin/ReportsPage.vue` remains the owner of report loading, current data transformations, Chart.js lifecycle, date controls, KPI rendering, chart panels, and top-products table.
- Existing Admin API methods and response fields remain unchanged.
- Existing stale-request protection and chart destruction/rebuild behavior must remain intact.
- Prefer local computed presentation structures and CSS over extracting one-use components.
- Do not modify `InventoryReportsPage.vue` unless a shared style collision must be prevented; no visual redesign is intended there.

## Testing and acceptance criteria

The redesign is accepted when:

1. All currently rendered business-report charts are visible simultaneously on desktop.
2. Charts appear in a balanced two-column grid and become one column on mobile.
3. Net revenue is visually dominant; profit, margin, and completion are the supporting primary KPIs.
4. Every existing metric remains available and keeps its current semantics.
5. The top-products table spans the full width and has the approved macOS-style treatment.
6. Date presets and custom range behavior remain unchanged.
7. Existing loading, error, invalid-date, empty-data, and stale-request behavior still works.
8. Tooltips retain exact currency/count information.
9. The page has no horizontal overflow except the intentional table scroller on narrow screens.
10. Keyboard focus is visible and status meaning is not color-only.
11. Existing focused tests pass, followed by `npm test` and `npm run build`.
12. Playwright Chromium desktop verifies the report layout, all chart canvases, the table, successful main report requests, and no console/page errors.
