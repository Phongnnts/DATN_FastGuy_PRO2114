import { expect, test } from '@playwright/test';

const ok = (data) => ({ status: 'success', data });

async function mockAdmin(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }));
  }, { value: token });
  await page.route('**/api/admin/dashboard*', route => route.fulfill({ json: ok({ customerCount: 0, activeProductCount: 0, totalOrders: 0, totalRevenue: 0, operationalOrderCount: 0, operationalCompletedCount: 0, completionRate: 0, ordersByStatus: {}, pendingCodAmount: 0, pendingCodCount: 0, ordersToday: 0, revenueToday: 0, revenueByMonth: [], topProducts: [], lowStockThreshold: 5, outOfStockSkuCount: 0, lowStockSkuCount: 0 }) }));
  await page.route('**/api/admin/inventory/items', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/inventory/transactions*', route => route.fulfill({ json: ok({ items: [], totalItems: 0 }) }));
  await page.route('**/api/admin/products*', route => route.fulfill({ json: ok([]) }));
  await page.route('**/api/admin/reports/full*', route => route.fulfill({ json: ok({ revenueByDay: [], monthlyFinancialTrend: [], ordersByStatus: [], topProducts: [], revenueByCategory: [], paymentMethodStats: [], revenueByHour: [], performanceByWeekday: [], refundTrend: [], exceptionReasons: [] }) }));
  await page.route('**/api/admin/reports/operating-profit*', route => route.fulfill({ json: ok({ costComplete: true, netRevenue: 0, cogs: 0, grossProfit: 0, operatingExpenses: 0, profitBeforeDepreciation: 0, depreciation: 0, operatingProfit: 0, missingCostItemCount: 0 }) }));
  await page.route('**/api/admin/inventory/reports/menu-performance*', route => route.fulfill({ json: ok({ netRevenue: 0, cost: 0, grossProfit: 0, foodCostPercent: 0, grossMarginPercent: 0, costComplete: true, missingCostItemCount: 0, items: [] }) }));
  await page.route('**/api/admin/operating-expenses*', route => route.fulfill({ json: ok([]) }));
}

function captureErrors(page) {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  return errors;
}

const isMobileProject = testInfo => testInfo.project.name.includes('mobile');

test('admin shell exposes one identity and current page title', async ({ page }, testInfo) => {
  await mockAdmin(page);
  const errors = captureErrors(page);
  await page.goto('/admin');

  const banner = page.getByRole('banner');
  const sidebar = page.locator('#admin-sidebar');
  const trigger = page.getByRole('button', { name: 'Mở menu quản trị' });
  await expect(page.getByText('FastGuy Admin', { exact: true })).toHaveCount(1);
  await expect(page.getByText('FastGuy Admin', { exact: true })).toBeVisible();
  await expect(page.getByText('Admin', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Trung tâm quản trị', { exact: true })).toHaveCount(0);
  await expect(page.getByText('Quản trị', { exact: true })).toHaveCount(0);
  await expect(banner.getByRole('heading', { level: 1, name: 'Tổng quan quản trị' })).toBeVisible();
  await expect(page.getByLabel('Mở website FastGuy')).toBeVisible();
  await expect(page.getByText('QV', { exact: true })).toHaveCount(1);

  await page.goto('/admin/inventory');
  await expect(banner.getByRole('heading', { level: 1, name: 'Tổng quan kho' })).toBeVisible();
  if (isMobileProject(testInfo)) await expect(trigger).toBeVisible();
  else {
    await expect(trigger).toBeHidden();
    await expect(sidebar).not.toHaveAttribute('role', 'dialog');
    await expect(sidebar).not.toHaveAttribute('aria-modal', 'true');
  }
  expect(errors).toEqual([]);
});

test('mobile drawer closes with Escape and restores trigger focus', async ({ page }, testInfo) => {
  test.skip(!isMobileProject(testInfo), 'Mobile drawer gate');
  await mockAdmin(page);
  const errors = captureErrors(page);
  await page.goto('/admin');

  const trigger = page.getByRole('button', { name: 'Mở menu quản trị', includeHidden: true });
  const main = page.locator('.main-content');
  const box = await trigger.boundingBox();
  expect(box.width).toBeGreaterThanOrEqual(44);
  expect(box.height).toBeGreaterThanOrEqual(44);
  await trigger.click();
  await expect(trigger).toHaveAttribute('aria-expanded', 'true');
  await expect(page.locator('#admin-sidebar')).toHaveAttribute('role', 'dialog');
  await expect(page.locator('#admin-sidebar')).toHaveAttribute('aria-modal', 'true');
  await expect(page.getByRole('button', { name: 'Đóng điều hướng quản trị' })).toBeFocused();
  await expect(page.locator('body')).toHaveCSS('overflow', 'hidden');
  await expect(main).toHaveAttribute('inert', '');
  await expect(main).toHaveAttribute('aria-hidden', 'true');

  await page.keyboard.press('Escape');
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');
  await expect(main).not.toHaveAttribute('inert', '');
  await expect(main).not.toHaveAttribute('aria-hidden', 'true');
  expect(errors).toEqual([]);
});

test('mobile drawer contains focus and closes through overlay and route change', async ({ page }, testInfo) => {
  test.skip(!isMobileProject(testInfo), 'Mobile drawer gate');
  await mockAdmin(page);
  const errors = captureErrors(page);
  await page.goto('/admin');

  const trigger = page.getByRole('button', { name: 'Mở menu quản trị' });
  const close = page.getByRole('button', { name: 'Đóng điều hướng quản trị' });
  const lastLink = page.getByRole('link', { name: 'Cài đặt' });
  await trigger.click();
  await expect(close).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(lastLink).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(close).toBeFocused();

  await page.getByRole('button', { name: 'Đóng menu quản trị' }).click({ position: { x: 300, y: 300 } });
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');

  await trigger.click();
  const inventoryLink = page.getByRole('link', { name: 'Tồn kho' });
  await inventoryLink.scrollIntoViewIfNeeded();
  await inventoryLink.click();
  await expect(page).toHaveURL(/\/admin\/inventory$/);
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');

  await trigger.click();
  await page.goBack();
  await expect(page).toHaveURL(/\/admin$/);
  await expect(trigger).toHaveAttribute('aria-expanded', 'false');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');
  expect(errors).toEqual([]);
});

test('tablet uses the accessible drawer at 1024px', async ({ page }, testInfo) => {
  test.skip(isMobileProject(testInfo), 'Explicit tablet viewport runs once');
  await page.setViewportSize({ width: 1024, height: 768 });
  await mockAdmin(page);
  const errors = captureErrors(page);
  await page.goto('/admin');

  const trigger = page.getByRole('button', { name: 'Mở menu quản trị' });
  const sidebar = page.locator('#admin-sidebar');
  await expect(trigger).toBeVisible();
  await expect(page.locator('.main-content')).toHaveCSS('margin-left', '0px');
  await expect(sidebar).not.toBeInViewport();
  await trigger.click();
  await expect(sidebar).toBeInViewport();
  await expect(sidebar).toHaveAttribute('role', 'dialog');
  await expect(sidebar).toHaveAttribute('aria-modal', 'true');
  await expect(page.getByRole('button', { name: 'Đóng điều hướng quản trị' })).toBeFocused();
  await expect(page.locator('body')).toHaveCSS('overflow', 'hidden');
  await page.keyboard.press('Escape');
  await expect(trigger).toBeFocused();
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');
  expect(errors).toEqual([]);
});

test('R1 inventory and report tabs preserve history, redirects and keyboard access', async ({ page }) => {
  await mockAdmin(page);
  const errors = captureErrors(page);

  await page.goto('/admin/inventory');
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toHaveAttribute('aria-selected', 'true');
  await page.getByRole('tab', { name: 'Lịch sử biến động' }).click();
  await expect(page).toHaveURL(/\/admin\/inventory\?tab=history$/);
  await expect(page.getByRole('tabpanel', { name: 'Lịch sử biến động' })).toBeVisible();
  await page.goBack();
  await expect(page).toHaveURL(/\/admin\/inventory$/);
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toHaveAttribute('aria-selected', 'true');
  await page.goForward();
  await expect(page.getByRole('tab', { name: 'Lịch sử biến động' })).toHaveAttribute('aria-selected', 'true');
  await page.reload();
  await expect(page.getByRole('tab', { name: 'Lịch sử biến động' })).toHaveAttribute('aria-selected', 'true');

  await page.goto('/admin/inventory/ledger');
  await expect(page).toHaveURL(/\/admin\/inventory\?tab=history$/);
  await page.goto('/admin/inventory/reports');
  await expect(page).toHaveURL(/\/admin\/reports\?tab=menu$/);
  await expect(page.getByRole('tab', { name: 'Hiệu quả món' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('button', { name: 'Xuất CSV' })).toHaveCount(0);
  await page.getByRole('tab', { name: 'Hiệu quả món' }).press('ArrowRight');
  await expect(page).toHaveURL(/\/admin\/reports\?tab=expenses$/);
  await expect(page.getByRole('tab', { name: 'Chi phí' })).toBeFocused();
  await page.goto('/admin/operating-expenses');
  await expect(page).toHaveURL(/\/admin\/reports\?tab=expenses$/);
  expect(errors).toEqual([]);
});

test('R1 mobile sidebar exposes task groups without hidden legacy entries', async ({ page }, testInfo) => {
  test.skip(!testInfo.project.name.includes('mobile'), 'Mobile navigation gate');
  await mockAdmin(page);
  const errors = captureErrors(page);
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  for (const group of ['Tổng quan', 'Vận hành', 'Bán hàng', 'Nhân sự', 'Kho hàng', 'Báo cáo', 'Hệ thống']) await expect(page.getByRole('heading', { name: group, exact: true })).toBeVisible();
  for (const label of ['Tài sản cố định', 'Lịch sử kho', 'Báo cáo theo món', 'Chi phí vận hành']) await expect(page.getByRole('link', { name: label })).toHaveCount(0);
  const inventoryLink = page.getByRole('link', { name: 'Tồn kho' });
  await inventoryLink.focus();
  await inventoryLink.press('Enter');
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toBeVisible();
  expect(errors).toEqual([]);
});
