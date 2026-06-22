# FRONTEND — FastGuy Vue

## 1. Cấu trúc thư mục

```
Frontend/src/
├── api/                  # Gọi API backend
│   ├── client.js         # Axios instance + interceptor (token, 401)
│   ├── index.js          # Export tất cả api module
│   ├── auth.js           # login, register
│   ├── product.js        # products, categories
│   ├── cart.js           # cart operations
│   ├── order.js          # orders, checkout
│   ├── deliveryZone.js   # delivery zones
│   ├── staff.js          # staff endpoints
│   ├── shipper.js        # shipper endpoints
│   ├── admin.js          # admin CRUD endpoints
│   └── user.js           # user profile
│
├── assets/styles/
│   ├── variables.css     # CSS variables (--primary, --border, --radius...)
│   └── global.css        # Global styles (btn, table, card, modal...)
│
├── components/
│   ├── common/           # Dùng chung cho nhiều role
│   ├── guest/            # Guest layout components
│   ├── staff/            # Staff-specific
│   └── admin/            # Admin-specific (stat card, chart...)
│
├── composables/          # Shared logic (useAuth.js)
├── layouts/              # 5 layouts: Guest, User, Staff, Shipper, Admin
├── router/index.js       # Vue Router config + role-based guard
├── stores/               # 7 Pinia stores
├── utils/
│   ├── constants.js      # ROLES, ORDER_STATUS, PAYMENT_METHOD...
│   └── format.js         # formatPrice, formatDate...
└── views/                # ~25 pages, grouped by role
```

## 2. Luồng dữ liệu

```
User click → Component (views/*.vue)
                ↓ gọi store action
             Pinia Store (stores/*.js)
                ↓ gọi api function
             Api Module (api/*.js)
                ↓ axios
             Backend API
                ↓ response JSON
             Api Module → trả data
                ↓
             Store → cập nhật state (ref/computed)
                ↓
             Component → re-render (template)
```

### Ví dụ cụ thể — Load danh sách sản phẩm

```vue
<!-- views/guest/HomePage.vue -->
<script setup>
import { useProductStore } from '@/stores/product'
const productStore = useProductStore()
onMounted(() => productStore.init())
// productStore.allProducts → đã có data, render trong template
</script>
```

```js
// stores/product.js
export const useProductStore = defineStore('product', () => {
  const allProducts = ref([])
  async function init() {
    const data = await productApi.getAll()    // gọi api
    allProducts.value = data                  // lưu vào store
  }
  return { allProducts, init }
})
```

```js
// api/product.js
export async function getAll() {
  return client.get('/products')  // axios trả về data luôn (nhờ interceptor)
}
```

## 3. Cách tạo 1 page mới

### Step 1: Tạo file `.vue`

```vue
<script setup>
import { ref, onMounted } from 'vue'
import { useProductStore } from '@/stores/product'

const store = useProductStore()

// State cục bộ của page (nếu cần)
const search = ref('')

onMounted(() => {
  store.fetchSomething()  // hoặc gọi API trực tiếp nếu không qua store
})
</script>

<template>
  <div class="my-page">
    <h1>Tiêu đề</h1>
    <div v-if="store.loading">Đang tải...</div>
    <div v-else>{{ store.data }}</div>
  </div>
</template>

<style scoped>
.my-page { padding: 24px 0; }
</style>
```

### Step 2: Thêm route

Trong `router/index.js`:

```js
{
  path: '/my-page',
  name: 'MyPage',
  component: () => import('@/views/guest/MyPage.vue'),
  meta: { guest: true },
}
```

Chọn `meta.role` phù hợp:
- `guest: true` — ai cũng xem được
- `requiresAuth: true, role: ROLES.USER` — chỉ user
- `requiresAuth: true, role: ROLES.STAFF` — chỉ staff
- `requiresAuth: true, role: ROLES.ADMIN` — chỉ admin

### Step 3: Gọi API

- Nếu API đã có trong `api/` → import và dùng trực tiếp
- Nếu chưa có → thêm function vào file API tương ứng

## 4. Bảng mapping Backend → Frontend

Các tên property được giữ **giống hệt** backend để dễ tra cứu.

| Backend (Java) | Frontend store | File |
|---|---|---|
| `User.fullName` | `user.fullName` | auth.js |
| `User.email` | `user.email` | auth.js |
| `User.phone` | `user.phone` | auth.js |
| `User.avatarUrl` | `user.avatarUrl` | auth.js |
| `User.role` | `user.role` | auth.js |
| `Product.id` | `product.id` | product.js |
| `Product.name` | `product.name` | product.js |
| `Product.price` | `product.price` | product.js |
| `Product.description` | `product.description` | product.js |
| `Product.imageUrl` | `product.image` | product.js |
| `Product.inStock` | `product.inStock` | product.js |
| `Product.discountPrice` | `product.discountPrice` | product.js |
| `Order.status` | `order.status` | order.js |
| `Order.totalPrice` | `order.totalPrice` | order.js |

> **Quy tắc:** Frontend store giữ nguyên tên field từ backend response.
> Không tự ý đổi tên (ví dụ: `fullName` → `name`, `avatarUrl` → `avatar`).

## 5. Vue DevTools + Pinia DevTools

1. Cài extension **Vue DevTools** cho Chrome
2. Mở DevTools (F12) → tab **Vue**
3. Chọn tab **Pinia** → xem state real-time
4. Click vào store name → thấy từng `ref`, `computed`, `action`

## 6. Lỗi thường gặp

| Lỗi | Nguyên nhân | Fix |
|---|---|---|
| `401 Unauthorized` | Token hết hạn | Tự động redirect `/login` (interceptor client.js) |
| `Cannot read properties of null` | Store chưa fetch xong | Check `v-if="store.data"` hoặc `computed` |
| `404 khi route` | Quên thêm route | Kiểm tra `router/index.js` |
| Dữ liệu không re-render | Dùng biến thường thay vì `ref` | Dùng `ref()` hoặc `reactive()` |
| API trả về lỗi CORS | Backend thiếu header | Check `setCors()` trong servlet |
