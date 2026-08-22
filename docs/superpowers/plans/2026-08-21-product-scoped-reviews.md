# Kế hoạch triển khai đánh giá theo sản phẩm trong từng đơn hàng

> **Cho agent triển khai:** Bắt buộc dùng `superpowers:subagent-driven-development` hoặc `superpowers:executing-plans`, thực hiện tuần tự từng task và giữ checkbox làm trạng thái. Không commit/push trong kế hoạch này.

**Mục tiêu:** Chuyển review từ cấp đơn sang bộ `(USER, order, product)`, cung cấp aggregate/public pagination, tích hợp ProductCard/ProductDetail/OrderDetail an toàn.

**Kiến trúc:** Migration 050 đưa `product_id` vào `Review` và bảo vệ bằng FK/unique/index. OpenAPI 3.1 được sửa trước implementation. Java giữ flow `ReviewServlet → ReviewService → ReviewDAO → Review/Product/Orders/OrderItem`; `ProductServlet` dùng batch review aggregate. Vue dùng API review riêng cho pagination và trạng thái review theo từng product.

**Tech stack:** SQL Server/T-SQL, Java 17, Jakarta Servlet 6.1, JPA/Hibernate 6.6, Jackson, JUnit 5, OpenAPI 3.1/Redocly, Vue 3, Pinia, Axios, Node test runner, Vite, Playwright.

## Ràng buộc toàn cục

- Chỉ triển khai sau gate runtime read-only xác nhận chính xác `DuckJo/FastGuyDB`; không đoán schema/request/response.
- Bằng chứng baseline: 17 review cũ đều `DELIVERED`; 16 review có đúng một `product_id` phân biệt để backfill; 1 review mơ hồ phải xóa.
- Trước khi tạo 050, kiểm tra nhánh tích hợp để tránh va chạm migration chưa merge. Va chạm thì dừng, phối hợp số mới; không ghi đè.
- Không thực thi migration trên retained/production DB trong lane này. Mọi thực thi retained cần phê duyệt riêng.
- Không chạy `database/init.sql` trên retained data. Task 1 phải đồng bộ canonical fresh-install sources `database/init.sql` và `database/DB_FastGuy.sql`; chỉ migration 050 dùng cho retained lane sau phê duyệt riêng.
- OpenAPI là source of truth; đổi contract trước backend/frontend.
- Một review cho mỗi `(user_id, order_id, product_id)`, độc lập variant và dòng `OrderItem` trùng.
- Chỉ owner của order `DELIVERED`, với product có trong `OrderItem`, được tạo review.
- Rating là integer 1..5; comment optional, trim, max 1000; không edit/delete.
- `homepageConsent` mặc định false; frontend bỏ hoặc gửi false. Không re-enable homepage/admin featured UI.
- Giữ nguyên mọi thay đổi UI-only chưa commit. Không reset/checkout/restore. Support/Combo/phí dịch vụ tiếp tục ẩn; giữ chữ `Kích cỡ`.
- Public review không lộ `userId`, `orderId`, avatar, consent, featured, `updatedAt`.
- Không dependency mới, không refactor ngoài phạm vi, không placeholder/TODO, không commit/push.
- Không tuyên bố production-ready; kết quả chỉ chứng minh source và disposable/local test environment.

## Bản đồ file

### Tạo

- `database/migrations/050_product_scoped_reviews.sql`: migration retained-data-safe, backfill xác định, xóa ambiguous/null, FK/NOT NULL/unique/index/history guard.
- `database/migrations/050_validate.sql`: validator catalog/data/idempotency postcondition.
- `Backend/FastGuy-FastFoodSite/src/test/java/entity/ReviewProductMappingTest.java`: policy JPA mapping `product_id`.
- `Backend/FastGuy-FastFoodSite/src/test/java/dao/ReviewDAOProductScopeTest.java`: DAO source/behavior contract cho ownership, membership, unique, aggregate/pagination.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/ReviewProductScopeServiceTest.java`: service validation, serializer, aggregate.
- `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ReviewProductScopeServletTest.java`: auth, routing, request validation, public allowlist/status.
- `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ProductReviewSummarySerializerTest.java`: product summary và batch policy.
- `frontend/test/product-review-contract.test.js`: OpenAPI/API/mapper contract.
- `frontend/tests/product-review-ui.test.mjs`: ProductCard/ProductDetail/OrderDetail source policy và trạng thái UI.
- `frontend/tests/e2e/product-reviews.spec.js`: luồng desktop/mobile thật.

### Sửa

- `database/migrations/RUNBOOK.md`: thêm 050 sau 049, validator/rerun, explicit approval.
- `openapi/fastguy.yaml`: review paths/schemas và public product summary fields.
- `Backend/FastGuy-FastFoodSite/src/main/java/entity/Review.java`: quan hệ Product bắt buộc.
- `Backend/FastGuy-FastFoodSite/src/main/java/dao/ReviewDAO.java`: triple create/read, public page, aggregate batch.
- `Backend/FastGuy-FastFoodSite/src/main/java/service/ReviewService.java`: validation và serializer phân tách customer/public/admin.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ReviewServlet.java`: contract POST/order/public product.
- `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ProductServlet.java`: `averageRating/reviewCount`, batch aggregate.
- `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ReviewServletConsentTest.java`: chữ ký create mới, consent false/omitted.
- `Backend/FastGuy-FastFoodSite/src/test/java/service/ReviewFeaturedMutationTest.java`: giữ featured backend dormant tương thích product mapping.
- `frontend/src/api/review.js`: `getByProduct` và payload product-scoped.
- `frontend/src/utils/productMapper.js`: map `averageRating/reviewCount` chính xác.
- `frontend/src/components/common/ProductCard.vue`: badge góc ảnh và accessible label có count.
- `frontend/src/views/guest/ProductDetailPage.vue`: summary/distribution/list/pagination endpoint riêng.
- `frontend/src/views/user/OrderDetailPage.vue`: dedupe theo product, form/trạng thái riêng.

---

### Task 1: Gate runtime read-only và chốt migration 050

**Files:**
- Inspect: `database/init.sql:428-444`
- Inspect: `database/DB_FastGuy.sql:451-467`
- Inspect: `database/migrations/048_homepage_merchandising.sql:34-47`
- Inspect: `database/migrations/049_category_images.sql:1-30`
- Inspect: `database/migrations/049_validate.sql:1-14`
- Create: `database/migrations/050_product_scoped_reviews.sql`
- Create: `database/migrations/050_validate.sql`
- Modify: `database/migrations/RUNBOOK.md:3-11`
- Modify: `database/init.sql:428-444`
- Modify: `database/DB_FastGuy.sql:451-467`

**Interfaces:**
- Consumes: SQL Server catalog của `DuckJo/FastGuyDB`, `SchemaMigrationHistory` có `049_category_images`.
- Produces: `Review.product_id int NOT NULL`, FK đến `Product(product_id)`, unique triple, index product/date, migration ID `050_product_scoped_reviews`.

- [ ] **Bước 1: Chạy gate read-only đúng target**

Dùng kết nối read-only đã cấu hình; chạy các truy vấn không ghi sau:

```sql
SELECT @@SERVERNAME AS server_name, DB_NAME() AS database_name;
SELECT c.name, TYPE_NAME(c.user_type_id) AS type_name, c.max_length, c.is_nullable
FROM sys.columns c
WHERE c.object_id = OBJECT_ID(N'dbo.Review')
ORDER BY c.column_id;
SELECT fk.name, OBJECT_NAME(fk.referenced_object_id) AS referenced_table, fk.is_disabled, fk.is_not_trusted
FROM sys.foreign_keys fk
WHERE fk.parent_object_id = OBJECT_ID(N'dbo.Review');
SELECT cc.name, cc.definition, cc.is_disabled, cc.is_not_trusted
FROM sys.check_constraints cc
WHERE cc.parent_object_id = OBJECT_ID(N'dbo.Review');
SELECT i.name, i.is_unique, i.has_filter, i.filter_definition, ic.key_ordinal, c.name AS column_name, ic.is_descending_key
FROM sys.indexes i
JOIN sys.index_columns ic ON ic.object_id = i.object_id AND ic.index_id = i.index_id
JOIN sys.columns c ON c.object_id = ic.object_id AND c.column_id = ic.column_id
WHERE i.object_id = OBJECT_ID(N'dbo.Review')
ORDER BY i.index_id, ic.key_ordinal, ic.index_column_id;
WITH DistinctProducts AS (
  SELECT r.review_id, COUNT(DISTINCT oi.product_id) AS distinct_products,
         MIN(oi.product_id) AS only_product_id
  FROM dbo.Review r
  JOIN dbo.Orders o ON o.order_id = r.order_id
  LEFT JOIN dbo.OrderItem oi ON oi.order_id = r.order_id
  GROUP BY r.review_id
)
SELECT
  (SELECT COUNT(*) FROM dbo.Review) AS total_reviews,
  (SELECT COUNT(*) FROM dbo.Review r JOIN dbo.Orders o ON o.order_id = r.order_id WHERE o.order_status = N'DELIVERED') AS delivered_reviews,
  SUM(CASE WHEN distinct_products = 1 THEN 1 ELSE 0 END) AS exact_reviews,
  SUM(CASE WHEN distinct_products <> 1 THEN 1 ELSE 0 END) AS ambiguous_reviews
FROM DistinctProducts;
```

Expected: `DuckJo`, `FastGuyDB`, `17`, `17`, `16`, `1`. Bất kỳ sai lệch nào: dừng tại source analysis; không tạo migration dựa trên giả định.

- [ ] **Bước 2: RED bằng disposable preflight**

Restore backup fresh vào DB disposable/local theo runbook, chạy migration đến 049, sau đó chạy `050_validate.sql` trước migration.

Expected: FAIL rõ ràng vì thiếu migration history `050_product_scoped_reviews` hoặc thiếu `product_id`; không chạm retained DB.

- [ ] **Bước 3: Viết migration tối thiểu trong một transaction**

`050_product_scoped_reviews.sql` phải thực hiện đúng thứ tự:

```sql
USE FastGuyDB;
GO
SET NOCOUNT ON;
SET XACT_ABORT ON;
GO
IF OBJECT_ID(N'dbo.SchemaMigrationHistory', N'U') IS NULL THROW 51500, 'Run 000_preflight_history.sql first.', 1;
IF NOT EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '049_category_images') THROW 51501, 'Run 049_category_images.sql first.', 1;
IF EXISTS (SELECT 1 FROM dbo.SchemaMigrationHistory WHERE migration_id = '050_product_scoped_reviews')
    PRINT '050_product_scoped_reviews already applied.';
ELSE
BEGIN
  BEGIN TRY
    BEGIN TRANSACTION;
    ALTER TABLE dbo.Review ADD product_id int NULL;
    WITH ExactProduct AS (
      SELECT r.review_id, MIN(oi.product_id) AS product_id
      FROM dbo.Review r
      JOIN dbo.OrderItem oi ON oi.order_id = r.order_id
      GROUP BY r.review_id
      HAVING COUNT(DISTINCT oi.product_id) = 1
    )
    UPDATE r SET product_id = ep.product_id
    FROM dbo.Review r JOIN ExactProduct ep ON ep.review_id = r.review_id;
    DELETE FROM dbo.Review WHERE product_id IS NULL;
    ALTER TABLE dbo.Review ADD CONSTRAINT FK_Review_Product FOREIGN KEY (product_id) REFERENCES dbo.Product(product_id);
    ALTER TABLE dbo.Review ALTER COLUMN product_id int NOT NULL;
    ALTER TABLE dbo.Review DROP CONSTRAINT UQ_Review_UserOrder;
    ALTER TABLE dbo.Review ADD CONSTRAINT UQ_Review_UserOrderProduct UNIQUE (user_id, order_id, product_id);
    CREATE INDEX IX_Review_ProductCreatedAt ON dbo.Review(product_id, created_at DESC, review_id DESC);
    INSERT dbo.SchemaMigrationHistory(migration_id, details)
    VALUES ('050_product_scoped_reviews', N'Scoped reviews to purchased products and removed ambiguous legacy rows');
    COMMIT TRANSACTION;
  END TRY
  BEGIN CATCH
    IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
    THROW;
  END CATCH;
END;
GO
```

Bổ sung shape guards trước từng DDL để script chỉ chấp nhận đúng trạng thái 049 hoặc trạng thái 050 hoàn chỉnh; không “sửa hộ” schema lạ.

- [ ] **Bước 4: Viết validator đầy đủ**

`050_validate.sql` phải THROW nếu thiếu/sai: migration history; `product_id int NOT NULL`; `FK_Review_Product` enabled/trusted đúng cột; `UQ_Review_UserOrderProduct` đúng ba key theo thứ tự; `IX_Review_ProductCreatedAt(product_id ASC, created_at DESC, review_id DESC)`; checks cũ enabled/trusted; orphan/null/duplicate triple. In row counts và `PRINT '050 product scoped reviews validation passed.'` khi pass.

- [ ] **Bước 5: GREEN trên disposable DB và rerun**

Chạy lần lượt: migration 050, validator, migration 050 lần hai, validator lần hai.

Expected: lần đầu migrate `16` review, xóa đúng `1`; validator pass; lần hai chỉ in `already applied`; validator vẫn pass. Chụp output làm bằng chứng local/disposable, không gọi đây là production validation.

- [ ] **Bước 6: Cập nhật runbook**

Thêm `050_product_scoped_reviews.sql`, `050_validate.sql` sau 049; ghi restore fresh disposable/apply/validate/rerun; retained execution cần backup verified, stop writes, approval riêng; giữ câu cấm `init.sql` trên retained.

### Task 2: Contract OpenAPI product-scoped review

**Files:**
- Modify: `openapi/fastguy.yaml:304-353`
- Modify: `openapi/fastguy.yaml:1233-1395`
- Modify: `openapi/fastguy.yaml:1709-1790`
- Modify: `frontend/test/openapi-contract.test.js`
- Create: `frontend/test/product-review-contract.test.js`

**Interfaces:**
- Consumes: DB invariant Task 1.
- Produces: `ReviewCreateRequest`, `ReviewByOrderResponse`, `ProductReviewPageResponse`, `PublicReviewItem`, product `averageRating/reviewCount` làm contract duy nhất cho backend/frontend.

- [ ] **Bước 1: Viết RED contract test**

Test parse `openapi/fastguy.yaml`, assert:

```js
assert.deepEqual(create.required.sort(), ['orderId', 'productId', 'rating']);
assert.equal(create.properties.rating.type, 'integer');
assert.equal(create.properties.rating.minimum, 1);
assert.equal(create.properties.rating.maximum, 5);
assert.equal(create.properties.comment.maxLength, 1000);
assert.equal(productPath.get.security.length, 0);
assert.equal(size.maximum, 50);
assert.deepEqual(Object.keys(publicItem.properties).sort(), ['comment', 'createdAt', 'productId', 'rating', 'reviewId', 'userName']);
```

Assert public item không có `userId/orderId/avatarUrl/homepageConsent/featured/updatedAt`; product summary required có `averageRating/reviewCount`.

- [ ] **Bước 2: Chạy RED**

Run:

```powershell
Set-Location frontend
node --test test/product-review-contract.test.js
```

Expected: FAIL vì chưa có `productId`, public product path và schemas mới.

- [ ] **Bước 3: Sửa contract tối thiểu**

- `POST /reviews`: required `orderId`, `productId`, `rating`; optional nullable `comment` max 1000; optional `homepageConsent` default false để tương thích.
- `GET /reviews/order/{orderId}`: data `{ orderId, reviews[] }`, mỗi item customer-safe có `reviewId,productId,rating,comment,createdAt`.
- Thêm public `GET /reviews/product/{productId}` với `security: []`, query `page` default/min 1, `size` default 10/min 1/max 50; 200/400/404.
- Response data required `items,total,page,size,averageRating,reviewCount,ratingDistribution`; `averageRating` type number; distribution required key chuỗi `1..5` integer >= 0.
- `PublicReviewItem.additionalProperties: false` và allowlist sáu field.
- Public product list/detail schemas thêm required `averageRating` number và `reviewCount` integer >= 0.

- [ ] **Bước 4: GREEN contract**

Run:

```powershell
Set-Location frontend
node --test test/openapi-contract.test.js test/product-review-contract.test.js
npm run contract:lint
```

Expected: tất cả PASS, Redocly không error.

### Task 3: JPA mapping và DAO write/read theo triple

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/entity/Review.java:16-77`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/ReviewDAO.java:17-114`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/entity/ReviewProductMappingTest.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/dao/ReviewDAOProductScopeTest.java`

**Interfaces:**
- Consumes: `Review.product_id NOT NULL`, contract required `orderId/productId/rating`.
- Produces: `findByUserOrder(int userId, int orderId)` trả list; `save(int userId, int orderId, int productId, int rating, String comment, boolean homepageConsent)`; `Review.getProduct()/setProduct(Product)`.

- [ ] **Bước 1: RED mapping test**

Dùng reflection assert field `product` có `@ManyToOne`, `@JoinColumn(name="product_id", nullable=false)` và type `Product`; assert getter/setter tồn tại.

- [ ] **Bước 2: RED DAO policy/behavior test**

Test source/DAO seam theo convention hiện tại cho các case:

- order owner + `DELIVERED` + product trong `OrderItem`: persist;
- non-owner;
- chưa `DELIVERED`;
- product không mua;
- duplicate triple;
- cùng order, product khác: được phép;
- hai dòng/variant cùng product: vẫn chỉ một triple.

Expected lỗi nghiệp vụ ổn định; duplicate là `IllegalStateException` để servlet map 409.

- [ ] **Bước 3: Chạy RED**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewProductMappingTest,ReviewDAOProductScopeTest test
```

Expected: FAIL vì entity/chữ ký/query triple chưa tồn tại.

- [ ] **Bước 4: GREEN entity/DAO tối thiểu**

- Thêm `Product product` vào `Review`.
- Đổi find order thành list sắp `productId` và fetch product khi serializer cần.
- Trong transaction `save`, lock `Orders`; xác nhận owner/status; query `COUNT(OrderItem)` theo `orderId + productId`; query duplicate theo cả ba ID; set Product reference; persist.
- Không dùng variant trong quyền hoặc unique.
- Giữ featured methods hiện có dormant; không mở UI.
- Map vi phạm unique race về cùng conflict domain message, không nuốt lỗi DB khác.

- [ ] **Bước 5: Chạy GREEN**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewProductMappingTest,ReviewDAOProductScopeTest test
```

Expected: PASS.

### Task 4: DAO/service aggregate, pagination và serializer allowlist

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/dao/ReviewDAO.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/service/ReviewService.java:15-84`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/service/ReviewProductScopeServiceTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/service/ReviewFeaturedMutationTest.java`

**Interfaces:**
- Consumes: DAO triple Task 3.
- Produces:
  - `Map<Integer, ReviewDAO.ProductReviewSummary> summariesByProductIds(List<Integer> productIds)`
  - `Map<String,Object> getByOrderId(int userId, int orderId)`
  - `Map<String,Object> getByProductId(int productId, int page, int size)`
  - `Map<String,Object> create(int userId, int orderId, int productId, int rating, String comment, boolean homepageConsent)`

- [ ] **Bước 1: RED service tests**

Test chính xác:

- integer rating 1..5; comment trim/empty-null/max 1000;
- create luôn truyền consent false khi omitted;
- order response `{orderId,reviews[]}` theo product;
- public item keys đúng allowlist;
- average `4.15` làm tròn HALF_UP thành JSON number `4.2`;
- zero review trả `0.0/0` và distribution đủ 1..5 bằng 0;
- page 1/size 10; reject page 0, size 0, size 51;
- items sort `createdAt DESC, reviewId DESC`;
- total là tổng product, không phải số item trang.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewProductScopeServiceTest,ReviewFeaturedMutationTest test
```

Expected: FAIL vì service vẫn trả review gộp toàn đơn và serializer public chưa tách.

- [ ] **Bước 3: GREEN DAO read models**

Trong `ReviewDAO` thêm record typed và ba query bounded:

- batch summary `GROUP BY product_id` cho list serializer;
- public page projection join user, offset `(page - 1) * size`, max size;
- một aggregate query cho count/average/distribution 1..5 hoặc projection nhóm rating, hoàn thiện khóa thiếu trong service.

Không query avatar, consent, featured cho public endpoint. Không loop query theo product ID.

- [ ] **Bước 4: GREEN service serializers**

Tách serializer theo audience:

- customer order item: `reviewId,productId,rating,comment,createdAt`;
- public item: thêm `userName`, không thêm private/admin fields;
- admin serializer giữ behavior featured dormant hiện có.

Dùng `BigDecimal.setScale(1, RoundingMode.HALF_UP)` rồi JSON number; không format thành string.

- [ ] **Bước 5: Chạy GREEN**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewProductScopeServiceTest,ReviewFeaturedMutationTest test
```

Expected: PASS.

### Task 5: ReviewServlet đúng contract auth/public

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ReviewServlet.java:22-86`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ReviewServletConsentTest.java`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ReviewProductScopeServletTest.java`

**Interfaces:**
- Consumes: service signatures Task 4, OpenAPI Task 2.
- Produces: routes POST `/reviews`, authenticated `/reviews/order/{orderId}`, public `/reviews/product/{productId}`.

- [ ] **Bước 1: RED servlet tests**

Test request/response:

- POST bắt buộc numeric integer `orderId/productId/rating`; comment string/null; consent boolean/null;
- decimal JSON number bị reject;
- omitted consent truyền false;
- duplicate map 409;
- invalid/not-purchased/not-delivered map 400;
- missing token map 401;
- order GET non-owner 404;
- product GET không token vẫn 200;
- product path ID invalid, page/size invalid map 400; product missing map 404;
- public JSON không chứa forbidden fields.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewServletConsentTest,ReviewProductScopeServletTest test
```

Expected: FAIL vì servlet lấy auth trước mọi GET, thiếu product route và productId POST.

- [ ] **Bước 3: GREEN route/validation**

- Parse route trước auth: `/product/` public; `/order/` yêu cầu auth.
- Validate query defaults `page=1`, `size=10`, max 50.
- POST validate ba integer required trước service.
- Dùng contract status 400/401/404/409; lỗi bất ngờ 500 không lộ exception.
- Giữ `homepageConsent` optional/default false; không yêu cầu consent.

- [ ] **Bước 4: Chạy GREEN**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ReviewServletConsentTest,ReviewProductScopeServletTest test
```

Expected: PASS.

### Task 6: Product serializer aggregate batch, không N+1

**Files:**
- Modify: `Backend/FastGuy-FastFoodSite/src/main/java/servlet/ProductServlet.java:197-247`
- Create: `Backend/FastGuy-FastFoodSite/src/test/java/servlet/ProductReviewSummarySerializerTest.java`
- Modify: `Backend/FastGuy-FastFoodSite/src/test/java/dao/ProductDAOPolicyTest.java`

**Interfaces:**
- Consumes: `ReviewDAO.summariesByProductIds(List<Integer>)` Task 4.
- Produces: mọi public product map có numeric `averageRating` và integer `reviewCount`; detail chỉ summary.

- [ ] **Bước 1: RED serializer/N+1 tests**

Assert list ba product gọi batch summary đúng một lần với ba IDs; không gọi summary từng product. Assert reviewed product `4.2/16`, unreviewed `0.0/0`; detail có summary, không có `reviews/items/ratingDistribution`.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ProductReviewSummarySerializerTest,ProductDAOPolicyTest test
```

Expected: FAIL vì serializer thiếu fields và ReviewDAO dependency.

- [ ] **Bước 3: GREEN batch serializer**

Inject/khởi tạo `ReviewDAO` theo convention servlet hiện có. Trong `toMaps`, lấy `ids`, gọi `summariesByProductIds(ids)` một lần, truyền summary vào overload `toMap`. Với single detail/related, dùng cùng API batch trên tập ID đang serialize; không gọi aggregate trong stream/map callback.

- [ ] **Bước 4: Chạy GREEN và backend suite**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn -Dtest=ProductReviewSummarySerializerTest,ProductDAOPolicyTest test
mvn test
```

Expected: PASS toàn bộ. Nếu suite lỗi, dừng; không chuyển frontend khi backend chưa xanh.

### Task 7: Frontend API, mapper và ProductCard badge

**Files:**
- Modify: `frontend/src/api/review.js:1-10`
- Modify: `frontend/src/utils/productMapper.js`
- Modify: `frontend/src/components/common/ProductCard.vue:1-76`
- Modify: `frontend/test/product-review-contract.test.js`
- Create: `frontend/tests/product-review-ui.test.mjs`

**Interfaces:**
- Consumes: OpenAPI Task 2; product summary Task 6.
- Produces: `reviewApi.getByProduct(productId, params)`, mapper fields, card badge/ARIA.

- [ ] **Bước 1: RED frontend tests**

Assert:

- API tạo URL `/reviews/product/45?page=1&size=10` qua Axios params;
- `mapProduct({ averageRating: 0, reviewCount: 0 })` giữ `0/0`;
- `mapProduct({ averageRating: 4.24, reviewCount: 16 })` giữ numeric data để UI render `4.2`;
- ProductCard source có `★ 4.2/5` logic, `Chưa có đánh giá`, accessible label gồm count;
- badge nằm trong `.product-image`, không thay stock/favorite/add actions.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location frontend
node --test test/product-review-contract.test.js tests/product-review-ui.test.mjs
```

Expected: FAIL vì API/badge chưa có.

- [ ] **Bước 3: GREEN API/mapper/card**

- Thêm `getByProduct(productId, params = { page: 1, size: 10 })` dùng `client.get(..., { params })`.
- Mapper dùng `Number.isFinite(Number(value))`, clamp display hợp lệ; `reviewCount` không âm.
- Computed card label: count > 0 thì `★ ${average.toFixed(1)}/5`, không thì `Chưa có đánh giá`.
- `aria-label`: `Đánh giá ${average.toFixed(1)} trên 5 từ ${count} lượt` hoặc `Chưa có đánh giá, 0 lượt`.
- CSS badge góc ảnh, không che `.badges`, `.stock-badge`; mobile hit targets hiện có không đổi.

- [ ] **Bước 4: Chạy GREEN**

```powershell
Set-Location frontend
node --test test/product-review-contract.test.js tests/product-review-ui.test.mjs
```

Expected: PASS.

### Task 8: ProductDetail summary, distribution, list và pagination

**Files:**
- Modify: `frontend/src/views/guest/ProductDetailPage.vue:1-278`
- Modify: `frontend/tests/product-review-ui.test.mjs`

**Interfaces:**
- Consumes: `reviewApi.getByProduct(productId,{page,size})` Task 7; `ProductReviewPageResponse` Task 2.
- Produces: product review section với state độc lập `reviewPage`, `reviewLoading`, `reviewError`, `reviewRequest`.

- [ ] **Bước 1: RED UI state tests**

Test helper/source contract cho:

- initial page 1, size 10;
- route product đổi reset page và tải request mới;
- stale response bị bỏ qua bằng request sequence;
- Prev disabled page 1; Next disabled khi `page * size >= total`;
- empty state `Chưa có đánh giá`;
- error có nút `Thử lại`;
- render đủ năm distribution rows và public fields, không avatar/order/consent/featured.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location frontend
node --test tests/product-review-ui.test.mjs
```

Expected: FAIL vì ProductDetail chưa tải review.

- [ ] **Bước 3: GREEN ProductDetail tối thiểu**

- Import `reviewApi`; tải product và review song song an toàn theo route ID.
- Summary ưu tiên response review để distribution/count đồng bộ; product summary là fallback khi review request lỗi.
- Render average một chữ số/count, năm hàng 5 xuống 1, list tên/rating/comment/date, empty/loading/error.
- Pagination gọi endpoint riêng; không nhét list vào product payload/store.
- Giữ nguyên purchase flow, `Kích cỡ`, Support/Combo ẩn và responsive hiện hữu.

- [ ] **Bước 4: Chạy GREEN**

```powershell
Set-Location frontend
node --test tests/product-review-ui.test.mjs
```

Expected: PASS.

### Task 9: Delivered OrderDetail dedupe và form độc lập theo product

**Files:**
- Modify: `frontend/src/views/user/OrderDetailPage.vue:1-270`
- Modify: `frontend/tests/product-review-ui.test.mjs`

**Interfaces:**
- Consumes: `reviewApi.getByOrder(orderId)`, `reviewApi.create({orderId,productId,rating,comment})`.
- Produces: `reviewProducts` dedupe theo `productId`; state map theo product cho form/pending/error/review.

- [ ] **Bước 1: RED dedupe/state tests**

Với items `[product 45 variant A, product 45 variant B, product 46]`, assert chỉ hai review entries `45/46`. Test:

- chỉ hiện section khi `DELIVERED`;
- GET order map review theo product;
- submit product 45 không đổi form/pending/error product 46;
- payload không có consent hoặc có `homepageConsent:false`;
- rating required 1..5, comment max 1000/đếm ký tự;
- thành công đổi riêng product thành đã đánh giá; 409 reload trạng thái order;
- text/source policy vẫn ẩn Support/Combo/phí dịch vụ và giữ `Kích cỡ` ở bề mặt liên quan.

- [ ] **Bước 2: Chạy RED**

```powershell
Set-Location frontend
node --test tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
```

Expected: FAIL review UI mới; policy UI-only cũ vẫn PASS.

- [ ] **Bước 3: GREEN OrderDetail tối thiểu**

- Import `reviewApi`.
- Dedupe sau map order bằng `Map(productId, firstItem)`; bỏ item thiếu productId khỏi review section nhưng không khỏi order display.
- Sau order `DELIVERED`, tải review state; dùng object/map key product ID.
- Form semantic có label rating/comment, button 44px mobile, `aria-live` cho pending/error/success.
- POST payload `{ orderId: order.id, productId, rating, comment: trimmed || null }`; không consent checkbox.
- Không thay reorder/cancel/payment/polling logic.

- [ ] **Bước 4: Chạy GREEN và frontend gates**

```powershell
Set-Location frontend
node --test tests/product-review-ui.test.mjs tests/ui-scope-hide-policy.test.mjs
npm test
npm run contract:lint
npm run build
```

Expected: tất cả PASS. Nếu bất kỳ lệnh lỗi, dừng và báo lệnh/error; không chuyển E2E.

### Task 10: Integration disposable, Playwright desktop/mobile và self-review

**Files:**
- Create: `frontend/tests/e2e/product-reviews.spec.js`
- Verify only: `database/migrations/050_product_scoped_reviews.sql`
- Verify only: `database/migrations/050_validate.sql`
- Verify only: mọi file Tasks 2-9

**Interfaces:**
- Consumes: feature hoàn chỉnh Tasks 1-9 và môi trường local/disposable.
- Produces: bằng chứng migration/API/UI end-to-end; không tạo claim production.

- [ ] **Bước 1: Viết RED E2E**

Fixture/local seed disposable phải có:

- một product không review;
- một product có nhiều rating để xác minh average/distribution/pagination;
- một order owner `DELIVERED` có product A lặp ở hai variant/dòng và product B;
- một order non-owner và một order chưa giao.

E2E assert card badge/ARIA, ProductDetail summary/distribution/page change, OrderDetail chỉ hai form, POST A thành công và B còn độc lập, duplicate/non-owner/not-delivered/not-purchased trả đúng 409/404/400/400 qua API test context.

- [ ] **Bước 2: Chạy RED E2E trước khi dùng implementation hoàn chỉnh**

```powershell
Set-Location frontend
npx playwright test tests/e2e/product-reviews.spec.js --project=chromium
```

Expected trước khi wiring/local fixture hoàn tất: FAIL tại review assertion cụ thể, không phải lỗi selector mơ hồ hoặc server không chạy.

- [ ] **Bước 3: Chạy lại migration integration trên restore fresh**

Restore DB disposable fresh; chạy toàn chuỗi đến 049; chạy 050/validator/rerun/validator. Query postcondition:

```sql
SELECT COUNT(*) AS null_products FROM dbo.Review WHERE product_id IS NULL;
SELECT user_id, order_id, product_id, COUNT(*) AS duplicates
FROM dbo.Review
GROUP BY user_id, order_id, product_id
HAVING COUNT(*) > 1;
```

Expected: `0`, không dòng duplicate; validator pass hai lần. Không dùng `init.sql` trên retained; không chạy production.

- [ ] **Bước 4: GREEN Playwright desktop/mobile và network/console**

Chạy desktop viewport và mobile viewport cấu hình trong test; capture console errors và response chính:

```powershell
Set-Location frontend
npx playwright test tests/e2e/product-reviews.spec.js --project=chromium
```

Expected: PASS; không `console.error`; GET `/reviews/product/{id}` và GET `/reviews/order/{id}` là 200; POST hợp lệ 200; payload có `orderId/productId/rating`, không consent true.

- [ ] **Bước 5: Chạy toàn bộ gate cuối**

```powershell
Set-Location Backend/FastGuy-FastFoodSite
mvn test
Set-Location ../../frontend
npm test
npm run contract:lint
npm run build
npx playwright test tests/e2e/product-reviews.spec.js --project=chromium
```

Expected: tất cả exit code 0. Không bỏ qua test fail/flaky; sửa nguyên nhân rồi chạy lại đúng gate.

- [ ] **Bước 6: Self-review diff và phạm vi**

Kiểm tra:

```powershell
git status --short
git diff --check
git diff -- database/migrations/050_product_scoped_reviews.sql database/migrations/050_validate.sql database/migrations/RUNBOOK.md openapi/fastguy.yaml Backend/FastGuy-FastFoodSite/src/main/java Backend/FastGuy-FastFoodSite/src/test/java frontend/src frontend/test frontend/tests
```

Checklist bắt buộc:

- Phủ đủ 14 tiêu chí chấp nhận trong design.
- Không `TBD`, `TODO`, placeholder, dependency mới, edit/delete review.
- Public serializer đúng allowlist.
- Không query aggregate review trong product loop.
- Không ghi đè UI-only uncommitted changes.
- Featured/homepage/admin UI vẫn dormant; Support/Combo/phí dịch vụ vẫn ẩn; `Kích cỡ` còn nguyên.
- Không file ngoài danh sách dự kiến, không commit/push.
- Báo rõ môi trường disposable/local đã test; không dùng từ ngữ khẳng định production-ready.

## Tiêu chí hoàn thành kế hoạch

- Mỗi task đã chạy RED đúng nguyên nhân rồi GREEN.
- Migration 050/validator/rerun pass trên restore fresh disposable; runtime retained chỉ read-only.
- OpenAPI lint, `mvn test`, `npm test`, `npm run build`, Playwright desktop/mobile đều exit 0.
- API security/ownership/purchase/delivery/duplicate/aggregate/pagination và public allowlist có test.
- UI card/detail/order có test; console sạch; request chính thành công.
- Không production claim, không commit/push, không mất thay đổi UI-only hiện có.
