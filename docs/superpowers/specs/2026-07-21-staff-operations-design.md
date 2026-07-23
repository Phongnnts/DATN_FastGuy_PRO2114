# Staff Operations Design

## Scope

Dashboard, order queue/detail/history, shipper assignment, shifts, support, notifications va operational inventory visibility.

## Shift And Authorization

- Endpoint current shift tra `NONE`, `UPCOMING`, `CHECK_IN_ALLOWED`, `CHECKED_IN`, `CHECKED_OUT`.
- Check-in chi cho ca hom nay trong khoang `startTime - 15 phut` den `endTime + 15 phut`.
- Check-out chi cho ca da check-in va tu `endTime` tro di; khong gioi han checkout muon.
- Dashboard va don dang xu ly chi mo khi shift `CHECKED_IN`; truoc check-in va sau check-out chuyen ve trang ca lam.
- Lich su don, ho tro va trang ca lam van mo cho Staff hop le ngoai ca.
- Backend la nguon phan quyen: dashboard, danh sach/chi tiet don dang xu ly, export, ghi chu, cap nhat trang thai va gan Shipper deu yeu cau shift `CHECKED_IN`.
- Mutation Staff chi cho account `ACTIVE`, role hien tai `STAFF`, shift `CHECKED_IN`.
- UI khong fail-open khi shift request loi.
- Ho tro nhieu ca cung ngay; hien ca hien tai va ca sap toi.

## Order Queue

- Trang thai: `PENDING`, `CONFIRMED`, `PREPARING`, `READY`, `ASSIGNED`; history cho terminal/delivery states.
- Desktop table; mobile cards; khong horizontal overflow bat buoc.
- Search order code, customer name, phone; server filter, date va pagination.
- SLA age, overdue sort, last-updated va retry state.
- Tab query dong bo URL; request cu khong ghi de tab moi.

## Order Detail

- Hien customer phone/address, item, variant, modifier snapshot, note, full amount breakdown, payment/refund va audit timeline.
- Action lay tu backend `allowedActions`; expected status/version kem mutation.
- `409` tai canonical order va thong bao conflict.
- Mutation success khong bi doi thanh failure neu reload sau do loi.
- Internal note la append-only record co author/time/max length.
- Print kitchen ticket/invoice dung semantic document; CSV escape va formula-neutralize.

## Assignment

- Staff gan Shipper duy nhat khi order `READY`.
- Danh sach chi gom Shipper `ACTIVE`, `CHECKED_IN`, con capacity; hien active order count.
- Assignment va reassignment co audit dung Staff actor.
- PII khong xuat hien trong pool Shipper chua duoc gan.

## Support

- Atomic claim `OPEN -> PROCESSING`; mot assignee.
- Chi assignee/Admin resolve; resolution bat buoc.
- Concurrent claim tra `409`; UI hien assignee va refresh.
- Reopen/message/attachment chi them neu can cho nghiep vu khieu nai sau khi core on dinh.

## UI And Accessibility

- Sidebar mobile co Escape, focus trap/restore va overlay.
- Tabs dung ARIA + keyboard.
- Modal cancellation/support co focus lifecycle day du.
- Notifications co live announcement, loading/error/retry.
- Charts co text/table equivalent.
- Touch target toi thieu `44x44` px; verify sau breakpoint.

## Acceptance

- Hai Staff thao tac dong thoi khong skip/ghi de state.
- Staff ngoai ca/bi khoa khong mutation duoc.
- Staff chua check-in hoac da check-out khong xem duoc dashboard va don dang xu ly, nhung van xem duoc lich su don va ho tro.
- Check-out truoc `endTime` bi tu choi; check-out tu `endTime` tro di thanh cong.
- Kitchen thay modifier va amount breakdown dung.
- Chi Shipper hop le duoc gan.
- Queue/history/support dung duoc tren 320px va keyboard-only.
