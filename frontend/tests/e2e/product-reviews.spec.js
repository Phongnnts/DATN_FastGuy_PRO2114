import { expect, test } from '@playwright/test';

const ratedProduct = {
  productId: 45,
  name: 'Burger đánh giá',
  categoryId: 1,
  categoryName: 'Burger',
  basePrice: 59000,
  price: 59000,
  imageUrl: '',
  description: 'Burger fixture',
  averageRating: 4.2,
  reviewCount: 11,
  inStock: true,
  isAvailableNow: true,
  variants: [{ variantId: 450, variantName: 'Tiêu chuẩn', price: 59000, quantityAvailable: 20, status: 'AVAILABLE' }],
  defaultVariant: { variantId: 450, variantName: 'Tiêu chuẩn', price: 59000, quantityAvailable: 20, status: 'AVAILABLE' },
  modifierGroups: [],
};
const emptyProduct = { ...ratedProduct, productId: 46, name: 'Burger chưa đánh giá', averageRating: 0, reviewCount: 0 };
const reviewItem = (reviewId, userName, rating, comment) => ({ reviewId, productId: 45, userName, rating, comment, createdAt: '2026-08-22T10:00:00' });

async function installFixtures(page) {
  const errors = [];
  const reviewRequests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('request', request => { if (new URL(request.url()).pathname.endsWith('/api/reviews/product/45')) reviewRequests.push(new URL(request.url())); });
  await page.route('**/api/categories', route => route.fulfill({ json: { status: 'success', data: [{ categoryId: 1, name: 'Burger' }] } }));
  await page.route('**/api/store/config', route => route.fulfill({ json: { status: 'success', data: { estimatedDeliveryMinutes: 30 } } }));
  await page.route(/\/api\/products(?:\?.*)?$/, route => route.fulfill({ json: { status: 'success', data: { content: [ratedProduct, emptyProduct], totalItems: 2, totalPages: 1, page: 0, size: 12 } } }));
  await page.route('**/api/products/45', route => route.fulfill({ json: { status: 'success', data: ratedProduct } }));
  await page.route('**/api/reviews/product/45**', route => {
    const pageNumber = Number(new URL(route.request().url()).searchParams.get('page') || 1);
    const items = pageNumber === 1
      ? [reviewItem(11, 'An', 5, 'Rất ngon')]
      : [reviewItem(1, 'Bình', 4, 'Sẽ mua lại')];
    return route.fulfill({ json: { status: 'success', data: { items, total: 11, page: pageNumber, size: 10, averageRating: 4.2, reviewCount: 11, ratingDistribution: { 1: 1, 2: 1, 3: 2, 4: 3, 5: 4 } } } });
  });
  return { errors, reviewRequests };
}

test('product cards expose rated and no-review badges accessibly', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.goto('/menu');
  await expect(page.getByLabel('Đánh giá 4.2 trên 5 từ 11 lượt')).toHaveText('★ 4.2/5');
  await expect(page.getByLabel('Chưa có đánh giá, 0 lượt')).toHaveText('Chưa có đánh giá');
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});

test('mocked authenticated order detail reviews products independently and reloads 409', async ({ page }) => {
  const errors = [];
  const payloads = [];
  let reviewLoads = 0;
  let releaseFirst;
  const firstPending = new Promise(resolve => { releaseFirst = resolve; });
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ token }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify({ id: 7, fullName: 'An', role: 'USER', email: 'an@example.com' }));
  }, { token });
  await page.route('**/api/auth/profile', route => route.fulfill({ json: { status: 'success', data: { id: 7, fullName: 'An', role: 'USER', email: 'an@example.com' } } }));
  await page.route('**/api/notifications**', route => route.fulfill({ json: { status: 'success', data: { items: [], unreadCount: 0 } } }));
  await page.route('**/api/favorites**', route => route.fulfill({ json: { status: 'success', data: [] } }));
  await page.route('**/api/orders/77', route => route.fulfill({ json: { status: 'success', data: {
    orderId: 77, orderCode: 'FG77', status: 'DELIVERED', paymentMethod: 'COD', paymentStatus: 'PAID',
    customerAddress: 'Hà Nội', createdAt: '2026-08-22T10:00:00', totalAmount: 177000, shippingFee: 0,
    discountAmount: 0, finalAmount: 177000, items: [
      { orderItemId: 1, productId: 45, variantId: 450, productName: 'Burger đánh giá', variantName: 'Nhỏ', unitPrice: 59000, quantity: 1, image: '', modifiers: [] },
      { orderItemId: 2, productId: 45, variantId: 451, productName: 'Burger đánh giá', variantName: 'Lớn', unitPrice: 59000, quantity: 1, image: '', modifiers: [] },
      { orderItemId: 3, productId: 46, variantId: 460, productName: 'Burger thứ hai', variantName: 'Nhỏ', unitPrice: 59000, quantity: 1, image: '', modifiers: [] },
    ], statusHistory: [{ status: 'DELIVERED', time: '2026-08-22T10:00:00' }],
  } } }));
  await page.route('**/api/reviews/order/77', route => {
    reviewLoads += 1;
    return route.fulfill({ json: { status: 'success', data: { orderId: 77, reviews: reviewLoads >= 2 ? [{ reviewId: 9, productId: 46, rating: 4, comment: 'Đã lưu', createdAt: '2026-08-22T10:00:00' }] : [] } } });
  });
  await page.route('**/api/reviews', async route => {
    const payload = route.request().postDataJSON();
    payloads.push(payload);
    if (payload.productId === 45) {
      await firstPending;
      return route.fulfill({ json: { status: 'success', data: { reviewId: 8, ...payload, createdAt: '2026-08-22T10:00:00' } } });
    }
    return route.fulfill({ status: 409, json: { status: 'error', message: 'ALREADY_REVIEWED' } });
  });

  await page.goto('/account/orders/77');
  await expect(page.getByRole('heading', { name: 'Đánh giá sản phẩm' })).toBeVisible();
  await expect(page.getByRole('article')).toHaveCount(2);
  await page.getByRole('article').filter({ hasText: 'Burger đánh giá' }).getByRole('button', { name: 'Đánh giá sản phẩm' }).click();
  await page.getByRole('article').filter({ hasText: 'Burger thứ hai' }).getByRole('button', { name: 'Đánh giá sản phẩm' }).click();
  const firstRating = page.getByRole('radiogroup', { name: 'Số sao cho Burger đánh giá' });
  await firstRating.getByRole('radio', { name: '5 sao' }).focus();
  await page.keyboard.press('ArrowLeft');
  await expect(firstRating.getByRole('radio', { name: '4 sao' })).toBeChecked();
  const forms = page.locator('form.review-form-block');
  await forms.nth(0).getByPlaceholder('Chia sẻ cảm nhận về sản phẩm...').fill('  Ngon  ');
  const firstSubmit = forms.nth(0).getByRole('button', { name: 'Gửi đánh giá' });
  await firstSubmit.click();
  await expect(page.getByRole('article').filter({ hasText: 'Burger đánh giá' }).locator('button[type="submit"]')).toBeDisabled();
  await forms.nth(1).getByRole('button', { name: 'Gửi đánh giá' }).click();
  await expect(page.getByText('Đã lưu')).toBeVisible();
  releaseFirst();
  await expect(page.getByText('Ngon', { exact: true })).toBeVisible();
  expect(payloads).toEqual([
    { orderId: 77, productId: 45, rating: 4, comment: 'Ngon' },
    { orderId: 77, productId: 46, rating: 5, comment: null },
  ]);
  expect(reviewLoads).toBe(2);
  const expectedConflicts = errors.filter(error => /status of 409 \(Conflict\)/.test(error));
  expect(expectedConflicts).toHaveLength(1);
  expect(errors.filter(error => !expectedConflicts.includes(error))).toEqual([]);
});

test('product detail renders review summary distribution list and pagination', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.goto('/product/45');
  await expect(page.getByRole('heading', { name: 'Đánh giá sản phẩm' })).toBeVisible();
  await expect(page.getByLabel('Đánh giá trung bình 4.2 trên 5 từ 11 lượt')).toBeVisible();
  for (const rating of [5, 4, 3, 2, 1]) await expect(page.getByLabel(new RegExp(`^${rating} sao:`))).toBeVisible();
  await expect(page.getByText('Rất ngon')).toBeVisible();
  await page.getByRole('button', { name: /Trang sau/ }).click();
  await expect(page.getByText('Sẽ mua lại')).toBeVisible();
  await expect(page.getByText('Trang 2 / 2')).toBeVisible();
  expect(observed.reviewRequests.map(url => url.searchParams.get('page'))).toEqual(['1', '2']);
  expect(observed.reviewRequests.every(url => url.searchParams.get('size') === '10')).toBeTruthy();
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});
