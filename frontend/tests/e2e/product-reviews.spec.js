import { expect, test } from '@playwright/test';

const ratedProduct = {
  productId: 45,
  name: 'Burger đánh giá',
  categoryId: 1,
  categoryName: 'Burger',
  productType: 'SIMPLE',
  basePrice: 75000,
  price: 59000,
  discountPrice: null,
  discountPercent: 20,
  originalPrice: 75000,
  imageUrl: '',
  description: 'Burger fixture',
  averageRating: 4.5,
  reviewCount: 18,
  soldCount: 18,
  bestSeller: true,
  isNew: true,
  inStock: true,
  isAvailableNow: true,
  variants: [{ variantId: 450, variantName: 'Tiêu chuẩn', price: 59000, quantityAvailable: 20, status: 'AVAILABLE' }],
  defaultVariant: { variantId: 450, variantName: 'Tiêu chuẩn', price: 59000, quantityAvailable: 20, status: 'AVAILABLE' },
  modifierGroups: [],
};
const emptyProduct = { ...ratedProduct, productId: 46, name: 'Burger chưa đánh giá', averageRating: 0, reviewCount: 0, soldCount: 0, bestSeller: false, isNew: false, discountPrice: null, discountPercent: null, originalPrice: null, price: 59000, variants: [{ ...ratedProduct.variants[0], variantId: 460 }], defaultVariant: { ...ratedProduct.defaultVariant, variantId: 460 } };
const optionProduct = { ...emptyProduct, productId: 47, name: 'Burger tùy chọn', variants: [{ ...ratedProduct.variants[0], variantId: 470 }, { ...ratedProduct.variants[0], variantId: 471 }], defaultVariant: { ...ratedProduct.defaultVariant, variantId: 470 }, modifierGroups: [{ modifierGroupId: 7, name: 'Sốt', minSelections: 1, maxSelections: 1, options: [{ modifierOptionId: 70, name: 'Sốt cay', price: 5000 }] }] };
const unavailableProduct = { ...optionProduct, productId: 48, name: 'Burger hết hàng', inStock: false, variants: [{ ...ratedProduct.variants[0], variantId: 480, quantityAvailable: 0, status: 'OUT_OF_STOCK' }], defaultVariant: null };
const reviewItem = (reviewId, userName, rating, comment) => ({ reviewId, productId: 45, userName, rating, comment, createdAt: '2026-08-22T10:00:00' });

async function installFixtures(page) {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ token }) => {
    localStorage.setItem('token', token);
    localStorage.setItem('user', JSON.stringify({ id: 7, fullName: 'An', role: 'USER', email: 'an@example.com' }));
  }, { token });
  const errors = [];
  const reviewRequests = [];
  const cartRequests = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.endsWith('/api/reviews/product/45')) reviewRequests.push(url);
    if (url.pathname.endsWith('/api/cart') && request.method() === 'POST') cartRequests.push(request.postDataJSON());
  });
  await page.route('**/api/auth/profile', route => route.fulfill({ json: { status: 'success', data: { id: 7, fullName: 'An', role: 'USER', email: 'an@example.com' } } }));
  await page.route('**/api/notifications**', route => route.fulfill({ json: { status: 'success', data: { items: [], unreadCount: 0 } } }));
  await page.route('**/api/favorites**', route => route.fulfill({ json: { status: 'success', data: [] } }));
  await page.route('**/api/cart', route => route.fulfill({ json: { status: 'success', data: { items: [] } } }));
  await page.route('**/api/categories', route => route.fulfill({ json: { status: 'success', data: [{ categoryId: 1, name: 'Burger' }] } }));
  await page.route('**/api/store/config', route => route.fulfill({ json: { status: 'success', data: { estimatedDeliveryMinutes: 30 } } }));
  await page.route(/\/api\/products(?:\?.*)?$/, route => route.fulfill({ json: { status: 'success', data: { content: [ratedProduct, emptyProduct, optionProduct], totalItems: 3, totalPages: 1, page: 0, size: 12 } } }));
  await page.route('**/api/products/45', route => route.fulfill({ json: { status: 'success', data: ratedProduct } }));
  await page.route('**/api/products/47', route => route.fulfill({ json: { status: 'success', data: optionProduct } }));
  await page.route('**/api/products/48', route => route.fulfill({ json: { status: 'success', data: unavailableProduct } }));

  await page.route('**/api/reviews/product/45**', route => {
    const pageNumber = Number(new URL(route.request().url()).searchParams.get('page') || 1);
    const items = pageNumber === 1
      ? [reviewItem(11, 'An', 5, 'Rất ngon')]
      : [reviewItem(1, 'Bình', 4, 'Sẽ mua lại')];
    return route.fulfill({ json: { status: 'success', data: { items, total: 11, page: pageNumber, size: 10, averageRating: 4.2, reviewCount: 11, ratingDistribution: { 1: 1, 2: 1, 3: 2, 4: 3, 5: 4 } } } });
  });
  return { errors, reviewRequests, cartRequests };
}

test('product cards render approved responsive content and action policy', async ({ page }, testInfo) => {
  const observed = await installFixtures(page);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/menu');

  await expect(page.getByLabel('Đánh giá 4.5 trên 5 từ 18 lượt')).toHaveText('4.5 · 18 đánh giá');
  await expect(page.getByLabel('Chưa có đánh giá, 0 lượt').first()).toHaveText('Chưa có đánh giá');
  await expect(page.getByText('0 đã bán', { exact: true }).first()).toBeVisible();
  const ratedCard = page.getByRole('article').filter({ hasText: 'Burger đánh giá' });
  await expect(ratedCard.locator('.best-badge .fa-fire')).toBeVisible();
  await expect(ratedCard.locator('.product-rating .fa-star')).toBeVisible();
  await expect(ratedCard.getByText('Mới', { exact: true })).toBeVisible();
  await expect(ratedCard.getByText('-20%', { exact: true })).toBeVisible();
  const noDiscountCard = page.getByRole('article').filter({ hasText: 'Burger chưa đánh giá' });
  await expect(noDiscountCard.locator('.hot-badge')).toHaveCount(0);
  await expect(page.getByText('-0%', { exact: true })).toHaveCount(0);
  await expect(page.locator('.price-old').first()).toHaveCSS('text-decoration-line', 'line-through');
  const add = page.getByRole('button', { name: /Thêm .* vào giỏ/ }).first();
  await expect(add).toHaveCSS('width', '44px');
  await expect(add).toHaveCSS('height', '44px');
  await expect(add.locator('.fa-plus')).toBeVisible();
  await expect(page.getByRole('link', { name: /Chọn món/ }).locator('.fa-chevron-right')).toBeVisible();
  await expect(page.getByRole('link', { name: /Chọn món/ })).toHaveAttribute('href', '/product/47');

  const expectedColumns = testInfo.project.name === 'mobile-chrome' ? 2 : 4;
  await expect.poll(() => page.locator('.content > .grid').evaluate(grid => getComputedStyle(grid).gridTemplateColumns.split(' ').length)).toBe(expectedColumns);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Product card grid tràn ngang');
  });
  await expect.poll(() => page.locator('.product-card').first().evaluate(card => parseFloat(getComputedStyle(card).transitionDuration))).toBeLessThanOrEqual(0.001);

  await add.click();
  await expect(page.getByRole('status')).toHaveText('Đã thêm vào giỏ hàng');
  await expect.poll(() => observed.cartRequests.length).toBe(1);
  await page.getByRole('link', { name: 'Chọn món Burger tùy chọn' }).click();
  await expect(page).toHaveURL('/product/47');
  expect(observed.cartRequests).toHaveLength(1);
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

test('mocked authenticated favorites hydrate full cards and preserve direct-add policy', async ({ page }) => {
  const observed = await installFixtures(page);
  let catalogRequests = 0;
  await page.route('**/api/favorites', route => route.fulfill({ json: { status: 'success', data: [
    { productId: 47, name: 'Burger tùy chọn', price: 59000, imageUrl: '' },
    { productId: 45, name: 'Burger đánh giá', price: 59000, imageUrl: '' },
  ] } }));
  page.on('request', request => {
    const url = new URL(request.url());
    if (url.pathname.endsWith('/api/products') && request.method() === 'GET') catalogRequests += 1;
  });

  await page.goto('/account/favorites');
  const cards = page.locator('.favorites-grid .product-card');
  await expect(cards).toHaveCount(2);
  await expect(cards.nth(0).locator('.product-name')).toHaveText('Burger tùy chọn');
  const ratedCard = cards.nth(1);
  await expect(ratedCard.locator('.product-rating')).toHaveText('4.5 · 18 đánh giá');
  await expect(ratedCard.locator('.product-sold')).toHaveText('18 đã bán');
  await expect(ratedCard.locator('.best-badge')).toHaveText('Bán chạy');
  await expect(ratedCard.getByText('Mới', { exact: true })).toBeVisible();
  await expect(ratedCard.getByText('-20%', { exact: true })).toBeVisible();
  await expect(ratedCard.getByRole('button', { name: 'Thêm Burger đánh giá vào giỏ' })).toBeVisible();
  await expect(cards.nth(0).getByRole('link', { name: 'Chọn món Burger tùy chọn' })).toHaveAttribute('href', '/product/47');
  expect(catalogRequests).toBe(1);
  expect(observed.errors).toEqual([]);
});

test('mocked favorites keep reduced cards visible with a nonblocking catalog warning', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.route('**/api/favorites', route => route.fulfill({ json: { status: 'success', data: [
    { productId: 45, name: 'Burger đánh giá', price: 59000, imageUrl: '' },
  ] } }));
  await page.route(/\/api\/products(?:\?.*)?$/, route => route.fulfill({ status: 503, json: { status: 'error', message: 'Catalog unavailable' } }));

  await page.goto('/account/favorites');
  await expect(page.locator('.favorites-grid .product-card')).toHaveCount(1);
  await expect(page.locator('.product-name')).toHaveText('Burger đánh giá');
  await expect(page.getByRole('status')).toContainText('Một số thông tin món ăn chưa được cập nhật');
  await expect(page.getByRole('heading', { name: 'Không thể tải món yêu thích' })).toHaveCount(0);
  const expectedCatalogErrors = observed.errors.filter(error => /503/.test(error));
  expect(observed.errors.filter(error => !expectedCatalogErrors.includes(error))).toEqual([]);
});

test('product favorites block double clicks and expose pressed and busy state', async ({ page }) => {
  const observed = await installFixtures(page);
  let favoriteRequests = 0;
  let releaseFavorite;
  const favoriteResponse = new Promise(resolve => { releaseFavorite = resolve; });
  await page.route('**/api/favorites/toggle/45', async route => {
    favoriteRequests += 1;
    await favoriteResponse;
    await route.fulfill({ json: { status: 'success', data: { favorite: true } } });
  });
  await page.goto('/product/45');
  const favorite = page.locator('.favorite-detail-btn');
  await expect(favorite).toHaveAccessibleName('Thêm vào yêu thích');
  await expect(favorite).toHaveAttribute('aria-pressed', 'false');
  await favorite.dblclick();
  await expect(favorite).toBeDisabled();
  await expect(favorite).toHaveAttribute('aria-busy', 'true');
  expect(favoriteRequests).toBe(1);
  releaseFavorite();
  await expect(favorite).toBeEnabled();
  await expect(favorite).toHaveAttribute('aria-pressed', 'true');
  expect(observed.errors).toEqual([]);
});

test('modifier groups announce labels, help, and visible validation errors', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.goto('/product/47');
  const sizeGroup = page.getByRole('group', { name: /Kích cỡ/ });
  const modifierGroup = page.getByRole('group', { name: /Sốt/ });
  await expect(sizeGroup).toBeVisible();
  await expect(modifierGroup).toBeVisible();
  const option = modifierGroup.getByRole('button', { name: /Sốt cay/ });
  const describedBy = await option.getAttribute('aria-describedby');
  await expect(page.locator(`#${describedBy}`)).toContainText('Bắt buộc');
  await page.getByRole('button', { name: /Thêm vào giỏ/ }).click();
  await expect(option).toHaveAttribute('aria-invalid', 'true');
  await expect(page.locator(`#${describedBy}`)).toHaveText('Vui lòng chọn ít nhất 1');
  expect(observed.cartRequests).toHaveLength(0);
  expect(observed.errors).toEqual([]);
});

test('product detail disables purchase actions when no variant is selectable', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.goto('/product/48');
  await expect(page.getByRole('button', { name: /Tiêu chuẩn/ })).toBeDisabled();
  await expect(page.getByRole('button', { name: /Thêm vào giỏ/ })).toBeDisabled();
  await expect(page.getByRole('button', { name: /Mua ngay/ })).toBeDisabled();
  expect(observed.errors).toEqual([]);
});

test('product detail renders review summary distribution list and pagination', async ({ page }) => {
  const observed = await installFixtures(page);
  await page.goto('/product/45');
  await expect(page.locator('.product-meta')).toContainText('Burger');
  await expect(page.locator('.detail-rating')).toContainText('4.2');
  await expect(page.getByRole('heading', { name: 'Burger đánh giá', level: 1 })).toBeVisible();
  await expect(page.locator('.price-row strong')).toHaveText('59.000₫');
  await expect(page.locator('.price-row del')).toHaveText('75.000₫');
  await expect(page.locator('.image-sale')).toHaveText('-20%');
  const size = page.getByRole('button', { name: /Tiêu chuẩn/ });
  await expect(size).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByText('Còn hàng, sẵn sàng giao nóng', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: /Thêm vào giỏ/ })).toBeEnabled();
  await expect(page.getByRole('button', { name: /Mua ngay/ })).toBeEnabled();
  await expect(page.getByText('Dự kiến 30 phút', { exact: true })).toBeVisible();
  await expect(page.getByText('Phí giao hàng theo địa chỉ', { exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đánh giá sản phẩm' })).toBeVisible();
  await expect(page.getByLabel('Đánh giá trung bình 4.2 trên 5 từ 11 lượt')).toBeVisible();
  for (const rating of [5, 4, 3, 2, 1]) await expect(page.getByLabel(new RegExp(`^${rating} sao:`))).toBeVisible();
  await expect(page.getByText('Rất ngon')).toBeVisible();
  await page.getByRole('button', { name: /Trang sau/ }).click();
  await expect(page.getByText('Sẽ mua lại')).toBeVisible();
  await expect(page.getByText('Trang 2 / 2')).toBeVisible();
  await expect(page.locator('.product-reviews')).toBeVisible();
  await expect(page.locator('.rating-distribution [role="progressbar"]')).toHaveCount(5);
  await expect(page.locator('.review-item').first()).toContainText('Bình');
  await expect(page.locator('.review-item').first().locator('.fa-star')).toHaveCount(5);
  await expect(page.getByRole('region', { name: 'Món cùng danh mục' })).toBeVisible();
  const relatedCards = page.locator('.related-products .product-card');
  await expect(relatedCards).toHaveCount(2);
  await expect(relatedCards.filter({ hasText: 'Burger đánh giá' })).toHaveCount(0);
  expect(observed.reviewRequests.map(url => url.searchParams.get('page'))).toEqual(['1', '2']);
  expect(observed.reviewRequests.every(url => url.searchParams.get('size') === '10')).toBeTruthy();
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});

test('premium product UI is keyboard accessible and visually stable', async ({ page }, testInfo) => {
  const observed = await installFixtures(page);
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.goto('/product/45');
  await expect(page.getByRole('heading', { name: 'Burger đánh giá', level: 1 })).toBeVisible();
  await page.locator('.detail-rating').click();
  await expect(page.getByRole('heading', { name: 'Đánh giá sản phẩm' })).toBeFocused();

  const interactive = page.locator('.product-purchase-panel button:not([disabled]), .product-purchase-panel a[href], .product-reviews button:not([disabled]), .related-section a[href], .related-section button:not([disabled])');
  const count = await interactive.count();
  expect(count).toBeGreaterThan(0);
  for (let index = 0; index < count; index += 1) {
    await interactive.nth(index).focus();
    await expect(interactive.nth(index)).toBeFocused();
    const box = await interactive.nth(index).boundingBox();
    expect(box?.height || 0).toBeGreaterThanOrEqual(44);
  }

  await expect(page.locator('img:not([alt])')).toHaveCount(0);
  await expect(page.locator('button:not([aria-label])').filter({ hasText: /^\s*$/ })).toHaveCount(0);
  await expect(page.locator('[role="progressbar"]')).toHaveCount(5);
  await expect.poll(() => page.locator('.main-image').evaluate(image => parseFloat(getComputedStyle(image).transitionDuration))).toBeLessThanOrEqual(0.001);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Premium product UI tràn ngang');
  });

  await testInfo.attach(`premium-product-${testInfo.project.name}`, {
    body: await page.screenshot({ fullPage: true }),
    contentType: 'image/png',
  });
  if (testInfo.project.name === 'mobile-chrome') {
    await page.setViewportSize({ width: 320, height: 800 });
    await page.locator('html').evaluate(element => {
      if (element.scrollWidth > element.clientWidth + 1) throw new Error('Premium product UI tràn ngang ở 320px');
    });
    await expect.poll(() => page.locator('.related-products').evaluate(grid => getComputedStyle(grid).gridTemplateColumns.split(' ').length)).toBe(1);
    const firstRelated = page.locator('.related-products .product-card').first();
    await expect(firstRelated.locator('.price-now')).toBeVisible();
    await expect(firstRelated.locator('.add-btn, .option-btn')).toBeVisible();
  }
  await page.waitForLoadState('networkidle');
  expect(observed.errors).toEqual([]);
});
