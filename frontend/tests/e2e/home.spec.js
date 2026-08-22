import { expect, test } from '@playwright/test';

async function expectHiddenHomepageScope(page) {
  await expect(page.getByText('Combo vừa ý cho hôm nay')).toHaveCount(0);
  await expect(page.getByText('Trải nghiệm thật từ khách hàng FastGuy')).toHaveCount(0);
  await expect(page.getByText(/Hỗ trợ khi cần|kênh hỗ trợ|đơn hàng đang ở đâu/i)).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Một trải nghiệm đặt món dễ dàng' })).toBeVisible();
  await expect(page.getByText('Món rõ giá, dễ chọn')).toBeVisible();
  await expect(page.getByText('Tùy chỉnh theo khẩu vị')).toBeVisible();
  await expect(page.getByText('Theo dõi trạng thái xử lý và giao đơn', { exact: true })).toBeVisible();
}

test('customer home renders homepage API content accessibly without hidden UI scope or overflow', async ({ page }, testInfo) => {
  const pageErrors = [];
  const consoleErrors = [];
  const homepageResponses = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(`${message.text()} ${message.location().url}`.trim()); });
  page.on('response', response => { if (new URL(response.url()).pathname.endsWith('/api/homepage')) homepageResponses.push(response); });

  await page.goto('/home');
  await expect(page.locator('.hero-counter')).toContainText(/01 \/ 0\d/);
  await expect(page.getByRole('heading', { name: /Món ngon khách hàng/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Ba bước để có bữa ăn vừa ý' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Khám phá thực đơn' }).last()).toHaveAttribute('href', '/menu');
  await expectHiddenHomepageScope(page);
  await expect.poll(() => homepageResponses.length).toBe(1);
  expect(homepageResponses[0].ok()).toBeTruthy();

  const storyImage = page.getByRole('img', { name: 'Người trẻ dùng bữa khi làm việc trên laptop' });
  await expect(storyImage).toBeVisible();
  await expect.poll(() => storyImage.evaluate(image => image.complete && image.naturalWidth > 0)).toBeTruthy();
  const productImages = page.locator('.featured img');
  if (await productImages.count()) await expect.poll(() => productImages.first().evaluate(image => image.complete && image.naturalWidth > 0)).toBeTruthy();
  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('button', { name: 'Mở menu' }).click();
    await expect(page.getByRole('navigation', { name: 'Điều hướng chính' })).toBeVisible();
    await page.getByRole('button', { name: 'Đóng menu' }).click();
  } else {
    await expect(page.getByRole('button', { name: 'Banner tiếp theo' })).toHaveCSS('width', '44px');
  }
  await page.waitForLoadState('networkidle');
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Trang chủ tràn ngang');
  });
});

test('customer home keeps proof reasons and hides removed scope when homepage API fails', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ status: 'error', message: 'Homepage data could not be loaded' }) }));
  await page.goto('/home');
  await expect(page.getByRole('alert')).toContainText('Homepage data could not be loaded');
  await expect(page.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
  await expectHiddenHomepageScope(page);
});

test('customer home hides removed scope for empty homepage data', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { bestSellers: [], occasionCombos: [], featuredReviews: [] } }) }));
  await page.goto('/home');
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
  await expectHiddenHomepageScope(page);
});

test('customer home ignores removed review and occasion API content', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { bestSellers: [], occasionCombos: [{ productId: 1, name: 'Combo ẩn' }], featuredReviews: [{ reviewId: 1, userName: 'Ẩn', rating: 5, comment: 'Đánh giá ẩn' }] } }) }));
  await page.goto('/home');
  await expect(page.getByText('Combo ẩn')).toHaveCount(0);
  await expect(page.getByText('Đánh giá ẩn')).toHaveCount(0);
  await expectHiddenHomepageScope(page);
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
