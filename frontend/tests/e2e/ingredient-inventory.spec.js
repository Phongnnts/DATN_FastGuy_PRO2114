import { expect, test } from '@playwright/test';

const variant = (overrides = {}) => ({
  variantId: 11,
  variantName: 'M',
  price: '50000',
  originalPrice: null,
  sku: 'VAR-11',
  quantityAvailable: 3,
  isDefault: true,
  status: 'AVAILABLE',
  availabilityStatus: 'IN_STOCK',
  remainingServings: 583,
  ...overrides,
});

const product = (overrides = {}) => ({
  productId: 1,
  name: 'Gà rán giòn',
  description: 'Gà rán kiểu FastGuy',
  categoryId: 1,
  categoryName: 'Gà',
  basePrice: 50000,
  price: 50000,
  discountPrice: null,
  defaultVariant: variant(),
  variants: [variant()],
  imageUrl: '',
  rating: 0,
  averageRating: 4.5,
  reviewCount: 12,
  soldCount: 100,
  totalSold: 100,
  bestSeller: false,
  isNew: false,
  discountPercent: null,
  originalPrice: null,
  productType: 'SIMPLE',
  availableFrom: '',
  availableTo: '',
  isAvailable: true,
  isAvailableNow: true,
  inStock: true,
  featured: false,
  modifierGroups: [{
    modifierGroupId: 91,
    name: 'FG MVP demo options',
    minSelections: 1,
    maxSelections: 1,
    isActive: true,
    options: [{ modifierOptionId: 911, name: 'Demo extra', price: 5000, isActive: true }],
  }],
  combo: null,
  ...overrides,
});

const catalog = [
  product(),
  product({
    productId: 2,
    name: 'Trà đào low stock',
    defaultVariant: variant({ variantId: 21, sku: 'VAR-21', availabilityStatus: 'LOW_STOCK', remainingServings: 2 }),
    variants: [variant({ variantId: 21, sku: 'VAR-21', availabilityStatus: 'LOW_STOCK', remainingServings: 2 })],
  }),
  product({
    productId: 3,
    name: 'Khoai tây tạm hết',
    inStock: false,
    defaultVariant: variant({ variantId: 31, sku: 'VAR-31', availabilityStatus: 'OUT_OF_STOCK' }),
    variants: [variant({ variantId: 31, sku: 'VAR-31', availabilityStatus: 'OUT_OF_STOCK' })],
  }),
];

async function mockCatalog(page, state = { count: 0 }) {
  await page.route(/\/api\/products\/\d+$/, (route) => {
    const id = Number(new URL(route.request().url()).pathname.split('/').pop());
    return route.fulfill({ json: catalog.find((item) => item.productId === id) || null });
  });
  await page.route(/\/api\/products(?:\?.*)?$/, (route) => {
    state.count += 1;
    return route.fulfill({ json: catalog });
  });
  await page.route(/\/api\/categories(?:\?.*)?$/, (route) => route.fulfill({ json: [] }));
}

function collectErrors(page) {
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() === 'error' && !/Failed to load resource.*409/.test(message.text())) errors.push(message.text());
  });
  return errors;
}

test('menu maps server availability to customer copy and locks sold-out CTAs', async ({ page }) => {
  const errors = collectErrors(page);
  await mockCatalog(page);
  await page.goto('/menu');
  await expect(page.getByRole('heading', { name: 'Kết quả món ăn' })).toBeVisible();

  const lowCard = page.locator('.product-card', { hasText: 'Trà đào low stock' });
  await expect(lowCard.locator('.stock-note')).toHaveText('Chỉ còn 2 phần');

  const soldOutCard = page.locator('.product-card', { hasText: 'Khoai tây tạm hết' });
  await expect(soldOutCard.locator('.stock-badge')).toHaveText('Tạm hết');
  const soldOutCta = soldOutCard.locator('.soldout-btn');
  await expect(soldOutCta).toBeDisabled();
  await expect(soldOutCta).toHaveText('Tạm hết');

  const inStockCard = page.locator('.product-card', { hasText: 'Gà rán giòn' });
  await expect(inStockCard.locator('.add-btn')).toBeEnabled();
  await expect(inStockCard.locator('.soldout-btn')).toHaveCount(0);

  expect(errors).toEqual([]);
});

test('product detail reflects server availability per variant', async ({ page }) => {
  const errors = collectErrors(page);
  await mockCatalog(page);
  await page.goto('/product/1');
  await expect(page.locator('.availability')).toContainText('Còn 583 phần');
  await expect(page.getByRole('group', { name: 'Kích cỡ' })).toBeVisible();
  await expect(page.getByRole('button', { name: /M.*50\.000.*Còn 583 phần/ })).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText('FG MVP demo options')).toHaveCount(0);
  await expect(page.getByText('Demo extra')).toHaveCount(0);
  await page.goto('/product/2');
  const availability = page.locator('.availability');
  await expect(availability).toContainText('Chỉ còn 2 phần');
  await expect(page.getByRole('button', { name: /Thêm vào giỏ/ })).toBeEnabled();

  await page.goto('/product/3');
  await expect(availability).toContainText('Tạm hết');
  await expect(availability).not.toContainText('Còn hàng, sẵn sàng giao nóng');
  await expect(page.getByRole('button', { name: /Thêm vào giỏ/ })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Mua ngay' })).toBeDisabled();
  expect(errors).toEqual([]);
});

test('product detail quantity accepts keyboard input and clamps to available stock', async ({ page }) => {
  const errors = collectErrors(page);
  await mockCatalog(page);
  await page.goto('/product/1');
  const quantity = page.getByRole('spinbutton', { name: 'Số lượng món' });
  await quantity.fill('3');
  await quantity.blur();
  await expect(quantity).toHaveValue('3');
  await expect(page.getByRole('button', { name: /Thêm vào giỏ - 150.000/ })).toBeVisible();
  await page.getByRole('button', { name: /Thêm vào giỏ - 150.000/ }).click();
  const cartItem = page.locator('.cart-item', { hasText: 'Gà rán giòn' });
  await expect(cartItem.locator('.qty-val')).toHaveText('3');
  await cartItem.getByRole('button', { name: 'Tăng số lượng Gà rán giòn' }).click();
  await expect(cartItem.locator('.qty-val')).toHaveText('4');

  await page.goto('/product/2');
  await quantity.fill('8');
  await quantity.blur();
  await expect(quantity).toHaveValue('2');
  expect(errors).toEqual([]);
});

test('product quantity over twenty shows a centered limit alert in detail and cart', async ({ page }) => {
  const errors=collectErrors(page);await mockCatalog(page);await page.goto('/product/1');
  const quantity=page.getByRole('spinbutton',{name:'Số lượng món'});await quantity.fill('21');await quantity.blur();
  let dialog=page.getByRole('alertdialog',{name:'Giới hạn số lượng'});await expect(dialog).toContainText('Mỗi sản phẩm chỉ được đặt tối đa 20 cái để đảm bảo đơn hàng hợp lệ.');await dialog.getByRole('button',{name:'Đã hiểu'}).click();
  await expect(quantity).toHaveValue('20');await page.getByRole('button',{name:/Thêm vào giỏ/}).click();
  const cartItem=page.locator('.cart-item',{hasText:'Gà rán giòn'});await expect(cartItem.locator('.qty-val')).toHaveText('20');
  await cartItem.getByRole('button',{name:'Tăng số lượng Gà rán giòn'}).click();dialog=page.getByRole('alertdialog',{name:'Giới hạn số lượng'});await expect(dialog).toBeVisible();await expect(dialog).toContainText('Mỗi sản phẩm chỉ được đặt tối đa 20 cái để đảm bảo đơn hàng hợp lệ.');
  expect(errors).toEqual([]);
});

test('checkout HTTP 409 refreshes availability, shows actionable message, never auto-retries', async ({ page }) => {
  const errors = collectErrors(page);
  const catalogState = { count: 0 };
  let checkoutAttempts = 0;
  await mockCatalog(page, catalogState);
  await page.route('**/api/store/config', (route) => route.fulfill({ json: { isOpen: true, openTime: '08:00', closeTime: '22:00', serviceFee: 0 } }));
  await page.route('**/api/orders/payment-capabilities', (route) => route.fulfill({ json: { availability: { BANK_TRANSFER: { enabled: false } } } }));
  await page.route('**/api/shipping/provinces', (route) => route.fulfill({ json: [{ ProvinceID: 201, ProvinceName: 'Thành phố Hồ Chí Minh' }] }));
  await page.route('**/api/shipping/districts*', (route) => route.fulfill({ json: [{ DistrictID: 1441, DistrictName: 'Quận 1' }] }));
  await page.route('**/api/shipping/wards*', (route) => route.fulfill({ json: [{ WardCode: '26734', WardName: 'Phường Bến Nghé' }] }));
  await page.route('**/api/shipping/fee', (route) => route.fulfill({ json: { fee: '20000' } }));
  await page.route('**/api/orders/guest-checkout', (route) => {
    checkoutAttempts += 1;
    return route.fulfill({ status: 409, json: { status: 'error', message: 'Insufficient inventory' } });
  });

  await page.addInitScript(() => {
    sessionStorage.setItem('cart_guest', JSON.stringify([{
      cartItemId: null,
      productId: 1,
      variantId: 11,
      key: '1_11_',
      name: 'Gà rán giòn',
      variantName: 'M',
      price: 50000,
      modifiers: [],
      image: '',
      quantity: 1,
      quantityAvailable: null,
      variantStatus: 'AVAILABLE',
      productStatus: 'AVAILABLE',
    }]));
  });

  await page.goto('/checkout');
  await page.getByPlaceholder('Họ tên người nhận').fill('Nguyễn Văn A');
  await page.getByPlaceholder('VD: 123 Nguyễn Huệ').fill('01 Nguyễn Huệ');
  await page.getByPlaceholder('Số điện thoại nhận hàng').fill('0912345678');
  const district = page.locator('.form-group', { hasText: 'Quận / Huyện' }).locator('select');
  await district.selectOption({ label: 'Quận 1' });
  await page.locator('.form-group', { hasText: 'Phường / Xã' }).locator('select').selectOption({ label: 'Phường Bến Nghé' });
  await expect(page.getByText('Phí giao hàng:')).toBeVisible();

  await page.getByRole('button', { name: 'Tiếp tục thanh toán' }).click();
  const placeOrder = page.getByRole('button', { name: 'Đặt hàng' });
  await expect(placeOrder).toBeEnabled();
  await placeOrder.click();

  await expect(page.locator('.toast-container')).toContainText('Một số món trong giỏ vừa hết hàng hoặc không đủ số lượng');
  expect(checkoutAttempts).toBe(1);
  expect(catalogState.count).toBeGreaterThanOrEqual(1);
  expect(errors).toEqual([]);
});
