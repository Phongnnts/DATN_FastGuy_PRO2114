import assert from 'node:assert/strict';
import test from 'node:test';
import { readFile } from 'node:fs/promises';
import { loadInventoryReports } from '../src/utils/inventoryReports.js';

test('admin API exposes the contracted inventory report endpoints', async () => {
  const source = await readFile(new URL('../src/api/admin.js', import.meta.url), 'utf8');
  assert.match(source, /getInventoryItemLoss\(params\)[\s\S]*\/admin\/inventory\/reports\/item-loss/);
  assert.match(source, /getInventoryMenuCost\(\)[\s\S]*\/admin\/inventory\/reports\/menu-cost/);
  assert.match(source, /getMenuPerformanceReport\(params\)[\s\S]*\/admin\/inventory\/reports\/menu-performance/);
});

test('report loader preserves successful reports when another report fails', async () => {
  const result = await loadInventoryReports({
    summary: async () => ({ totalLossCost: 12000 }),
    itemLoss: async () => { throw new Error('Mất kết nối'); },
    menuCost: async () => [{ variantId: 7 }],
  });

  assert.deepEqual(result, {
    summary: { data: { totalLossCost: 12000 }, error: '' },
    itemLoss: { data: null, error: 'Mất kết nối' },
    menuCost: { data: [{ variantId: 7 }], error: '' },
  });
});
