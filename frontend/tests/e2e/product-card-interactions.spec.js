import { expect, test } from '@playwright/test';

async function openProductCard(page) {
  await page.goto('/home');
  await expect(page.locator('.featured .product-card').first()).toBeVisible();
}

test('ProductCard controls expose approved interaction states without overflow', async ({ page }) => {
  await openProductCard(page);
  const favorite = page.locator('.featured .fav-btn').first();
  const option = page.locator('.featured .option-btn').first();
  const arrow = option.locator('i');

  await expect(favorite).toHaveCSS('background-color', 'rgb(255, 255, 255)');
  await expect(favorite).toHaveCSS('border-color', 'rgb(231, 229, 228)');
  await favorite.hover();
  await expect(favorite).toHaveCSS('color', 'rgb(225, 29, 72)');
  await expect(favorite).toHaveCSS('background-color', 'rgb(255, 241, 242)');
  await expect(favorite).toHaveCSS('border-color', 'rgb(251, 113, 133)');
  await expect.poll(() => favorite.evaluate(element => getComputedStyle(element).transform)).not.toBe('none');

  await favorite.evaluate(element => element.classList.add('active'));
  await expect(favorite).toHaveCSS('color', 'rgb(255, 255, 255)');
  await expect(favorite).toHaveCSS('background-color', 'rgb(225, 29, 72)');

  await expect(option).toHaveCSS('min-height', '44px');
  await expect(option).toHaveCSS('color', 'rgb(255, 255, 255)');
  await expect(option).toHaveCSS('background-color', 'rgb(242, 106, 46)');
  await option.hover();
  await expect(option).toHaveCSS('background-color', 'rgb(220, 79, 25)');
  await expect.poll(() => option.evaluate(element => getComputedStyle(element).transform)).not.toBe('none');
  await expect.poll(() => arrow.evaluate(element => getComputedStyle(element).transform)).not.toBe('none');
  await option.focus();
  await expect(option).toHaveCSS('outline-color', 'rgb(251, 113, 133)');

  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('ProductCard tràn ngang');
  });
});

test('ProductCard disables control motion when reduced motion is requested', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await openProductCard(page);
  const favorite = page.locator('.featured .fav-btn').first();
  const option = page.locator('.featured .option-btn').first();
  await favorite.hover();
  await expect.poll(() => favorite.evaluate(element => parseFloat(getComputedStyle(element).transitionDuration))).toBeLessThanOrEqual(.00001);
  await expect(favorite).toHaveCSS('transform', 'none');
  await option.hover();
  await expect.poll(() => option.evaluate(element => parseFloat(getComputedStyle(element).transitionDuration))).toBeLessThanOrEqual(.00001);
  await expect(option).toHaveCSS('transform', 'none');
  await expect(option.locator('i')).toHaveCSS('transform', 'none');
});
