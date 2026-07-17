# Module Guest

**Nhánh git:** `module/guest`
**Người phụ trách:** Người 2

---

## Trạng thái

**Backend Product + Cart đã xong hoàn toàn.** Cần sửa frontend.

---

## API Product

### GET /api/products

```json
[
  {
    "productId": 1,
    "name": "Classic Beef Burger",
    "description": "Burger bò cổ điển...",
    "basePrice": 45000,
    "price": 45000,
    "defaultVariant": { "variantId": 1, "variantName": "Mặc định", "price": 45000 },
    "imageUrl": "",
    "categoryId": 1,
    "categoryName": "Burger"
  }
]
```

### GET /api/products/{id}

```json
{
  "productId": 1,
  "name": "Classic Beef Burger",
  "basePrice": 45000,
  "price": 45000,
  "defaultVariant": { "variantId": 1, "variantName": "Mặc định", "price": 45000 },
  "variants": [
    { "variantId": 1, "variantName": "Mặc định", "price": 45000, "isDefault": true, "status": "AVAILABLE" },
    { "variantId": 2, "variantName": "Size L", "price": 55000, "isDefault": false, "status": "AVAILABLE" }
  ],
  "galleryImages": ["https://..."]
}
```

## API Cart (đã thay đổi)

### POST /api/cart

**Request** (mới — không gửi `unitPrice`, không gửi `optionData`):

```json
{ "productId": 1, "variantId": 2, "quantity": 1 }
```

**Response:** `{ "success": true, "message": "Added to cart" }`

### GET /api/cart

**Response** (mới — có `variantId`, `variantName`):

```json
{
  "cartId": 1,
  "items": [
    {
      "cartItemId": 1,
      "productId": 1,
      "variantId": 2,
      "productName": "Classic Beef Burger",
      "variantName": "Size L",
      "imageUrl": "",
      "quantity": 1,
      "unitPrice": 55000
    }
  ]
}
```

---

## File cần sửa

| File | Việc |
|---|---|
| `frontend/src/stores/cart.js` | **Quan trọng nhất.** Sửa addItem, mapping, bỏ optionData |
| `frontend/src/stores/product.js` | Mapping theo response mới |
| `frontend/src/views/guest/ProductDetailPage.vue` | Chọn variant, add cart |
| `frontend/src/views/guest/CartPage.vue` | Hiển thị variantName |
| `frontend/src/views/guest/MenuPage.vue` | Hiển thị basePrice |

---

## AI Prompt 1 — stores/cart.js

```
Sửa file `frontend/src/stores/cart.js`.

Backend Cart API đã đổi:
- addItem() chỉ nhận `productId`, `variantId`, `quantity`. Backend tự lấy giá từ ProductVariant.
- Cart API trả: `{ cartItemId, productId, variantId, productName, variantName, imageUrl, quantity, unitPrice }`
- Bỏ hoàn toàn `optionData`, `optionKey`, `unitPrice` trong request

Yêu cầu:
1. addItem(productId, variantId, quantity):
   - Nếu user logged in: gọi `cartApi.addItem({ productId, variantId, quantity })`
   - Nếu không: gọi `addLocalItem(productId, variantId, quantity)`
2. addLocalItem(productId, variantId, quantity):
   - Tìm product trong productStore
   - Tìm variant tương ứng
   - Lưu: `{ productId, variantId, name: product.name, variantName: variant.variantName, price: variant.price, image: product.image, quantity }`
   - Key để kiểm tra trùng: `productId + '_' + variantId`
3. fetchCart():
   - Map API response → item: `{ cartItemId, productId, variantId, name: productName, variantName, price: unitPrice, image: imageUrl, quantity }`
4. subtotal: `items.value.reduce((sum, i) => sum + i.price * i.quantity, 0)`
5. updateQuantity(productId, variantId): tìm item với key `productId + '_' + variantId`
6. migrateToUser(): addItem theo cặp (productId, variantId, quantity)
```

---

## AI Prompt 2 — ProductDetailPage.vue

```
Sửa `frontend/src/views/guest/ProductDetailPage.vue`.

API product detail trả:
```
{
  "productId": 1, "name": "Classic Beef Burger",
  "basePrice": 45000,
  "variants": [
    { "variantId": 1, "variantName": "Mặc định", "price": 45000, "isDefault": true },
    { "variantId": 2, "variantName": "Size L", "price": 55000, "isDefault": false }
  ],
  "galleryImages": []
}
```

Yêu cầu:
1. Hiển thị danh sách variant dạng radio button
2. Mặc định chọn variant có isDefault = true
3. Giá hiển thị thay đổi theo variant được chọn (giá mới = variant.price)
4. Nút "Thêm vào giỏ" gọi `cartStore.addItem(productId, selectedVariantId, quantity)`
5. Hiển thị gallery images (mảng string URLs)
6. Không còn optionData / extraPrice

import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useCartStore } from '@/stores/cart'
import { useProductStore } from '@/stores/product'
```

---

## AI Prompt 3 — CartPage.vue

```
Sửa `frontend/src/views/guest/CartPage.vue`.

Cart item mới:
```
{
  cartItemId: 1,
  productId: 1,
  variantId: 2,
  name: "Classic Beef Burger",
  variantName: "Size L",
  price: 55000,
  image: "",
  quantity: 1
}
```

Yêu cầu:
1. Mỗi dòng hiển thị: ảnh, tên product, variantName, đơn giá, số lượng, tổng
2. variantName hiển thị dạng: "Size L" hoặc "Mặc định"
3. Bỏ hoàn toàn optionData / optionName / extraPrice
4. Tổng tiền = price * quantity
```

---

## Checklist test

- [ ] MenuPage hiển thị `basePrice` / `defaultVariant.price`
- [ ] ProductDetailPage load variants
- [ ] Chọn variant → giá đổi đúng
- [ ] Add cart → gửi variantId
- [ ] Cart hiển thị variantName
- [ ] Cart tổng tiền đúng
- [ ] Không còn lỗi optionData
