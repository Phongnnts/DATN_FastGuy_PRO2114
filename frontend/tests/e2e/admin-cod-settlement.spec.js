import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });

async function setup(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => {
    localStorage.setItem('token', value);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' }));
  }, token);
  let status = 'SUBMITTED';
  await page.route('**/api/cod-settlements/admin?*', route => route.fulfill({ json: ok([{
    settlementId: 9, shipperId: 2, shipperName: 'Shipper An', shiftId: 4,
    shiftDate: '2026-09-01', startTime: '08:00:00', endTime: '12:00:00', status,
    expectedAmount: 120000, submittedAmount: 119000, differenceAmount: -1000,
    verifiedAmount: status === 'SUBMITTED' ? null : 119000, reason: status === 'SUBMITTED' ? null : 'Thiếu 1.000đ',
    receivedByName: status === 'SUBMITTED' ? null : 'Admin', submittedAt: '2026-09-01T12:01:00', verifiedAt: status === 'SUBMITTED' ? null : '2026-09-01T12:05:00',
  }]) }));
  await page.route('**/api/cod-settlements/9/verify', async route => {
    const body = route.request().postDataJSON();
    expect(body).toEqual({ expectedStatus: 'SUBMITTED', status: 'SHORT', verifiedAmount: 118000, reason: 'Thiếu 1.000đ' });
    status = 'SHORT';
    await route.fulfill({ json: ok({ settlementId: 9, status: 'SHORT' }) });
  });
}

test('admin verifies a COD shortage on desktop and mobile', async ({ page }) => {
  await setup(page);
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await page.goto('/admin/cod-settlements?status=SUBMITTED');
  await expect(page.getByText('-1.000₫').first()).toBeVisible();
  await page.getByRole('button', { name: /Xác nhận bàn giao|Xác nhận/ }).first().click();
  await page.getByLabel('Kết quả').selectOption('SHORT');
  await page.getByLabel('Số tiền kiểm đếm').fill('118000');
  await page.getByLabel('Lý do').fill('Thiếu 1.000đ');
  await page.getByRole('dialog', { name: 'Xác nhận bàn giao COD' }).getByRole('button', { name: 'Xác nhận', exact: true }).click();
  await expect(page.getByRole('status')).toContainText('Đã xác nhận');
  expect(errors).toEqual([]);
});
