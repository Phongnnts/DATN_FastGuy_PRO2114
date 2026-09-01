import { expect, test } from '@playwright/test';
import path from 'node:path';

const ok = data => ({ status: 'success', data });

async function setup(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(value => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' })); }, token);
  let status = 'PENDING';
  await page.route('**/api/admin/refunds*', route => {
    const url = new URL(route.request().url());
    if (url.pathname !== '/api/admin/refunds') return route.fallback();
    return route.fulfill({ json: ok([{
    orderId: 9, orderCode: 'FG-R9', customerName: 'An', customerPhone: '0900000000', finalAmount: 100000,
    paymentMethod: 'BANK_TRANSFER', paymentStatus: status === 'REFUNDED' ? 'REFUNDED' : 'PAID', refundStatus: status,
    refundAmount: status === 'REFUNDED' ? 100000 : null, refundNote: 'Đã chuyển', refundReference: status === 'REFUNDED' ? 'BANK-9' : null,
    refundProcessedBy: status === 'REFUNDED' ? 1 : null, refundProcessedByName: status === 'REFUNDED' ? 'Admin' : null,
    proofAvailable: status === 'REFUNDED', cancelledAt: '2026-09-01T08:00:00', paidAt: '2026-09-01T07:00:00', refundedAt: status === 'REFUNDED' ? '2026-09-01T09:00:00' : null,
    failureReason: 'Khách yêu cầu', createdAt: '2026-09-01T07:00:00',
  }]) });
  });
  await page.route('**/api/admin/refunds/9', async route => {
    expect(route.request().headers()['content-type']).toContain('multipart/form-data');
    const body = route.request().postDataBuffer().toString('latin1');
    for (const field of ['expectedStatus', 'refundAmount', 'refundReference', 'proof']) expect(body).toContain(`name="${field}"`);
    status = 'REFUNDED';
    await route.fulfill({ json: ok(null) });
  });
  await page.route('**/api/admin/refunds/9/proof-url', route => route.fulfill({ json: ok({ viewUrl: 'https://example.test/private-proof', expiresAt: '2026-09-01T09:05:00Z' }) }));
}

test('admin records external refund proof and requests a short-lived view URL', async ({ page }) => {
  await setup(page);
  const opened = [];
  await page.addInitScript(() => { window.open = url => { window.__opened = url; }; });
  await page.goto('/admin/refunds?status=PENDING');
  await page.getByRole('button', { name: /Xử lý/ }).click();
  await page.getByLabel('Mã tham chiếu hoàn tiền *').fill('BANK-9');
  await page.getByLabel('Ảnh bằng chứng riêng tư *').setInputFiles(path.resolve('tests/fixtures/refund-proof.png'));
  await page.getByRole('button', { name: 'Xác nhận', exact: true }).click();
  await page.getByRole('button', { name: /Đã hoàn/ }).click();
  await expect(page.getByRole('button', { name: 'Xem chi tiết' })).toBeVisible();
  await page.getByRole('button', { name: 'Xem chi tiết' }).click();
  await page.getByRole('button', { name: 'Xem ảnh riêng tư' }).click();
  expect(await page.evaluate(() => window.__opened)).toBe('https://example.test/private-proof');
  expect(await page.evaluate(() => ({ local: localStorage.getItem('refundProofUrl'), session: sessionStorage.getItem('refundProofUrl') }))).toEqual({ local: null, session: null });
  expect(opened).toEqual([]);
});
