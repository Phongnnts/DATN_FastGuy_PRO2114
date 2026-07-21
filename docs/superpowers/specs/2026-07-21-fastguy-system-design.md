# FastGuy System Completion Design

## Goal

Hoan thien luong dat hang tu Customer den Staff va Shipper; dong bo Admin; dam bao bao mat, tinh dung du lieu, responsive va accessibility.

## Delivery Strategy

Trien khai theo lat cat nghiep vu. Moi lat cat gom schema, backend, API, frontend, test. Thu tu phu thuoc:

1. Va loi bao mat va schema Critical.
2. Hop nhat order state machine, cancellation, audit va authorization.
3. Hoan thien Customer.
4. Hoan thien Staff.
5. Hoan thien Shipper.
6. Hoan thien refund, notification va inventory lifecycle.
7. Dong bo Admin.
8. E2E, responsive, accessibility va security verification.

## Canonical Order Lifecycle

```text
PENDING -> CONFIRMED -> PREPARING -> READY -> ASSIGNED -> PICKED_UP -> DELIVERED
```

- Bo `WAITING_STOCK_CONFIRM`; checkout da reserve ton kho.
- Staff la tac nhan duy nhat gan Shipper.
- Shipper khong duoc huy don.
- Giao that bai tao `DELIVERY_FAILED`; Staff quyet dinh giao lai hoac nhan hang hoan.
- Huy truoc che bien release reservation va coupon.
- Sau `PREPARING`, khong tu dong cong hang da che bien vao ton ban.

## Shared Invariants

- Moi mutation kiem role, ownership, account `ACTIVE`, current shift, expected status va version.
- Order, inventory effect va history commit trong mot transaction.
- Notification phat qua outbox; read state rieng tung user.
- Checkout dung `Idempotency-Key`; mot request logic chi tao mot order.
- PayOS webhook la nguon thanh toan chuan; event trung lap khong tao side effect.
- Late payment sau cancellation tao refund `REQUESTED`.
- Refund noi bo: `REQUESTED -> PROCESSING -> SUCCEEDED | FAILED | REJECTED`.
- Delivery OTP luu hash, co expiry, gioi han lan thu, chong replay.
- COD tach `COLLECTED` va `REMITTED`.
- Endpoint privileged doc lai status va role hien tai tu DB.

## Immediate Critical Fixes

1. Authenticated User khong duoc doc Guest order qua numeric ID.
2. Shipper khong duoc huy don khong thuoc minh hoac don cua minh.
3. Payment verification muon khong duoc tao `CANCELLED + PAID` ma khong co refund.
4. Admin khong duoc cancel qua generic status transition bo qua inventory/coupon/refund.

## Error Contract

API error thong nhat:

```json
{
  "code": "ORDER_STATUS_CONFLICT",
  "message": "Don hang da duoc cap nhat",
  "fieldErrors": {},
  "retryable": false
}
```

- `400`: input sai.
- `401`: thieu/het han token.
- `403`: sai role, account/shift khong hop le.
- `404`: khong ton tai hoac khong thuoc actor.
- `409`: stale version, transition conflict, duplicate command.
- `500`: message chung; khong lo exception noi bo.

## Verification Gates

- Backend: unit/integration cho transition, ownership, race, idempotency, inventory, payment, OTP, COD, shift, support.
- Frontend: build; loading, empty, error, retry, conflict va double-submit.
- E2E: Guest/User COD va PayOS den `DELIVERED`; delivery failure; cancellation; late payment; refund.
- Responsive: `320`, `375`, `480`, `768`, `1024`, `1440` px.
- Accessibility: keyboard-only, focus, screen reader, zoom 200%, reduced motion, touch target `44x44` px.
- Security: cross-order access, wrong role, inactive account, outside shift, brute-force tracking va OTP.

## Subproject Specs

- `2026-07-21-core-order-workflow-design.md`
- `2026-07-21-customer-experience-design.md`
- `2026-07-21-staff-operations-design.md`
- `2026-07-21-shipper-delivery-design.md`
