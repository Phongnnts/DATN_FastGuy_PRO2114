# Module Guest

**Người phụ trách**: Người 2

## Mục tiêu

Khách xem menu, xem chi tiết món, chọn `ProductVariant`, thêm giỏ hàng và xem cart đúng biến thể.

---

## Files

### Backend phối hợp

- `servlet/ProductServlet.java`
- `servlet/CategoryServlet.java`
- `servlet/CartServlet.java`
- `dao/ProductDAO.java`
- `entity/Product.java`
- `entity/ProductVariant.java`

### Frontend

- `views/guest/HomePage.vue`
- `views/guest/MenuPage.vue`
- `views/guest/ProductDetailPage.vue`
- `views/guest/CartPage.vue`
- `views/guest/TrackOrderPage.vue`
- `stores/cart.js`
- `stores/product.js`
- `api/product.js`

---

## API Product cần dùng

### Product list

```http
GET /api/products
```

Cần trả:

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "imageUrl": "",
  "defaultVariant": {
    "variantId": 1,
    "price": 45000
  }
}
```

### Product detail

```http
GET /api/products/{id}
```

Cần trả:

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "galleryImages": [],
  "variants": [
    {
      "variantId": 1,
      "variantName": "Mặc định",
      "price": 45000,
      "isDefault": true,
      "status": "AVAILABLE"
    }
  ]
}
```

---

## Việc cần làm

### Frontend

- [ ] `MenuPage.vue`: hiển thị `basePrice` hoặc giá variant mặc định.
- [ ] `ProductDetailPage.vue`: load `variants`.
- [ ] `ProductDetailPage.vue`: cho khách chọn variant.
- [ ] `ProductDetailPage.vue`: đổi giá theo variant.
- [ ] `ProductDetailPage.vue`: add cart gửi `productId`, `variantId`, `quantity`.
- [ ] `CartPage.vue`: hiển thị `variantName`.
- [ ] `CartPage.vue`: bỏ phụ thuộc `optionData`.
- [ ] `stores/cart.js`: lưu cart item theo variant.

### Backend phối hợp

- [ ] Product detail phải trả variants.
- [ ] Cart API phải nhận `variantId`.
- [ ] Backend validate variant thuộc product.

---

## Checklist test

- [ ] Menu hiển thị giá đúng.
- [ ] Product detail hiển thị variant.
- [ ] Chọn variant đổi giá đúng.
- [ ] Add cart đúng variant.
- [ ] Cart hiển thị đúng variant.
- [ ] Không còn lỗi option cũ.

---

## Phụ thuộc

- **Admin**: cần ProductVariant CRUD.
- **Common**: cần format tiền, constants.
- **User**: Checkout dùng cart item mới.
