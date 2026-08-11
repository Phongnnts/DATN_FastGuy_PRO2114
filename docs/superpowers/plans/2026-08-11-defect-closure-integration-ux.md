# Defect Closure and Integration UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Đóng BUG-ST-001/002/003/004/007 bằng regression test, GHN fail-closed rõ ràng và PayOS disabled có lý do.

**Architecture:** Giữ backend làm trust boundary cho phí giao hàng và payment method. Mở rộng capability response tương thích ngược; frontend luôn render COD/BANK_TRANSFER và dùng availability để khóa lựa chọn. Product/auth chỉ sửa nếu regression test chứng minh lỗi.

**Tech Stack:** Java 17, Jakarta Servlet/JPA, JUnit 5, Vue 3, Node test runner, Vite.

## Global Constraints

- Không thêm schema, migration hoặc dependency.
- GHN không khả dụng phải chặn checkout; không fallback phí.
- PayOS thiếu cấu hình phải render `BANK_TRANSFER` disabled kèm lý do.
- Không sửa payment lifecycle, webhook, refund hoặc reconciliation.
- Chỉ đóng defect sau khi test tương ứng pass.

---

### Task 1: Xác nhận product và auth regression

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/entity/ProductGalleryDefaultPolicyTest.java`
- Verify: `Backend/FastGuy-FastFoodSite/src/test/java/service/AuthLoginLockPolicyTest.java`
- Modify only if test fails: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Product.java`
- Modify only if test fails: `Backend/FastGuy-FastFoodSite/src/main/java/service/AuthService.java`

**Interfaces:**
- Consumes: `Product.getGalleryImages()`, `AuthService.login(String, String)` policy.
- Produces: regression proof for BUG-ST-001 and BUG-ST-007.

- [ ] **Step 1:** Add entity behavior assertion: new `Product()` returns `"[]"` for `getGalleryImages()`.
- [ ] **Step 2:** Run `mvn -Dtest=ProductGalleryDefaultPolicyTest,AuthLoginLockPolicyTest test`; expect all tests pass.
- [ ] **Step 3:** If a test fails, make smallest implementation change matching approved spec.
- [ ] **Step 4:** Re-run focused tests; expect PASS.

### Task 2: Lock GHN checkout on unavailable fee

**Files:**
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/CheckoutShippingPolicyTest.java`
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Create: `Frontend/test/checkout-integration-ux.test.js`

**Interfaces:**
- Consumes: `ShippingService.calculateFee(...)`, `shippingApi.calculateFee(data)`.
- Produces: `shippingFee === null` as unavailable state; submit remains disabled until numeric fee exists.

- [ ] **Step 1:** Write backend policy test proving registered/guest checkout call `calculateGhnFee`, invalid GHN result throws, and no configured fee fallback exists.
- [ ] **Step 2:** Write frontend regression test proving GHN failure resets fee, shows approved unavailable message, offers retry and blocks submit.
- [ ] **Step 3:** Run focused backend/frontend tests; frontend test should fail on old generic message.
- [ ] **Step 4:** Replace generic GHN error with `Dịch vụ giao hàng chưa được cấu hình hoặc tạm không khả dụng. Vui lòng thử lại sau.` and add explicit pre-submit unavailable guard.
- [ ] **Step 5:** Re-run focused tests; expect PASS.

### Task 3: Render PayOS capability as disabled

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/OrderServlet.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/PaymentCapabilitiesPolicyTest.java`
- Modify: `Frontend/src/views/user/CheckoutPage.vue`
- Modify: `Frontend/test/checkout-integration-ux.test.js`

**Interfaces:**
- Produces backend JSON fields: `methods: ["COD", "BANK_TRANSFER"]` and `availability: { COD: { enabled: true }, BANK_TRANSFER: { enabled: boolean, reason: string|null } }`.
- Frontend produces `isPaymentEnabled(key)` and blocks disabled payment before request.

- [ ] **Step 1:** Write backend contract test requiring both methods, availability state and existing registered/guest guards.
- [ ] **Step 2:** Write frontend tests requiring disabled class, `aria-disabled`, reason text and submit guard.
- [ ] **Step 3:** Run focused tests; expect new capability/UI assertions fail.
- [ ] **Step 4:** Extend `/orders/payment-capabilities` response without removing string `methods` compatibility.
- [ ] **Step 5:** Parse availability in checkout; always render both methods; prevent click, keyboard selection and submit when disabled.
- [ ] **Step 6:** Re-run focused tests; expect PASS.

### Task 4: Verify and close documentation

**Files:**
- Modify: `docs/system-test-defect.md`
- Modify: `docs/product-backlog.md`

**Interfaces:**
- Consumes: passing regression results from Tasks 1-3.
- Produces: defect/backlog state matching tested code.

- [ ] **Step 1:** Run backend `mvn test` and `mvn package`; expect BUILD SUCCESS.
- [ ] **Step 2:** Run frontend `npm test` and `npm run build`; expect success. No lint script exists.
- [ ] **Step 3:** Update BUG-ST-001/002/003/004/007 only when corresponding tests pass; update US008 to `Xong` with BUG-ST-007.
- [ ] **Step 4:** Run `git diff --check` and inspect final diff; expect no unrelated changes.
