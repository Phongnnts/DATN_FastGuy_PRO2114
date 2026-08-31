import { expect, test } from '@playwright/test';

const ok = data => ({ status: 'success', data });
const orders = [
  { orderId: 7, orderCode: 'FG-0007', status: 'DELIVERY_FAILED', customerName: 'An', paymentMethod: 'COD', paymentStatus: 'UNPAID', itemCount: 1, finalAmount: 90000, serviceFee: 0, cancelledBy: null, failureNote: 'Không liên lạc được', deliveryFailureCode: 'CUSTOMER_UNREACHABLE', deliveryAttemptCount: 1, deliveryAttemptLimit: 2, deliveryFailedAt: '2026-08-29T10:00:00', retryScheduledAt: null, returnedToStoreAt: null, refundStatus: 'PENDING', refundAmount: 90000, refundedAt: null, refundNote: null, createdAt: '2026-08-29T09:00:00', attentionReasons: ['DELIVERY_FAILED', 'PENDING_REFUND'], waitingMinutes: 80, allowedActions: ['PICKED_UP','RETURNED_TO_STORE'] },
  { orderId: 5, orderCode: 'FG-0005', status: 'CONFIRMED', customerName: 'Bình', paymentMethod: 'BANK_TRANSFER', paymentStatus: 'PAID', itemCount: 2, finalAmount: 120000, serviceFee: 0, cancelledBy: null, failureNote: null, deliveryFailureCode: null, deliveryAttemptCount: 0, deliveryAttemptLimit: 2, deliveryFailedAt: null, retryScheduledAt: null, returnedToStoreAt: null, refundStatus: null, refundAmount: null, refundedAt: null, refundNote: null, createdAt: '2026-08-29T08:00:00', attentionReasons: ['PROCESSING_OVERDUE'], waitingMinutes: 120, allowedActions: ['PREPARING','CANCELLED'] },
];

async function setup(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Admin', role: 'ADMIN' })); }, { value: token });
  await page.route('**/api/admin/orders/7', route => route.fulfill({ json: ok({ ...orders[0], customerPhone: '0900000000', customerAddress: '1 Đường Test', totalAmount: 90000, shippingFee: 0, discountAmount: 0, deliveryNote: null, failureReason: 'Không liên lạc được', cancelledAt: null, confirmedAt: null, deliveredAt: null, staffName: 'Staff', shipperName: 'Shipper', internalNote: null, review: null, payment: null, items: [{ productName: 'Burger', variantName: 'L', quantity: 1, unitPrice: 90000, totalPrice: 90000, imageUrl: '' }], statusHistory: [], statusEnteredAt: '2026-08-29T09:00:00', expiresAt: null, remainingSeconds: null, timeoutPolicy: null, ownerShiftCode: null }) }));
  await page.route('**/api/admin/orders?*', async route => {
    const url = new URL(route.request().url());
    if (url.pathname !== '/api/admin/orders') return route.fallback();
    expect(url.searchParams.get('attentionOnly')).toBe('true');
    expect(url.searchParams.has('fromDate')).toBe(false);
    expect(url.searchParams.has('toDate')).toBe(false);
    await route.fulfill({ json: ok({ items: orders, pagination: { page: 1, pageSize: 20, totalItems: 2, totalPages: 1 } }) });
  });
}

test('R4 attention queue works on desktop and mobile projects', async ({ page }, testInfo) => {
    await setup(page);
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
    await page.goto('/admin/orders?status=ATTENTION');
    await expect(page.getByRole('tab', { name: /Cần xử lý/ })).toHaveAttribute('aria-selected', 'true');
    await expect(page.locator('.attention-reasons').getByText('Giao thất bại', { exact: true })).toBeVisible();
    await expect(page.getByText('Chờ hoàn tiền', { exact: true })).toBeVisible();
    await expect(page.getByText('Quá hạn xử lý', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Từ ngày')).toBeDisabled();
    await page.reload();
    await expect(page).toHaveURL(/status=ATTENTION/);
    const quickView = page.getByRole('button', { name: 'Xem nhanh đơn hàng FG-0007' });
    await expect(quickView).toHaveCount(1);
    await quickView.click();
    await expect(page.getByRole('dialog', { name: 'FG-0007' })).toBeVisible();
    await expect(page.getByText('0900000000')).toBeVisible();
    await page.getByRole('button', { name: 'Đóng chi tiết đơn hàng' }).click();
    await expect(quickView).toBeFocused();
    expect(errors, testInfo.project.name).toEqual([]);
});
