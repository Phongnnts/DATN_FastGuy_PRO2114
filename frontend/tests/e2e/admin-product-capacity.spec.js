import { expect, test } from '@playwright/test';

test('product list shows recipe capacity and limiting ingredient per visible variant', async ({ page }) => {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ value }) => { localStorage.setItem('token', value); localStorage.setItem('user', JSON.stringify({ id: 1, role: 'ADMIN' })); }, { value: token });
  const fulfill = data => ({ status: 'success', data });
  const products = [{ productId: 1, name: 'Classic Beef Burger', categoryId: 1, categoryName: 'Burger', basePrice: 69000, status: 'AVAILABLE', productType: 'SIMPLE', variants: [{ variantId: 11, variantName: 'Mặc định', inventoryMode: 'INGREDIENT' }, { variantId: 12, variantName: 'Size L', inventoryMode: 'FINISHED_GOOD' }], galleryImages: [] }];
  const requested = [];
  await page.route('**/api/admin/products*', route => route.fulfill({ json: fulfill(products) }));
  await page.route('**/api/admin/categories', route => route.fulfill({ json: fulfill([{ categoryId: 1, name: 'Burger' }]) }));
  await page.route('**/api/admin/dashboard', route => route.fulfill({ json: fulfill({ lowStockThreshold: 5 }) }));
  await page.route('**/api/admin/product-variants/*/inventory-capacity', route => {
    const id = Number(new URL(route.request().url()).pathname.split('/').at(-2)); requested.push(id);
    return route.fulfill({ json: fulfill(id === 11 ? { inventoryMode: 'INGREDIENT', availableServings: 2, ingredients: [{ name: 'Thịt bò', limiting: true }] } : { inventoryMode: 'FINISHED_GOOD', availableServings: 8, ingredients: [] }) });
  });
  const errors=[];page.on('pageerror',error=>errors.push(error.message));page.on('console',message=>{if(message.type()==='error')errors.push(message.text())});
  await page.goto('/admin/products');
  await expect(page.getByRole('heading', { name: 'Quản lý sản phẩm' })).toBeVisible();
  await expect(page.locator('.capacity-label:visible', { hasText: 'Chỉ còn 2 phần' })).toBeVisible();
  await expect(page.locator('.variant-capacities small:visible', { hasText: 'Giới hạn: Thịt bò' })).toBeVisible();
  await expect(page.locator('.capacity-label:visible', { hasText: 'Tồn thành phẩm: 8 phần' })).toBeVisible();
  await expect.poll(() => [...requested].sort()).toEqual([11,12]);
  expect(errors).toEqual([]);
});
