import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const recipes = read('../src/views/admin/RecipesPage.vue');
const receipts = read('../src/views/admin/GoodsReceiptsPage.vue');
const counts = read('../src/views/admin/StockCountsPage.vue');
const categories = read('../src/views/admin/CategoriesPage.vue');

test('recipes present product variant and ingredient relationships before capacity evidence', () => {
  assert.match(recipes, /class="panel selector-panel recipe-context"/);
  assert.match(recipes, /class="ingredient-relationship-workspace"/);
  assert.ok(recipes.indexOf('ingredient-relationship-workspace') < recipes.indexOf('aria-label="Năng lực và chi phí"'));
  assert.match(recipes, /lineErrorMessage\(index\)/);
});

test('goods receipts preserve guided composition and immutable approved evidence', () => {
  assert.match(receipts, /class="panel guided-receipt-workflow"/);
  assert.match(receipts, /<legend><span>1<\/span> Thông tin phiếu/);
  assert.match(receipts, /<legend><span>2<\/span> Nguyên liệu nhận được/);
  assert.match(receipts, /<legend><span>3<\/span> Tóm tắt phiếu/);
  assert.match(receipts, /class="panel receipt-history immutable-receipt-evidence"/);
  assert.match(receipts, /phiếu không thể sửa lại/);
});

test('stock counts lead with variance and keep reasons adjacent to discrepant lines', () => {
  assert.match(counts, /class="workspace variance-review-workspace"/);
  assert.match(counts, /class="summary variance-summary"/);
  assert.match(counts, /v-if="hasVariance\(line\)"[\s\S]*Bắt buộc khi có chênh lệch/);
  assert.match(counts, /Phiếu đã duyệt và khóa chỉnh sửa/);
});

test('categories expose compact textual status and guarded actions', () => {
  assert.match(categories, /class="content-card category-management-workspace"/);
  assert.match(categories, /Đang sử dụng/);
  assert.match(categories, /Danh mục trống/);
  assert.match(categories, /:disabled="deletingId !== null" @click="remove\(category\)"/);
  assert.match(categories, /role="dialog" aria-modal="true"/);
});
