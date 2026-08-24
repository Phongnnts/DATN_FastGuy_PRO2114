import { expect, test } from '@playwright/test';

test('menu filter polish keeps desktop realtime and mobile draft apply behavior', async ({ page }, testInfo) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error') errors.push(message.text()); });

  await page.goto('/menu');
  await expect(page.getByRole('heading', { name: 'Kết quả món ăn' })).toBeVisible();
  const search = page.getByRole('combobox', { name: 'Tìm món' });
  await search.fill('gà');
  await expect(page.getByRole('listbox')).toBeVisible();
  await search.press('ArrowDown');
  await expect(search).toHaveAttribute('aria-activedescendant', /menu-suggestion-/);
  const target = await search.getAttribute('aria-activedescendant');
  const productId = target.match(/\d+$/)[0];
  await search.press('Enter');
  await expect(page).toHaveURL(new RegExp(`/product/${productId}$`));
  await page.goto('/menu');
  await expect(page.locator('.category-chips')).toHaveCSS('white-space', testInfo.project.name === 'mobile-chrome' ? 'nowrap' : 'normal');

  if (testInfo.project.name === 'mobile-chrome') {
    await page.getByRole('button', { name: /Bộ lọc/ }).click();
    const dialog = page.getByRole('dialog', { name: 'Bộ lọc' });
    await expect(dialog).toBeVisible();
    await dialog.getByLabel('Đang giảm giá').check();
    await expect(page).not.toHaveURL(/discounted=true/);
    await dialog.getByRole('button', { name: /Xem \d+ món/ }).click();
    await expect(page).toHaveURL(/discounted=true/);
    await expect(page.getByText('Đang lọc:')).toBeVisible();
  } else {
    await page.getByRole('button', { name: /Bộ lọc/ }).click();
    await page.locator('#menu-filters').getByLabel('Đang giảm giá').check();
    await expect(page).toHaveURL(/discounted=true/);
    await expect(page.getByText('Đang lọc:')).toBeVisible();
    await expect(page.locator('.compact-toolbar')).toHaveCSS('position', 'sticky');
  }

  const soldOutCtas = page.locator('.product-card .soldout-btn');
  const soldOutCount = await soldOutCtas.count();
  for (let index = 0; index < soldOutCount; index += 1) await expect(soldOutCtas.nth(index)).toBeDisabled();

  expect(errors).toEqual([]);
  await page.locator('html').evaluate(element => {
    if (element.scrollWidth > element.clientWidth + 1) throw new Error('Trang Thực đơn tràn ngang');
  });
});
