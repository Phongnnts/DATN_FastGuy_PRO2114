import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildGoodsReceiptPayload,
  buildStockCountPayload,
  dateRangeForDays,
  goodsReceiptPreview,
  recipeCost,
  stockCountVariance,
  validateGoodsReceipt,
  validateStockCount,
} from '../src/utils/inventoryOperations.js';

test('goods receipt helpers validate lines and build the contracted payload', () => {
  const form = {
    supplierName: ' Nhà cung cấp A ', invoiceNumber: ' HD-01 ', receivedAt: '2026-08-24T10:30',
    items: [{ inventoryItemId: '2', purchaseQuantity: '3', purchaseUnit: 'thùng', conversionFactor: '12', purchaseUnitPrice: '240000' }],
  };
  assert.deepEqual(validateGoodsReceipt(form), {});
  assert.deepEqual(buildGoodsReceiptPayload(form), {
    supplierName: 'Nhà cung cấp A', invoiceNumber: 'HD-01', receivedAt: '2026-08-24T10:30',
    items: [{ inventoryItemId: 2, purchaseQuantity: 3, purchaseUnit: 'thùng', conversionFactor: 12, purchaseUnitPrice: 240000 }],
  });
  assert.deepEqual(goodsReceiptPreview(form.items[0]), { baseQuantity: 36, lineTotal: 720000, baseUnitCost: 20000 });
  assert.equal(validateGoodsReceipt({ ...form, items: [...form.items, { ...form.items[0] }] }).items, 'Mỗi mặt hàng chỉ được nhập một lần');
});

test('stock count helpers require every actual quantity and preserve theoretical snapshots', () => {
  const lines = [
    { inventoryItemId: 1, theoreticalQuantity: 10, actualQuantity: '8.5', reasonCode: 'DAMAGE', note: ' Hỏng ' },
    { inventoryItemId: 2, theoreticalQuantity: 4, actualQuantity: '4', reasonCode: '', note: '' },
  ];
  assert.deepEqual(validateStockCount(lines), {});
  assert.deepEqual(buildStockCountPayload(lines), { items: [
    { inventoryItemId: 1, theoreticalQuantity: 10, actualQuantity: 8.5, reasonCode: 'DAMAGE', note: 'Hỏng' },
    { inventoryItemId: 2, theoreticalQuantity: 4, actualQuantity: 4, reasonCode: null, note: null },
  ] });
  assert.deepEqual(stockCountVariance(lines[0], 12000), { quantity: -1.5, cost: -18000 });
  assert.equal(validateStockCount([{ ...lines[0], actualQuantity: '' }]).items, 'Nhập số lượng thực tế cho tất cả mặt hàng');
});

test('recipe cost uses inventory average costs and optional contracted variant price', () => {
  const recipe = { yieldQuantity: 2, items: [{ inventoryItemId: 1, quantity: 3 }, { inventoryItemId: 2, quantity: 1 }] };
  const inventory = { 1: { averageUnitCost: 1000 }, 2: { averageUnitCost: 5000 } };
  assert.deepEqual(recipeCost(recipe, inventory, 20000), { cost: 4000, foodCostPercent: 20 });
  assert.deepEqual(recipeCost(recipe, inventory), { cost: 4000, foodCostPercent: null });
  assert.deepEqual(recipeCost(recipe, { 1: inventory[1] }, 20000), { cost: null, foodCostPercent: null });
});

test('dateRangeForDays returns inclusive local calendar ranges', () => {
  const now = new Date(2026, 7, 24, 15, 30);
  assert.deepEqual(dateRangeForDays(1, now), { fromDate: '2026-08-24', toDate: '2026-08-24' });
  assert.deepEqual(dateRangeForDays(7, now), { fromDate: '2026-08-18', toDate: '2026-08-24' });
});
