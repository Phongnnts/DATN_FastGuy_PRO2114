import { expect, test } from '@playwright/test';

test('customer home loads API content without browser errors', async ({ page }, testInfo) => {
  const pageErrors = [];
  const consoleErrors = [];
  const apiResponses = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  page.on('console', message => {
    if (message.type() === 'error') consoleErrors.push(`${message.text()} ${message.location().url}`.trim());
  });
  page.on('response', response => {
    if (new URL(response.url()).pathname.includes('/api/')) apiResponses.push(response);
  });

  await page.goto('/home');

  await expect(page.getByRole('heading', { name: 'Danh mục món ăn' })).toBeVisible();
  await expect(page.locator('.category-card').first()).toBeVisible();
  await expect(page.getByRole('heading', { name: /Món ngon khách hàng/ })).toBeVisible();
  await expect(page.locator('.featured .product-card').first()).toBeVisible();
  await page.waitForLoadState('networkidle');
  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('button', { name: 'Mở menu' }).click();
    await expect(page.getByRole('navigation', { name: 'Điều hướng chính' })).toBeVisible();
    await page.getByRole('button', { name: 'Đóng menu' }).click();
  }
  expect(pageErrors).toEqual([]);
  expect(consoleErrors).toEqual([]);
  expect(apiResponses.length).toBeGreaterThan(0);
  expect(apiResponses.filter(response => !response.ok()).map(response => `${response.status()} ${response.url()}`)).toEqual([]);
  const pageWidth = await page.evaluate(() => ({
    client: document.documentElement.clientWidth,
    scroll: document.documentElement.scrollWidth,
  }));
  if (pageWidth.scroll > pageWidth.client + 1) {
    pageWidth.elements = await page.evaluate(() => [...document.querySelectorAll('body *')]
      .filter(element => element.getBoundingClientRect().right > document.documentElement.clientWidth + 1)
      .filter(element => {
        for (let parent = element.parentElement; parent; parent = parent.parentElement) {
          if (['auto', 'scroll'].includes(getComputedStyle(parent).overflowX)) return false;
        }
        return true;
      })
      .slice(0, 10)
      .map(element => `${element.tagName.toLowerCase()}.${element.className}`));
  }
  expect(pageWidth).toEqual({ client: pageWidth.client, scroll: pageWidth.client });
});
