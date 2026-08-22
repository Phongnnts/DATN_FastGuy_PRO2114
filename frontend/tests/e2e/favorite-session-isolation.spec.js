import { expect, test } from '@playwright/test';

function token() {
  const payload = Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url');
  return `header.${payload}.signature`;
}

async function authenticate(page) {
  await page.goto('/');
  await page.evaluate(({ sessionToken }) => {
    localStorage.setItem('token', sessionToken);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Người dùng thử', role: 'USER', email: 'user@example.com' }));
  }, { sessionToken: token() });
  await expect.poll(() => page.evaluate(() => Boolean(localStorage.getItem('token') && localStorage.getItem('user')))).toBe(true);
}

async function mockSharedRequests(page) {
  await page.route('**/api/products**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { content: [] } }) }));
}

function observeErrors(page) {
  const pageErrors = [];
  const consoleErrors = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error' && !/Failed to load resource: the server responded with a status of 401/.test(message.text())) consoleErrors.push(message.text());
  });
  return { pageErrors, consoleErrors };
}

test('favorite fallback navigates to detail while hydrated simple product keeps direct add', async ({ page }) => {
  const errors = observeErrors(page);
  await authenticate(page);
  await page.route('**/api/notifications**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { items: [], unreadCount: 0 } }) }));
  await page.route('**/api/favorites**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: [{ productId: 92, name: 'Fallback', price: 50000 }, { productId: 93, name: 'Hydrated', price: 60000 }] }) }));
  await page.route('**/api/products**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { content: [{ productId: 93, name: 'Hydrated', price: 60000, productType: 'SIMPLE', inStock: true, isAvailableNow: true, variants: [{ variantId: 930, price: 60000, quantityAvailable: 2, status: 'AVAILABLE' }], defaultVariant: { variantId: 930, price: 60000, quantityAvailable: 2, status: 'AVAILABLE' }, modifierGroups: [] }] } }) }));
  await page.goto('/account/favorites');
  await expect(page.getByRole('link', { name: 'Chọn món Fallback' })).toHaveAttribute('href', '/product/92');
  await expect(page.getByRole('button', { name: 'Thêm Fallback vào giỏ' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Thêm Hydrated vào giỏ' })).toBeVisible();
  expect(errors.pageErrors).toEqual([]);
  expect(errors.consoleErrors).toEqual([]);
});

test('401 during favorite hydration clears session and blocks stale account data', async ({ page }) => {
  const errors = observeErrors(page);
  await mockSharedRequests(page);
  await authenticate(page);
  let resolveFavorites;
  const favoritesReady = new Promise(resolve => { resolveFavorites = resolve; });
  await page.route('**/api/favorites**', async route => {
    await favoritesReady;
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: [{ productId: 91, name: 'Món tài khoản cũ', price: 45000 }] }) });
  });
  await page.route('**/api/notifications**', route => route.fulfill({ status: 401, contentType: 'application/json', body: JSON.stringify({ status: 'error', message: 'Phiên đăng nhập hết hạn' }) }));

  await page.goto('/account/favorites', { waitUntil: 'domcontentloaded' });
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  resolveFavorites();
  await expect(page.getByText('Món tài khoản cũ')).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => ({ token: localStorage.getItem('token'), user: localStorage.getItem('user') }))).toEqual({ token: null, user: null });
  expect(errors.pageErrors).toEqual([]);
  expect(errors.consoleErrors).toEqual([]);
});

test('manual logout clears favorite session before returning to login', async ({ page }, testInfo) => {
  const errors = observeErrors(page);
  await mockSharedRequests(page);
  await authenticate(page);
  await page.route('**/api/notifications**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { items: [], unreadCount: 0 } }) }));
  await page.route('**/api/favorites**', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: [{ productId: 92, name: 'Món trước đăng xuất', price: 50000 }] }) }));

  await page.goto('/account/favorites');
  await expect(page).toHaveURL(/\/account\/favorites$/);
  await expect.poll(() => errors.pageErrors).toEqual([]);
  await expect(page.getByRole('heading', { name: 'Món yêu thích', exact: true })).toBeVisible();
  await expect(page.getByText('Món trước đăng xuất')).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thêm Món trước đăng xuất vào giỏ' })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Chọn món Món trước đăng xuất' })).toHaveAttribute('href', '/product/92');
  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('button', { name: 'Mở menu' }).click();
    const logout = page.getByRole('button', { name: 'Đăng xuất', exact: true });
    await logout.focus();
    await page.keyboard.press('Enter');
  } else {
    await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  }
  await expect(page.getByRole('heading', { name: 'Đăng nhập' })).toBeVisible();
  await expect(page.getByText('Món trước đăng xuất')).toHaveCount(0);
  await expect.poll(() => page.evaluate(() => ({ token: localStorage.getItem('token'), user: localStorage.getItem('user') }))).toEqual({ token: null, user: null });
  expect(errors.pageErrors).toEqual([]);
  expect(errors.consoleErrors).toEqual([]);
});
