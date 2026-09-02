import { expect, test } from '@playwright/test';

const ok = (data) => ({ status: 'success', data });

async function mockAdmin(page, user = { id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value, storedUser }) => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify(storedUser));
  }, { value: token, storedUser: user });
  const fixtures = new Map([
    ['/api/auth/profile', ok({ ...user, userId: user.id, avatarUrl: null })],
    ['/api/admin/dashboard', ok({ netCashRevenueToday: 0, operationalOrderCountToday: 0, aovToday: 0, completionRateToday: 0, activeOrderCount: 0, activeOrdersByStatus: {}, revenueLast7Days: [], topProductsLast7Days: [], lowStockProducts: [], attentionItems: [], sectionAvailability: { financial: 'AVAILABLE', orders: 'AVAILABLE', refunds: 'AVAILABLE', cod: 'AVAILABLE', inventory: 'AVAILABLE', staffing: 'AVAILABLE' } })],
    ['/api/admin/orders', ok({ items: [], pagination: { page: 1, pageSize: 8, totalItems: 0, totalPages: 0 } })],
    ['/api/admin/inventory/items', ok([])],
    ['/api/admin/inventory/transactions', ok({ items: [], totalItems: 0 })],
    ['/api/admin/products', ok([])],
    ['/api/admin/reports/full', ok({ revenueByDay: [], monthlyFinancialTrend: [], ordersByStatus: [], topProducts: [], revenueByCategory: [], paymentMethodStats: [], revenueByHour: [], performanceByWeekday: [], refundTrend: [], exceptionReasons: [] })],
    ['/api/admin/reports/operating-profit', ok({ costComplete: true, netRevenue: 0, cogs: 0, grossProfit: 0, operatingExpenses: 0, profitBeforeDepreciation: 0, depreciation: 0, operatingProfit: 0, missingCostItemCount: 0 })],
    ['/api/admin/inventory/reports/menu-performance', ok({ netRevenue: 0, cost: 0, grossProfit: 0, foodCostPercent: 0, grossMarginPercent: 0, costComplete: true, missingCostItemCount: 0, items: [] })],
    ['/api/admin/operating-expenses', ok([])],
    ['/api/shifts/current', ok({ state: 'NOT_CHECKED_IN', shift: null })],
    ['/api/shifts/week', ok({ shifts: [] })],
    ['/api/shifts/attendance', ok([])],
    ['/api/staff/orders/ownership-count', ok({ activeOwnershipCount: 0 })],
  ]);
  await page.route('**/*', route => {
    const path = new URL(route.request().url()).pathname;
    if (!path.startsWith('/api/')) return route.continue();
    const fixture = fixtures.get(path);
    if (fixture) return route.fulfill({ status: 200, json: fixture });
    return route.fulfill({ status: 501, json: { status: 'error', message: `Missing E2E fixture for ${path}` } });
  });
}

function captureTraffic(page) {
  const errors = [];
  const successfulApiPaths = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('requestfailed', request => errors.push(`requestfailed ${request.method()} ${request.url()}: ${request.failure()?.errorText || 'unknown'}`));
  page.on('response', response => {
    if (response.status() < 200 || response.status() >= 300) {
      errors.push(`HTTP ${response.status()} ${response.request().method()} ${response.url()}`);
      return;
    }
    if (response.url().includes('/api/')) successfulApiPaths.push(new URL(response.url()).pathname);
  });
  return { errors, successfulApiPaths };
}

const isMobileProject = testInfo => testInfo.project.name.includes('mobile');

test('admin shell exposes one identity and current page title', async ({ page }, testInfo) => {
  await mockAdmin(page);
  const { errors, successfulApiPaths } = captureTraffic(page);
  await page.goto('/admin');

  const banner = page.getByRole('banner');
  const sidebar = page.locator('#admin-sidebar');
  const trigger = page.getByRole('button', { name: 'Mở menu quản trị' });
  await expect(page.getByText('FastGuy', { exact: true })).toHaveCount(1);
  await expect(page.getByText('Operations Admin', { exact: true })).toBeVisible();
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
    await expect(sidebar).toHaveCSS('width', '232px');
    await expect(sidebar).not.toHaveAttribute('role', 'dialog');
    await expect(sidebar).not.toHaveAttribute('aria-modal', 'true');
  }
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  await expect.poll(() => successfulApiPaths.includes('/api/admin/dashboard')).toBe(true);
  await expect.poll(() => successfulApiPaths.includes('/api/admin/inventory/items')).toBe(true);
  await expect.poll(() => successfulApiPaths.includes('/api/admin/inventory/transactions')).toBe(true);
  await expect.poll(() => successfulApiPaths.includes('/api/admin/products')).toBe(true);
  expect(errors).toEqual([]);
});

test('mobile drawer closes with Escape and restores trigger focus', async ({ page }, testInfo) => {
  test.skip(!isMobileProject(testInfo), 'Mobile drawer gate');
  await mockAdmin(page);
  const { errors, successfulApiPaths } = captureTraffic(page);
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
  await expect(main).not.toHaveAttribute('aria-hidden', 'true');

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
  const { errors, successfulApiPaths } = captureTraffic(page);
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
  const { errors, successfulApiPaths } = captureTraffic(page);
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

test('resize transfers focus from open drawer to a visible desktop control', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await mockAdmin(page);
  const { errors } = captureTraffic(page);
  await page.goto('/admin');

  await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  await expect(page.getByRole('button', { name: 'Đóng điều hướng quản trị' })).toBeFocused();
  await page.setViewportSize({ width: 1280, height: 800 });

  await expect(page.getByLabel('Mở website FastGuy')).toBeFocused();
  await expect(page.getByRole('button', { name: 'Mở menu quản trị' })).toBeHidden();
  await expect(page.locator('#admin-sidebar')).not.toHaveAttribute('role', 'dialog');
  await expect(page.locator('body')).not.toHaveCSS('overflow', 'hidden');
  expect(errors).toEqual([]);
});

test('resize transfers focus from desktop sidebar to the drawer trigger', async ({ page }) => {
  await page.setViewportSize({ width: 1280, height: 800 });
  await mockAdmin(page);
  const { errors } = captureTraffic(page);
  await page.goto('/admin');

  await page.getByRole('link', { name: 'Dashboard', exact: true }).focus();
  await expect(page.getByRole('link', { name: 'Dashboard', exact: true })).toBeFocused();
  await page.setViewportSize({ width: 1024, height: 768 });

  await expect(page.getByRole('button', { name: 'Mở menu quản trị' })).toBeFocused();
  await expect(page.locator('#admin-sidebar')).toHaveAttribute('inert', '');
  await expect(page.locator('.main-content')).not.toHaveAttribute('inert', '');
  expect(errors).toEqual([]);
});

test('768px keeps tablet spacing while retaining the drawer', async ({ page }) => {
  await page.setViewportSize({ width: 768, height: 1024 });
  await mockAdmin(page);
  const { errors } = captureTraffic(page);
  await page.goto('/admin');

  await expect(page.getByRole('button', { name: 'Mở menu quản trị' })).toBeVisible();
  await expect(page.locator('.topbar')).toHaveCSS('padding-left', '32px');
  await expect(page.locator('.page-content')).toHaveCSS('padding-left', '32px');
  expect(errors).toEqual([]);
});

test('staff keeps its coherent mobile shell at 768px', async ({ page }) => {
  await page.setViewportSize({ width: 768, height: 1024 });
  await mockAdmin(page, { id: 2, fullName: 'Nhân viên', role: 'STAFF', email: 'staff@example.com' });
  const { errors } = captureTraffic(page);
  await page.goto('/staff/shifts');

  const trigger = page.getByRole('button', { name: 'Mở menu nhân viên' });
  const sidebar = page.locator('#staff-sidebar');
  await expect(trigger).toBeVisible();
  await expect(page.locator('.main-content')).toHaveCSS('margin-left', '0px');
  expect((await sidebar.boundingBox()).x).toBeLessThan(0);
  await trigger.click();
  await expect.poll(async () => (await sidebar.boundingBox()).x).toBe(0);
  await expect(page.getByRole('button', { name: 'Đóng menu nhân viên' })).toBeVisible();
  expect(errors).toEqual([]);
});

test('R1 inventory and report tabs preserve history, redirects and keyboard access', async ({ page }) => {
  await mockAdmin(page);
  const { errors, successfulApiPaths } = captureTraffic(page);

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
  const { errors, successfulApiPaths } = captureTraffic(page);
  await page.goto('/admin');
  await page.getByRole('button', { name: 'Mở menu quản trị' }).click();
  for (const group of ['Vận hành', 'Kho', 'Tài chính', 'Marketing', 'Nhân sự', 'Cấu hình']) await expect(page.getByRole('heading', { name: group, exact: true })).toBeVisible();
  for (const label of ['Tài sản cố định', 'Lịch sử kho', 'Báo cáo theo món', 'Chi phí vận hành']) await expect(page.getByRole('link', { name: label })).toHaveCount(0);
  const inventoryLink = page.getByRole('link', { name: 'Tồn kho' });
  await inventoryLink.focus();
  await inventoryLink.press('Enter');
  await expect(page.getByRole('tab', { name: 'Tồn hiện tại' })).toBeVisible();
  expect(errors).toEqual([]);
});
