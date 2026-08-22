import { expect, test } from '@playwright/test';

async function expectSafeHomepageClaims(page) {
  await expect(page.getByText(/Hỗ trợ khi cần|kênh hỗ trợ|đơn hàng đang ở đâu/i)).toHaveCount(0);
  const story = page.locator('.brand-manifesto');
  await expect(story.getByRole('heading')).toContainText('Bận không có nghĩa');
  await expect(page.locator('.making-story').getByRole('listitem')).toHaveCount(3);
}

test('customer home renders homepage API content accessibly without hidden UI scope or overflow', async ({ page }, testInfo) => {
  const pageErrors = [];
  const consoleErrors = [];
  const homepageResponses = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(`${message.text()} ${message.location().url}`.trim()); });
  page.on('response', response => { if (new URL(response.url()).pathname.endsWith('/api/homepage')) homepageResponses.push(response); });

  await page.goto('/home');
  await expect(page.getByRole('heading', { name: 'Bữa ngon cho ngày bận rộn.' })).toBeVisible();
  await expect(page.locator('.signature-product')).toBeVisible();
  await expect(page.locator('.promo-strip')).toBeVisible();
  await expect(page.getByRole('heading', { name: /Món ngon khách hàng/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Hôm nay ăn gì?' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Đang được yêu thích' })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Bận không có nghĩa/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Nhanh ở việc đặt/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Có người đứng sau/ })).toBeVisible();
  const map = page.locator('.store-location iframe');
  if (await map.count()) {
    await expect(map).toHaveAttribute('src', /google\.com\/maps/);
    await expect(page.getByRole('link', { name: /Mở trong Google Maps/ })).toBeVisible();
  }
  await expect(page.locator('footer')).toHaveCount(1);
  const rankingItem = page.locator('.ranking-list a').first();
  await rankingItem.hover();
  await expect(rankingItem).toHaveCSS('background-color', 'rgb(242, 106, 46)');
  await expect(page.getByRole('tab').first()).toBeVisible();
  await page.getByRole('tab').last().click();
  await expect(page.getByRole('tab').last()).toHaveAttribute('aria-selected', 'true');
  if (await page.getByRole('heading', { name: 'Khách hàng nói gì' }).count()) await expect(page.getByRole('heading', { name: 'Khách hàng nói gì' })).toBeVisible();
  await expectSafeHomepageClaims(page);
  await expect.poll(() => homepageResponses.length).toBe(1);
  expect(homepageResponses[0].ok()).toBeTruthy();

  const storyImage = page.getByRole('img', { name: 'Người trẻ dùng bữa FastGuy trong ngày bận rộn' });
  await expect(storyImage).toBeVisible();
  await expect.poll(() => storyImage.evaluate(image => image.complete && image.naturalWidth > 0)).toBeTruthy();
  const productCards = page.locator('.featured .product-card');
  await expect(productCards.first()).toBeVisible();
  await expect(productCards.first().locator('.product-name')).not.toBeEmpty();
  await expect(productCards.first().locator('.product-rating')).toBeVisible();
  await expect(productCards.first().locator('.product-sold')).toContainText('đã bán');
  await expect(productCards.first().locator('.price-now')).not.toBeEmpty();
  await expect(productCards.first().getByText('Bán chạy', { exact: true })).toBeVisible();
  const expectedColumns = testInfo.project.name === 'mobile-chrome' ? 2 : 4;
  await expect.poll(() => page.locator('.featured .grid').evaluate(grid => getComputedStyle(grid).gridTemplateColumns.split(' ').length)).toBe(expectedColumns);
  const productImages = page.locator('.featured img');
  if (await productImages.count()) await expect.poll(() => productImages.first().evaluate(image => image.complete && image.naturalWidth > 0)).toBeTruthy();
  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('button', { name: 'Mở menu' }).click();
    await expect(page.getByRole('navigation', { name: 'Điều hướng chính' })).toBeVisible();
    await page.getByRole('button', { name: 'Đóng menu' }).click();
  } else {
    await expect(page.getByRole('button', { name: 'Ưu đãi tiếp theo' })).toBeVisible();
  }
  await page.waitForLoadState('networkidle');
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Trang chủ tràn ngang');
  });
});

test('public product detail and cart surfaces stay stable without overflow', async ({ page }) => {
  const pageErrors = [];
  const consoleErrors = [];
  const productResponses = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(`${message.text()} ${message.location().url}`.trim()); });
  page.on('response', response => {
    if (/\/api\/products\/\d+$/.test(new URL(response.url()).pathname)) productResponses.push(response);
  });

  await page.goto('/menu');
  const productLink = page.locator('a.product-main[href^="/product/"]').first();
  await expect(productLink).toBeVisible();
  await productLink.click();
  await expect(page).toHaveURL(/\/product\/\d+$/);
  await expect(page.locator('.product-purchase-panel h1')).toBeVisible();
  await expect.poll(() => productResponses.some(response => response.ok())).toBeTruthy();
  await page.goto('/cart');
  await expect(page.getByRole('heading', { name: 'Giỏ hàng trống' }).or(page.getByRole('heading', { name: 'Đơn hàng' }))).toBeVisible();
  await page.waitForLoadState('networkidle');
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Product Detail hoặc giỏ hàng tràn ngang');
  });
});

test('customer home keeps proof reasons and hides removed scope when homepage API fails', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ status: 'error', message: 'Homepage data could not be loaded' }) }));
  await page.goto('/home');
  await expect(page.getByRole('alert')).toContainText('Homepage data could not be loaded');
  await expect(page.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  await expect(page.locator('.signature-hero h1')).toBeVisible();
  await expectSafeHomepageClaims(page);
});

test('customer home hides removed scope for empty homepage data', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { bestSellers: [], occasionCombos: [], featuredReviews: [] } }) }));
  await page.goto('/home');
  await expect(page.locator('.signature-hero h1')).toBeVisible();
  await expectSafeHomepageClaims(page);
});

test('customer home renders truthful review and occasion API content', async ({ page }) => {
  const product = { productId: 1, name: 'Combo thật', description: '', price: 99000, imageUrl: '', soldCount: 4, averageRating: 5, reviewCount: 1, productType: 'COMBO', inStock: true, isAvailableNow: true, variants: [], modifierGroups: [] };
  await page.route('**/api/homepage', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { bestSellers: [product], occasionCombos: [{ occasion: 'GROUP', product }], featuredReviews: [{ reviewId: 1, userName: 'An', rating: 5, comment: 'Đánh giá thật' }] } }) }));
  await page.goto('/home');
  await expect(page.getByText('Combo thật').first()).toBeVisible();
  await expect(page.getByText('Đánh giá thật')).toBeVisible();
  await expectSafeHomepageClaims(page);
});

for (const path of ['/account/support', '/staff/support']) {
  test(`${path} resolves to NotFound`, async ({ page }) => {
    await page.goto(path);
    await expect(page.getByRole('heading', { name: '404' })).toBeVisible();
    await expect(page.getByText('Trang bạn tìm không tồn tại')).toBeVisible();
  });
}

test('public menu and cart hide Combo and service fee presentation', async ({ page }) => {
  for (const path of ['/menu', '/cart']) {
    await page.goto(path);
    await expect(page.getByText('Combo', { exact: true })).toHaveCount(0);
    await expect(page.getByText(/Phí dịch vụ|Phí phục vụ/i)).toHaveCount(0);
  }
});
