import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const inventory = read('../src/views/admin/InventoryPage.vue');
const dashboard = read('../src/views/admin/DashboardPage.vue');
const store = read('../src/stores/admin.js');
const variants = read('../src/components/admin/product-editor/ProductVariantsSection.vue');

test('inventory stock changes only through adjustment and waste workflows', () => {
  assert.doesNotMatch(inventory, /saveStock|draftStock|updateVariant\([^]*quantityAvailable/);
  assert.doesNotMatch(inventory, /class="stock-edit"/);
  assert.match(inventory, /\{\{ row\.stock === null \? 'Không giới hạn' : row\.stock \}\}/);
  assert.match(inventory, /adminApi\.adjustInventory/);
  assert.match(inventory, /adminApi\.wasteInventory/);
});

test('persisted variant update omits stock while create keeps initial stock', () => {
  assert.match(variants, /adminApi\.updateVariant\(row\.variantId, variantPayload\(row, \{ includeStock: false \}\)\)/);
  assert.match(variants, /adminApi\.createVariant\(props\.productId, variantPayload\(row\)\)/);
  assert.match(variants, /:disabled="busy \|\| mutating \|\| Boolean\(row\.variantId\)"[^>]*variant-qty|variant-qty-[^]*:disabled="busy \|\| mutating \|\| Boolean\(row\.variantId\)"/);
});

test('dashboard exposes loading error retry and ready without failure KPI fallback', () => {
  assert.match(dashboard, /loadState = ref\('loading'\)/);
  assert.match(dashboard, /loadState\.value = 'error'/);
  assert.match(dashboard, /loadState\.value = 'ready'/);
  assert.match(dashboard, /role="status"[^]*Đang tải/);
  assert.match(dashboard, /role="alert"[^]*@click="loadDashboard"[^]*Thử lại/);
  assert.match(dashboard, /v-else-if="loadState === 'ready'"/);
  assert.doesNotMatch(dashboard, /adminStore\.dashboard \|\| \{[^]*totalUsers: 0/);
});

test('dashboard accepts only current mounted success before charts', () => {
  assert.match(dashboard, /requestGeneration/);
  assert.match(dashboard, /stopped/);
  assert.match(dashboard, /request\.generation !== requestGeneration/);
  assert.match(dashboard, /loadState\.value = 'ready'[^]*buildCharts\(\)/);
  assert.doesNotMatch(dashboard, /watch\(\s*\(\) => adminStore\.dashboard/);
});

test('admin dashboard store records error and rethrows request failure', () => {
  const fetchDashboard = store.slice(store.indexOf('async function fetchDashboard()'), store.indexOf('async function fetchUsers()'));
  assert.match(fetchDashboard, /error\.value = ''/);
  assert.match(fetchDashboard, /catch \(e\)[^]*error\.value = e\.message[^]*throw e/);
  assert.doesNotMatch(fetchDashboard, /return null/);
});
