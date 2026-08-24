import assert from 'node:assert/strict';
import test from 'node:test';
import {
  buildGoodsReceiptPayload,
  buildStockCountPayload,
  dateRangeForDays,
  goodsReceiptPreview,
  goodsReceiptTotal,
  recipeCost,
  recipeLinePresentation,
  stockCountProgress,
  stockCountSummary,
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
  assert.equal(goodsReceiptTotal([...form.items, { ...form.items[0], purchaseQuantity: '2' }]), 1200000);
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
  assert.equal(validateStockCount([{ ...lines[0], reasonCode: '' }]).items, 'Nhập lý do cho mặt hàng có chênh lệch');
  assert.deepEqual(stockCountProgress(lines), { counted: 2, total: 2, percent: 100 });
  assert.deepEqual(stockCountProgress([{ ...lines[0], actualQuantity: '' }, lines[1]]), { counted: 1, total: 2, percent: 50 });
  assert.deepEqual(stockCountSummary(lines, { 1: { averageUnitCost: 12000 }, 2: { averageUnitCost: 5000 } }), {
    shortageItemCount: 1, surplusItemCount: 0, lossCost: 18000,
  });
});

test('blank stock counts stay uncounted and never become variance or loss', () => {
  const lines = [
    { inventoryItemId: 1, theoreticalQuantity: 10, actualQuantity: '', reasonCode: '' },
    { inventoryItemId: 2, theoreticalQuantity: 4, actualQuantity: '5', reasonCode: 'DELIVERY' },
  ];
  assert.deepEqual(stockCountVariance(lines[0], 12000), { quantity: null, cost: null });
  assert.deepEqual(stockCountProgress(lines), { counted: 1, total: 2, percent: 50 });
  assert.deepEqual(stockCountSummary(lines, {
    1: { averageUnitCost: 12000, baseUnit: 'G' },
    2: { averageUnitCost: 5000, baseUnit: 'ML' },
  }), { shortageItemCount: 0, surplusItemCount: 1, lossCost: 0 });
  assert.equal(validateStockCount(lines).items, 'Nhập số lượng thực tế cho tất cả mặt hàng');
});

test('stock count reason is required only for counted nonzero variance', () => {
  const blank = { inventoryItemId: 1, theoreticalQuantity: 10, actualQuantity: '', reasonCode: '' };
  assert.equal(validateStockCount([blank]).items, 'Nhập số lượng thực tế cho tất cả mặt hàng');
  assert.deepEqual(validateStockCount([{ ...blank, actualQuantity: '10' }]), {});
  assert.equal(validateStockCount([{ ...blank, actualQuantity: '9' }]).items, 'Nhập lý do cho mặt hàng có chênh lệch');
  assert.deepEqual(validateStockCount([{ ...blank, actualQuantity: '9', reasonCode: 'DAMAGE' }]), {});
});

test('recipe cost uses inventory average costs and optional contracted variant price', () => {
  const recipe = { yieldQuantity: 2, items: [{ inventoryItemId: 1, quantity: 3 }, { inventoryItemId: 2, quantity: 1 }] };
  const inventory = { 1: { averageUnitCost: 1000 }, 2: { averageUnitCost: 5000 } };
  assert.deepEqual(recipeCost(recipe, inventory, 20000), { cost: 4000, foodCostPercent: 20 });
  assert.deepEqual(recipeCost(recipe, inventory), { cost: 4000, foodCostPercent: null });
  assert.deepEqual(recipeCost(recipe, { 1: inventory[1] }, 20000), { cost: null, foodCostPercent: null });
  assert.deepEqual(recipeLinePresentation({ inventoryItemId: 1, quantity: 3 }, inventory[1], 2), {
    amountPerServing: 1.5, availableQuantity: null, estimatedServings: null, costPerServing: 1500,
  });
  assert.deepEqual(recipeLinePresentation({ inventoryItemId: 1, quantity: 3 }, { ...inventory[1], availableQuantity: 15 }, 2), {
    amountPerServing: 1.5, availableQuantity: 15, estimatedServings: 10, costPerServing: 1500,
  });
});

test('dateRangeForDays returns inclusive local calendar ranges', () => {
  const now = new Date(2026, 7, 24, 15, 30);
  assert.deepEqual(dateRangeForDays(1, now), { fromDate: '2026-08-24', toDate: '2026-08-24' });
  assert.deepEqual(dateRangeForDays(7, now), { fromDate: '2026-08-18', toDate: '2026-08-24' });
});
