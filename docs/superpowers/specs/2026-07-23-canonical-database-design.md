# Canonical Database Design

## Scope

`database/init.sql` la nguon fresh schema va du lieu mau cho local/demo. Script nay drop va tao lai `FastGuyDB`, huy toan bo du lieu cu. Database hien co duoc nang cap tai cho bang chuoi `database/migrations/000..040` theo `RUNBOOK.md`; migration bao toan du lieu va legacy trong cutover.

## Final Folder

```text
database/
  init.sql
  migrations/
    RUNBOOK.md
    000..040
    rollback/
```

Khong xoa chuoi migration retained-data. Bang/cot legacy duoc giu trong cutover; archive/remove chi bang migration duoc phe duyet rieng sau khi ung dung da ngung doc/ghi chung.

## Initialization

- **Canh bao destructive:** chi dung `init.sql` cho local/demo co the xoa; khong chay tren database can giu du lieu.
- Chuyen sang `master`, dong ket noi va drop `FastGuyDB` neu ton tai.
- Tao lai `FastGuyDB`, bat cac SQL Server session options can thiet.
- Tao schema theo thu tu phu thuoc FK.
- Tao constraints va indexes.
- Seed du lieu demo theo thu tu phu thuoc.
- Ket thuc bang cac truy van dem de kiem tra seed.
- Chay lai script cho ket qua giong nhau vi database luon duoc tao lai.

## Schema Contract

- Tao du 26 bang duoc khai bao trong `persistence.xml` va bang cau hinh `ShippingConfig`.
- Database la schema source of truth; `hibernate.hbm2ddl.auto` tiep tuc la `none`.
- Khong tao `Role`, `DeliveryZone`, `FavoriteProduct`, `CartItemModifier`, `OrderItemModifier` hoac cac bang legacy khac.
- `Users.role_name`, `Users.favorite_ids_json`, `CartItem.modifiers_json` va `OrderItem.modifiers_json` la mo hinh hien hanh.
- Khong tao `CartItem.selected_modifier_option_ids` hoac index dua tren cot CSV legacy.
- Payment method chi dung `COD` va `BANK_TRANSFER`.
- Order lifecycle chi dung `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `ASSIGNED`, `PICKED_UP`, `DELIVERED`, `CANCELLED`.

## Integrity

- Moi FK co index phu hop voi truy van runtime.
- Them unique constraints cho order code, idempotency key co filter, payment attempt/order, inventory reservation/order-variant, review/user-order, coupon redemption va cac quan he mot-mot.
- Them check constraints cho amount khong am, quantity duong, rating 1-5, role/status vocabulary, selection bounds va gio ca hop le.
- Moi product chi co toi da mot default variant bang filtered unique index.
- Seed truc tiep gia tri `NOT NULL`, status, timestamps va default flags; khong dua vao update sua du lieu sau seed neu khong can.

## Demo Data

- Tai khoan `ADMIN`, `STAFF`, `SHIPPER`, `USER` dang `ACTIVE`, dung password hash tuong thich backend.
- Danh muc, san pham, variant, modifier group/option va combo.
- Shipping config, banner va coupon con hieu luc theo ngay tuong doi tai thoi diem chay.
- Dia chi, cart va cart item.
- Ca lam cho Staff va Shipper, gom ca hom nay de demo check-in.
- Don hang dai dien cho cac trang thai chinh, co order items va status history.
- Inventory reservation/transaction phu hop trang thai don.
- Payment attempt cho mot don PayOS demo ma khong chua secret/provider credential; runtime chi co capability khi du `PAYOS_CLIENT_ID`, `PAYOS_API_KEY`, `PAYOS_CHECKSUM_KEY`.
- Guest PayOS return dung `orderCode` va opaque `returnProof`; database chi luu `guest_return_proof_hash`, browser return khong la proof thanh toan.
- Coupon redemption, loyalty transaction, review, support ticket va notification.

## Validation

- `init.sql` chay thanh cong tren SQL Server bang `sqlcmd`.
- Tat ca bang trong `persistence.xml` ton tai; khong co bang legacy bi cam.
- Moi cot entity map toi cot SQL dung ten va kieu tuong thich.
- FK check thanh cong; khong co orphan seed.
- Backend `mvn clean test` va frontend tests/build van pass.
- Khong commit credential, token hoac secret vao seed.
