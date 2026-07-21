# Customer Experience Design

## Scope

Tat ca Guest/User pages: Home, Menu, Product Detail, Cart, Checkout, payment return, tracking, auth, profile/address, orders, favorites, promotions, support va notifications.

## Responsive System

- Mobile-first; verify `320/375/480/768/1024/1440px`.
- Product grid mot cot tai 320px; hai cot tu 400px; tang dan tren desktop.
- Guest header thu gon login/account tren man hinh hep; drawer keyboard-accessible.
- Account navigation horizontal scroll hoac menu gon; khong tran ngang.
- Checkout mot cot tren mobile; desktop hai cot khi summary hien. Step 1 khong de cot trong.
- Modal thanh bottom sheet tren mobile; action khong bi keyboard/safe area che.

## Visual Language

- Dung design tokens chung; bo token khong khai bao.
- Chuan hoa page shell, heading, toolbar, panel, card, form, badge, modal va toast.
- Moi API region co loading, empty, error + retry, success feedback.
- Touch target toi thieu `44x44` px; text khong vo tai zoom 200%.

## Ordering

- Product Detail reset stale product khi route ID doi; hien gia goc/giam dung backend contract.
- Variant/modifier validation ro rang; favorite co pending/error feedback.
- Checkout 3 buoc: dia chi, uu dai, thanh toan.
- User chua co address van chon duoc province/district/ward; co tuy chon luu address.
- Guest/User nhap coupon thu cong; server checkout tinh lai authoritative totals.
- Shipping quote dung cart dimensions authoritative, khong dung kich thuoc co dinh.
- Guest PayOS dung opaque return token; cart chi clear khi payment thanh cong, co recovery khi cancel/timeout.
- Login thanh cong khong bi bao that bai neu cart migration loi; item loi duoc giu lai.

## Orders And Tracking

- Order Detail hien variant, modifier, subtotal, shipping, service fee, discount, payment, refund va timeline.
- Reorder giu configuration; item khong con hop le yeu cau cau hinh lai.
- Tracking hien payment, ETA va delivery state; rate limit va access credential an toan.
- Review chi danh gia toan don theo quyet dinh hien tai; mot review sau delivered.

## Account And Support

- Profile hydrate tu backend, khong suy email/phone tu login identifier.
- Address CRUD co inline validation, accessible confirmation, unsaved-change guard.
- Support ticket hien assignee/status/resolution va retry; pham vi message/attachment chi them khi plan Staff Support can.
- Notification co loading/error/retry; read state rieng user; focus restore.

## Accessibility

- Label co `for/id`; autocomplete dung semantic.
- Error co `aria-invalid`, `aria-describedby`, `role=alert`/`aria-live`.
- Dialog trap focus, Escape close, background inert, restore trigger focus.
- Tabs ho tro Arrow, Home, End va panel association.
- Icon decorative an voi screen reader; icon button co accessible name.

## Acceptance

- Guest/User COD va PayOS dat hang thanh cong o sau breakpoint.
- User khong address va guest coupon hoat dong.
- Khong mat cart sau payment fail; khong hien stale product.
- Totals/modifiers/refund/payment trung backend.
- Keyboard-only hoan thanh auth, menu, cart, checkout, orders va support.
