import assert from 'node:assert/strict';
import test from 'node:test';
import { createCapacityPageLoader, variantCapacityPresentation } from '../src/utils/adminStockOperations.js';

test('capacity loader requests only visible page variants and ignores stale completion', async () => {
  const pending = new Map();
  const state = { values: {}, loading: false, error: '' };
  const loader = createCapacityPageLoader(state, id => new Promise(resolve => pending.set(id, resolve)));
  const first = loader.load([{ variants: [{ variantId: 1 }, { variantId: 2 }] }]);
  const second = loader.load([{ variants: [{ variantId: 3 }] }]);
  pending.get(3)({ inventoryMode: 'INGREDIENT', availableServings: 4 });
  await second;
  pending.get(1)({ inventoryMode: 'INGREDIENT', availableServings: 9 });
  pending.get(2)({ inventoryMode: 'UNTRACKED' });
  await first;
  assert.deepEqual(state.values, { 3: { inventoryMode: 'INGREDIENT', availableServings: 4 } });
});

test('capacity presentation distinguishes recipe finished good untracked and suspended modes', () => {
  assert.deepEqual(variantCapacityPresentation({ inventoryMode: 'INGREDIENT', availableServings: 2, ingredients: [{ limiting: true, name: 'Thịt bò' }] }), { label: 'Chỉ còn 2 phần', detail: 'Giới hạn: Thịt bò', tone: 'warning' });
  assert.deepEqual(variantCapacityPresentation({ inventoryMode: 'FINISHED_GOOD', availableServings: 8 }), { label: 'Tồn thành phẩm: 8 phần', detail: '', tone: 'success' });
  assert.equal(variantCapacityPresentation({ inventoryMode: 'UNTRACKED' }).label, 'Không theo dõi tồn');
  assert.equal(variantCapacityPresentation({ inventoryMode: 'SUSPENDED' }).label, 'Tạm ngừng bán');
});

test('capacity presentation keeps loading unknown and error distinct', () => {
  assert.deepEqual(variantCapacityPresentation(undefined, true), { label: 'Đang tải năng lực bán', detail: '', tone: 'loading' });
  assert.deepEqual(variantCapacityPresentation(undefined, false), { label: 'Chưa có dữ liệu', detail: '', tone: 'secondary' });
  assert.deepEqual(variantCapacityPresentation({ error: 'Mất kết nối' }, false), { label: 'Không thể tải năng lực bán', detail: 'Mất kết nối', tone: 'danger' });
});
