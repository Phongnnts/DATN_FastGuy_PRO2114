import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { promisify } from 'node:util';
import test from 'node:test';

const contractUrl = new URL('../../openapi/fastguy.yaml', import.meta.url);
const execFileAsync = promisify(execFile);

function schemaSection(contract, name, nextName) {
  return contract.slice(contract.indexOf(`    ${name}:`), contract.indexOf(`    ${nextName}:`));
}

test('OpenAPI contracts Staff dispatch filters and classifications', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const paths = contract.paths;
  const operation = paths['/staff/orders/dispatch']?.get;
  const filter = operation?.parameters.find((parameter) => parameter.name === 'filter');
  const dispatchOrder = contract.components.schemas.StaffDispatchOrder;
  const dispatchCounts = contract.components.schemas.StaffDispatchCounts;
  const dispatchResponse = contract.components.schemas.StaffDispatchResponse;
  const orderFields = [
    'orderId', 'orderCode', 'userId', 'customerName', 'customerPhone', 'customerAddress', 'status', 'orderStatus', 'itemCount', 'items',
    'totalAmount', 'shippingFee', 'serviceFee', 'discountAmount', 'paymentMethod', 'paymentStatus', 'finalAmount',
    'refundAmount', 'refundedAt', 'shipperId', 'shipperName', 'assignedAt', 'updatedAt', 'endedAt', 'createdAt',
    'deliveryAttemptCount', 'deliveryAttemptLimit', 'deliveryFailureCode', 'failureNote', 'deliveryFailedAt',
    'retryScheduledAt', 'returnedToStoreAt', 'readyAt', 'classification', 'minutesUntilClose',
    'statusEnteredAt', 'expiresAt', 'remainingSeconds', 'timeoutPolicy', 'ownerShiftCode',
  ];

  assert.ok(operation);
  assert.equal(filter.in, 'query');
  assert.equal(filter.required, true);
  assert.deepEqual(filter.schema.enum, ['PRIORITY', 'NEW', 'REVIEW']);
  assert.equal(operation.responses['200'].content['application/json'].schema.$ref, '#/components/schemas/StaffDispatchResponse');
  for (const status of ['400', '401', '403']) {
    assert.equal(operation.responses[status].$ref, `#/components/responses/${status === '400' ? 'BadRequest' : status === '401' ? 'Unauthorized' : 'Forbidden'}`);
  }
  assert.equal(dispatchOrder.additionalProperties, false);
  assert.deepEqual(dispatchOrder.required, orderFields);
  assert.deepEqual(Object.keys(dispatchOrder.properties), orderFields);
  assert.deepEqual(dispatchOrder.properties.classification.enum, ['PRIORITY', 'NEW', 'REVIEW']);
  assert.deepEqual(dispatchOrder.properties.readyAt.type, ['string', 'null']);
  assert.deepEqual(dispatchOrder.properties.minutesUntilClose.type, ['integer', 'null']);
  assert.deepEqual(dispatchCounts.required, ['priority', 'new', 'review']);
  assert.deepEqual(Object.keys(dispatchCounts.properties), ['priority', 'new', 'review']);
  assert.deepEqual(dispatchResponse.required, ['items', 'counts', 'serverTime', 'openTime', 'closeTime']);
  assert.deepEqual(Object.keys(dispatchResponse.properties), ['items', 'counts', 'serverTime', 'openTime', 'closeTime']);
  assert.equal(dispatchResponse.properties.items.items.$ref, '#/components/schemas/StaffDispatchOrder');
  assert.equal(dispatchResponse.properties.counts.$ref, '#/components/schemas/StaffDispatchCounts');
  const refs = [];
  JSON.stringify(contract, (key, value) => {
    if (key === '$ref') refs.push(value);
    return value;
  });
  assert.ok(refs.length > 0);
  assert.ok(refs.every((ref) => ref.startsWith('#/')));
});

test('OpenAPI contracts Staff shift ownership, handover, and checkout conflict', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const list = contract.paths['/staff/orders/handover']?.get;
  const claim = contract.paths['/staff/orders/{orderId}/handover']?.put;
  const ownershipCount = contract.paths['/staff/orders/ownership-count']?.get;
  const checkout = contract.paths['/shifts/{shiftId}/check-out']?.post;
  const item = contract.components.schemas.StaffHandoverOrder;
  const request = contract.components.schemas.ClaimStaffHandoverRequest;

  assert.equal(list.operationId, 'listStaffHandoverOrders');
  assert.equal(claim.operationId, 'claimStaffOrderHandover');
  assert.equal(ownershipCount.operationId, 'getStaffOwnershipCount');
  assert.equal(ownershipCount.responses['200'].content['application/json'].schema.$ref, '#/components/schemas/StaffOwnershipCountResponse');
  assert.deepEqual(contract.components.schemas.StaffOwnershipCountData.required, ['activeOwnershipCount']);
  assert.equal(contract.components.schemas.StaffOwnershipCountData.properties.activeOwnershipCount.minimum, 0);
  assert.equal(checkout.responses['409'].content['application/json'].schema.$ref, '#/components/schemas/ShiftCheckoutConflictResponse');
  assert.deepEqual(contract.components.schemas.ShiftCheckoutConflictData.anyOf, [{ required: ['activeOwnershipCount'] }, { required: ['settlementStatus'] }]);
  assert.deepEqual(item.required, ['orderId', 'orderCode', 'status', 'customerName', 'itemCount', 'waitingSince', 'staffShiftId', 'ownerShiftLabel', 'ownerShiftCode', 'handoverRequired']);
  assert.deepEqual(item.properties.status.enum, ['CONFIRMED', 'PREPARING', 'READY', 'DELIVERY_FAILED']);
  assert.deepEqual(item.properties.staffShiftId.type, ['integer', 'null']);
  assert.deepEqual(item.properties.ownerShiftLabel.type, ['string', 'null']);
  assert.deepEqual(request.required, ['expectedStatus', 'expectedOwnerShiftId']);
  assert.deepEqual(request.properties.expectedOwnerShiftId.type, ['integer', 'null']);
});

test('OpenAPI contracts a multi-person weekly schedule for STAFF and SHIPPER', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const request = contract.components.schemas.AdminShiftWeekSlotRequest;
  const update = contract.components.schemas.AdminShiftWeekUpdateRequest;
  const response = contract.components.schemas.ShiftWeekData;

  assert.deepEqual(request.properties.role.enum, ['STAFF', 'SHIPPER']);
  assert.equal(update.properties.slots.maxItems, 126);
  assert.equal(response.properties.shifts.maxItems, 126);
  assert.equal(contract.paths['/admin/shifts/week'].get.summary, 'Get the operational schedule for one Monday-based week');
  assert.equal(contract.paths['/admin/shifts/week'].put.summary, 'Replace the operational schedule for one Monday-based week');
});

test('OpenAPI contracts the complete shipper order workflow', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const paths = contract.paths;
  const operations = {
    '/shipper/dashboard': ['get', 'getShipperDashboard'],
    '/shipper/orders/mine': ['get', 'listShipperOrders'],
    '/shipper/orders/active': ['get', 'listShipperActiveOrders'],
    '/shipper/orders/ready': ['get', 'listShipperReadyOrders'],
    '/shipper/orders/history': ['get', 'listShipperOrderHistory'],
    '/shipper/orders/{orderId}': ['get', 'getShipperOrder'],
    '/shipper/orders/{orderId}/claim': ['put', 'claimShipperReadyOrder'],
    '/shipper/orders/{orderId}/pickup': ['put', 'pickUpShipperOrder'],
    '/shipper/orders/{orderId}/deliver': ['put', 'deliverShipperOrder'],
    '/shipper/orders/{orderId}/fail': ['post', 'reportShipperDeliveryFailure'],
  };
  for (const [path, [method, operationId]] of Object.entries(operations)) {
    assert.equal(paths[path]?.[method]?.operationId, operationId, path);
    for (const status of ['401', '403']) assert.ok(paths[path][method].responses[status], `${path} ${status}`);
  }
  assert.deepEqual(contract.components.schemas.ShipperDeliveryFailureRequest.required, ['expectedStatus', 'reasonCode']);
  assert.deepEqual(contract.components.schemas.ShipperClaimRequest.required, ['expectedStatus']);
  assert.equal(contract.components.schemas.ShipperOrderListResponse.properties.data.items.$ref, '#/components/schemas/ShipperOrderListItem');
});

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
  const adminService = await readFile(new URL('../../Backend/FastGuy-FastFoodSite/src/main/java/service/AdminService.java', import.meta.url), 'utf8');
  assert.match(contract, /^  \/admin\/dashboard:$/m);
  assert.match(contract, /^      operationId: getAdminDashboard$/m);
  assert.match(contract, /^  \/admin\/reports\/full:$/m);
  assert.match(contract, /^      operationId: getAdminFullReport$/m);
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const document = JSON.parse(stdout);
  assert.deepEqual(document.paths['/admin/dashboard'].get.responses['500'], { $ref: '#/components/responses/InternalServerError' });
  const schema = document.components.schemas.AdminDashboardData;
  const canonicalTypes = {
    netCashRevenueToday: 'number',
    activeOrderCount: 'integer',
    pendingRefundCount: 'integer',
    pendingCodCount: 'integer',
    lowStockItemCount: 'integer',
    staffingGapCount: 'integer',
    activeOrdersByStatus: 'object',
    operationalOrderCountToday: 'integer',
    operationalCompletedCountToday: 'integer',
    completionRateToday: 'number',
    revenueLast7Days: 'array',
    topProductsLast7Days: 'array',
    lowStockProducts: 'array',
  };
  const canonicalFields = [...Object.keys(canonicalTypes), 'attentionItems', 'sectionAvailability'];
  const dashboardStart = adminService.indexOf('public Map<String, Object> getDashboardWithPeriod');
  const dashboardEnd = adminService.indexOf('private static void addAttention');
  const dashboardSource = adminService.slice(dashboardStart, dashboardEnd);
  const periodStart = dashboardSource.indexOf('if (period != null)');
  const periodEnd = dashboardSource.indexOf('long lowStockItemCount');
  assert.ok(dashboardStart >= 0 && dashboardEnd > dashboardStart);
  assert.ok(periodStart >= 0 && periodEnd > periodStart);
  const emittedFields = [...new Set([...dashboardSource.matchAll(/data\.put\("([^"]+)"/g)].map((match) => match[1]))];
  const periodFields = new Set([...dashboardSource.slice(periodStart, periodEnd).matchAll(/data\.put\("([^"]+)"/g)].map((match) => match[1]));
  const alwaysEmittedFields = emittedFields.filter((field) => !periodFields.has(field));
  const compatibilityFields = emittedFields.filter((field) => !canonicalFields.includes(field));
  const expectedProperties = [...new Set([...canonicalFields, ...emittedFields])].sort();
  const expectedRequired = [...new Set([...canonicalFields, ...alwaysEmittedFields])].sort();

  assert.equal(schema.additionalProperties, false);
  assert.deepEqual(Object.keys(schema.properties).sort(), expectedProperties);
  assert.deepEqual([...schema.required].sort(), expectedRequired);
  assert.deepEqual(Object.keys(schema.properties).filter((field) => schema.properties[field].deprecated !== true).sort(), [...canonicalFields].sort());
  assert.deepEqual(Object.keys(schema.properties).filter((field) => !canonicalFields.includes(field)).sort(), compatibilityFields.sort());
  for (const [field, type] of Object.entries(canonicalTypes)) assert.equal(schema.properties[field].type, type, field);
  assert.deepEqual(schema.properties.activeOrdersByStatus.additionalProperties, { type: 'integer', minimum: 0 });
  assert.deepEqual(schema.properties.attentionItems, { type: 'array', items: { $ref: '#/components/schemas/AdminAttentionItem' } });
  assert.deepEqual(schema.properties.sectionAvailability, { $ref: '#/components/schemas/AdminDashboardSectionAvailability' });
  assert.equal(schema.properties.netCashRevenueToday.description, 'Delivered-and-paid revenue recorded today minus refunds processed today.');
  assert.equal(schema.properties.activeOrderCount.description, 'All currently non-terminal operational orders, including orders created before today.');
  for (const field of canonicalFields) assert.notEqual(schema.properties[field].deprecated, true, field);
  for (const field of compatibilityFields) assert.equal(schema.properties[field].deprecated, true, field);
  const nullableCompatibilityFields = ['activeProductCount', 'customerCount', 'lowStockThreshold', 'totalProducts', 'totalUsers'];
  for (const field of nullableCompatibilityFields) assert.deepEqual(schema.properties[field].type, ['integer', 'null'], field);
  assert.deepEqual(
    Object.entries(schema.properties)
      .filter(([field, property]) => !canonicalFields.includes(field) && Array.isArray(property.type) && property.type.includes('null'))
      .map(([field]) => field)
      .sort(),
    [...nullableCompatibilityFields, 'grossProfitToday'].sort(),
  );

  const availability = document.components.schemas.AdminDashboardSectionAvailability;
  const sections = ['financial', 'orders', 'refunds', 'cod', 'inventory', 'staffing'];
  assert.equal(availability.additionalProperties, false);
  assert.deepEqual(availability.required, sections);
  assert.deepEqual(Object.keys(availability.properties), sections);
  for (const section of sections) assert.deepEqual(availability.properties[section], { type: 'string', enum: ['AVAILABLE', 'UNAVAILABLE'] });
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
  assert.match(contract, /^  \/admin\/products\/\{productId\}\/restore:$/m);
  assert.match(contract, /^      operationId: restoreAdminProduct$/m);
  assert.match(contract, /^  \/admin\/products\/\{productId\}\/permanent:$/m);
  assert.match(contract, /^      operationId: permanentlyDeleteAdminProduct$/m);
  const permanentPath = contract.slice(contract.indexOf('  /admin/products/{productId}/permanent:'), contract.indexOf('  /admin/orders/{orderId}:'));
  assert.match(permanentPath, /^        '409':$/m);
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

test('OpenAPI restricts guest checkout to PayOS bank transfer and contracts payment verification', async () => {
  const contract = await readFile(contractUrl, 'utf8');
  assert.match(contract, /^  \/orders\/guest-checkout:$/m);
  assert.match(contract, /^      operationId: createGuestOrder$/m);
  assert.match(contract, /^  \/orders\/guest-payment-status:$/m);
  assert.match(contract, /^      operationId: getGuestPaymentStatus$/m);
  const request = schemaSection(contract, 'GuestCheckoutRequest', 'GuestCheckoutResponse');
  assert.match(request, /paymentMethod:\s+type: string\s+const: BANK_TRANSFER/s);
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
  const detailFields = ['orderId', 'orderCode', 'status', 'customerName', 'customerPhone', 'customerAddress', 'totalAmount', 'shippingFee', 'serviceFee', 'finalAmount', 'discountAmount', 'paymentMethod', 'paymentStatus', 'deliveryNote', 'cancelledBy', 'failureNote', 'failureReason', 'deliveryFailureCode', 'deliveryAttemptCount', 'deliveryAttemptLimit', 'deliveryFailedAt', 'retryScheduledAt', 'returnedToStoreAt', 'refundStatus', 'refundAmount', 'refundNote', 'refundedAt', 'createdAt', 'confirmedAt', 'cancelledAt', 'deliveredAt', 'staffName', 'shipperName', 'internalNote', 'review', 'payment', 'items', 'statusHistory', 'statusEnteredAt', 'expiresAt', 'remainingSeconds', 'timeoutPolicy', 'ownerShiftCode', 'allowedActions'];
  for (const field of detailFields) assert.match(detail, new RegExp(`^        ${field}:`, 'm'));
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

test('OpenAPI contracts weekly shifts, monitoring, cutoff, and order timeout metadata', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const schemas = contract.components.schemas;
  const adminWeek = contract.paths['/admin/shifts/week'];
  const publicWeek = contract.paths['/shifts/week']?.get;
  const monitoring = contract.paths['/admin/shifts/monitoring']?.get;
  const request = schemas.AdminShiftWeekUpdateRequest;
  const orderFields = ['statusEnteredAt', 'expiresAt', 'remainingSeconds', 'timeoutPolicy'];

  assert.deepEqual(schemas.ShiftCode.enum, ['MORNING', 'AFTERNOON', 'EVENING']);
  assert.deepEqual(schemas.ShiftMutationSource.enum, ['MANUAL', 'AUTO']);
  assert.equal(adminWeek.get.operationId, 'getAdminShiftWeek');
  assert.equal(adminWeek.put.operationId, 'replaceAdminShiftWeek');
  assert.equal(publicWeek.operationId, 'getShiftWeek');
  assert.equal(monitoring.operationId, 'getAdminShiftMonitoring');
  assert.ok(adminWeek.put.security?.[0]?.bearerAuth);
  assert.equal(request.additionalProperties, false);
  assert.deepEqual(request.required, ['weekStart', 'slots']);
  assert.equal(request.properties.slots.maxItems, 126);
  assert.equal(request.properties.slots.items.$ref, '#/components/schemas/AdminShiftWeekSlotRequest');
  assert.deepEqual(schemas.AdminShiftWeekSlotRequest.properties.role.enum, ['STAFF', 'SHIPPER']);
  assert.deepEqual(schemas.ShiftMonitoringState.enum, ['SCHEDULED', 'CHECK_IN_WINDOW', 'LATE', 'ACTIVE_MANUAL', 'ACTIVE_AUTO', 'CHECK_OUT_WINDOW', 'COMPLETED_MANUAL', 'COMPLETED_AUTO', 'MISSING_STAFF', 'MISSING_NEXT_SHIFT', 'ROLLOVER_BLOCKED']);
  assert.deepEqual(schemas.ShiftAlertSeverity.enum, ['INFO', 'WARNING', 'CRITICAL']);
  for (const schemaName of ['ShiftWeekItem', 'ShiftMonitoringItem']) {
    for (const field of ['shiftCode', 'checkInSource', 'checkOutSource']) assert.ok(schemas[schemaName].required.includes(field));
    assert.equal(schemas[schemaName].properties.shiftCode.$ref, '#/components/schemas/ShiftCode');
  }
  const publicConfigFields = ['isOpen', 'openTime', 'closeTime', 'orderCutoffTime', 'serviceFee', 'taxRate', 'deliveryFee', 'minOrderAmount', 'estimatedDeliveryMinutes', 'storeName', 'storePhone', 'storeAddress', 'storeLogo', 'morningCountNotice'];
  assert.equal(schemas.PublicStoreConfig.additionalProperties, false);
  assert.deepEqual(schemas.PublicStoreConfig.required, publicConfigFields);
  assert.deepEqual(Object.keys(schemas.PublicStoreConfig.properties), publicConfigFields);
  assert.equal(schemas.PublicStoreConfig.properties.orderCutoffTime.pattern, '^([01]\\d|2[0-3]):[0-5]\\d$');
  for (const schemaName of ['StaffDispatchOrder', 'AdminOrderDetail']) {
    for (const field of orderFields) {
      assert.ok(schemas[schemaName].required.includes(field));
      assert.ok(Object.hasOwn(schemas[schemaName].properties, field));
    }
    assert.ok(schemas[schemaName].required.includes('ownerShiftCode'));
    assert.ok(Object.hasOwn(schemas[schemaName].properties, 'ownerShiftCode'));
  }
  assert.ok(schemas.StaffHandoverOrder.required.includes('ownerShiftCode'));
  assert.equal(schemas.StaffHandoverOrder.properties.ownerShiftCode.$ref, '#/components/schemas/NullableShiftCode');
  for (const operation of [adminWeek.get, adminWeek.put, publicWeek, monitoring]) {
    for (const status of ['400', '401', '403']) {
      if (operation.responses[status]) assert.ok(operation.responses[status].$ref.startsWith('#/components/responses/'));
    }
  }
});

test('OpenAPI contracts operating expenses, fixed assets, and operating profit', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const schemas = contract.components.schemas;
  const expenses = contract.paths['/admin/operating-expenses'];
  const expense = contract.paths['/admin/operating-expenses/{expenseId}'];
  const assets = contract.paths['/admin/fixed-assets'];
  const asset = contract.paths['/admin/fixed-assets/{assetId}'];
  const retire = contract.paths['/admin/fixed-assets/{assetId}/retire']?.put;
  const report = contract.paths['/admin/reports/operating-profit']?.get;

  assert.deepEqual(Object.keys(expenses), ['get', 'post']);
  assert.deepEqual(Object.keys(expense), ['parameters', 'get', 'put', 'delete']);
  assert.deepEqual(Object.keys(assets), ['get', 'post']);
  assert.deepEqual(Object.keys(asset), ['parameters', 'get', 'put']);
  assert.equal(retire.operationId, 'retireFixedAsset');
  assert.deepEqual(schemas.FixedAssetRetireRequest.required, ['expectedStatus']);
  assert.equal(schemas.FixedAssetRetireRequest.properties.expectedStatus.const, 'ACTIVE');
  assert.deepEqual(report.parameters.map(({ name }) => name), ['fromDate', 'toDate']);
  assert.ok(report.parameters.every(({ required }) => required));
  for (const operation of [expenses.get, expenses.post, expense.get, expense.put, expense.delete, assets.get, assets.post, asset.get, asset.put, retire, report]) {
    assert.ok(operation.security?.[0]?.bearerAuth);
    assert.ok(Object.values(operation.responses).every((response) => response.$ref?.startsWith('#/components/responses/') || response.content || response.description));
  }
  assert.deepEqual(schemas.OperatingExpenseCategory.enum, ['RENT', 'UTILITIES', 'SALARY', 'MARKETING', 'MAINTENANCE', 'OTHER']);
  for (const name of ['OperatingExpense', 'OperatingExpenseRequest', 'FixedAsset', 'FixedAssetRequest', 'FixedAssetRetireRequest', 'OperatingProfitData']) assert.equal(schemas[name].additionalProperties, false);
  assert.deepEqual(schemas.OperatingExpense.required, ['expenseId', 'expenseDate', 'category', 'description', 'amount', 'createdBy', 'createdByName', 'createdAt', 'updatedAt']);
  assert.deepEqual(schemas.OperatingExpenseRequest.required, ['expenseDate', 'category', 'description', 'amount']);
  assert.equal(schemas.OperatingExpenseRequest.properties.description.maxLength, 500);
  assert.equal(schemas.OperatingExpenseRequest.properties.amount.exclusiveMinimum, 0);
  assert.deepEqual(schemas.FixedAsset.required, ['assetId', 'assetName', 'acquisitionCost', 'salvageValue', 'depreciationStartDate', 'usefulLifeMonths', 'status', 'retiredAt', 'createdBy', 'createdByName', 'createdAt', 'updatedAt']);
  assert.deepEqual(schemas.FixedAssetRequest.required, ['assetName', 'acquisitionCost', 'salvageValue', 'depreciationStartDate', 'usefulLifeMonths']);
  assert.equal(schemas.FixedAssetRequest.properties.acquisitionCost.exclusiveMinimum, 0);
  assert.equal(schemas.FixedAssetRequest.properties.salvageValue.minimum, 0);
  assert.equal(schemas.FixedAssetRequest.properties.usefulLifeMonths.minimum, 1);
  assert.deepEqual(schemas.FixedAssetStatus.enum, ['ACTIVE', 'RETIRED']);
  assert.deepEqual(schemas.FixedAsset.properties.retiredAt.type, ['string', 'null']);
  const reportFields = ['grossRevenue', 'refundTotal', 'netRevenue', 'cogs', 'grossProfit', 'operatingExpenses', 'storeExpenses', 'profitBeforeDepreciation', 'estimatedOperatingResult', 'depreciation', 'operatingProfit', 'includesManualSalary', 'costComplete', 'missingCostItemCount', 'fromDate', 'toDate'];
  assert.deepEqual(schemas.OperatingProfitData.required, reportFields);
  assert.deepEqual(Object.keys(schemas.OperatingProfitData.properties), reportFields);
  for (const field of ['cogs', 'grossProfit', 'profitBeforeDepreciation', 'estimatedOperatingResult', 'operatingProfit']) assert.ok(schemas.OperatingProfitData.properties[field].oneOf.some((item) => item.type === 'null'));
  for (const name of ['OperatingExpense', 'FixedAsset']) assert.doesNotMatch(JSON.stringify(schemas[name]), /deletedBy|retiredBy|approvedBy/);
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

test('OpenAPI contracts paginated R7 admin activity logs', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const operation = contract.paths['/admin/activity-logs']?.get;
  const parameters = Object.fromEntries(operation.parameters.map((parameter) => [parameter.name, parameter]));
  const schemas = contract.components.schemas;
  const actions = ['ORDER_CANCELLED', 'ORDER_REFUND_RECORDED', 'DELIVERY_ATTEMPT_OVERRIDDEN', 'ATTENDANCE_APPROVED', 'STAFF_PAY_RATE_CREATED', 'STOCK_COUNT_APPROVED'];
  const itemFields = ['activityLogId', 'actor', 'actionType', 'targetType', 'targetId', 'summary', 'metadata', 'createdAt'];

  assert.equal(operation.operationId, 'listAdminActivityLogs');
  assert.deepEqual(operation.security, [{ bearerAuth: [] }]);
  assert.deepEqual(Object.keys(parameters), ['from', 'to', 'actionType', 'actorUserId', 'page', 'pageSize']);
  assert.equal(parameters.from.schema.format, 'date-time');
  assert.equal(parameters.to.schema.format, 'date-time');
  assert.deepEqual(parameters.actionType.schema.enum, actions);
  assert.equal(parameters.actorUserId.schema.minimum, 1);
  assert.equal(parameters.page.schema.minimum, 1);
  assert.equal(parameters.page.schema.default, 1);
  assert.equal(parameters.pageSize.schema.minimum, 1);
  assert.ok(parameters.pageSize.schema.maximum);
  assert.ok(parameters.pageSize.schema.default);
  assert.match(operation.description, /newest first/i);
  assert.equal(operation.responses['200'].content['application/json'].schema.$ref, '#/components/schemas/ActivityLogListResponse');
  for (const [status, response] of Object.entries({ 400: 'BadRequest', 401: 'Unauthorized', 403: 'Forbidden' })) {
    assert.equal(operation.responses[status].$ref, `#/components/responses/${response}`);
  }
  assert.deepEqual(schemas.ActivityLogActionType.enum, actions);
  assert.equal(schemas.ActivityLog.additionalProperties, false);
  assert.deepEqual(schemas.ActivityLog.required, itemFields);
  assert.deepEqual(Object.keys(schemas.ActivityLog.properties), itemFields);
  assert.deepEqual(schemas.ActivityLogActor.required, ['userId', 'fullName']);
  assert.deepEqual(schemas.ActivityLog.properties.targetId.type, ['integer', 'null']);
  assert.deepEqual(schemas.ActivityLog.properties.metadata.additionalProperties.type, ['string', 'number', 'integer', 'boolean', 'null']);
  assert.deepEqual(schemas.Pagination.required, ['page', 'pageSize', 'totalItems', 'totalPages']);
  assert.deepEqual(schemas.ActivityLogListData.required, ['items', 'pagination']);
  assert.deepEqual(schemas.ActivityLogListResponse.required, ['status', 'data']);
});

test('OpenAPI contracts inventory analytics trends and health', async () => {
  const contract = await readFile(new URL('../../openapi/fastguy.yaml', import.meta.url), 'utf8');
  const path = contract.slice(contract.indexOf('  /admin/inventory/analytics:'), contract.indexOf('  /admin/inventory/reports/summary:'));
  assert.match(path, /operationId: getInventoryAnalytics/);
  for (const field of ['fromDate', 'toDate', 'granularity']) assert.match(path, new RegExp(`name: ${field}`));
  for (const schema of ['InventoryAnalytics', 'InventoryAnalyticsPoint', 'InventoryHealth', 'InventoryAttentionItem']) assert.match(contract, new RegExp(`^    ${schema}:`, 'm'));
  for (const field of ['inventoryValue', 'receiptValue', 'consumptionValue', 'wasteValue', 'adjustmentLossValue', 'adjustmentGainValue']) assert.match(contract, new RegExp(`${field}:`));
});

test('OpenAPI contracts Slice 2 admin Operations APIs', async () => {
  const cli = fileURLToPath(new URL('../node_modules/@redocly/cli/bin/cli.js', import.meta.url));
  const { stdout } = await execFileAsync(process.execPath, [cli, 'bundle', fileURLToPath(contractUrl), '--ext', 'json']);
  const contract = JSON.parse(stdout);
  const schemas = contract.components.schemas;
  const paths = contract.paths;
  const orderStatuses = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERY_FAILED', 'RETURNED_TO_STORE', 'DELIVERED', 'CANCELLED'];
  const orderActions = ['CONFIRMED', 'CANCELLED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP', 'DELIVERED', 'DELIVERY_FAILED', 'RETURNED_TO_STORE'];
  const responseRef = operation => operation.responses['200'].content['application/json'].schema.$ref;
  const requestRef = operation => operation.requestBody.content['application/json'].schema.$ref;
  const assertErrors = (operation, expected) => {
    assert.deepEqual(Object.keys(operation.responses).filter(status => status !== '200'), expected);
    const names = { 400: 'BadRequest', 401: 'Unauthorized', 403: 'Forbidden', 404: 'NotFound', 409: 'Conflict', 422: 'UnprocessableEntity', 500: 'InternalServerError' };
    for (const status of expected) assert.equal(operation.responses[status].$ref, `#/components/responses/${names[status]}`);
  };

  const orders = paths['/admin/orders']?.get;
  const detail = paths['/admin/orders/{orderId}']?.get;
  const cancel = paths['/admin/orders/{orderId}/cancel']?.put;
  const status = paths['/admin/orders/{orderId}/status']?.put;
  const notes = paths['/admin/orders/{orderId}/notes']?.post;
  const override = paths['/admin/orders/{orderId}/delivery-attempt-override']?.post;
  const codList = paths['/cod-settlements/admin']?.get;
  const codVerify = paths['/cod-settlements/{settlementId}/verify']?.put;
  const refundList = paths['/admin/refunds']?.get;
  const refundMutation = paths['/admin/refunds/{orderId}']?.put;
  const refundProof = paths['/admin/refunds/{orderId}/proof-url']?.get;

  assert.equal(orders.operationId, 'listAdminOrders');
  assert.equal(detail.operationId, 'getAdminOrderDetail');
  assert.equal(cancel.operationId, 'cancelAdminOrder');
  assert.equal(status.operationId, 'updateAdminOrderStatus');
  assert.equal(notes.operationId, 'addAdminOrderNote');
  assert.equal(override.operationId, 'overrideAdminOrderDeliveryAttempt');
  assert.equal(codList.operationId, 'listAdminCodSettlements');
  assert.equal(codVerify.operationId, 'verifyCodSettlement');
  assert.equal(refundList.operationId, 'listAdminRefunds');
  assert.equal(refundMutation.operationId, 'updateAdminRefund');
  assert.equal(refundProof.operationId, 'getAdminRefundProofUrl');
  for (const operation of [orders, detail, cancel, status, notes, override, codList, codVerify, refundList, refundMutation, refundProof]) {
    assert.deepEqual(operation.security, [{ bearerAuth: [] }]);
  }

  const orderParameters = Object.fromEntries(orders.parameters.map(parameter => [parameter.name, parameter]));
  assert.deepEqual(Object.keys(orderParameters), ['search', 'status', 'attentionOnly', 'paymentStatus', 'refundStatus', 'fromDate', 'toDate', 'sort', 'page', 'pageSize']);
  assert.deepEqual(orderParameters.status.schema.enum, orderStatuses);
  assert.deepEqual(orderParameters.paymentStatus.schema.enum, ['UNPAID', 'PAID', 'FAILED', 'REFUNDED']);
  assert.deepEqual(orderParameters.refundStatus.schema.enum, ['PENDING', 'REFUNDED', 'REJECTED']);
  assert.deepEqual(orderParameters.sort.schema.enum, ['WAITING_DESC', 'CREATED_DESC']);
  assert.equal(orderParameters.page.schema.minimum, 1);
  assert.equal(orderParameters.pageSize.schema.minimum, 1);
  assert.equal(orderParameters.pageSize.schema.maximum, 100);
  assert.equal(responseRef(orders), '#/components/schemas/AdminOrderListResponse');
  assert.equal(responseRef(detail), '#/components/schemas/AdminOrderDetailResponse');
  assertErrors(orders, ['400', '401', '403', '500']);
  assertErrors(detail, ['401', '403', '404', '500']);

  assert.deepEqual(schemas.OrderStatus.enum, orderStatuses);
  assert.deepEqual(schemas.AdminOrderAction.enum, orderActions);
  assert.equal(schemas.AdminOrderListItem.additionalProperties, false);
  assert.ok(schemas.AdminOrderListItem.required.includes('waitingMinutes'));
  assert.ok(schemas.AdminOrderListItem.required.includes('allowedActions'));
  assert.deepEqual(schemas.AdminOrderListItem.properties.waitingMinutes, { type: 'integer', minimum: 0 });
  assert.equal(schemas.AdminOrderListItem.properties.allowedActions.items.$ref, '#/components/schemas/AdminOrderAction');
  assert.equal(schemas.AdminOrderListData.additionalProperties, false);
  assert.deepEqual(schemas.AdminOrderListData.required, ['items', 'pagination']);
  assert.equal(schemas.AdminOrderListData.properties.items.items.$ref, '#/components/schemas/AdminOrderListItem');
  assert.equal(schemas.AdminOrderListData.properties.pagination.$ref, '#/components/schemas/Pagination');
  assert.equal(schemas.AdminOrderListResponse.properties.data.$ref, '#/components/schemas/AdminOrderListData');
  assert.equal(schemas.Pagination.additionalProperties, false);
  assert.deepEqual(schemas.Pagination.required, ['page', 'pageSize', 'totalItems', 'totalPages']);

  const orderMutations = [
    [cancel, 'AdminOrderCancelRequest', ['expectedStatus', 'reason']],
    [status, 'AdminOrderStatusRequest', ['expectedStatus', 'status']],
    [notes, 'AdminOrderNoteRequest', ['expectedStatus', 'note']],
    [override, 'AdminOrderDeliveryAttemptOverrideRequest', ['expectedStatus', 'note']],
  ];
  for (const [operation, schemaName, required] of orderMutations) {
    assert.equal(requestRef(operation), `#/components/schemas/${schemaName}`);
    assert.equal(schemas[schemaName].additionalProperties, false);
    assert.deepEqual(schemas[schemaName].required, required);
    assert.equal(responseRef(operation), '#/components/schemas/AdminMutationResponse');
  }
  assert.equal(schemas.AdminOrderCancelRequest.properties.expectedStatus.$ref, '#/components/schemas/OrderStatus');
  assert.equal(schemas.AdminOrderStatusRequest.properties.expectedStatus.$ref, '#/components/schemas/OrderStatus');
  assert.equal(schemas.AdminOrderStatusRequest.properties.status.$ref, '#/components/schemas/OrderStatus');
  assert.equal(schemas.AdminOrderNoteRequest.properties.expectedStatus.$ref, '#/components/schemas/OrderStatus');
  assert.equal(schemas.AdminOrderDeliveryAttemptOverrideRequest.properties.expectedStatus.$ref, '#/components/schemas/OrderStatus');
  assertErrors(cancel, ['400', '401', '403', '404', '409', '422', '500']);
  assertErrors(status, ['400', '401', '403', '404', '409', '422', '500']);
  assertErrors(notes, ['400', '401', '403', '404', '409', '500']);
  assertErrors(override, ['400', '401', '403', '404', '409', '422', '500']);

  const codParameters = Object.fromEntries(codList.parameters.map(parameter => [parameter.name, parameter]));
  assert.deepEqual(Object.keys(codParameters), ['status']);
  assert.equal(codParameters.status.required, true);
  assert.deepEqual(codParameters.status.schema.enum, ['SUBMITTED', 'SHORT', 'OVER', 'SETTLED']);
  assert.equal(responseRef(codList), '#/components/schemas/AdminCodSettlementListResponse');
  assert.equal(requestRef(codVerify), '#/components/schemas/CodSettlementVerifyRequest');
  assertErrors(codList, ['400', '401', '403', '500']);
  assertErrors(codVerify, ['400', '401', '403', '404', '409', '500']);
  const codFields = ['settlementId', 'shipperId', 'shipperName', 'shiftId', 'shiftDate', 'startTime', 'endTime', 'status', 'expectedAmount', 'submittedAmount', 'differenceAmount', 'verifiedAmount', 'reason', 'receivedByName', 'submittedAt', 'verifiedAt'];
  assert.equal(schemas.AdminCodSettlement.additionalProperties, false);
  assert.deepEqual(schemas.AdminCodSettlement.required, codFields);
  assert.deepEqual(Object.keys(schemas.AdminCodSettlement.properties), codFields);
  assert.equal(schemas.AdminCodSettlement.properties.expectedAmount.$ref, '#/components/schemas/Money');
  assert.equal(schemas.AdminCodSettlement.properties.submittedAmount.$ref, '#/components/schemas/Money');
  assert.equal(schemas.AdminCodSettlement.properties.verifiedAmount.oneOf[0].$ref, '#/components/schemas/Money');
  assert.match(schemas.AdminCodSettlement.properties.differenceAmount.description, /submittedAmount - expectedAmount/);
  assert.match(schemas.AdminCodSettlement.properties.differenceAmount.description, /negative.*short.*positive.*over/i);
  assert.equal(schemas.CodSettlementVerifyRequest.additionalProperties, false);
  assert.deepEqual(schemas.CodSettlementVerifyRequest.required, ['expectedStatus', 'status', 'verifiedAmount']);
  assert.equal(schemas.CodSettlementVerifyRequest.properties.expectedStatus.const, 'SUBMITTED');
  assert.deepEqual(schemas.CodSettlementVerifyRequest.properties.status.enum, ['SETTLED', 'SHORT', 'OVER']);
  assert.equal(schemas.CodSettlementVerifyRequest.properties.reason.maxLength, 500);

  const refundParameters = Object.fromEntries(refundList.parameters.map(parameter => [parameter.name, parameter]));
  assert.deepEqual(Object.keys(refundParameters), ['status', 'search', 'fromDate', 'toDate']);
  assert.deepEqual(refundParameters.status.schema.enum, ['PENDING', 'REFUNDED', 'REJECTED']);
  assert.equal(refundParameters.fromDate.schema.format, 'date');
  assert.equal(refundParameters.toDate.schema.format, 'date');
  assert.equal(responseRef(refundList), '#/components/schemas/AdminRefundListResponse');
  assertErrors(refundList, ['400', '401', '403', '500']);
  const refundFields = ['orderId', 'orderCode', 'customerName', 'customerPhone', 'finalAmount', 'paymentMethod', 'paymentStatus', 'refundStatus', 'refundAmount', 'refundNote', 'refundReference', 'refundProcessedBy', 'refundProcessedByName', 'cancelledAt', 'paidAt', 'refundedAt', 'failureReason', 'createdAt', 'proofAvailable'];
  assert.equal(schemas.AdminRefund.additionalProperties, false);
  assert.deepEqual(schemas.AdminRefund.required, refundFields);
  assert.deepEqual(Object.keys(schemas.AdminRefund.properties), refundFields);
  assert.deepEqual(schemas.AdminRefund.properties.proofAvailable, { type: 'boolean' });

  const multipart = refundMutation.requestBody.content['multipart/form-data'];
  assert.ok(multipart);
  assert.deepEqual(Object.keys(refundMutation.requestBody.content), ['multipart/form-data']);
  assert.equal(multipart.schema.$ref, '#/components/schemas/RefundMutation');
  assert.equal(multipart.encoding.proof.contentType, 'image/jpeg, image/png, image/webp');
  assert.equal(schemas.RefundMutation.additionalProperties, false);
  assert.deepEqual(schemas.RefundMutation.required, ['expectedStatus', 'status']);
  assert.deepEqual(Object.keys(schemas.RefundMutation.properties), ['expectedStatus', 'status', 'refundAmount', 'refundNote', 'refundReference', 'proof']);
  assert.equal(schemas.AdminRefund.properties.finalAmount.oneOf[0].$ref, '#/components/schemas/Money');
  assert.equal(schemas.AdminRefund.properties.refundAmount.oneOf[0].$ref, '#/components/schemas/Money');
  assert.equal(schemas.RefundMutation.properties.expectedStatus.const, 'PENDING');
  assert.deepEqual(schemas.RefundMutation.properties.status.enum, ['REFUNDED', 'REJECTED']);
  assert.equal(schemas.RefundMutation.properties.proof.format, 'binary');
  assert.equal(schemas.RefundMutation.properties.proof.maxLength, 5 * 1024 * 1024);
  assert.match(schemas.RefundMutation.description, /REFUNDED.*full amount.*reference.*proof/is);
  assert.match(schemas.RefundMutation.description, /REJECTED.*note.*no proof/is);
  assert.equal(responseRef(refundMutation), '#/components/schemas/AdminMutationResponse');
  assertErrors(refundMutation, ['400', '401', '403', '404', '409', '422', '500']);

  assert.match(refundProof.description, /five minutes/i);
  assert.equal(responseRef(refundProof), '#/components/schemas/RefundProofViewResponse');
  assertErrors(refundProof, ['401', '403', '404', '500']);
  assert.equal(schemas.RefundProofViewData.additionalProperties, false);
  assert.deepEqual(schemas.RefundProofViewData.required, ['viewUrl', 'expiresAt']);
  assert.equal(schemas.RefundProofViewData.properties.viewUrl.format, 'uri');
  assert.equal(schemas.RefundProofViewData.properties.expiresAt.format, 'date-time');
  assert.equal(schemas.RefundProofViewResponse.properties.data.$ref, '#/components/schemas/RefundProofViewData');
  assert.equal(schemas.ErrorResponse.additionalProperties, false);

  const refs = [];
  JSON.stringify(contract, (key, value) => {
    if (key === '$ref') refs.push(value);
    return value;
  });
  assert.ok(refs.every(ref => ref.startsWith('#/')));
});
