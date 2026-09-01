# FastGuy Apple-Inspired Admin Operations Design

**Date:** 2026-09-01
**Status:** Approved in visual review

## Objective

Apply one coherent Apple-inspired operational design to the FastGuy admin shell, Dashboard, and Orders workspace without copying macOS UI or weakening FastGuy brand identity.

The result must help an administrator understand store conditions, find exceptions, and act on orders quickly. It remains an internal operational product, not a decorative analytics showcase.

## Scope

- Redesign the shared admin shell: sidebar, topbar, navigation states, account area, responsive behavior.
- Refine the existing Dashboard analytics and add a compact priority-order preview.
- Redesign Orders as the full operational workspace.
- Replace the desktop order detail drawer with a centered modal; use a full-screen sheet on mobile.
- Preserve existing API contracts and workflows unless implementation evidence proves a minimal contract extension is necessary.

## Non-goals

- No global redesign outside the admin shell and the Dashboard/Orders surfaces it contains.
- No database or API change solely for visual decoration.
- No invented trends, comparisons, counts, statuses, or order facts.
- No dark mode.
- No macOS control cloning, excessive glassmorphism, decorative gradients, floating cards, or animation without operational purpose.
- No duplicate full analytics suite on Orders.

## Product structure

### Dashboard

Dashboard is the summary cockpit. It contains:

1. Page heading, current date, refresh state, and one primary action to open urgent orders.
2. Four KPI cards:
   - Net revenue today.
   - Orders today.
   - Average order value.
   - Completion rate.
3. Seven-day revenue chart and actionable attention list.
4. Active-order status donut, top-five products, and low-capacity product list.
5. A compact preview of five to eight priority orders.
6. A clear route to the full Orders workspace.

The existing four KPIs, three charts, attention data, low-stock capacity policy, and partial-section behavior remain the source implementation. The priority-order preview may use only existing contracted fields or a minimal contract-first extension.

### Orders

Orders remains the complete operational workspace. It contains:

1. Page heading and concise operating purpose.
2. Status tabs with contract-backed counts.
3. Compact two-level filtering:
   - Always visible: search, payment, refund, date, sort, advanced-filter trigger.
   - Advanced: only currently supported filters.
4. Applied-filter chips and a conditional clear-all action.
5. Sticky filter toolbar and sticky semantic table header where viewport space permits.
6. Full-row hover, keyboard focus, row opening, status chips, and contextual actions.
7. Pagination with URL-persisted state.
8. Centered order-detail modal on desktop and full-screen detail sheet on mobile.

Orders does not repeat Dashboard revenue, top-product, or stock analytics.

### Admin shell

- Expanded desktop sidebar: 248px.
- Collapsed sidebar is excluded from the first implementation; responsive navigation continues using the existing drawer behavior.
- Sticky topbar: 64px.
- Dynamic page context, global admin search only when its behavior is real, refresh where page-supported, notifications only when a real destination/data source exists, and account access.
- One persistent FastGuy identity.
- Existing approved navigation grouping remains authoritative.

## Visual direction

### Character

The direction is **Apple-inspired FastGuy Operations**:

- Calm, bright, precise, and tactile.
- Strong typography and whitespace.
- Restrained translucent material only where layering has meaning.
- Dense enough for operations, lighter than a traditional ERP.
- FastGuy orange remains the recognizable signature.

The signature device remains the narrow status rail for urgent rows and attention items. It always appears with text or an icon; color is never the sole state indicator.

### Tokens

| Role | Value | Use |
|---|---:|---|
| Canvas | `#EEF1F5` | Application background |
| Surface | `#FFFFFF` | Primary cards, tables, modal |
| Surface muted | `#F7F8FA` | Toolbars, grouped facts |
| Foreground | `#182230` | Primary text |
| Muted | `#667085` | Secondary text |
| Subtle | `#98A2B3` | Metadata |
| Border | `#E4E7EC` | Dividers and controls |
| Brand | `#F45B2A` | Primary action, active state |
| Brand dark | `#D9481C` | Brand hover/pressed |
| Brand soft | `#FFF0EA` | Selected navigation and tabs |
| Success | `#23885B` | Successful state |
| Warning | `#A35C00` | Attention state |
| Danger | `#B42318` | Failed/destructive state |
| Info | `#2764C8` | Informational state |

Existing admin semantic tokens should be updated or extended rather than duplicating raw values throughout components.

### Typography

- Keep Be Vietnam Pro for brand and Vietnamese readability.
- Use the system font stack only as fallback.
- Page title: 28–30px, 700, compact tracking.
- Section title: 14–17px, 600–700.
- KPI value: 24–30px, 700, tabular numerals.
- Body: 14px minimum for production UI.
- Metadata: 12–13px.
- Avoid repeated uppercase headings; one restrained operational eyebrow is allowed at the main Dashboard heading.

### Shape, depth, spacing

- Desktop page padding: 24–28px.
- Grid and card gap: 12–16px.
- Card padding: 16–20px.
- Controls: 10–12px radius.
- Panels: 14–18px radius.
- Modal: 18–22px radius.
- Cards use one-pixel borders and very light shadows.
- Blur is limited to sticky topbar, overlays, and modal material where background separation is necessary.
- Avoid card nesting unless the inner surface represents a real interaction or information group.

## Dashboard details

### KPI cards

Each KPI includes a label, primary value, and optional comparison only when backend-defined. A small icon or sparkline is allowed only when it carries real data or improves recognition. Static decorative sparklines are forbidden.

### Charts

- Revenue: smooth line/area chart, seven-day chronological series, restrained orange fill.
- Status: semantic-color donut with a clearly labeled total.
- Top products: horizontal bars with labels and sold quantities.
- Charts respect reduced motion.
- Each chart has an equivalent accessible textual or tabular representation.

### Attention

- Maximum four to five priority categories.
- Each row has status rail, label, concise qualifier, count, and exact filtered destination.
- Highest operational urgency appears first.
- Zero state is compact.

### Priority orders

- Five to eight rows only.
- Prioritize attention reason and waiting age over raw creation timestamp.
- Clicking a row opens the same order-detail experience used by Orders or navigates to Orders with preserved context.
- “Xem tất cả” opens the full Orders workspace.

## Orders details

### Tabs

- Horizontal, scrollable when needed.
- Active tab uses brand-soft background and strong brand text.
- Counts appear only when contract-backed.
- Keyboard behavior follows accessible tabs or navigation semantics consistently.

### Filters

- Visible controls remain compact but retain programmatic labels.
- Advanced filters expose only supported query fields.
- Applied chips describe active state and support individual removal.
- URL query remains the durable source for meaningful search, filter, sort, status, and page state.
- Browser Back/Forward restores the visible workspace.

### Table

- Native semantic table on desktop.
- Dominant order cell: code, creation time, and waiting age where actionable.
- Customer: name plus masked phone.
- Items: count plus concise first-item summary.
- Financial values align right with tabular figures.
- Payment and order status remain distinct.
- Hover changes the full row background and adds the FastGuy status rail without moving content.
- Keyboard focus is at least as visible as hover.
- Whole-row opening must not conflict with nested buttons or links.
- Mobile becomes purpose-built order cards preserving all decision-critical fields and actions.

### Order detail

Desktop uses a centered modal, 880px wide with `max-width: calc(100vw - 48px)` and at most 85dvh. Mobile uses a full-screen sheet.

Content:

- Order identity and statuses.
- Customer and delivery information.
- Ordered items and totals.
- Payment/refund facts when available.
- Timeline and ownership context.
- Existing contracted actions with exact state-transition rules.

Behavior:

- Focus containment.
- Escape closes when no destructive confirmation is active.
- Close control has an accessible name.
- Trigger focus restores on close.
- Header may remain sticky; footer is sticky only when persistent actions require it.
- Refresh conflict preserves context and offers re-evaluation.

## Responsive behavior

### Desktop

- Twelve-column Dashboard grid.
- Four KPI cards in one row.
- Revenue/attention split 8/4.
- Status/top products/stock split 4/5/3.
- Orders uses full semantic table and centered modal.

### Tablet

- KPI cards become 2×2.
- Dashboard panels stack according to decision priority.
- Sidebar becomes compact or drawer-based.
- Orders preserves critical columns and moves secondary detail into disclosure/modal.

### Mobile

- KPI cards use one or two columns according to content width.
- Charts stack vertically.
- Tabs scroll horizontally.
- Advanced filters open in a sheet.
- Orders table becomes order cards.
- Order detail fills the viewport using `100dvh`.
- Primary targets aim for 44×44px; no critical target is below 24×24px.

## State model

Dashboard and Orders preserve:

- Initial loading with layout-matched skeletons.
- Refreshing without clearing valid prior data.
- Empty and filtered-empty states.
- Recoverable error with retry.
- Authorization failure.
- Partial Dashboard sections.
- Stale-request rejection during fast route/filter changes.
- Mutation busy, validation, conflict, success, and failure states.

A section failure must not blank unrelated valid Dashboard sections.

## Accessibility

Target WCAG 2.2 AA:

- Semantic HTML before ARIA.
- Text contrast 4.5:1; UI boundaries and large text 3:1.
- Visible focus with non-obscured indicators.
- Color never carries status alone.
- Icon-only actions have accessible names.
- Table headers retain correct associations.
- Modal/sheet contains focus, supports Escape, and restores focus.
- Dynamic refresh and errors use appropriate live regions.
- Charts expose equivalent data.
- Content reflows at 400% zoom without losing actions.
- `prefers-reduced-motion` disables nonessential animation.

## Data and architecture constraints

- `openapi/fastguy.yaml` remains authoritative for contracted endpoints.
- No frontend field is inferred from the mockup.
- Existing Dashboard store/API flow remains the source for current analytics.
- Existing Orders route, store, pagination, stale-request, action-policy, and URL-state behavior remains intact.
- Any priority-order Dashboard data gap follows `DATABASE → OpenAPI → BACKEND → FRONTEND`.
- Runtime schema must be verified before any DB-dependent provider change.
- No new frontend dependency is required; use Vue, Chart.js, Bootstrap Icons, existing Tailwind v4 support, and native CSS/HTML.
- Tailwind utilities use the mandatory `tw:` prefix; migration remains incremental.

## Verification

### Focused checks

- Dashboard source/state tests for all sections and priority-order preview.
- Orders source/state tests for tabs, filters, URL state, table interaction, and modal behavior.
- Accessibility checks for keyboard tabs, row focus, modal focus containment, Escape, and focus restoration.
- Contract/provider tests for any changed field.

### Required gates

- Relevant focused frontend tests.
- `npm test`.
- `npm run build`.
- Desktop Chromium Playwright for Dashboard and Orders primary flows.
- Zero uncaught page or console errors.
- Critical requests successful and contract-conformant.
- `git diff --check`.
- Backend `mvn test` and integration tests only when backend/API behavior changes.

## Acceptance criteria

- Dashboard, Orders, and admin shell share one Apple-inspired FastGuy visual language.
- Dashboard remains the analytics summary; Orders remains the full operational workspace.
- First Dashboard viewport exposes current state and the highest-priority action.
- Orders supports quick scanning, filtering, keyboard access, full-row opening, and safe contextual actions.
- Desktop order detail uses a centered modal; mobile uses a full-screen sheet.
- Existing business truth, permissions, state transitions, partial data, URL state, and stale-request protections remain correct.
- No mockup-only field, comparison, notification, search capability, or interaction is shipped without a real source.
- The UI meets WCAG 2.2 AA requirements for primary workflows.
- No unrelated refactor, dependency, database mutation, or global admin redesign is introduced.
