import { expect, test } from '@playwright/test';

const email = process.env.FASTGUY_E2E_ADMIN_EMAIL;
const password = process.env.FASTGUY_E2E_STAFF_PASSWORD;
const runId = process.env.FASTGUY_E2E_RUN_ID;
test.skip(!runId || !email || !password, 'Requires operations real-backend harness');

test('R4 real backend returns and renders the actionable order queue', async ({ page }) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  await page.goto('/');
  await page.getByPlaceholder('your@email.com').fill(email);
  await page.getByPlaceholder('••••••').fill(password);
  await page.getByRole('button', { name: 'Đăng nhập' }).click();
  await expect(page).toHaveURL(/\/admin/);

  const attentionResponse = page.waitForResponse(response => {
    const url = new URL(response.url());
    return url.pathname === '/api/admin/orders' && url.searchParams.get('attentionOnly') === 'true';
  });
  await page.goto('/admin/orders?status=ATTENTION');
  const response = await attentionResponse;
  expect(response.status()).toBeLessThan(400);
  const url = new URL(response.url());
  expect(url.searchParams.has('fromDate')).toBe(false);
  expect(url.searchParams.has('toDate')).toBe(false);
  const payload = await response.json();
  const fixture = payload.data.filter(order => order.orderCode?.includes(runId));
  expect(fixture.map(order => order.orderCode)).toEqual([
    `E2E-${runId}-MULTI`,
    `E2E-${runId}-OVERDUE`,
    `E2E-${runId}-REFUND`,
  ]);
  expect(fixture[0].attentionReasons).toEqual(['DELIVERY_FAILED', 'PENDING_REFUND']);
  expect(fixture[1].attentionReasons).toEqual(['PROCESSING_OVERDUE']);
  expect(fixture[2].attentionReasons).toEqual(['PENDING_REFUND']);

  await expect(page.getByRole('tab', { name: /Cần xử lý/ })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByRole('link', { name: `Xem đơn hàng E2E-${runId}-MULTI` })).toHaveCount(1);
  await expect(page.locator('.attention-reasons').getByText('Giao thất bại', { exact: true })).toBeVisible();
  await expect(page.locator('.attention-reasons').getByText('Quá hạn xử lý', { exact: true })).toHaveCount(2);
  await expect(page.locator('.attention-reasons').getByText('Chờ hoàn tiền', { exact: true })).toHaveCount(2);
  await expect(page.getByLabel('Từ ngày')).toBeDisabled();
  await page.reload();
  await expect(page).toHaveURL(/status=ATTENTION/);
  await expect(page.getByRole('link', { name: `Xem đơn hàng E2E-${runId}-MULTI` })).toHaveCount(1);
  expect(errors).toEqual([]);
});
