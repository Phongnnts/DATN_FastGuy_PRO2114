import { expect, test } from '@playwright/test';

const token = () => `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
const fulfill = data => ({ status: 'success', data });
const cartItem = { cartItemId: 1, productId: 1, variantId: 11, key: '1_11_', name: 'Gà rán', variantName: 'M', price: 50000, modifiers: [], image: '', quantity: 1, quantityAvailable: null, remainingServings: 20, inventoryMode: 'INGREDIENT', variantStatus: 'AVAILABLE', productStatus: 'AVAILABLE' };

async function mockCheckoutBase(page) {
  await page.route('**/api/orders/payment-capabilities', route => route.fulfill({ json: fulfill({ methods: ['COD', 'BANK_TRANSFER'], availability: { COD: { enabled: true }, BANK_TRANSFER: { enabled: true } } }) }));
  await page.route('**/api/store/config', route => route.fulfill({ json: fulfill({ isOpen: true, openTime: '00:00', closeTime: '23:59', serviceFee: 0 }) }));
  await page.route('**/api/shipping/provinces', route => route.fulfill({ json: fulfill([{ ProvinceID: 202, ProvinceName: 'Thành phố Hồ Chí Minh' }]) }));
  await page.route('**/api/shipping/districts*', route => route.fulfill({ json: fulfill([{ DistrictID: 1441, DistrictName: 'Quận 1' }]) }));
  await page.route('**/api/shipping/wards*', route => route.fulfill({ json: fulfill([{ WardCode: '26734', WardName: 'Phường Bến Nghé' }]) }));
  await page.route('**/api/shipping/fee', route => route.fulfill({ json: fulfill({ fee: 20000, expectedDeliveryTime: '30 phút' }) }));
  await page.route('**/api/products*', route => route.fulfill({ json: fulfill([]) }));
  await page.route('**/api/categories*', route => route.fulfill({ json: fulfill([]) }));
}

test('guest COD opens account requirement while PayOS remains selected', async ({ page }) => {
  await mockCheckoutBase(page);
  await page.addInitScript(item => sessionStorage.setItem('cart_guest', JSON.stringify([item])), cartItem);
  await page.goto('/checkout');
  await page.getByPlaceholder('Họ tên người nhận').fill('Nguyễn Văn A');
  await page.getByPlaceholder('VD: 123 Nguyễn Huệ').fill('123 Nguyễn Huệ');
  await page.getByPlaceholder('Số điện thoại nhận hàng').fill('0912345678');
  await page.locator('.form-group', { hasText: 'Quận / Huyện' }).locator('select').selectOption('1441');
  await page.locator('.form-group', { hasText: 'Phường / Xã' }).locator('select').selectOption('26734');
  await expect(page.getByText('Phí giao hàng:')).toBeVisible();
  await page.getByRole('button', { name: 'Tiếp tục thanh toán' }).click();
  await expect(page.getByRole('radio', { name: /Chuyển khoản|QR/i })).toHaveAttribute('aria-checked', 'true');
  await page.getByRole('radio', { name: 'Tiền mặt (COD)' }).click();
  const dialog = page.getByRole('dialog', { name: 'COD chỉ dành cho khách có tài khoản' });
  await expect(dialog).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Đăng nhập' })).toBeVisible();
  await expect(dialog.getByRole('button', { name: 'Đăng ký' })).toBeVisible();
});

test('authenticated default address loads GHN hierarchy and fee automatically', async ({ page }) => {
  await mockCheckoutBase(page);
  await page.addInitScript(({ value, item }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'USER' })); localStorage.setItem('cart_user_1', JSON.stringify([item])); }, { value: token(), item: cartItem });
  await page.route('**/api/cart', route => route.fulfill({ json: fulfill({ items: [cartItem] }) }));
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ userId: 1, role: 'USER', fullName: 'Nguyễn Văn A' }) }));
  await page.route('**/api/favorites*', route => route.fulfill({ json: fulfill([]) }));
  await page.route('**/api/user/addresses*', route => route.fulfill({ json: fulfill([{ addressId: 5, recipientName: 'Nguyễn Văn A', phone: '0912345678', street: '123 Nguyễn Huệ', isDefault: true, ghnProvinceId: 201, ghnDistrictId: 1441, ghnWardCode: '26734', provinceName: 'TP.HCM', districtName: 'Quận 1', wardName: 'Phường Bến Nghé' }]) }));
  await page.route('**/api/coupons/claimed*', route => route.fulfill({ json: fulfill([]) }));
  await page.goto('/checkout');
  await expect(page.locator('.saved-address-detail', { hasText: '123 Nguyễn Huệ' })).toBeVisible();
  await expect(page.getByText('Phí giao hàng:')).toContainText('20.000');
});

test('PayOS return verifies authenticated payment before showing success', async ({ page }) => {
  await page.addInitScript(value => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'USER' })); }, token());
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ userId: 1, role: 'USER', fullName: 'Nguyễn Văn A' }) }));
  await page.route('**/api/favorites*', route => route.fulfill({ json: fulfill([]) }));
  let verificationCalls = 0;
  await page.route('**/api/orders/42/payment-status', route => { verificationCalls += 1; return route.fulfill({ json: fulfill({ paymentStatus: 'PAID', paidAt: '2026-08-25T04:00:00' }) }); });
  await page.goto('/payment-return?orderId=42&orderCode=ORD-42');
  await expect(page.getByRole('heading', { name: 'Thanh toán thành công!' })).toBeVisible();
  expect(verificationCalls).toBe(1);
});

test('PayOS guest return verifies with callback token without session storage', async ({ page }) => {
  let verificationCalls = 0;
  await page.route('**/api/orders/guest-payment-status*', route => {
    verificationCalls += 1;
    const url = new URL(route.request().url());
    expect(url.searchParams.get('orderCode')).toBe('GST-42');
    expect(url.searchParams.get('token')).toBe('one-time-proof');
    return route.fulfill({ json: fulfill({ orderCode: 'GST-42', paymentStatus: 'PAID', orderStatus: 'PENDING' }) });
  });
  await page.goto('/payment-return?orderId=42&orderCode=GST-42&token=one-time-proof');
  await expect(page.getByRole('heading', { name: 'Thanh toán thành công!' })).toBeVisible();
  expect(verificationCalls).toBe(1);
});


test('PayOS return stops immediately when the canonical order is cancelled', async ({ page }) => {
  await page.addInitScript(value => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'USER' })); }, token());
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ userId: 1, role: 'USER', fullName: 'Nguyễn Văn A' }) }));
  await page.route('**/api/favorites*', route => route.fulfill({ json: fulfill([]) }));
  let verificationCalls = 0;
  await page.route('**/api/orders/42/payment-status', route => {
    verificationCalls += 1;
    return route.fulfill({ json: fulfill({ paymentStatus: 'UNPAID', orderStatus: 'CANCELLED', paidAt: null }) });
  });
  await page.goto('/payment-return?orderId=42&orderCode=ORD-42');
  await expect(page.getByRole('heading', { name: 'Đã hủy thanh toán' })).toBeVisible();
  expect(verificationCalls).toBe(1);
});

test('admin settings loads and exposes morning-count notice fields', async ({ page }) => {
  await page.addInitScript(value => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'ADMIN' })); }, token());
  await page.route('**/api/auth/profile', route => route.fulfill({ json: fulfill({ userId: 1, role: 'ADMIN', fullName: 'Quản trị viên' }) }));
  await page.route('**/api/favorites*', route => route.fulfill({ json: fulfill([]) }));
  await page.route('**/api/admin/settings', route => route.fulfill({ json: fulfill({ morning_count_notice_enabled: '1', morning_count_notice_title: 'Kiểm kê đầu ngày', morning_count_notice_message: 'Đang chuẩn bị nguyên liệu', morning_count_notice_cta_label: 'Xem thông báo' }) }));
  await page.route('**/api/orders/payment-capabilities', route => route.fulfill({ json: fulfill({ methods: ['COD', 'BANK_TRANSFER'] }) }));
  await page.goto('/admin/settings');
  await page.getByRole('tab', { name: 'Thông báo' }).click();
  await expect(page.getByLabel('Tiêu đề')).toHaveValue('Kiểm kê đầu ngày');
  await expect(page.getByLabel('Nội dung')).toHaveValue('Đang chuẩn bị nguyên liệu');
  await expect(page.getByLabel('Nhãn nút')).toHaveValue('Xem thông báo');
});
