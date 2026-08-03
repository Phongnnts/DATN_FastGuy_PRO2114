# Shipper Delivery Design

## Scope

Assigned-order dashboard, pickup, navigation, OTP delivery, delivery failure, COD collection/remittance visibility va shifts.

## Dispatch Model

- Staff assignment duy nhat; bo open pool va self-claim.
- Shipper chi thay don duoc gan cho minh.
- Order PII chi tra sau ownership check.
- Shipper `ACTIVE`, role hien tai `SHIPPER`, shift `CHECKED_IN` moi thao tac.

## Lifecycle

```text
ASSIGNED -> PICKED_UP -> DELIVERED
                    |
                    +-> DELIVERY_FAILED
```

- Pickup xac nhan order code va handoff count.
- Shipper khong co cancel command.
- Delivery failure bat buoc reason code: `CUSTOMER_UNREACHABLE`, `WRONG_ADDRESS`, `CUSTOMER_REFUSED`, `PAYMENT_FAILED`, `DAMAGED`, `OTHER`.
- `DELIVERY_FAILED` khong release inventory/coupon/refund. Staff quyet dinh reassign, retry hoac return.

## Delivery Proof

- He thong tao OTP 4-6 so; chi luu hash.
- OTP co expiry, attempt limit, rate limit va one-time consumption.
- Deliver command can OTP hop le.
- Khong them image proof; Cloudinary khong thuoc scope delivery.

## Payment

- Online order chi deliver khi payment `PAID`.
- COD yeu cau amount chinh xac, non-negative va khong submit trung.
- Deliver tao COD `COLLECTED`; khong dong nghia `REMITTED`.
- Dashboard hien tong COD dang giu va da doi soat; Staff/Admin cap nhat remittance.

## API Behavior

- Pickup, deliver va fail idempotent.
- Retry sau response loss tra canonical success, khong lap history/payment/loyalty.
- Expected status/version bat buoc; stale command tra `409`.
- List/detail DTO thong nhat payment, notes va timestamps.
- Dashboard dem delivery theo `deliveredAt`, khong theo `createdAt`.

## Mobile UI And Accessibility

- Mobile-first cho 320-480px; desktop van dung duoc.
- Primary action sticky o cuoi, co safe-area padding, khong che content.
- Card la semantic link; tabs keyboard-accessible; search co label.
- Modal delivery failure co dialog semantics, focus trap, Escape va restore.
- Action khoa khi request; loading/error/success duoc announce.
- Touch target toi thieu `44x44` px.
- Navigation mo native maps bang toa do neu co, fallback dia chi; khong background tracking.

## Acceptance

- Shipper A khong doc/thao tac order B.
- Shipper ngoai ca/bi khoa khong pickup/deliver/fail.
- OTP sai, het han, replay bi tu choi.
- COD sai khong deliver; retry dung khong lap side effect.
- Failure khong restock tu dong.
- Toan bo flow dung duoc tai 320px, keyboard-only va network retry.
