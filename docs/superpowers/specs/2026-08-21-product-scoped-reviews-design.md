# Thiết kế đánh giá theo sản phẩm trong từng đơn hàng

**Ngày:** 2026-08-21
**Trạng thái:** Thiết kế cố định, sẵn sàng lập kế hoạch triển khai
**Phạm vi:** Database, OpenAPI, Java Servlet/JPA, Vue 3, kiểm thử. Không triển khai trong tài liệu này.

## CAPABILITY

Khách hàng sở hữu đơn `DELIVERED` có thể đánh giá riêng từng sản phẩm thực sự xuất hiện trong `OrderItem` của đơn. Mỗi bộ `(user_id, order_id, product_id)` có tối đa một đánh giá; biến thể hoặc nhiều dòng trùng sản phẩm không tạo thêm quyền đánh giá. Người xem công khai có thể đọc đánh giá theo sản phẩm với phân trang và số liệu tổng hợp; danh sách/chi tiết sản phẩm hiển thị điểm trung bình và số lượt đánh giá mà không gây N+1.

## Nguồn chuẩn và bằng chứng

- Runtime read-only bắt buộc trỏ chính xác `DuckJo/FastGuyDB`; không chấp nhận server/database khác.
- Bằng chứng runtime đã chốt: 17 đánh giá cũ, tất cả thuộc đơn `DELIVERED`; 16 đơn chứa đúng một `product_id` phân biệt nên ánh xạ xác định; 1 đánh giá mơ hồ phải xóa.
- Source schema hiện tại trong `database/init.sql` và `database/DB_FastGuy.sql`: `Review` có FK `FK_Review_User`, `FK_Review_Order`; unique `UQ_Review_UserOrder(user_id, order_id)`; check `CK_Review_Rating`; check `CK_Review_FeaturedConsent`; index `IX_Review_Order`; index lọc `IX_Review_FeaturedCreatedAt`.
- Migration mới nhất trên nhánh đích: `049_category_images.sql` cùng `049_validate.sql`. Migration tính năng dùng `database/migrations/050_product_scoped_reviews.sql` và `database/migrations/050_validate.sql`.
- Trước khi tạo file 050, phải kiểm tra lại migration mới nhất trên nhánh tích hợp để tránh va chạm với nhánh chưa merge; nếu số 050 đã bị dùng, dừng và phối hợp đánh số, không tự ghi đè.
- OpenAPI 3.1 tại `openapi/fastguy.yaml` là nguồn chuẩn request/response trước khi sửa backend hoặc frontend.
- Các thay đổi UI-only chưa commit trong worktree là dữ liệu người dùng, phải giữ nguyên. Không reset, checkout, restore hoặc ghi đè chúng.

## CONSTRAINTS

### Quy tắc nghiệp vụ cố định

1. Chủ thể đánh giá là `USER`; quyền phát sinh từ quyền sở hữu đơn, trạng thái `DELIVERED`, và sản phẩm có mặt trong ít nhất một `OrderItem` của đơn.
2. Một đánh giá duy nhất cho mỗi `(user_id, order_id, product_id)`.
3. Các dòng `OrderItem` cùng `product_id`, dù khác `variant_id`, chỉ tạo một mục đánh giá.
4. `rating` bắt buộc là số nguyên từ 1 đến 5. `comment` tùy chọn, trim; chuỗi rỗng thành `null`; tối đa 1000 ký tự.
5. Không sửa, không xóa đánh giá trong phạm vi sản phẩm này.
6. Tạo đánh giá sản phẩm không yêu cầu consent nổi bật. `POST /reviews` chỉ nhận `homepageConsent` khi cần tương thích contract cũ; frontend gửi `false` hoặc bỏ field. Backend mặc định `false`.
7. `is_featured`, `homepage_consent`, luồng featured/homepage hiện có được giữ dormant; không xóa backend/schema, không mở lại UI homepage/admin featured review.
8. Support, Combo, phí dịch vụ vẫn ẩn. Nhãn biến thể người dùng vẫn là `Kích cỡ`.

### Invariant dữ liệu

- `Review.product_id` sau migration là `NOT NULL`, FK đến `Product(product_id)`.
- Unique cũ `(user_id, order_id)` được thay bằng `(user_id, order_id, product_id)`.
- `CK_Review_Rating`, `CK_Review_FeaturedConsent`, FK user/order, `IX_Review_Order`, `IX_Review_FeaturedCreatedAt` vẫn hợp lệ và trusted.
- Index đọc công khai theo sản phẩm/ngày hỗ trợ `product_id, created_at DESC` với tie-break `review_id DESC` trong query.
- Backfill chỉ cập nhật review có đơn chứa đúng một `product_id` phân biệt. Mọi dòng còn `product_id IS NULL`, gồm một review mơ hồ đã xác nhận, bị xóa trước `NOT NULL`.
- Migration có transaction, `XACT_ABORT`, precondition migration 049, schema-shape guards, migration-history guard và rerun an toàn.

### Trust boundary

- Servlet xác thực JWT cho POST và GET theo đơn.
- GET công khai theo sản phẩm không yêu cầu JWT, nhưng vẫn validate `productId`, `page`, `size`.
- Service/DAO phải kiểm tra đồng thời owner, `DELIVERED`, membership trong `OrderItem`; không tin `productId` từ client.
- Unique constraint là lớp bảo vệ cuối cho race condition; conflict trùng trả HTTP 409.
- Public serializer dùng allowlist; không được rò `userId`, `orderId`, avatar, consent, featured, `updatedAt`.

## IMPLEMENTATION CONTRACT

### Actor và bề mặt

| Actor | Bề mặt | Khả năng |
|---|---|---|
| Khách hàng đăng nhập | Chi tiết đơn đã giao | Xem trạng thái và gửi form riêng cho từng sản phẩm phân biệt |
| Người xem công khai | ProductCard | Xem badge điểm hoặc trạng thái chưa có đánh giá |
| Người xem công khai | ProductDetail | Xem tổng quan, phân phối sao, danh sách phân trang |
| Backend | Review API | Enforce quyền tạo, đọc theo đơn, đọc công khai theo sản phẩm |
| Database operator | Migration 050 | Backfill 16 review xác định, xóa 1 review mơ hồ, áp constraint/index |

### Data model

`entity.Review` thêm quan hệ `@ManyToOne Product product` ánh xạ `product_id`, `nullable = false`. DAO tạo review trong một transaction có lock đơn; xác nhận owner/status và `EXISTS OrderItem` theo `orderId/productId`; kiểm tra trùng triple trước persist, đồng thời chuyển lỗi unique race thành conflict.

Aggregate dùng DTO/record nhỏ trong `ReviewDAO`, không thêm dependency:

- `ProductReviewSummary(productId, averageRating, reviewCount)` cho batch product serializer.
- `PublicReview(reviewId, productId, rating, comment, userName, createdAt)` cho danh sách công khai.
- `ProductReviewPage(items, total, page, size, averageRating, reviewCount, ratingDistribution)` ở service/serializer.

`averageRating` là JSON number làm tròn một chữ số thập phân; không có review trả `0.0`, `reviewCount = 0`, phân phối 1..5 đều bằng 0. Phân phối luôn chứa đủ khóa `1`, `2`, `3`, `4`, `5`.

### API contract

#### `POST /reviews`

Bảo vệ bằng bearer JWT.

Request:

```json
{
  "orderId": 123,
  "productId": 45,
  "rating": 5,
  "comment": "Ngon"
}
```

- Required: `orderId`, `productId`, `rating`.
- Optional: `comment`; `homepageConsent` chỉ được giữ optional để tương thích, mặc định `false`, frontend bỏ hoặc gửi `false`.
- `200`: public-safe created review hoặc schema customer review đã contract hóa.
- `400`: sai kiểu/range/comment quá dài, product không thuộc đơn, đơn chưa giao.
- `401`: thiếu/sai token.
- `409`: triple đã tồn tại.

#### `GET /reviews/order/{orderId}`

Bảo vệ bằng bearer JWT. Chỉ owner đọc được; đơn không thuộc user trả `404`. Response là danh sách theo sản phẩm, đủ để frontend suy ra từng trạng thái:

```json
{
  "status": "success",
  "data": {
    "orderId": 123,
    "reviews": [
      {
        "reviewId": 17,
        "productId": 45,
        "rating": 5,
        "comment": "Ngon",
        "createdAt": "2026-08-21T10:00:00"
      }
    ]
  }
}
```

Danh sách sắp theo `productId`; không trả trạng thái gộp `reviewed` toàn đơn.

#### `GET /reviews/product/{productId}?page=1&size=10`

Public, `security: []`. `page` bắt đầu từ 1; mặc định 1. `size` mặc định 10, min 1, max 50. `productId` dương. Product không tồn tại trả `404`; tham số sai trả `400`.

```json
{
  "status": "success",
  "data": {
    "items": [
      {
        "reviewId": 17,
        "productId": 45,
        "rating": 5,
        "comment": "Ngon",
        "userName": "Nguyễn An",
        "createdAt": "2026-08-21T10:00:00"
      }
    ],
    "total": 16,
    "page": 1,
    "size": 10,
    "averageRating": 4.2,
    "reviewCount": 16,
    "ratingDistribution": {
      "1": 1,
      "2": 0,
      "3": 2,
      "4": 5,
      "5": 8
    }
  }
}
```

Items sắp `createdAt DESC, reviewId DESC`. Public item chỉ có `reviewId`, `productId`, `rating`, `comment`, `userName`, `createdAt`.

### Product serializer

Các response danh sách và chi tiết public của `ProductServlet` thêm `averageRating`, `reviewCount` theo OpenAPI. `toMaps(List<Product>)` lấy aggregate một lần cho toàn bộ ID, cùng mô hình batch hiện dùng cho sold/default variant/flags; cấm query review trên từng product. Chi tiết chỉ cần summary; phân trang review dùng endpoint riêng để tránh phình payload và tách lifecycle tải lại.

### Frontend

#### ProductCard

Badge nằm ở góc ảnh, không che các badge/trạng thái hiện có:

- Có review: `★ 4.2/5`.
- Không có review: `Chưa có đánh giá`.
- Accessible label gồm điểm và count, ví dụ `Đánh giá 4.2 trên 5 từ 16 lượt`; khi chưa có: `Chưa có đánh giá, 0 lượt`.

`productMapper.js` giữ số thực của `averageRating` và số nguyên `reviewCount`, không dùng truthy fallback làm mất `0.0`.

#### ProductDetail

Tải product summary và `reviewApi.getByProduct(productId, { page: 1, size: 10 })`. Hiển thị:

- average/count;
- năm hàng phân phối sao với count;
- danh sách review public;
- loading/error/empty state;
- điều khiển phân trang, giữ `size = 10`, vô hiệu hóa Prev/Next ở biên;
- request cũ không được ghi đè route sản phẩm mới.

Không hiển thị avatar, user ID, order ID, consent, featured. Không thêm review vào payload product detail.

#### OrderDetail của user

Sau khi tải đơn `DELIVERED`, tạo danh sách sản phẩm đánh giá bằng dedupe `productId`; tên/ảnh lấy dòng đầu, biến thể và dòng trùng không tạo form thứ hai. Gọi `GET /reviews/order/{orderId}`, map review theo `productId`. Mỗi sản phẩm có độc lập:

- trạng thái chưa đánh giá và nút mở form;
- rating 1..5 bắt buộc;
- comment tùy chọn, `maxlength=1000` và đếm ký tự;
- pending/error riêng;
- trạng thái đã đánh giá sau POST thành công, không ảnh hưởng form sản phẩm khác.

Payload POST chứa `orderId`, `productId`, `rating`, `comment`; `homepageConsent` bỏ hoặc `false`. Không có checkbox consent. Giữ các thay đổi UI-only hiện hữu: Support/Combo/phí dịch vụ ẩn; `Kích cỡ` không đổi.

## Migration và vận hành an toàn

1. Gate read-only: xác nhận `@@SERVERNAME = DuckJo`, `DB_NAME() = FastGuyDB`; đọc catalog Review/FK/check/index và tái chạy truy vấn phân loại 17/16/1. Sai target hoặc số liệu lệch thì dừng, không viết migration dựa trên suy đoán.
2. Chỉ tạo source `050_product_scoped_reviews.sql` và `050_validate.sql`; không chạy lên retained DB khi chưa có phê duyệt riêng.
3. Trên DB disposable/local mới: restore backup fresh; chạy chuỗi migration đến 049; chạy 050; chạy validator; chạy lại 050 để chứng minh history guard/idempotency; chạy lại validator.
4. Validator kiểm tra migration history, column type/nullability, FK Product trusted, unique triple đúng thứ tự, index product/date đúng thứ tự, checks cũ trusted, không còn null/orphan/duplicate, và row count theo baseline thử nghiệm.
5. `init.sql` không bao giờ chạy trên retained data. Đồng bộ `database/init.sql` và `database/DB_FastGuy.sql` trong Task 1 chỉ cập nhật canonical fresh-install schema; retained lane chỉ dùng migration 050 sau phê duyệt riêng.
6. Production/retained execution cần backup/restore verification, stop writes và xác nhận riêng. Thiết kế/plan/test local không đồng nghĩa sẵn sàng production.

## Error handling

| Trường hợp | HTTP/UI |
|---|---|
| Token thiếu/sai ở POST/order GET | 401 |
| Order không thuộc user | 404 để không lộ tài nguyên |
| Order chưa `DELIVERED` | 400, thông báo nghiệp vụ |
| Product không có trong `OrderItem` | 400 |
| Triple trùng | 409 |
| Rating không nguyên/ngoài 1..5 | 400 |
| Comment quá 1000 | 400 |
| Public product không tồn tại | 404 |
| page/size sai, size > 50 | 400 |
| UI request lỗi | Giữ dữ liệu hợp lệ hiện có; trạng thái lỗi và nút thử lại phù hợp bề mặt |

## Kiểm thử bắt buộc

- JPA mapping: `Review.product`, join column, nullable.
- DAO/service: owner, delivered, membership; duplicate triple; khác variant/dòng trùng vẫn một review; không mua; chưa giao; không phải owner.
- Aggregate: average một chữ số dạng number, count, phân phối đủ 1..5, zero-review, sort/tie-break, pagination page 1, size max 50.
- Servlet: validation kiểu/range; auth/public boundary; status codes; serializer allowlist.
- OpenAPI: request required fields, response schemas, public field exclusion, product summary fields.
- Product serializer: list batch aggregate; test chính sách/chứng minh không có N+1; detail summary-only.
- Vue: mapper, ProductCard label/badge, ProductDetail states/pagination, OrderDetail dedupe và form/status độc lập.
- Full gates: `mvn test`; `npm test`; `npm run contract:lint`; `npm run build`.
- Integration disposable DB: restore fresh, apply/validate/rerun 050.
- Playwright desktop và mobile: product list/card, product detail reviews/pagination, delivered OrderDetail nhiều sản phẩm/dòng trùng; không console error; request GET/POST chính thành công và payload đúng contract.

## Tiêu chí chấp nhận nghiêm ngặt

1. Runtime gate xác nhận đúng `DuckJo/FastGuyDB`, đúng 17 review cũ, 16 ánh xạ xác định, 1 mơ hồ; không thao tác ghi runtime khi chưa phê duyệt.
2. Migration source 050 backfill đúng quy tắc, xóa mọi null/mơ hồ còn lại, tạo FK/NOT NULL/unique/index; validator và rerun pass trên DB disposable fresh.
3. Database không thể lưu hai review cùng triple; vẫn cho phép cùng user/order đánh giá hai product khác nhau.
4. POST từ non-owner, order chưa giao, product không mua đều thất bại; POST hợp lệ thành công; consent mặc định false.
5. Order GET chỉ owner đọc được và trả danh sách review theo `productId`.
6. Product GET public phân trang đúng, `size <= 50`, aggregate chính xác, average là JSON number một chữ số.
7. Public item không chứa `userId`, `orderId`, avatar, consent, featured hoặc field ngoài allowlist.
8. Product list/detail đều có `averageRating/reviewCount`; list dùng batch aggregate, không N+1.
9. ProductCard hiển thị đúng hai trạng thái và accessible label có count.
10. ProductDetail hiển thị summary, phân phối, empty/error/loading, danh sách và pagination từ endpoint riêng.
11. Delivered OrderDetail dedupe theo `productId`; từng sản phẩm có form/pending/error/status độc lập; variant/dòng trùng không nhân form.
12. Support/Combo/phí dịch vụ, homepage featured review và admin featured UI vẫn ẩn; chữ `Kích cỡ` được giữ.
13. OpenAPI lint, backend tests, frontend tests/build, integration disposable, Playwright desktop/mobile đều pass; console không lỗi, API chính thành công.
14. Không có claim production. Triển khai retained/production vẫn bị chặn bởi phê duyệt riêng và runbook.

## NON-GOALS

- Sửa hoặc xóa review bởi customer/admin.
- Re-enable homepage featured review hoặc admin featured controls.
- Bắt customer consent để review sản phẩm.
- Review theo variant, modifier hoặc từng `OrderItem`.
- Đổi Support, Combo, phí dịch vụ hoặc wording `Kích cỡ`.
- Chạy migration trên retained/production database.
- Chạy `database/init.sql` trên retained data; retained migration lane chỉ dùng migration 050 sau phê duyệt riêng.
- Refactor ngoài luồng review/product serializer liên quan.

## OPEN QUESTIONS

Không còn quyết định sản phẩm chặn implementation. Chỉ có gate vận hành: xác nhận số migration chưa va chạm tại thời điểm triển khai và xin phê duyệt riêng trước mọi thao tác retained/production.

## HANDOFF

Sẵn sàng triển khai theo kế hoạch `docs/superpowers/plans/2026-08-21-product-scoped-reviews.md`, bắt buộc theo thứ tự `DATABASE → OpenAPI → Backend → Frontend → E2E`, TDD RED/GREEN và không commit/push tự động.
