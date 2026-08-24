import { expect, test } from '@playwright/test';

const apiPaths = [
  '/api/admin/inventory/items',
  '/api/admin/inventory/receipts',
  '/api/admin/inventory/stock-counts',
  '/api/admin/inventory/reports/summary',
  '/api/admin/inventory/reports/item-loss',
  '/api/admin/inventory/reports/menu-cost',
];

test('admin reviews receipts, stock counts, and inventory reports', async ({ page }) => {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ sessionToken }) => {
    localStorage.setItem('token', sessionToken);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }));
  }, { sessionToken: token });

  const errors = [];
  const successfulGets = new Set();
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const path = new URL(response.url()).pathname;
    if (response.request().method() === 'GET' && apiPaths.includes(path) && response.ok()) successfulGets.add(path);
  });

  const fulfill = data => ({ status: 'success', data });
  const item = { inventoryItemId: 7, inventoryCode: 'NL-007', name: 'Ức gà', baseUnit: 'kg', averageUnitCost: 85000 };
  await page.route('**/api/admin/inventory/items', route => route.fulfill({ json: fulfill([item]) }));
  await page.route('**/api/admin/inventory/receipts', route => route.fulfill({ json: fulfill([{ goodsReceiptId: 21, supplierName: 'Thực phẩm Sạch', invoiceNumber: 'HD-2026-21', receivedAt: '2026-08-24T08:30:00', status: 'DRAFT', items: [{ inventoryItemId: 7 }] }]) }));
  await page.route('**/api/admin/inventory/stock-counts', route => route.fulfill({ json: fulfill([{ stockCountId: 31, countDate: '2026-08-24', frequency: 'DAILY', status: 'DRAFT', items: [{ inventoryItemId: 7 }] }]) }));
  await page.route('**/api/admin/inventory/reports/summary?*', route => route.fulfill({ json: fulfill({ purchaseCost: 1000000, consumptionCost: 600000, wasteCost: 50000, stockCountLossCost: 25000, stockCountGainCost: 10000, totalLossCost: 75000, endingInventoryValue: 335000, lossRate: 7.5 }) }));
  await page.route('**/api/admin/inventory/reports/item-loss?*', route => route.fulfill({ json: fulfill([{ inventoryItemId: 7, inventoryCode: 'NL-007', name: 'Ức gà', wasteQuantity: 0.5, wasteCost: 50000, stockCountLossQuantity: 0.25, stockCountLossCost: 25000, totalLossCost: 75000 }]) }));
  await page.route('**/api/admin/inventory/reports/menu-cost', route => route.fulfill({ json: fulfill([{ variantId: 11, sku: 'GA-RAN-M', variantName: 'Gà rán cỡ M', yieldQuantity: 4, recipeCost: 120000, costPerServing: 30000 }]) }));

  await page.goto('/admin/inventory/receipts');
  await expect(page.getByRole('heading', { name: 'Ghi nhận hàng vừa nhận' })).toBeVisible();
  await expect(page.getByText('Thực phẩm Sạch')).toBeVisible();
  await expect(page.getByText('HD-2026-21')).toBeVisible();

  await page.goto('/admin/inventory/stock-counts');
  await expect(page.getByRole('heading', { name: 'Đếm và đối chiếu tồn kho' })).toBeVisible();
  await expect(page.getByText('#31 · 2026-08-24')).toBeVisible();
  await expect(page.getByText('Hàng ngày · 1 mặt hàng')).toBeVisible();

  await page.goto('/admin/inventory/reports');
  await expect(page.getByRole('heading', { name: 'Báo cáo giá trị kho' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Thất thoát theo mặt hàng' })).toBeVisible();
  await expect(page.getByText('Ức gà')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Giá vốn công thức món' })).toBeVisible();
  await expect(page.getByText('Gà rán cỡ M')).toBeVisible();

  await expect.poll(() => [...successfulGets].sort()).toEqual([...apiPaths].sort());
  expect(errors).toEqual([]);
});
