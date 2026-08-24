import { expect, test } from '@playwright/test';

test('admin product lifecycle defaults to active and safely restores or rejects permanent delete', async ({ page }) => {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'ADMIN' })); }, { value: token });
  const fulfill = data => ({ status: 'success', data });
  let products = [
    { productId: 1, name: 'Đang bán', categoryId: 1, categoryName: 'Món', basePrice: 10000, status: 'AVAILABLE', productType: 'SIMPLE', variants: [], galleryImages: [] },
    { productId: 2, name: 'Đã ẩn', categoryId: 1, categoryName: 'Món', basePrice: 10000, status: 'UNAVAILABLE', productType: 'SIMPLE', variants: [], galleryImages: [] },
  ];
  let restoreCalls = 0;
  let deleteCalls = 0;
  await page.route('**/api/admin/products/2/restore', route => { restoreCalls += 1; products = products.map(p => p.productId === 2 ? { ...p, status: 'AVAILABLE' } : p); return route.fulfill({ json: fulfill(null) }); });
  await page.route('**/api/admin/products/2/permanent', route => { deleteCalls += 1; return route.fulfill({ status: 409, json: { status: 'error', message: 'Sản phẩm đã có dữ liệu người dùng và không thể xóa vĩnh viễn' } }); });
  await page.route('**/api/admin/products*', route => route.fulfill({ json: fulfill(products) }));
  await page.route('**/api/admin/categories', route => route.fulfill({ json: fulfill([{ categoryId: 1, name: 'Món' }]) }));
  await page.route('**/api/admin/dashboard', route => route.fulfill({ json: fulfill({ lowStockThreshold: 5 }) }));
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  page.on('console', message => { if (message.type() === 'error' && !/409/.test(message.text())) errors.push(message.text()); });

  await page.goto('/admin/products');
  const visibleCatalog = page.locator('.desktop-catalog:visible, .mobile-catalog:visible');
  await expect(page.getByLabel('Lọc trạng thái')).toHaveValue('AVAILABLE');
  await expect(visibleCatalog.getByText('Đang bán', { exact: true }).first()).toBeVisible();
  await expect(page.getByText('Đã ẩn', { exact: true })).toHaveCount(0);

  await page.getByLabel('Lọc trạng thái').selectOption('UNAVAILABLE');
  await expect(visibleCatalog.getByText('Đã ẩn', { exact: true }).first()).toBeVisible();
  await visibleCatalog.getByRole('button', { name: /Xóa vĩnh viễn.*Đã ẩn|Xóa vĩnh viễn sản phẩm/ }).first().click();
  const dialog = page.getByRole('dialog', { name: 'Xóa vĩnh viễn sản phẩm' });
  await expect(dialog).toBeVisible();
  await dialog.getByRole('button', { name: 'Xóa vĩnh viễn' }).click();
  await expect(page.locator('.toast-container')).toContainText('Sản phẩm đã có dữ liệu người dùng');
  expect(deleteCalls).toBe(1);
  await dialog.getByRole('button', { name: 'Hủy' }).click();

  await visibleCatalog.getByRole('button', { name: /Khôi phục.*Đã ẩn|Khôi phục sản phẩm/ }).first().click();
  await expect.poll(() => restoreCalls).toBe(1);
  await expect(page.getByText('Đã ẩn', { exact: true })).toHaveCount(0);
  expect(errors).toEqual([]);
});
