import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/admin.js');
const editor = read('../src/views/admin/ProductEditorPage.vue');
const general = read('../src/components/admin/product-editor/ProductGeneralSection.vue');
const order = read('../src/views/admin/OrderDetailPage.vue');

test('product editor exposes and submits the contracted product merchandising fields', () => {
  assert.match(general, />Món mới</);
  assert.match(general, /Không cay/);
  assert.match(general, /Cay nhẹ/);
  assert.match(general, /Cay vừa/);
  assert.match(general, /Rất cay/);
  assert.match(editor, /buildProductPayload/);
});

test('combo API and editor controls stay removed', () => {
  assert.doesNotMatch(api, /createCombo|updateCombo|deleteCombo/);
  assert.doesNotMatch(editor, /ProductComboSection|activeSection === 'combo'/);
});

test('admin review feature stays dormant outside reachable order UI', () => {
  assert.match(api, /updateFeaturedReview\(orderId, featured\)[\s\S]*client\.put\(`\/admin\/orders\/\$\{orderId\}\/featured-review`, \{ featured \}\)/);
  assert.doesNotMatch(order, /updateFeaturedReview|order\.review|review-card/);
});
