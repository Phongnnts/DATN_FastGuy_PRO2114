import { expect, test } from '@playwright/test';

const apiPaths = [
  '/api/admin/inventory/items',
  '/api/admin/inventory/analytics',
  '/api/admin/inventory/receipts',
  '/api/admin/inventory/stock-counts',
  '/api/admin/inventory/reports/menu-performance',
];

test('admin reviews receipts, stock counts, and inventory reports', async ({ page }, testInfo) => {
  const today = new Date().toLocaleDateString('en-CA');
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ sessionToken }) => {
    localStorage.setItem('token', sessionToken);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }));
  }, { sessionToken: token });

  const errors = [];
  const successfulGets = new Set();
  const receiptWrites = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('response', response => {
    const path = new URL(response.url()).pathname;
    if (response.request().method() === 'GET' && apiPaths.includes(path) && response.ok()) successfulGets.add(path);
  });

  const fulfill = data => ({ status: 'success', data });
  const item = { inventoryItemId: 7, inventoryCode: 'NL-007', name: 'Ức gà', baseUnit: 'G', onHandQuantity: 70000, averageUnitCost: 85 };
  const finishedGood = { inventoryItemId: 8, inventoryCode: 'INV-000001', name: 'Classic Beef Burger / Mặc định', itemType: 'FINISHED_GOOD', active: true, baseUnit: 'PIECE' };
  item.itemType = 'INGREDIENT'; item.active = true;
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }) }));
  await page.route('**/api/admin/dashboard', route => route.fulfill({ json: fulfill({ unavailableVariantCount: 0 }) }));
  await page.route('**/api/admin/products*', route => route.fulfill({ json: fulfill([]) }));
  await page.route('**/api/admin/inventory/transactions*', route => route.fulfill({ json: fulfill({ items: [], totalItems: 0, totalPages: 0 }) }));
  await page.route('**/api/admin/inventory/items', route => route.fulfill({ json: fulfill([item, finishedGood]) }));
  await page.route('**/api/admin/inventory/analytics*', route => route.fulfill({ json: fulfill({ period: { fromDate: '2026-08-05', toDate: '2026-09-03', granularity: 'DAY' }, comparisonPeriod: { fromDate: '2026-07-06', toDate: '2026-08-04', granularity: 'DAY' }, kpis: { itemCount: 2, lowStockCount: 1, outOfStockCount: 0, inventoryValue: 5950000 }, previousKpis: { itemCount: 2, lowStockCount: 0, outOfStockCount: 0, inventoryValue: 5500000 }, series: [{ date: '2026-09-03', inventoryValue: 5950000, receiptValue: 600000, consumptionValue: 250000, wasteValue: 10000, adjustmentLossValue: 0, adjustmentGainValue: 0 }], health: { healthyCount: 1, attentionCount: 0, lowStockCount: 1, outOfStockCount: 0 }, attentionItems: [{ inventoryItemId: 7, inventoryCode: 'NL-007', name: 'Ức gà', baseUnit: 'G', onHandQuantity: 70000, availableQuantity: 70000, minimumQuantity: 80000, healthRatio: 0.875, healthState: 'LOW' }] }) }));
  await page.route('**/api/admin/inventory/receipts/22/approve', route => { receiptWrites.push({ method: route.request().method(), path: '/approve' }); return route.fulfill({ json: fulfill({ goodsReceiptId: 22, status: 'APPROVED', items: [] }) }); });
  await page.route('**/api/admin/inventory/receipts', route => {
    if (route.request().method() === 'POST') {
      receiptWrites.push({ method: 'POST', body: route.request().postDataJSON() });
      return route.fulfill({ status: 201, json: fulfill({ goodsReceiptId: 22, supplierName: 'Không khai báo', invoiceNumber: null, receivedAt: '2026-08-24T08:30:00', status: 'DRAFT', items: [{ inventoryItemId: 7, purchaseQuantity: 3, purchaseUnit: 'thùng', conversionFactor: 10000, baseQuantity: 30000, purchaseUnitPrice: 1800000, lineTotal: 5400000 }] }) });
    }
    return route.fulfill({ json: fulfill([{ goodsReceiptId: 21, supplierName: 'Thực phẩm Sạch', invoiceNumber: 'HD-2026-21', receivedAt: `${today}T08:30:00`, status: 'DRAFT', items: [{ inventoryItemId: 7, lineTotal: 900000 }] }, { goodsReceiptId: 20, supplierName: 'Kho trung tâm', invoiceNumber: null, receivedAt: `${today}T07:30:00`, status: 'APPROVED', items: [{ inventoryItemId: 7, lineTotal: 600000 }] }]) });
  });
  await page.route('**/api/admin/inventory/stock-counts', route => route.fulfill({ json: fulfill([{ stockCountId: 31, countDate: '2026-08-24', frequency: 'DAILY', status: 'DRAFT', items: [{ inventoryItemId: 7 }] }]) }));
  await page.route('**/api/admin/inventory/reports/menu-performance?*', route => route.fulfill({ json: fulfill({ grossRevenue: 1000000, allocatedDiscount: 50000, netRevenue: 950000, cost: 400000, costComplete: true, missingCostItemCount: 0, grossProfit: 550000, foodCostPercent: 42.11, grossMarginPercent: 57.89, items: [{ variantId: 11, productName: 'Gà rán', variantName: 'Cỡ M', quantitySold: 10, grossRevenue: 1000000, allocatedDiscount: 50000, netRevenue: 950000, cost: 400000, costComplete: true, grossProfit: 550000, foodCostPercent: 42.11, grossMarginPercent: 57.89 }] }) }));

  await page.goto('/admin/inventory');
  await expect(page.getByRole('region', { name: 'Chỉ số tồn kho' })).toContainText('5.950.000 ₫');
  await expect(page.getByRole('region', { name: 'Xu hướng tồn kho' })).toBeVisible();

  await page.goto('/admin/inventory/receipts');
  await expect(page.getByRole('heading', { name: 'Nhập hàng' })).toBeVisible();
  await expect(page.getByText('Thực phẩm Sạch')).toBeVisible();
  await expect(page.getByText('HD-2026-21')).toBeVisible();
  const ingredientSelect = page.getByRole('combobox', { name: /^Nguyên liệu/ });
  await expect(ingredientSelect.getByRole('option', { name: /Classic Beef Burger/ })).toHaveCount(0);
  await ingredientSelect.selectOption('7');
  await expect(page.getByRole('region', { name: 'Tổng hợp phiếu nhập' })).toContainText('2');
  await expect(page.getByRole('region', { name: 'Tổng hợp phiếu nhập' })).toContainText('600.000 ₫');
  await page.getByLabel('Số lượng nhận').fill('3');
  await page.getByLabel('Đơn vị mua').selectOption('kg');
  await page.getByLabel('Giá mỗi kg').fill('200000');
  await expect(page.getByText('Quy đổi tự động: 1 kg = 1.000 g trong hệ thống')).toBeVisible();
  await expect(page.getByText('+3 kg', { exact: true })).toBeVisible();
  await expect(page.getByText('600.000 ₫', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('200.000 ₫/kg', { exact: true })).toBeVisible();
  await expect(page.getByText('Mỗi đơn vị mua có')).toHaveCount(0);
  await page.getByLabel('Đơn vị mua').selectOption('thùng');
  await page.getByRole('spinbutton', { name: '1 thùng chứa' }).fill('10');
  await page.getByLabel('Đơn vị bên trong').selectOption('kg');
  await page.getByLabel('Giá mỗi thùng').fill('1800000');
  await expect(page.getByText('3 thùng × 10 kg = 30 kg')).toBeVisible();
  await expect(page.getByText('+30 kg', { exact: true })).toBeVisible();
  await expect(page.getByText('5.400.000 ₫', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('180.000 ₫/kg', { exact: true })).toBeVisible();
  const reviewButton=page.getByRole('button', { name: 'Kiểm tra & duyệt' });
  if(testInfo.project.name==='mobile-chrome'){await reviewButton.focus();await page.keyboard.press('Enter');}else await reviewButton.click();
  const approval = page.getByRole('dialog', { name: 'Duyệt phiếu nhập #22?' });
  await expect(approval).toContainText('Ức gà: +30 kg');
  await expect(approval).toContainText('5.400.000 ₫');
  const approveButton=approval.getByRole('button', { name: 'Duyệt phiếu' });
  if(testInfo.project.name==='mobile-chrome'){await approveButton.focus();await page.keyboard.press('Enter');}else await approveButton.click();
  await expect(approval).toHaveCount(0);
  expect(receiptWrites).toEqual([
    { method: 'POST', body: { supplierName: '', invoiceNumber: null, receivedAt: expect.any(String), items: [{ inventoryItemId: 7, purchaseQuantity: 3, purchaseUnit: 'thùng', conversionFactor: 10000, purchaseUnitPrice: 1800000 }] } },
    { method: 'POST', path: '/approve' },
  ]);

  await page.goto('/admin/inventory/stock-counts');
  await expect(page.getByRole('main').getByRole('heading', { name: 'Kiểm kê kho' })).toBeVisible();
  await expect(page.getByText('#31 · 2026-08-24')).toBeVisible();
  await expect(page.getByText('Hàng ngày · 1 mặt hàng')).toBeVisible();

  await page.goto('/admin/inventory/reports');
  await expect(page.getByRole('heading', { name: 'Báo cáo hiệu quả món ăn' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Chi tiết theo món' })).toBeVisible();
  await expect(page.getByText('Gà rán', { exact: true })).toBeVisible();
  await expect(page.getByText('Cỡ M', { exact: true })).toBeVisible();

  await expect.poll(() => [...successfulGets].sort()).toEqual([...apiPaths].sort());
  expect(errors).toEqual([]);
});
