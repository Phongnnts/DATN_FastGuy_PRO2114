# FastGuy Apple-Inspired HR Operations Design

**Date:** 2026-09-02
**Status:** Approved in visual review

## Objective

Create a coherent HR administration workspace for FastGuy using the approved bright Apple-inspired visual language. The workspace must help administrators manage accounts, schedule fixed shifts, monitor current operations, approve attendance, and maintain effective-dated pay rates without changing existing business truth.

## Scope

- Apply the refined sidebar and header treatment to the entire Admin shell while preserving the complete route inventory.
- Add a dedicated HR Dashboard.
- Redesign Users as the authoritative account and role-management workspace.
- Redesign Shifts with Week and Month scheduling plus a separate Monitoring view.
- Redesign Attendance & estimated pay with separate attendance-approval and pay-rate workflows.
- Preserve existing APIs, permissions, optimistic conflict checks, stale-response guards, URL state, focus behavior, and responsive behavior.

## Non-goals

- No database or API changes solely for visual presentation.
- No invented staffing targets, payroll totals, departments, trends, comparisons, notifications, or calendar events.
- No claim that estimated pay is finalized payroll.
- No free-form calendar event model; shifts remain Morning, Afternoon, and Evening.
- No future scheduling unless the backend contract is explicitly changed.
- No dark mode, heavy glassmorphism, decorative gradients, thick borders, excessive color, or oversized radii.
- No redesign outside Admin.
- No licensed Yellow Images assets or template source.

## Information architecture

The HR navigation group contains:

1. **Dashboard nhân sự** — workforce and current-operation overview.
2. **Người dùng** — account, role, status, and account-related order history.
3. **Ca làm** — Week/Month scheduling and current-day Monitoring.
4. **Chấm công & tiền công** — attendance approval and effective-dated pay rates.

Users remains an all-role account list including customers, staff, shippers, and administrators. HR metrics do not replace or overload Users.

## Shared Admin shell

### Sidebar

- Desktop sidebar is a distinct white workspace surface with a thin translucent boundary, 16px outer radius, and a restrained two-layer shadow.
- FastGuy identity uses a compact orange `FG` mark, product name, and “Operations Admin” context.
- Navigation is grouped by real operational domains with small muted group labels.
- Default items are monochrome and quiet.
- The active route uses a pale orange surface, orange text/icon, and no heavy rail or thick border.
- The complete existing Admin route inventory remains available.
- The account block is anchored at the bottom on desktop.
- Narrow layouts retain the existing accessible drawer behavior, inert background, focus containment, Escape handling, and trigger-focus restoration.

### Header

- Header is inset from the page edges as a separate white surface.
- It uses a 14px radius, hairline boundary, light layered shadow, and restrained backdrop blur only when sticky layering requires it.
- Global search or notifications appear only when backed by real behavior.
- Page-specific primary actions remain visually dominant.

## Visual system

### Character

The approved direction is bright, minimal, modern, and operational:

- White canvas rather than gray application background.
- Strong hierarchy through spacing and typography.
- FastGuy orange as the single brand accent.
- Semantic green, amber, and red used sparingly for state recognition.
- Notion-like calendar density combined with Apple-like surface treatment.
- Color supports recognition but never carries meaning alone.

### Tokens

| Role | Value | Use |
|---|---:|---|
| Canvas | `#FFFFFF` | Global Admin background |
| Surface | `#FFFFFF` | Cards, tables, dialogs |
| Surface subtle | `#FAFAFD` | Table headers and grouped rows |
| Foreground | `#20212B` | Primary text |
| Muted | `#858794` | Supporting text |
| Hairline | `rgba(20,20,35,.075)` | Boundaries and dividers |
| Brand | `#FF7448` | Primary action and active state |
| Brand soft | `#FFF1EB` | Active navigation and selected items |
| Success | `#16845B` | Active/approved state |
| Success soft | `#EDF8F4` | Success background |
| Warning | `#946000` | Pending/attention state |
| Warning soft | `#FFF7E7` | Warning background |
| Danger | `#C74848` | Missing/disabled/destructive state |
| Danger soft | `#FFF1F1` | Danger background |

Production implementation should extend existing semantic variables rather than repeat raw colors.

### Shape and depth

- Standard controls: 8–10px radius.
- Cards and panels: 12–14px radius.
- Sidebar and dialogs: 14–16px radius.
- Boundaries are hairline and low contrast.
- Cards use nearly invisible borders and light two-layer shadows.
- Buttons use thin boundaries and subtle elevation; primary buttons use a restrained orange shadow.
- Nested surfaces use smaller radii than their containers.
- No `transition: all`; motion is short, property-specific, and disabled when reduced motion is requested.

### Typography and spacing

- Retain Be Vietnam Pro for production Vietnamese UI.
- Page title: 27–30px, 700, compact tracking.
- Section heading: 16–18px, 650–700.
- KPI value: 25–28px with tabular numerals.
- Body: at least 14px in production.
- Metadata: 11–13px where contrast and zoom remain compliant.
- Desktop content uses a bounded readable width and 30–34px page padding.
- Major sections use 12–16px gaps.

## HR Dashboard

### Purpose

Provide a current workforce overview and direct paths to exact operational destinations. It is not a speculative analytics dashboard.

### Structure

1. Heading and current date/context.
2. Four contract-backed KPI candidates:
   - Active workforce/account count when derivable from current Users data.
   - Today’s staffed fixed shifts, shown as a fraction of three.
   - Pending attendance approvals.
   - Current monitoring exceptions.
3. Today’s three fixed shifts with assigned employee, time, and monitoring/scheduling status.
4. Attention list ordered by operational severity.
5. Compact Week calendar preview with a route to Shifts.

Every KPI must be verified against existing responses or introduced contract-first. Candidate mockup numbers are not production defaults.

### Partial data

Independent Users, Shifts, Monitoring, and Attendance reads preserve valid sections when another read fails. No client-side recreation of backend monitoring-state calculations is allowed.

## Users

### Purpose

Users is the authoritative account-management workspace, not the HR analytics page.

### Structure

1. Heading and one primary action: **Thêm người dùng**.
2. Four truthful account summaries:
   - Total accounts.
   - Active accounts.
   - STAFF plus SHIPPER count.
   - Inactive accounts.
3. Optional proportions derived only from the same loaded account collection.
4. Search by name, email, phone, or ID.
5. Role filters: All, Customer, Staff, Shipper, Admin.
6. Semantic table containing identity, contact, role, status, loyalty points, and actions.
7. Pagination.
8. A right-side detail inspector for quick account review.
9. Add/Edit account dialog.
10. Role-aware order-history dialog preserving current behavior.

### Interaction and safeguards

- Search/filter changes reset pagination.
- Status and destructive actions retain confirmation.
- Self-account role/status/delete safeguards remain enforced.
- Avatar upload/remove behavior and current validation remain intact.
- Dialogs contain focus, support Escape, and restore trigger focus.
- Mobile converts the table to readable account cards or a purpose-built compact list.

## Shifts

### Primary modes

The Shifts workspace has two top-level modes:

1. **Lịch phân ca** — editable scheduling truth.
2. **Theo dõi hôm nay** — current operational monitoring truth.

These modes remain separate because their data semantics and mutation rules differ.

### Week view

- Week is the default calendar mode.
- Seven columns run Monday through Sunday.
- Three fixed rows represent Morning, Afternoon, and Evening.
- Each assignment shows employee identity, time, and relevant text status.
- Empty cells clearly expose assignment action.
- Today uses an orange date marker and text label.
- Previous/current-week navigation follows backend policy; future navigation stays disabled while future weeks are forbidden.
- Save keeps the current canonical-week freshness check and conflict behavior.
- View, period, selected date, and current Shifts tab should persist in URL state where supported.

### Month view

- Month uses a quiet Notion-like hairline grid.
- Day cells show only the three fixed shift assignments as compact labeled entries.
- Month is an overview and navigation mode, not a free-form event calendar.
- Selected day opens a desktop inspector containing Morning/Afternoon/Evening details.
- Mobile renders the inspector as a full-width sheet/panel.
- Month data must be obtained only through an approved contract strategy. Repeated weekly reads or a month-range endpoint must be decided during implementation planning after provider/contract analysis.

### Monitoring

- Monitoring displays three current-business-date cards or compact rows.
- Each includes fixed shift identity, assigned employee, check-in/out source/time when contracted, textual monitoring state, severity, and exact action where supported.
- Current, warning, and critical states use restrained semantic tints plus labels/icons.
- Existing 30-second polling applies only while Monitoring is active.
- Existing generation guards prevent stale responses.
- Backend `WorkShiftService` remains authoritative for monitoring states and severity.

## Attendance & estimated pay

### Primary modes

1. **Duyệt chấm công** — default operational workflow.
2. **Mức tiền công** — effective-dated rate management.

The separation prevents rate editing from competing with attendance approval and prevents estimated pay from looking like finalized payroll.

### Attendance approval

- Filters remain Month, Status, and Staff.
- Summary shows only values derivable from the currently loaded response:
  - Calculated approved total for rows carrying calculated snapshots.
  - Pending count.
  - Rows missing effective pay rate.
  - Legacy rows without historical snapshot.
- Table preserves actual minutes, eligible overlap, late/early minutes, overtime, editable approved minutes, approved overtime, note, pay state, and approval action.
- Pay presentation distinguishes:
  - **Calculated / snapshot finalized for the attendance row.**
  - **Estimated preview.**
  - **Missing effective pay rate.**
  - **Legacy unavailable snapshot.**
- Approval remains disabled when already approved, another approval is in progress, or effective rate is missing.
- Optimistic `expectedUpdatedAt` conflict handling remains intact; conflict reloads canonical data and keeps the error visible.

### Pay rates

- Staff selection precedes rate creation/history.
- Rate form contains effective date, regular hourly rate, and overtime hourly rate only.
- History is chronological and visibly effective-dated.
- A new rate does not imply rewriting prior approved snapshots.
- Duplicate effective dates retain the current conflict error.

## State model

All four HR surfaces support relevant states:

- Initial loading with layout-matched feedback.
- Refreshing without discarding valid prior data.
- Empty and filtered-empty states.
- Recoverable API errors with retry.
- Disabled mutation and mutation-in-progress states.
- Success feedback.
- Conflict/stale-data recovery.
- Partial HR Dashboard data.
- Responsive desktop, tablet, and mobile reflow.

## Accessibility

Target WCAG 2.2 AA:

- Semantic landmarks, headings, forms, tables, and buttons before ARIA.
- Programmatic labels for every search, filter, calendar control, input, and icon-only action.
- Primary targets aim for 40–44px; no critical target is below 24px.
- Visible, unobscured focus indicators.
- Tab/segmented controls expose selected state and keyboard behavior consistently.
- Status is always communicated through text or icon plus color.
- Tables retain header associations; mobile transformations preserve field labels.
- Dialogs/sheets contain focus, close on Escape when safe, and restore focus.
- Dynamic loading, errors, and mutation results use appropriate live regions.
- Content reflows at 400% zoom without losing primary actions.
- Nonessential animation respects `prefers-reduced-motion`.

## Data and architecture constraints

- `openapi/fastguy.yaml` remains authoritative for contracted endpoints and fields.
- No frontend field is inferred from mockups or screenshots.
- Scheduling/monitoring/attendance business calculations remain in backend services.
- Existing Vue API clients, Pinia stores, request-generation guards, and mutation safeguards remain intact.
- Any API gap follows `DATABASE → OpenAPI → BACKEND → FRONTEND`.
- Runtime SQL Server identity and schema must be freshly verified before any DB-dependent backend change.
- No new calendar or UI dependency is required unless implementation analysis proves existing Vue/native CSS insufficient.
- Use existing Bootstrap Icons or approved current icon system; do not ship text glyphs from mockups as final icons.
- Tailwind v4 utilities, if used, require the `tw:` prefix.

## Verification

### Focused checks

- Admin shell navigation inventory, active state, drawer focus containment, Escape, inert background, and focus restoration.
- HR Dashboard independent loading, truthful summary derivation, partial failures, and exact destinations.
- Users search, role filters, pagination, self-account safeguards, add/edit validation, avatar behavior, status/delete confirmations, and order-history dialog.
- Shifts Week default, URL restoration, period controls, save freshness check, stale guards, Week/Month switching, selected-day inspector, Monitoring polling lifecycle, and monitoring states.
- Attendance filters, stale guards, editable approval fields, missing-rate disablement, expected-update conflict reload, pay-state labels, rate history, and duplicate-rate conflict.
- Accessibility tests for keyboard controls, dialogs/sheets, tables, status labels, focus, and reduced motion.

### Required gates

- Relevant focused frontend tests.
- `npm test`.
- `npm run build`.
- Desktop Chromium Playwright for HR Dashboard, Users, Shifts Week/Month/Monitoring, and Attendance approval/pay-rate flows.
- Narrow/mobile Chromium verification for sidebar drawer, calendar reflow/inspector, Users list, and attendance approval.
- Zero uncaught console errors.
- Critical requests successful and contract-conformant.
- `git diff --check`.
- Backend `mvn test` and disposable/local integration tests only when backend/API behavior changes.

## Acceptance criteria

- The Admin shell and all HR routes share one bright Apple-inspired FastGuy visual system.
- Sidebar/header are distinct, lightly elevated surfaces with moderate radii and hairline boundaries.
- HR Dashboard summarizes only verified workforce and operational facts.
- Users remains the account-management source of truth and preserves every current safeguard.
- Shifts opens in Week mode, supports Month overview, and keeps Monitoring separate from editable scheduling.
- Calendar styling is quiet and Notion-like without changing the fixed-shift domain model.
- Attendance approval and pay-rate management are separate, understandable workflows.
- Estimated, calculated, missing-rate, and legacy-unavailable pay states cannot be confused.
- Existing contracts, permissions, conflict controls, stale-response guards, URL state, and responsive/accessibility behavior remain correct.
- No mockup-only number, field, trend, notification, search function, or calendar capability ships without a real source.
- No unrelated refactor, dependency, database mutation, or non-Admin redesign is introduced.
