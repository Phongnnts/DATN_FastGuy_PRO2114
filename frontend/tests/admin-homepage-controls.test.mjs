import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/admin.js');
const editor = read('../src/views/admin/ProductEditorPage.vue');
const general = read('../src/components/admin/product-editor/ProductGeneralSection.vue');
const combo = read('../src/components/admin/product-editor/ProductComboSection.vue');
const order = read('../src/views/admin/OrderDetailPage.vue');

test('product editor exposes and submits the contracted product merchandising fields', () => {
  assert.match(general, />Món mới</);
  assert.match(general, /Không cay/);
  assert.match(general, /Cay nhẹ/);
  assert.match(general, /Cay vừa/);
  assert.match(general, /Rất cay/);
  assert.match(editor, /buildProductPayload/);
});

test('combo API preserves POST create and PUT update with exact bodies', () => {
  assert.match(api, /createCombo\(productId, data\)[\s\S]*client\.post\(`\/admin\/products\/\$\{productId\}\/combo`, data\)/);
  assert.match(api, /updateCombo\(productId, data\)[\s\S]*client\.put\(`\/admin\/products\/\$\{productId\}\/combo`, data\)/);
});

test('combo editor creates null combo and updates existing combo', () => {
  assert.match(combo, /QUICK_BREAK/);
  assert.match(combo, /OFFICE_LUNCH/);
  assert.match(combo, /STUDENT/);
  assert.match(combo, /GROUP/);
  assert.match(combo, /buildComboHomepagePayload/);
  assert.match(combo, /comboSaveMethod\(Boolean\(combo\.value\)\)/);
  assert.match(combo, /adminApi\.createCombo\(props\.productId, buildComboHomepagePayload/);
  assert.match(combo, /adminApi\[method\]\(props\.productId, payload\)/);
});

test('admin review feature stays dormant outside reachable order UI', () => {
  assert.match(api, /updateFeaturedReview\(orderId, featured\)[\s\S]*client\.put\(`\/admin\/orders\/\$\{orderId\}\/featured-review`, \{ featured \}\)/);
  assert.doesNotMatch(order, /updateFeaturedReview|order\.review|review-card/);
});
