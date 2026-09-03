# Homepage Banner and Shared Form Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the homepage hero near-full-width and add consistent accessible field validation to login, registration, forgot-password, and checkout/address forms.

**Architecture:** Add one presentational `FormField` component and one pure validation utility, while each page retains its own form state, API calls, loading state, and navigation. Update only the homepage hero shell CSS/template needed for width and responsiveness.

**Tech Stack:** Vue 3 Composition API, Vite, Node test runner, Playwright Chromium, existing CSS tokens.

## Global Constraints

- No database, backend, API, or OpenAPI changes.
- No support/contact feature.
- Scope is limited to login, registration, forgot-password, checkout/address, and homepage hero.
- No new dependency and no unrelated redesign.
- Validate on blur and submit; clear errors while editing once values become valid.
- Error color is `#ef4444`, text size 12–13px, margin-top 4px.
- Preserve existing payloads, address hierarchy, HCM delivery restriction, loading guards, and API-level errors.

---

### Task 1: Shared field and validation primitives

**Files:**
- Create: `frontend/src/components/common/FormField.vue`
- Create: `frontend/src/utils/formValidation.js`
- Create: `frontend/tests/form-validation.test.mjs`

**Interfaces:**
- Produces: `required(value)`, `validEmail(value)`, `validPhone(value)`, `validPassword(value)`, `matchesPassword(value, password)` returning booleans.
- Produces: `FormField` props `id`, `label`, `required`, `error`; default slot receives shared control attributes/classes.

- [ ] Write failing tests for trimmed required values, email, Vietnamese phone, 8–72 character letter+number password, confirmation matching, required marker, error text, and ARIA contract.
- [ ] Run `node --test tests/form-validation.test.mjs`; expect failures because files do not exist.
- [ ] Implement pure validators using the existing phone and registration-password policies.
- [ ] Implement `FormField` with semantic label, required marker, stable error id, `aria-invalid`, `aria-describedby`, red border class, and in-flow error text.
- [ ] Run `node --test tests/form-validation.test.mjs`; expect all tests to pass.

### Task 2: Authentication forms

**Files:**
- Modify: `frontend/src/views/guest/LoginPage.vue`
- Modify: `frontend/src/views/guest/RegisterPage.vue`
- Modify: `frontend/src/views/guest/ForgotPasswordPage.vue`
- Create: `frontend/tests/auth-field-validation.test.mjs`

**Interfaces:**
- Consumes: shared validators and `FormField` from Task 1.
- Preserves: auth store/API calls, cart migration, redirects, password visibility, loading and server errors.

- [ ] Write failing tests asserting exact placeholders, required markers, per-field errors, blur handlers, submit blocking, and reactive error clearing.
- [ ] Run `node --test tests/auth-field-validation.test.mjs`; expect failures against current forms.
- [ ] Add per-field `touched`/`errors` state and field validators to login; use `Vui lòng nhập email`, `Email không hợp lệ`, and `Vui lòng nhập mật khẩu`.
- [ ] Add per-field validation to registration while preserving current name, phone, and password policy; use requested placeholders and confirmation validation.
- [ ] Add exact forgot-password messages and prevent API submission for invalid email.
- [ ] Replace only field wrappers with `FormField`; retain general API error regions.
- [ ] Run `node --test tests/form-validation.test.mjs tests/auth-field-validation.test.mjs`; expect pass.

### Task 3: Checkout and address validation

**Files:**
- Modify: `frontend/src/views/user/CheckoutPage.vue`
- Modify: the existing user address form view identified by route/source inspection if it renders the same recipient/district/ward/street/phone fields.
- Create: `frontend/tests/shipping-field-validation.test.mjs`

**Interfaces:**
- Consumes: `FormField`, `required`, and `validPhone`.
- Preserves: `loadAddressHierarchy`, saved-address selection, province ID 202 restriction, district/ward dependency, fee calculation, checkout payload and idempotency behavior.

- [ ] Write failing tests for exact placeholders/default options and exact required/phone messages.
- [ ] Run `node --test tests/shipping-field-validation.test.mjs`; expect failures.
- [ ] Add touched/errors state for recipient, district, ward, street, and phone.
- [ ] Validate each field on blur and validate all before existing `placeOrder` logic; return before request on failure.
- [ ] Revalidate touched fields on input/change; when district changes reset ward and recalculate dependent errors.
- [ ] Render the five requested fields through `FormField`, retaining current grid and controls.
- [ ] Apply the same behavior to the existing address-management form only if it has the same shipping fields; do not broaden scope to unrelated profile fields.
- [ ] Run `node --test tests/form-validation.test.mjs tests/shipping-field-validation.test.mjs`; expect pass.

### Task 4: Near-full-width homepage hero

**Files:**
- Modify: `frontend/src/views/guest/HomePage.vue`
- Create or modify: `frontend/tests/homepage-hero-width.test.mjs`

**Interfaces:**
- Preserves: banner API/fallback data, carousel controls, CTA routes, reduced motion and accessible labels.

- [ ] Write a failing source-policy test asserting the hero is not constrained by the shared outer `.container`, has bounded viewport gutters, background cover, responsive inner padding, and no horizontal overflow rule violation.
- [ ] Run `node --test tests/homepage-hero-width.test.mjs`; expect failure.
- [ ] Move width responsibility to the hero and keep a separate inner content shell; use responsive viewport gutters matching the supplied reference without changing content hierarchy.
- [ ] Keep background image cover/position and overlay contrast; ensure mobile controls remain inside the viewport.
- [ ] Run `node --test tests/homepage-hero-width.test.mjs`; expect pass.

### Task 5: Full verification

**Files:**
- Test: all files above and existing frontend tests.

- [ ] Run focused tests: `node --test tests/form-validation.test.mjs tests/auth-field-validation.test.mjs tests/shipping-field-validation.test.mjs tests/homepage-hero-width.test.mjs`.
- [ ] Run `npm test`; require zero failures.
- [ ] Run `npm run build`; require success.
- [ ] Run the relevant Playwright Chromium desktop spec against a known test environment; verify banner width/no overflow, each form's blur/submit/live-clear behavior, no invalid submit request, and zero console/page errors.
- [ ] Run `git diff --check` and inspect `git diff --stat`/`git diff` to ensure only intended files changed.
- [ ] Do not commit or push unless explicitly requested by the user.
