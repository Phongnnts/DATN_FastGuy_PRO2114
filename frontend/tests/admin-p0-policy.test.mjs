import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const inventory = read('../src/views/admin/InventoryPage.vue');
const dashboard = read('../src/views/admin/DashboardPage.vue');
const variants = read('../src/components/admin/product-editor/ProductVariantsSection.vue');

test('inventory stock changes only through item-level workflows', () => {
  assert.doesNotMatch(inventory, /saveStock|draftStock|updateVariant\([^]*quantityAvailable/);
  assert.doesNotMatch(inventory, /class="stock-edit"/);
  assert.match(inventory, /adminApi\.getInventoryItems\(\)/);
  assert.match(inventory, /adjustInventoryItem/);
  assert.doesNotMatch(inventory, /receiptInventory|buildReceiptPayload|kind === 'receipt'/);
});

test('variant rows persist without stock fields and link to recipe management', () => {
  assert.match(variants, /inventoryMode/);
  assert.match(variants, /openRecipes\(row\)/);
  assert.doesNotMatch(variants, /buildVariantUpdatePayload|submitVariantUpdate|Quản lý tồn kho/);
  assert.match(variants, /adminApi\.createVariant\(props\.productId, variantPayload\(row\)\)/);
});

test('dashboard exposes loading error retry and ready without failure KPI fallback', () => {
  assert.match(dashboard, /loadState = ref\('loading'\)/);
  assert.match(dashboard, /loadState\.value = 'error'/);
  assert.match(dashboard, /loadState\.value = 'ready'/);
  assert.match(dashboard, /role="status"[^]*Đang tải/);
  assert.match(dashboard, /role="alert"[^]*@click="loadDashboard"[^]*Thử lại/);
  assert.match(dashboard, /dashboardViewState\(data\.value, loadState\.value, loadError\.value\)/);
  assert.match(dashboard, /<template v-else>/);
  assert.doesNotMatch(dashboard, /adminStore\.dashboard \|\| \{[^]*totalUsers: 0/);
});

test('dashboard accepts only current mounted success before charts', () => {
  assert.match(dashboard, /requestGeneration/);
  assert.match(dashboard, /stopped/);
  assert.match(dashboard, /request\.generation !== requestGeneration/);
  assert.match(dashboard, /loadState\.value = 'ready'[^]*buildCharts\(\)/);
  assert.doesNotMatch(dashboard, /watch\(\s*\(\) => adminStore\.dashboard/);
});

