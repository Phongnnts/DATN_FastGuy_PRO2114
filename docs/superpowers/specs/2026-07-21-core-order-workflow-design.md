# Core Order Workflow Design

## Scope

Order lifecycle, authorization, inventory, payment, refund, audit, notification va shift enforcement. Day la contract cho Customer, Staff, Shipper va Admin.

## State Machine

```text
PENDING -> CONFIRMED -> PREPARING -> READY -> ASSIGNED -> PICKED_UP -> DELIVERED
                                                   |
                                                   +-> DELIVERY_FAILED
                                                        -> ASSIGNED
                                                        -> RETURNED
                                                        -> CANCELLED
```

- `WAITING_STOCK_CONFIRM` bi loai khoi code, UI va du lieu sau migration.
- Cancellation dung command rieng; generic status endpoint khong nhan `CANCELLED`.
- Customer chi huy `PENDING` theo policy.
- Staff chi huy truoc `READY` theo policy; khong huy hang dang giao.
- Shipper bao giao that bai, khong huy.

## Authorization

- User order endpoint chi tra order co `user.id` trung token user.
- Guest dung opaque access token cho payment return/tracking nhay cam.
- Staff/Shipper mutation doc lai account status, role va current checked-in shift.
- Assigned Shipper moi doc PII va thao tac delivery order.
- Unauthorized ownership tra `404` de han che enumeration.

## Concurrency

- Them `@Version` cho `Orders` hoac expected version trong conditional update.
- Moi command nhan expected status/version; stale command tra `409` va canonical order.
- Checkout va external callback co idempotency/deduplication key unique.
- History ghi trong cung transaction; notification outbox ghi cung transaction.

## Inventory

- Checkout reserve variant atomically.
- Reservation release mot lan khi payment expiry/cancel truoc preparation.
- `PREPARING` consume reservation.
- Cancellation sau consume tao waste/return decision; khong restock tu dong.
- Inventory transaction la immutable ledger: `RESERVE`, `RELEASE`, `CONSUME`, `RETURN`, `ADJUSTMENT`.

## Payment And Refund

- PayOS `PaymentAttempt` luu provider reference, amount, status, expiry va dedupe key.
- Signed webhook la source of truth; browser return chi doc trang thai qua opaque token.
- Late success sau cancellation tao refund `REQUESTED`.
- Refund la quy trinh noi bo; Admin cap nhat `PROCESSING`, `SUCCEEDED`, `FAILED`, `REJECTED` sau doi soat.
- UI khong bao da chuyen tien khi chi moi cap nhat DB.
- Partial refund chi them khi co policy loyalty cu the; ban dau full refund.

## Schema

- Dong bo `Orders.updated_at`, shipping metadata va timestamps voi entity.
- Chon mot modifier snapshot model; uu tien `OrderItemModifier`, bo mapping trung `modifiers_json` neu khong duoc persist.
- Them CHECK cho enums, so tien khong am va timestamp invariants.
- Them indexes cho order status, assigned shipper, payment expiry, notification receipt.
- Them `PaymentAttempt`, `Refund`, `InventoryReservation`, `InventoryTransaction`, `NotificationReceipt`, `OutboxEvent`, `DeliveryAttempt` khi task tuong ung can.

## Audit And Notifications

- Audit ghi dung actor user/staff/shipper/admin/system.
- Payment event khong ghi vao `OrderStatusHistory`.
- Broadcast role tao recipient/read receipt rieng tung user.
- Outbox retry notification sau commit; business state khong phu thuoc provider notification.

## Acceptance

- Khong co cross-order access.
- Concurrent commands chi mot command thang; side effects dung mot lan.
- Inventory, coupon, payment, refund, loyalty va history doi soat duoc.
- COD va PayOS chay tron luong den delivered.
- Late payment, delivery failure va cancellation tao state hop le.
