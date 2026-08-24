import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';
import { normalizeApiError } from '../src/api/error.js';

const root = new URL('../src/', import.meta.url);
const read = (path) => readFile(new URL(path, root), 'utf8');

const api = await read('api/admin.js');
const inventory = await read('views/admin/InventoryPage.vue');
const recipes = await read('views/admin/RecipesPage.vue');
const ledger = await read('views/admin/InventoryLedgerPage.vue');
const inventoryItemUtils = await read('utils/inventoryItem.js');
const variantsSection = await read('components/admin/product-editor/ProductVariantsSection.vue');
const router = await read('router/index.js');
const layout = await read('layouts/AdminLayout.vue');

test('admin API client matches OpenAPI ingredient inventory operations exactly', () => {
  assert.match(api, /getInventoryItems\(\) \{\s*return client\.get\('\/admin\/inventory\/items'\);\s*\}/);
  assert.match(api, /createInventoryItem\(data\) \{\s*return client\.post\('\/admin\/inventory\/items', data\);\s*\}/);
  assert.match(api, /updateInventoryItem\(id, data\) \{\s*return client\.put\(`\/admin\/inventory\/items\/\$\{id\}`, data\);\s*\}/);
  assert.doesNotMatch(api, /receiptInventory|\/admin\/inventory\/transactions\/receipts/);
  assert.match(api, /createGoodsReceipt\(data\) \{\s*return client\.post\('\/admin\/inventory\/receipts', data\);\s*\}/);
  assert.match(api, /adjustInventoryItem\(data\) \{\s*return client\.post\('\/admin\/inventory\/transactions\/adjustments', data\);\s*\}/);
  assert.match(api, /getVariantRecipe\(variantId\) \{\s*return client\.get\(`\/admin\/product-variants\/\$\{variantId\}\/recipe`\);\s*\}/);
  assert.match(api, /replaceVariantRecipe\(variantId, data\) \{\s*return client\.put\(`\/admin\/product-variants\/\$\{variantId\}\/recipe`, data\);\s*\}/);
  assert.match(api, /getVariantInventorySettings\(variantId\)[\s\S]*\/inventory-settings/);
  assert.match(api, /updateVariantInventorySettings\(variantId, data\)[\s\S]*\/inventory-settings/);
  assert.match(api, /getVariantInventoryCapacity\(variantId\)[\s\S]*\/inventory-capacity/);
  assert.match(api, /getVariantAvailability\(variantId\) \{\s*return client\.get\(`\/admin\/product-variants\/\$\{variantId\}\/availability`\);\s*\}/);
  assert.doesNotMatch(api, /transactions\/waste/);
  assert.doesNotMatch(api, /\{ variantId, \.\.\.data \}/);
});

test('normalized errors expose contracted conflict currentOnHandQuantity', () => {
  const error = normalizeApiError({
    message: 'Request failed',
    response: { status: 409, data: { status: 'error', message: 'Stale expected quantity', currentOnHandQuantity: 12.5 } },
  });
  assert.equal(error.status, 409);
  assert.equal(error.currentOnHandQuantity, 12.5);
});

test('inventory page shows phase-1 KPIs and item-level table columns', () => {
  for (const label of ['Tổng mặt hàng', 'Dưới mức tối thiểu', 'Kích cỡ hết hàng', 'Giao dịch gần đây']) {
    assert.ok(inventory.includes(label), label);
  }
  for (const column of ['Mặt hàng', 'Loại', 'Hiện có', 'Đã giữ', 'Khả dụng', 'Đơn vị', 'Tối thiểu', 'Trạng thái']) {
    assert.ok(inventory.includes(column), column);
  }
  assert.match(inventory, /formatQuantity\(/);
  assert.match(inventory, /availableQuantity/);
});

test('inventory page keeps accessible dialogs with stale-conflict recovery', () => {
  assert.match(inventory, /role="dialog"/);
  assert.match(inventory, /aria-modal="true"/);
  assert.match(inventory, /event\.key === 'Escape'/);
  assert.match(inventory, /currentOnHandQuantity/);
  assert.match(inventory, /adjustInventoryItem/);
  assert.match(inventory, /createInventoryItem/);
  assert.match(inventory, /updateInventoryItem/);
  assert.doesNotMatch(inventory, /receiptInventory|buildReceiptPayload|kind === 'receipt'/);
  assert.match(inventoryItemUtils, /expectedOnHandQuantity/);
  assert.doesNotMatch(inventory, /openWaste|wasteInventory|Lãng phí/);
});

test('inventory page links the ledger', () => {
  assert.match(inventory, /name: 'AdminInventoryLedger'/);
});

test('recipes page edits whole recipe with duplicate and non-positive guards', () => {
  assert.match(recipes, /replaceVariantRecipe/);
  assert.match(recipes, /validateRecipeForm|buildRecipePayload/);
  assert.match(inventoryItemUtils, /Mặt hàng đã được chọn/);
  assert.match(inventoryItemUtils, /Số lượng phải lớn hơn 0/);
  assert.match(recipes, /Công thức cho 1 phần/);
  assert.match(recipes, /getVariantInventoryCapacity/);
  assert.match(recipes, /getVariantInventorySettings/);
  assert.doesNotMatch(recipes, /v-model="form\.inventoryMode"|#\{\{ item\.inventoryItemId \}\}/);
  assert.match(recipes, /error\.status === 409[\s\S]*Dữ liệu đã thay đổi/);
});

test('inventory item update surfaces backend invariant conflicts without changing local rows', () => {
  assert.match(inventory, /if \(error\.status === 409\) \{\s*dialogError\.value = error\.message \|\| 'Mặt hàng đang được sử dụng và không thể đổi loại hoặc ngừng hoạt động\.';\s*return;\s*\}/);
  assert.match(inventory, /const saved = kind === 'create'[\s\S]*else syncRow\(saved\);/);
});

test('ledger filters by item order type date with zero-based paging', () => {
  assert.match(ledger, /params\.inventoryItemId/);
  assert.match(ledger, /params\.orderId/);
  assert.match(ledger, /params\.transactionType/);
  assert.match(ledger, /params\.fromDate/);
  assert.match(ledger, /params\.toDate/);
  assert.match(ledger, /page\.value - 1/);
  assert.match(ledger, /totalItems/);
  assert.doesNotMatch(ledger, /params\.variantId|params\.productId|orderCode|createdByName/);
});

test('router registers recipes route next to inventory pages', () => {
  assert.match(router, /path: 'recipes',\s*name: 'AdminRecipes',[\s\S]*RecipesPage\.vue/);
  assert.match(router, /AdminRecipes: 'Công thức định lượng'/);
  assert.match(router, /AdminInventory: 'Tổng quan kho'/);
});

test('sidebar groups simplified inventory destinations', () => {
  assert.match(layout, /Quản lý kho/);
  assert.match(layout, /label: 'Tổng quan', path: '\/admin\/inventory'/);
  assert.match(layout, /label: 'Công thức món', path: '\/admin\/recipes'/);
  assert.match(layout, /label: 'Lịch sử kho', path: '\/admin\/inventory\/ledger'/);
  assert.match(layout, /label: 'Báo cáo theo món', path: '\/admin\/inventory\/reports'/);
});

test('variant editor owns mode selection without embedded BOM editor', () => {
  assert.match(variantsSection, /inventoryMode/);
  assert.match(variantsSection, /AdminRecipes/);
  assert.doesNotMatch(variantsSection, /Số phần đầu ra|Định lượng nguyên liệu|recipe-yield/);
  assert.doesNotMatch(variantsSection, /buildVariantUpdatePayload|submitVariantUpdate/);
  assert.doesNotMatch(variantsSection, /Quản lý tồn kho|expectedQuantity/);
});
