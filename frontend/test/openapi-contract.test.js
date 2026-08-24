import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

const contractUrl = new URL('../../openapi/fastguy.yaml', import.meta.url);

function schemaSection(contract, name, nextName) {
  return contract.slice(contract.indexOf(`    ${name}:`), contract.indexOf(`    ${nextName}:`));
}

test('OpenAPI contracts current-shift shipper listing and assignment', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  assert.match(contract, /^  \/staff\/orders\/shippers:$/m);
  assert.match(contract, /^      operationId: listAvailableShippers$/m);
  assert.match(contract, /^  \/staff\/orders\/\{orderId\}\/assign-shipper:$/m);
  assert.match(contract, /^      operationId: assignOrderShipper$/m);
  for (const schema of ['AvailableShipper', 'AvailableShipperListResponse', 'AssignShipperRequest', 'AssignShipperResponse']) {
    assert.match(contract, new RegExp(`^    ${schema}:$`, 'm'));
  }
  assert.match(contract, /^        '422':$/m);
});

test('OpenAPI contract covers the categories response consumed by Vue', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  assert.match(contract, /^openapi: 3\.1\.0$/m);
  assert.match(contract, /^  \/categories:$/m);
  assert.match(contract, /^      operationId: listCategories$/m);
  for (const field of ['categoryId', 'name', 'description', 'imageUrl', 'sortOrder', 'productCount']) {
    assert.match(contract, new RegExp(`^        ${field}:$`, 'm'));
  }
  assert.doesNotMatch(contract, /\$ref:\s*['"]?https?:\/\//);
});

test('OpenAPI contracts shared profiles and admin user avatar CRUD', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  assert.match(contract, /^  \/auth\/profile:$/m);
  assert.match(contract, /^      operationId: getProfile$/m);
  assert.match(contract, /^      operationId: updateProfile$/m);
  assert.match(contract, /^  \/admin\/users:$/m);
  assert.match(contract, /^      operationId: listAdminUsers$/m);
  assert.match(contract, /^      operationId: createAdminUser$/m);
  assert.match(contract, /^  \/admin\/users\/\{userId\}:$/m);
  assert.match(contract, /^      operationId: getAdminUser$/m);
  assert.match(contract, /^      operationId: updateAdminUser$/m);
  assert.match(contract, /^      operationId: deleteAdminUser$/m);
  for (const schema of ['UserProfile', 'UserProfileUpdateRequest', 'AdminUser', 'AdminUserCreateRequest', 'AdminUserUpdateRequest']) {
    assert.match(contract, new RegExp(`^    ${schema}:$`, 'm'));
  }
  const avatarSchemas = [
    ['UserProfile', 'UserProfileUpdateRequest'],
    ['UserProfileUpdateRequest', 'UserProfileResponse'],
    ['AdminUser', 'AdminUserCreateRequest'],
    ['AdminUserCreateRequest', 'AdminUserUpdateRequest'],
    ['AdminUserUpdateRequest', 'AdminUserResponse'],
  ];
  for (const [schemaName, nextSchema] of avatarSchemas) {
    const section = schemaSection(contract, schemaName, nextSchema);
    assert.match(section, /^        avatarUrl:$/m);
    assert.match(section, /avatarUrl:\s+type: \[string, 'null'\]\s+format: uri/s);
    assert.ok(section.includes("pattern: '^https://'"));
  }
});

test('OpenAPI contracts operational dashboard and reconcilable financial reports', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  assert.match(contract, /^  \/admin\/dashboard:$/m);
  assert.match(contract, /^      operationId: getAdminDashboard$/m);
  assert.match(contract, /^  \/admin\/reports\/full:$/m);
  assert.match(contract, /^      operationId: getAdminFullReport$/m);
  const dashboard = schemaSection(contract, 'AdminDashboardData', 'AdminDashboardResponse');
  for (const field of ['customerCount', 'activeProductCount', 'ordersByStatus', 'operationalOrderCount', 'operationalCompletedCount', 'completionRate']) assert.match(dashboard, new RegExp(`^        ${field}:`, 'm'));
  const report = schemaSection(contract, 'AdminFullReportData', 'AdminFullReportResponse');
  for (const field of ['itemRevenue', 'shippingRevenue', 'serviceFeeRevenue', 'discountTotal', 'grossRevenue', 'refundTotal', 'netCashRevenue', 'operationalOrderCount', 'operationalCompletedCount', 'completionRate', 'revenueByHour', 'performanceByWeekday', 'refundTrend', 'exceptionReasons', 'monthlyFinancialTrend']) assert.match(report, new RegExp(`^        ${field}:`, 'm'));
});

test('frontend categories client uses the contractized endpoint', async () => {
  const source = await readFile(new URL('../src/api/product.js', import.meta.url), 'utf8');

  assert.match(source, /getCategories\(\)\s*{\s*return client\.get\('\/categories'\);/s);
});

test('OpenAPI contract defines the public homepage envelope without private review fields', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  assert.match(contract, /^  \/homepage:$/m);
  assert.match(contract, /^      operationId: getHomepage$/m);
  const homepagePath = contract.slice(contract.indexOf('  /homepage:'), contract.indexOf('  /admin/products/{productId}:'));
  assert.match(homepagePath, /^        '500':\s+\$ref: '#\/components\/responses\/InternalServerError'$/m);
  for (const schema of ['HomepageResponse', 'HomepageData', 'ProductSummary', 'FeaturedReview']) {
    assert.match(contract, new RegExp(`^    ${schema}:$`, 'm'));
  }
  assert.match(contract, /bestSellers:\s+type: array\s+maxItems: 6/s);
  assert.match(contract, /featuredReviews:\s+type: array\s+maxItems: 3/s);
  assert.doesNotMatch(contract, /^    (ProductCombo|OccasionCombo):$/m);
  assert.doesNotMatch(contract, /^  \/(support|notifications)(\/|:)/m);
  for (const field of ['isNew', 'spiceLevel', 'bestSeller', 'defaultVariant', 'variants', 'modifierGroups']) {
    assert.match(contract, new RegExp(`^        ${field}:$`, 'm'));
  }
  const productSummary = schemaSection(contract, 'ProductSummary', 'ProductVariantSummary');
  assert.match(productSummary, /required: \[[^\]]*defaultVariant[^\]]*variants[^\]]*modifierGroups[^\]]*\]/);
  for (const field of ['productId', 'name', 'price', 'imageUrl', 'categoryId', 'categoryName', 'inStock', 'isAvailableNow', 'isNew', 'spiceLevel', 'bestSeller']) {
    assert.match(productSummary, new RegExp(`required: \\[[^\\]]*${field}[^\\]]*\\]`));
  }
  const featuredReview = schemaSection(contract, 'FeaturedReview', 'AdminProductUpdateRequest');
  for (const field of ['reviewId', 'rating', 'comment', 'userName', 'avatarUrl', 'createdAt']) {
    assert.match(featuredReview, new RegExp(`^        ${field}:$`, 'm'));
  }
  assert.match(featuredReview, /avatarUrl:\s+type: \[string, 'null'\]/s);
  assert.match(featuredReview, /createdAt:\s+type: string\s+pattern: '\^\\d\{4\}-\\d\{2\}-\\d\{2\}T\\d\{2\}:\\d\{2\}:\\d\{2\}/s);
  assert.doesNotMatch(featuredReview, /format: date-time/);
  assert.doesNotMatch(featuredReview, /^        (orderId|userId|email|phone|contact):$/m);
  assert.doesNotMatch(contract, /\$ref:\s*['"]?https?:\/\//);
});

test('OpenAPI contract extends existing admin product read and mutation paths', async () => {
  const contract = await readFile(contractUrl, 'utf8');

  assert.match(contract, /^  \/admin\/products\/\{productId\}:$/m);
  assert.match(contract, /^      operationId: getAdminProduct$/m);
  assert.match(contract, /^      operationId: updateAdminProduct$/m);
  const productPath = contract.slice(contract.indexOf('  /admin/products/{productId}:'), contract.indexOf('  /admin/orders/{orderId}:'));
  assert.match(productPath, /\$ref: '#\/components\/schemas\/AdminProductDetailResponse'/);
  assert.doesNotMatch(contract, /^  \/admin\/products\/\{productId\}\/combo/m);

  assert.match(contract, /^  \/admin\/orders\/\{orderId\}\/featured-review:$/m);
  assert.match(contract, /^      operationId: updateFeaturedReview$/m);
  const featuredPath = contract.slice(contract.indexOf('  /admin/orders/{orderId}/featured-review:'), contract.indexOf('components:'));
  assert.match(featuredPath, /^        '422':$/m);
  assert.match(featuredPath, /description: Review is not eligible for homepage publication/);

  const productRequest = schemaSection(contract, 'AdminProductUpdateRequest', 'ReviewCreateRequest');
  const productFields = ['categoryId', 'name', 'description', 'basePrice', 'status', 'availableFrom', 'availableTo', 'imageUrl', 'galleryImages', 'isNew', 'spiceLevel'];
  for (const field of productFields) assert.match(productRequest, new RegExp(`^        ${field}:$`, 'm'));
  assert.doesNotMatch(productRequest, /^        bestSeller:$/m);
  assert.equal([...productRequest.matchAll(/^        (\w+):$/gm)].map((match) => match[1]).sort().join(','), productFields.sort().join(','));

  const productDetail = schemaSection(contract, 'AdminProductDetail', 'AdminProductDetailResponse');
  for (const field of ['productId', 'name', 'categoryId', 'categoryName', 'basePrice', 'imageUrl', 'description', 'status', 'availableFrom', 'availableTo', 'isNew', 'spiceLevel', 'galleryImages', 'variants', 'modifierGroups', 'discountPrice', 'rating', 'reviewCount', 'inStock', 'featured']) assert.match(productDetail, new RegExp(`^        ${field}:$`, 'm'));
  assert.doesNotMatch(contract, /^    AdminCombo/m);
});


test('OpenAPI contracts the exact admin order-detail serializer and review fields', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  assert.match(contract, /^  \/admin\/products:$/m);
  assert.match(contract, /^      operationId: listAdminProducts$/m);
  assert.match(contract, /^      operationId: createAdminProduct$/m);
  const productsPath = contract.slice(contract.indexOf('  /admin/products:'), contract.indexOf('  /admin/products/{productId}:'));
  assert.match(productsPath, /\$ref: '#\/components\/schemas\/AdminProductListResponse'/);
  assert.doesNotMatch(contract, /^  \/admin\/products\/\{productId\}\/combo/m);
  assert.doesNotMatch(contract, /^    AdminCombo/m);
  assert.match(contract, /^  \/admin\/orders\/\{orderId\}:$/m);
  assert.match(contract, /^      operationId: getAdminOrderDetail$/m);
  const review = schemaSection(contract, 'AdminOrderReview', 'AdminOrderItem');
  const detail = schemaSection(contract, 'AdminOrderDetail', 'AdminOrderDetailResponse');
  const detailFields = ['orderId', 'orderCode', 'status', 'customerName', 'customerPhone', 'customerAddress', 'totalAmount', 'shippingFee', 'serviceFee', 'finalAmount', 'discountAmount', 'paymentMethod', 'paymentStatus', 'deliveryNote', 'cancelledBy', 'failureNote', 'failureReason', 'deliveryFailureCode', 'deliveryAttemptCount', 'deliveryAttemptLimit', 'deliveryFailedAt', 'retryScheduledAt', 'returnedToStoreAt', 'refundStatus', 'refundAmount', 'refundNote', 'refundedAt', 'createdAt', 'confirmedAt', 'cancelledAt', 'deliveredAt', 'staffName', 'shipperName', 'internalNote', 'review', 'payment', 'items', 'statusHistory'];
  for (const field of detailFields) assert.match(detail, new RegExp(`^        ${field}:$`, 'm'));
  assert.match(detail, /additionalProperties: false/);
  assert.deepEqual(detail.match(/required: \[([^\]]+)\]/)[1].split(', '), detailFields);
  for (const field of ['reviewId', 'rating', 'comment', 'createdAt', 'updatedAt', 'userName', 'avatarUrl', 'orderId', 'featured', 'homepageConsent', 'featureEligible', 'featureIneligibilityReason']) assert.match(review, new RegExp(`^        ${field}:$`, 'm'));
  assert.match(review, /additionalProperties: false/);
  assert.match(review, /comment:\s+type: \[string, 'null'\]/s);
  assert.match(review, /createdAt:\s+type: \[string, 'null'\]\s+pattern: '\^\\d\{4\}-\\d\{2\}-\\d\{2\}T\\d\{2\}:\\d\{2\}:\\d\{2\}'/s);
  assert.match(review, /updatedAt:\s+type: \[string, 'null'\]\s+pattern: '\^\\d\{4\}-\\d\{2\}-\\d\{2\}T\\d\{2\}:\\d\{2\}:\\d\{2\}'/s);
  assert.match(review, /userName:\s+type: \[string, 'null'\]/s);
  assert.match(review, /avatarUrl:\s+type: \[string, 'null'\]/s);
  assert.match(review, /orderId:\s+type: \[integer, 'null'\]/s);
  assert.match(review, /homepageConsent:\s+type: boolean/s);
  assert.match(review, /featureEligible:\s+type: boolean/s);
  assert.match(review, /featureIneligibilityReason:\s+type: \[string, 'null'\]\s+enum: \[MISSING_HOMEPAGE_CONSENT, MISSING_COMMENT, INACTIVE_USER, MISSING_USER_NAME, MISSING_CREATED_AT, null\]/s);
});

test('OpenAPI contracts review consent without exposing consent on the public homepage', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  assert.match(contract, /^  \/reviews\/order\/\{orderId\}:$/m);
  assert.match(contract, /^      operationId: getReviewByOrder$/m);
  assert.match(contract, /\$ref: '#\/components\/schemas\/ReviewByOrderResponse'/);
  const reviewByOrder = schemaSection(contract, 'ReviewByOrderData', 'ReviewByOrderResponse');
  assert.match(reviewByOrder, /required: \[orderId, reviews\]/);
  assert.match(reviewByOrder, /reviews:\s+type: array\s+items:\s+\$ref: '#\/components\/schemas\/CustomerReviewItem'/s);
  assert.match(contract, /^  \/reviews:$/m);
  assert.match(contract, /^      operationId: createReview$/m);
  const request = schemaSection(contract, 'ReviewCreateRequest', 'CustomerReviewItem');
  const review = schemaSection(contract, 'CustomerReviewItem', 'PublicReviewItem');
  assert.match(request, /homepageConsent:\s+type: boolean\s+default: false/s);
  assert.doesNotMatch(review, /homepageConsent/);
  assert.doesNotMatch(schemaSection(contract, 'FeaturedReview', 'AdminVariantDetail'), /homepageConsent/);
});

test('OpenAPI requires optimistic recipe and inventory settings versions', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  const recipeRequest = schemaSection(contract, 'RecipeRequest', 'InventorySettings');
  const settings = schemaSection(contract, 'InventorySettings', 'InventorySettingsRequest');
  const settingsRequest = schemaSection(contract, 'InventorySettingsRequest', 'InventorySettingsResponse');
  const variant = schemaSection(contract, 'AdminVariantDetail', 'AdminModifierOption');

  assert.match(recipeRequest, /additionalProperties: false/);
  assert.match(recipeRequest, /required: \[yieldQuantity, active, items, expectedUpdatedAt\]/);
  assert.match(recipeRequest, /^        expectedUpdatedAt: \{ type: \[string, 'null'\] \}$/m);
  assert.match(settings, /required: \[variantId, inventoryMode, updatedAt\]/);
  assert.match(settings, /^        updatedAt: \{ type: string \}$/m);
  assert.match(settingsRequest, /additionalProperties: false/);
  assert.match(settingsRequest, /required: \[inventoryMode, expectedUpdatedAt\]/);
  assert.match(settingsRequest, /^        expectedUpdatedAt: \{ type: string \}$/m);
  assert.match(variant, /required: \[[^\]]*updatedAt[^\]]*\]/);
  assert.match(variant, /^        updatedAt: \{ type: string \}$/m);
});
