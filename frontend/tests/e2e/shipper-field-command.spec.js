import { expect, test } from '@playwright/test';

const success = data => ({ status: 'success', data });

async function authenticate(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 5, fullName: 'Shipper Demo', role: 'SHIPPER' }));
  }, token);
}

async function mockShipper(page) {
  const shift = { shiftId: 9, role: 'SHIPPER', shiftDate: new Date().toISOString().slice(0, 10), startTime: '07:30', endTime: '16:30', checkInAt: '07:31', status: 'CHECKED_IN' };
  const order = { orderId: 340, orderCode: 'FG-340', status: 'PICKED_UP', customerName: 'Nguyễn Minh Anh', customerPhone: '0912345678', customerAddress: '82 Lê Văn Sỹ, Quận 3', finalAmount: 210000, itemCount: 2, paymentMethod: 'COD', paymentStatus: 'UNPAID', assignedAt: '2026-08-25T08:00:00', allowedActions: ['DELIVERED'] };
  await page.route('**/api/shifts/current', route => route.fulfill({ json: success({ state: 'CHECKED_IN', shift }) }));
  await page.route('**/api/shifts/mine', route => route.fulfill({ json: success([shift]) }));
  await page.route('**/api/shipper/dashboard', route => route.fulfill({ json: success({ todayDelivered: 8, totalDelivered: 41, todayCodCollected: 860000 }) }));
  await page.route('**/api/shipper/orders/active', route => route.fulfill({ json: success([order]) }));
  await page.route('**/api/auth/profile', route => route.fulfill({ json: success({ userId: 5, fullName: 'Shipper Demo', role: 'SHIPPER' }) }));
}

test('field command shell reflows across desktop and mobile', async ({ page }, testInfo) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await authenticate(page);
  await mockShipper(page);
  await page.goto('/shipper');

  const sidebar = page.locator('.shipper-sidebar');
  const bottomNav = page.locator('.shipper-bottom-nav');
  await expect(page.getByRole('heading', { name: 'Hôm nay' })).toBeVisible();
  await expect(page.locator('.priority-order')).toContainText('FG-340');

  if (testInfo.project.name === 'mobile-chrome') {
    await expect(sidebar).toBeHidden();
    await expect(bottomNav).toBeVisible();
    await expect(bottomNav.getByRole('link')).toHaveCount(5);
    await expect(page.getByRole('link', { name: 'Xem ca làm' })).toBeVisible();
  } else {
    await expect(sidebar).toBeVisible();
    await expect(sidebar.getByRole('link')).toHaveCount(6);
    await expect(bottomNav).toBeHidden();
  }

  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  expect(errors).toEqual([]);
});
