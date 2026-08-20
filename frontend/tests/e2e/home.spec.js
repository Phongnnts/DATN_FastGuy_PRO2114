import { expect, test } from '@playwright/test';

test('customer home renders homepage API content accessibly without overflow', async ({ page }, testInfo) => {
  const pageErrors = [];
  const consoleErrors = [];
  const homepageResponses = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') consoleErrors.push(`${message.text()} ${message.location().url}`.trim()); });
  page.on('response', response => { if (new URL(response.url()).pathname.endsWith('/api/homepage')) homepageResponses.push(response); });

  await page.goto('/home');
  await expect(page.getByRole('heading', { name: /Bạn muốn ăn gì hôm nay/ })).toBeVisible();
  await expect(page.locator('.hero-counter')).toContainText(/01 \/ 0\d/);
  await expect(page.getByRole('heading', { name: /Món ngon khách hàng/ })).toBeVisible();
  await expect(page.locator('.featured .homepage-card')).toHaveCount(6);
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Ba bước để có bữa ăn vừa ý' })).toBeVisible();
  await expect(page.getByRole('link', { name: 'Khám phá thực đơn' }).last()).toHaveAttribute('href', '/menu');
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

test('customer home keeps story and proof reasons when homepage API fails', async ({ page }) => {

  await page.route('**/api/homepage', route => route.fulfill({ status: 500, contentType: 'application/json', body: JSON.stringify({ status: 'error', message: 'Homepage data could not be loaded' }) }));
  await page.goto('/home');
  await expect(page.getByRole('alert')).toContainText('Homepage data could not be loaded');
  await expect(page.getByRole('status').filter({ hasText: 'Không thể tải đánh giá' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Thử lại' })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
  await expect(page.getByRole('list', { name: 'Lý do chọn FastGuy' })).toBeVisible();
  await expect(page.locator('.reviews')).toHaveCount(0);
});

test('customer home omits empty occasion and review data without fallback content', async ({ page }) => {
  await page.route('**/api/homepage', route => route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ status: 'success', data: { bestSellers: [], occasionCombos: [], featuredReviews: [] } }) }));
  await page.goto('/home');
  await expect(page.locator('.occasion-grid')).toHaveCount(0);
  await expect(page.locator('.reviews')).toHaveCount(0);
  await expect(page.getByRole('status').filter({ hasText: 'Chưa có đánh giá nổi bật' })).toBeVisible();
  await expect(page.getByRole('list', { name: 'Lý do chọn FastGuy' })).toBeVisible();
  await expect(page.getByRole('heading', { name: /Cuộc sống có thể vội/ })).toBeVisible();
});
