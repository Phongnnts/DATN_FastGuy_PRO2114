# Module Guest

**Mục tiêu**: Xem thực đơn, chi tiết sản phẩm (kèm option Size/Combo + gallery ảnh Cloudinary), giỏ hàng, đặt hàng (checkout), tra cứu đơn.

**Người phụ trách**: Người 2

---

## Files

### Backend
- `servlet/ProductServlet.java` (sửa wildcard + thêm `/{id}`)
- `servlet/CategoryServlet.java`
- `servlet/DeliveryZoneServlet.java`
- `servlet/CartServlet.java`
- `servlet/OrderServlet.java`
- `service/CartService.java`
- `service/OrderService.java`

### Frontend
- `views/guest/HomePage.vue`
- `views/guest/MenuPage.vue`
- `views/guest/ProductDetailPage.vue`
- `views/guest/CartPage.vue`
- `views/guest/TrackOrderPage.vue`
- `stores/cart.js`

---

## API Endpoints

| Method | Path | Trạng thái |
|--------|------|------------|
| GET | `/api/products` | ✅ Có |
| GET | `/api/products/{id}` (kèm options + image_url) | ❌ Sửa |
| GET | `/api/categories` | ✅ Có |
| GET | `/api/delivery-zones` | ✅ Có |
| GET/POST/PUT/DELETE | `/api/cart/*` | ✅ Có |
| POST | `/api/orders/` (checkout) | ✅ Có |
| GET | `/api/orders/track?code=` | ✅ Có |

---

## Việc cần làm

- [ ] Backend: sửa `ProductServlet` exact path → wildcard `/api/products/*`
- [ ] Backend: thêm handler `GET /{id}` trả về product + options + image_url
- [ ] Frontend: `ProductDetailPage` — radio chọn Size/Combo + cộng extra price
- [ ] Frontend: `ProductDetailPage` — gallery Cloudinary (nhiều ảnh)
- [ ] Frontend: `ProductDetailPage` — giữ chỗ hiển thị review (chưa cần dữ liệu)
- [ ] Frontend: `CartPage` — hiển thị option đã chọn trên từng item

---

## Phụ thuộc

- **Auth**: Cần Auth để checkout (lấy user từ JWT)
- **Common**: Cần Cloudinary config để hiển thị ảnh
