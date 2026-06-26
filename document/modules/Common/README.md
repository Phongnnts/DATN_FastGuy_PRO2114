# Module Common

**Nhánh git:** `module/common`
**Người phụ trách:** Người 6

---

## Mục tiêu

1. Tạo GHN backend (ShippingServlet, ShippingService, GhnClient)
2. Cập nhật constants/layout
3. Hỗ trợ các module khác

---

## 1. GHN Backend — Files cần tạo

### Backend files mới

| File | Nội dung |
|---|---|
| `Backend/src/main/java/utils/GhnClient.java` | HTTP client gọi GHN API |
| `Backend/src/main/java/service/ShippingService.java` | Business logic + fallback |
| `Backend/src/main/java/servlet/ShippingServlet.java` | REST API |
| `Backend/src/main/java/utils/AppConfig.java` | Config GHN từ env/file |

### API cần implement

```http
GET  /api/shipping/provinces
GET  /api/shipping/districts?provinceId=202
GET  /api/shipping/wards?districtId=1442
POST /api/shipping/fee
```

### AI Prompt — GhnClient.java

```
Tạo `Backend/src/main/java/utils/GhnClient.java`.

Yêu cầu:
1. Dùng `java.net.http.HttpClient` (Java 17) hoặc `HttpURLConnection`
2. Đọc token từ AppConfig
3. Các method:
   - getProvinces() → List<Map> (provinceId, provinceName)
   - getDistricts(int provinceId) → List<Map> (districtId, districtName)
   - getWards(int districtId) → List<Map> (wardCode, wardName)
   - calculateFee(int toDistrictId, String toWardCode, int weight, int length, int width, int height) → Map (fee, serviceId, expectedDeliveryTime)
4. Host mặc định: https://dev-online-gateway.ghn.vn
5. Parse JSON bằng Jackson hoặc JsonUtil
6. Xử lý lỗi: trả về null hoặc map có error field

Cấu trúc:
```
package utils;
public class GhnClient {
  public GhnClient() {}
  public List<Map<String, Object>> getProvinces() { ... }
  public List<Map<String, Object>> getDistricts(int provinceId) { ... }
  public List<Map<String, Object>> getWards(int districtId) { ... }
  public Map<String, Object> calculateFee(int toDistrictId, String toWardCode, int weight, int length, int width, int height) { ... }
}
```
```

### AI Prompt — ShippingService.java

```
Tạo `Backend/src/main/java/service/ShippingService.java`.

Yêu cầu:
1. Dùng GhnClient gọi GHN
2. Nếu GHN lỗi/timeout → fallback sang DeliveryZone
3. Các method:
   - getProvinces()
   - getDistricts(int provinceId)
   - getWards(int districtId)
   - calculateFee(int toDistrictId, String toWardCode, int weight, int length, int width, int height)
4. Fallback: lấy DeliveryZone gần nhất hoặc mặc định

Struct:
```
package service;
public class ShippingService {
  private GhnClient ghnClient = new GhnClient();
  private DeliveryZoneDAO deliveryZoneDAO = new DeliveryZoneDAO();
  
  public Map<String, Object> calculateFee(...) {
    try {
      Map<String, Object> result = ghnClient.calculateFee(toDistrictId, toWardCode, weight, length, width, height);
      if (result != null) {
        result.put("shippingProvider", "GHN");
        return result;
      }
    } catch (Exception e) { e.printStackTrace(); }
    // fallback
    Map<String, Object> fallback = new HashMap<>();
    fallback.put("shippingProvider", "FALLBACK_ZONE");
    fallback.put("shippingFee", 15000);
    return fallback;
  }
}
```
```

### AI Prompt — ShippingServlet.java

```
Tạo `Backend/src/main/java/servlet/ShippingServlet.java`.

Yêu cầu:
- @WebServlet("/api/shipping/*")
- doGet: provinces, districts, wards
- doPost: calculate fee
- Không cần auth (public)
- Trả về ApiResponse

Endpoints:
1. GET /api/shipping/provinces → list provinces
2. GET /api/shipping/districts?provinceId=202 → list districts
3. GET /api/shipping/wards?districtId=1442 → list wards
4. POST /api/shipping/fee → body: { toDistrictId, toWardCode, weight, length, width, height } → { shippingProvider, shippingFee, ... }
```

### AI Prompt — AppConfig.java

```
Tạo `Backend/src/main/java/utils/AppConfig.java`.

Yêu cầu:
1. Đọc GHN token từ biến môi trường: GHN_TOKEN, GHN_SHOP_ID
2. Nếu không có env, dùng giá trị mặc định dev
3. Cung cấp getter static:
   - getGhnToken()
   - getGhnShopId()
   - getGhnHost()
```

---

## 2. Frontend Common — Files cần kiểm tra

| File | Việc |
|---|---|
| `frontend/src/utils/constants.js` | Thêm Cloudinary, order status, payment status, shipping provider |
| `frontend/src/utils/format.js` | Kiểm tra formatPrice, formatDate |
| `frontend/src/components/common/OrderStatusBadge.vue` | Đủ màu cho PENDING/CONFIRMED/PREPARING/READY/CANCELLED |
| `frontend/src/components/common/OrderTimeline.vue` | Không duplicate status |
| `frontend/src/router/index.js` | Đã bỏ ingredient routes |

---

## AI Prompt — constants.js

```
Cập nhật `frontend/src/utils/constants.js`.

Thêm:
```
export const ROLES = { GUEST: 'GUEST', USER: 'USER', STAFF: 'STAFF', ADMIN: 'ADMIN' };

export const ORDER_STATUS = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Đã xác nhận',
  PREPARING: 'Đang chế biến',
  READY: 'Đã sẵn sàng',
  CANCELLED: 'Đã hủy',
};

export const PAYMENT_STATUS = { UNPAID: 'Chưa thanh toán', PAID: 'Đã thanh toán' };

export const SHIPPING_PROVIDER = { GHN: 'GHN', FALLBACK_ZONE: 'Giao hàng nội bộ' };

export const CLOUDINARY = {
  cloudName: 'ds4dnsj0o',
  uploadPreset: 'upload-fastguy',
  uploadUrl: 'https://api.cloudinary.com/v1_1/ds4dnsj0o/image/upload',
};
```

Kiểm tra các file import constants để xài đúng.
```

---

## 3. Checklist cleanup

- [ ] Backend compile: `mvn clean compile`
- [ ] Frontend build: `npm run build`
- [ ] GHN provinces API hoạt động
- [ ] GHN districts API hoạt động
- [ ] GHN wards API hoạt động
- [ ] GHN fee API hoạt động
- [ ] Fallback DeliveryZone khi GHN lỗi
- [ ] constants.js có đủ Cloudinary, order status
- [ ] Sidebar/layout không còn link Ingredients/LowStock
