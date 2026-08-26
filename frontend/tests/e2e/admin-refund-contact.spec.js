import { expect, test } from '@playwright/test';

test('pending refund dialog exposes customer phone as a call link', async ({ page }) => {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'ADMIN' })); }, token);
  const fulfill = data => ({ status: 'success', data });
  const row = { orderId: 114, orderCode: 'FG-DEMO-ORDER-114', customerName: 'Khách Demo 056', customerPhone: '0912345678', finalAmount: 74000, paymentMethod: 'BANK_TRANSFER', paymentStatus: 'PAID', refundStatus: 'PENDING', createdAt: '2026-08-25T04:00:00' };
  await page.route('**/api/admin/refunds*', route => route.fulfill({ json: fulfill([row]) }));
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ userId: 1, role: 'ADMIN', fullName: 'Admin' }) }));
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });

  await page.goto('/admin/refunds');
  await page.getByRole('button', { name: /Xử lý hoàn tiền|Xử lý/i }).first().click();
  const dialog = page.getByRole('dialog', { name: /FG-DEMO-ORDER-114/ });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByText('Số điện thoại', { exact: true })).toBeVisible();
  await expect(dialog.getByRole('link', { name: '0912345678' })).toHaveAttribute('href', 'tel:0912345678');
  expect(errors).toEqual([]);
});
