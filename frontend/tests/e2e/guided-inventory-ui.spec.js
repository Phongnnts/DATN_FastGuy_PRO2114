import { expect, test } from '@playwright/test';

const fulfill = (data) => ({ status: 'success', data });
const item = { inventoryItemId: 7, inventoryCode: 'NL-007', name: 'Ức gà', itemType: 'INGREDIENT', baseUnit: 'G', onHandQuantity: 1000, reservedQuantity: 100, availableQuantity: 900, minimumQuantity: 200, countFrequency: 'DAILY', averageUnitCost: 85, lastCountedAt: '2026-08-23T08:00:00', active: true };
const sauce = { inventoryItemId: 8, inventoryCode: 'NL-008', name: 'Sốt tiêu', itemType: 'INGREDIENT', baseUnit: 'ML', onHandQuantity: 800, reservedQuantity: 50, availableQuantity: 750, minimumQuantity: 100, countFrequency: 'DAILY', averageUnitCost: null, lastCountedAt: '2026-08-23T08:00:00', active: true };
const finishedGood = { inventoryItemId: 9, inventoryCode: 'TP-009', name: 'Gà rán làm sẵn', itemType: 'FINISHED_GOOD', baseUnit: 'PIECE', onHandQuantity: 5, reservedQuantity: 0, availableQuantity: 5, minimumQuantity: 1, countFrequency: 'DAILY', averageUnitCost: 20000, lastCountedAt: null, active: true };
const product = { productId: 1, name: 'Gà rán', variants: [
  { variantId: 11, variantName: 'M', price: 50000 },
  { variantId: 12, variantName: 'L', price: 60000 },
  { variantId: 13, variantName: 'Thành phẩm', price: 60000 },
  { variantId: 14, variantName: 'Tạm ngừng', price: 60000 },
  { variantId: 15, variantName: 'Không theo dõi', price: 60000 },
] };

test.beforeEach(async ({ page }) => {
  const token = `x.${Buffer.from(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.x`;
  await page.addInitScript(({ sessionToken }) => {
    localStorage.setItem('token', sessionToken);
    localStorage.setItem('user', JSON.stringify({ id: 1, fullName: 'Quản trị viên', role: 'ADMIN', email: 'admin@example.com' }));
  }, { sessionToken: token });
  await page.route('**/api/admin/inventory/items', (route) => route.fulfill({ json: fulfill([item, sauce, finishedGood]) }));
  await page.route('**/api/admin/products*', (route) => route.fulfill({ json: fulfill([product]) }));
  await page.route('**/api/admin/product-variants/11/availability', (route) => route.fulfill({ json: fulfill({ availabilityStatus: 'IN_STOCK', remainingServings: 6 }) }));
  await page.route(/\/api\/admin\/product-variants\/(12|13|14|15)\/availability$/, (route) => route.fulfill({ json: fulfill({ availabilityStatus: 'IN_STOCK' }) }));
  await page.route('**/api/admin/product-variants/11/recipe', (route) => route.fulfill({ json: fulfill({ variantId: 11, yieldQuantity: 2, active: true, items: [{ inventoryItemId: 7, quantity: 300 }], updatedAt: '2026-08-24T08:00:00' }) }));
  let settingsMode = 'INGREDIENT';
  await page.route('**/api/admin/product-variants/11/inventory-settings', async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON();
      if (body.inventoryMode === 'FINISHED_GOOD') return route.fulfill({ status: 409, json: { status: 'error', message: 'Chưa thể bật quản lý thành phẩm: cần một mặt hàng kho thành phẩm đang hoạt động được liên kết với kích cỡ này.' } });
      settingsMode = body.inventoryMode;
    }
    return route.fulfill({ json: fulfill({ variantId: 11, inventoryMode: settingsMode, updatedAt: '2026-08-24T08:00:00' }) });
  });
  await page.route('**/api/admin/product-variants/11/inventory-capacity', (route) => route.fulfill({ json: fulfill({ variantId: 11, inventoryMode: 'INGREDIENT', availableServings: 6, limitingInventoryItemId: 7, calculatedAt: '2026-08-24T08:00:00Z', variantPrice: 50000, ingredients: [{ inventoryItemId: 7, name: 'Ức gà', baseUnit: 'G', onHandQuantity: 1000, reservedQuantity: 100, availableQuantity: 900, requiredPerServing: 150, availableServings: 6, averageUnitCost: 85, costPerServing: 12750, costAvailable: true, limiting: true, active: true }], costStatus: 'INCOMPLETE', recipeCostPerServing: null, foodCostPercent: null, missingCostItemCount: 1, missingCostItems: [{ inventoryItemId: 8, name: 'Sốt tiêu' }] }) }));
  for (const [variant, mode] of [[13, 'FINISHED_GOOD'], [14, 'SUSPENDED'], [15, 'UNTRACKED']]) {
    await page.route(`**/api/admin/product-variants/${variant}/recipe`, (route) => route.fulfill({ status: 404, json: { status: 'error', message: 'Recipe not found' } }));
    await page.route(`**/api/admin/product-variants/${variant}/inventory-settings`, (route) => route.fulfill({ json: fulfill({ variantId: variant, inventoryMode: mode }) }));
    await page.route(`**/api/admin/product-variants/${variant}/inventory-capacity`, (route) => route.fulfill({ json: fulfill({ variantId: variant, inventoryMode: mode, availableServings: mode === 'UNTRACKED' ? null : 4, limitingInventoryItemId: null, calculatedAt: '2026-08-24T08:00:00Z', variantPrice: 60000, ingredients: [{ inventoryItemId: 99, name: 'Snapshot thành phẩm', baseUnit: 'PIECE', onHandQuantity: 4, reservedQuantity: 0, availableQuantity: 4, requiredPerServing: 1, availableServings: 4, averageUnitCost: 10000, costPerServing: 10000, costAvailable: true, limiting: false, active: true }], costStatus: 'NOT_APPLICABLE', recipeCostPerServing: null, foodCostPercent: null, missingCostItemCount: 0, missingCostItems: [] }) }));
  }
  await page.route('**/api/admin/inventory/transactions*', (route) => route.fulfill({ json: fulfill({ items: [], totalItems: 0, totalPages: 0 }) }));
  await page.route('**/api/admin/inventory/receipts', (route) => route.fulfill({ json: fulfill([{ goodsReceiptId: 21, supplierName: 'Thực phẩm Sạch', invoiceNumber: 'HD-21', receivedAt: '2026-08-24T08:30:00', status: 'DRAFT', items: [{ inventoryItemId: 7 }] }]) }));
  await page.route('**/api/admin/inventory/stock-counts', (route) => route.fulfill({ json: fulfill([{ stockCountId: 31, countDate: '2026-08-24', frequency: 'DAILY', status: 'DRAFT', items: [{ inventoryItemId: 7 }] }]) }));
  await page.route('**/api/admin/inventory/stock-counts/31', (route) => route.fulfill({ json: fulfill({ stockCountId: 31, countDate: '2026-08-24', frequency: 'DAILY', status: 'DRAFT', items: [{ inventoryItemId: 7, theoreticalQuantity: 1000, actualQuantity: null, reasonCode: null, note: null }] }) }));
});

test('stale recipe and settings saves retain drafts until confirmed reload', async ({ page }) => {
  const writes = [];
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error' && !message.text().includes('409 (Conflict)')) errors.push(message.text()); });
  await page.route('**/api/admin/product-variants/11/recipe', async (route) => {
    if (route.request().method() === 'PUT') {
      writes.push(route.request().postDataJSON());
      return route.fulfill({ status: 409, json: { status: 'error', message: 'Dữ liệu đã thay đổi. Tải lại dữ liệu mới trước khi lưu.' } });
    }
    return route.fulfill({ json: fulfill({ variantId: 11, yieldQuantity: 2, active: true, items: [{ inventoryItemId: 7, quantity: 300 }], updatedAt: '2026-08-24T08:00:01' }) });
  });
  await page.route('**/api/admin/product-variants/11/inventory-settings', async (route) => {
    if (route.request().method() === 'PUT') {
      writes.push(route.request().postDataJSON());
      return route.fulfill({ status: 409, json: { status: 'error', message: 'Dữ liệu đã thay đổi. Tải lại dữ liệu mới trước khi lưu.' } });
    }
    return route.fulfill({ json: fulfill({ variantId: 11, inventoryMode: 'INGREDIENT', updatedAt: '2026-08-24T08:00:02' }) });
  });

  await page.goto('/admin/recipes?variantId=11');
  const quantity = page.locator('[data-recipe-line="0"] input:visible');
  await quantity.fill('777');
  await page.getByRole('button', { name: 'Lưu công thức' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận lưu công thức' }).getByRole('button', { name: 'Xác nhận lưu công thức' }).click();
  await expect(page.getByText('Bản nháp của bạn vẫn được giữ.')).toBeVisible();
  await expect(quantity).toHaveValue('777');
  expect(writes[0].expectedUpdatedAt).toBe('2026-08-24T08:00:01');

  const settingsDraft = page.getByRole('radio', { name: 'Không quản lý tồn' });
  await settingsDraft.check();
  await page.getByRole('button', { name: 'Lưu cách quản lý tồn' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận cách quản lý tồn' }).getByRole('button', { name: 'Xác nhận lưu cách quản lý tồn' }).click();
  await expect(settingsDraft).toBeChecked();
  await expect(quantity).toHaveValue('777');
  expect(writes[1]).toEqual({ inventoryMode: 'UNTRACKED', expectedUpdatedAt: '2026-08-24T08:00:02' });

  await page.getByRole('button', { name: 'Tải lại dữ liệu mới' }).click();
  const reload = page.getByRole('dialog', { name: 'Tải lại dữ liệu mới?' });
  await reload.getByRole('button', { name: 'Hủy' }).click();
  await expect(quantity).toHaveValue('777');
  await page.getByRole('button', { name: 'Tải lại dữ liệu mới' }).click();
  await reload.getByRole('button', { name: 'Tải lại dữ liệu mới' }).click();
  await expect(quantity).toHaveValue('300');
  await expect(settingsDraft).not.toBeChecked();
  expect(writes).toHaveLength(2);
  expect(errors).toEqual([]);
});

test('new variant creates recipe with null version then saves settings separately', async ({ page }) => {
  const writes = [];
  let recipe = null;
  let settings = { variantId: 12, inventoryMode: 'UNTRACKED', updatedAt: '2026-08-24T08:00:00' };
  await page.route('**/api/admin/product-variants/12/recipe', async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON();
      writes.push({ resource: 'recipe', body });
      recipe = { variantId: 12, ...body, updatedAt: '2026-08-24T08:00:01' };
      return route.fulfill({ json: fulfill(recipe) });
    }
    return recipe ? route.fulfill({ json: fulfill(recipe) }) : route.fulfill({ status: 404, json: { status: 'error', message: 'Recipe not found' } });
  });
  await page.route('**/api/admin/product-variants/12/inventory-settings', async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON();
      writes.push({ resource: 'settings', body });
      settings = { variantId: 12, inventoryMode: body.inventoryMode, updatedAt: '2026-08-24T08:00:02' };
    }
    return route.fulfill({ json: fulfill(settings) });
  });
  await page.route('**/api/admin/product-variants/12/inventory-capacity', (route) => route.fulfill({ json: fulfill({ variantId: 12, inventoryMode: settings.inventoryMode, availableServings: 0, limitingInventoryItemId: null, calculatedAt: '2026-08-24T08:00:00Z', variantPrice: 60000, ingredients: [], costStatus: 'NOT_APPLICABLE', recipeCostPerServing: null, foodCostPercent: null, missingCostItemCount: 0, missingCostItems: [] }) }));

  await page.goto('/admin/recipes?variantId=12');
  await page.getByRole('button', { name: 'Thêm nguyên liệu' }).click();
  await page.getByRole('dialog', { name: 'Chọn nguyên liệu' }).getByRole('button', { name: /Ức gà/ }).click();
  await page.getByLabel('Số lượng (g)').fill('100');
  await page.getByRole('button', { name: 'Thêm', exact: true }).click();
  await page.getByRole('button', { name: 'Lưu công thức' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận lưu công thức' }).getByRole('button', { name: 'Xác nhận lưu công thức' }).click();
  await expect.poll(() => writes.length).toBe(1);
  expect(writes[0].body.expectedUpdatedAt).toBeNull();

  await page.getByRole('radio', { name: 'Tạm ngừng bán' }).check();
  await page.getByRole('button', { name: 'Lưu cách quản lý tồn' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận cách quản lý tồn' }).getByRole('button', { name: 'Xác nhận lưu cách quản lý tồn' }).click();
  await expect.poll(() => writes.length).toBe(2);
  expect(writes.map(({ resource }) => resource)).toEqual(['recipe', 'settings']);
  expect(writes[1].body.expectedUpdatedAt).toBe('2026-08-24T08:00:00');
});

test('dirty variant switch cancels or confirms without ever saving the stale payload', async ({ page }) => {
  const writes = [];
  await page.route('**/api/admin/product-variants/12/recipe', (route) => route.fulfill({ json: fulfill({ variantId: 12, yieldQuantity: 1, active: true, items: [{ inventoryItemId: 8, quantity: 20 }] }) }));
  await page.route('**/api/admin/product-variants/12/inventory-settings', (route) => route.fulfill({ json: fulfill({ variantId: 12, inventoryMode: 'INGREDIENT' }) }));
  await page.route('**/api/admin/product-variants/12/inventory-capacity', (route) => route.fulfill({ json: fulfill({ variantId: 12, inventoryMode: 'INGREDIENT', availableServings: 0, limitingInventoryItemId: null, calculatedAt: '2026-08-24T08:00:00Z', variantPrice: 60000, ingredients: [], costStatus: 'NOT_APPLICABLE', recipeCostPerServing: null, foodCostPercent: null, missingCostItemCount: 0, missingCostItems: [] }) }));
  page.on('request', (request) => { if (request.method() === 'PUT') writes.push({ url: request.url(), body: request.postDataJSON() }); });

  await page.goto('/admin/recipes?variantId=11');
  await expect(page.locator('[data-recipe-line="0"] input:visible')).toHaveValue('300');
  await page.locator('[data-recipe-line="0"] input:visible').fill('777');
  await page.getByLabel('Kích cỡ', { exact: true }).selectOption('12');
  const confirmation = page.getByRole('dialog', { name: 'Bỏ thay đổi chưa lưu?' });
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Hủy' }).click();
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toHaveValue('11');
  await expect(page.locator('[data-recipe-line="0"] input:visible')).toHaveValue('777');

  await page.getByLabel('Kích cỡ', { exact: true }).selectOption('12');
  await confirmation.getByRole('button', { name: 'Bỏ thay đổi' }).click();
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toHaveValue('12');
  await expect(page.locator('[data-recipe-line="0"] input:visible')).toHaveValue('20');
  expect(writes).toEqual([]);
});

test('recipe and settings saves preserve the other dirty draft without cross-resource writes', async ({ page }) => {
  let recipe = { variantId: 11, yieldQuantity: 2, active: true, items: [{ inventoryItemId: 7, quantity: 300 }] };
  let settingsMode = 'INGREDIENT';
  const writes = [];
  await page.route('**/api/admin/product-variants/11/recipe', async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON();
      writes.push({ resource: 'recipe', body });
      recipe = { variantId: 11, ...body };
    }
    await route.fulfill({ json: fulfill(recipe) });
  });
  await page.route('**/api/admin/product-variants/11/inventory-settings', async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON();
      writes.push({ resource: 'settings', body });
      settingsMode = body.inventoryMode;
    }
    await route.fulfill({ json: fulfill({ variantId: 11, inventoryMode: settingsMode }) });
  });

  await page.goto('/admin/recipes?variantId=11');
  const quantity = page.locator('[data-recipe-line="0"] input:visible');
  const settingsDraft = page.getByRole('radio', { name: 'Không quản lý tồn' });
  await quantity.fill('600');
  await settingsDraft.check();

  await page.getByRole('button', { name: 'Lưu công thức' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận lưu công thức' }).getByRole('button', { name: 'Xác nhận lưu công thức' }).click();
  await expect(page.getByRole('button', { name: 'Lưu công thức' })).toBeDisabled();
  await expect(settingsDraft).toBeChecked();
  await expect(page.getByRole('button', { name: 'Lưu cách quản lý tồn' })).toBeEnabled();
  expect(writes.map(({ resource }) => resource)).toEqual(['recipe']);

  await quantity.fill('700');
  await page.getByRole('button', { name: 'Lưu cách quản lý tồn' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận cách quản lý tồn' }).getByRole('button', { name: 'Xác nhận lưu cách quản lý tồn' }).click();
  await expect(page.getByRole('button', { name: 'Lưu cách quản lý tồn' })).toBeDisabled();
  await expect(quantity).toHaveValue('700');
  await expect(page.getByRole('button', { name: 'Lưu công thức' })).toBeEnabled();
  expect(writes.map(({ resource }) => resource)).toEqual(['recipe', 'settings']);
});

test('delayed recipe save locks recipe controls while preserving editable settings draft', async ({ page }) => {
  let releaseRecipe;
  await page.route('**/api/admin/product-variants/11/recipe', async (route) => {
    if (route.request().method() !== 'PUT') return route.fulfill({ json: fulfill({ variantId: 11, yieldQuantity: 2, active: true, items: [{ inventoryItemId: 7, quantity: 300 }] }) });
    await new Promise((resolve) => { releaseRecipe = resolve; });
    await route.fulfill({ json: fulfill({ variantId: 11, ...route.request().postDataJSON() }) });
  });

  await page.goto('/admin/recipes?variantId=11');
  const quantity = page.locator('[data-recipe-line="0"] input:visible');
  await quantity.fill('600');
  await page.getByRole('button', { name: 'Lưu công thức' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận lưu công thức' }).getByRole('button', { name: 'Xác nhận lưu công thức' }).click();
  await expect(page.getByRole('button', { name: 'Đang lưu công thức...' })).toBeDisabled();
  await expect(quantity).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Thêm nguyên liệu' })).toBeDisabled();
  await expect(page.getByRole('button', { name: 'Hủy thay đổi' })).toBeDisabled();
  const settingsDraft = page.getByRole('radio', { name: 'Không quản lý tồn' });
  await expect(settingsDraft).toBeEnabled();
  await settingsDraft.check();
  await expect(settingsDraft).toBeChecked();
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toBeDisabled();
  releaseRecipe();
  await expect(page.getByRole('button', { name: 'Lưu cách quản lý tồn' })).toBeEnabled();
  await expect(settingsDraft).toBeChecked();
});

test('delayed settings save locks settings controls while preserving editable recipe draft', async ({ page }) => {
  let releaseSettings;
  await page.route('**/api/admin/product-variants/11/inventory-settings', async (route) => {
    if (route.request().method() !== 'PUT') return route.fulfill({ json: fulfill({ variantId: 11, inventoryMode: 'INGREDIENT' }) });
    await new Promise((resolve) => { releaseSettings = resolve; });
    await route.fulfill({ json: fulfill({ variantId: 11, ...route.request().postDataJSON() }) });
  });

  await page.goto('/admin/recipes?variantId=11');
  const settingsDraft = page.getByRole('radio', { name: 'Không quản lý tồn' });
  await settingsDraft.check();
  await page.getByRole('button', { name: 'Lưu cách quản lý tồn' }).click();
  await page.getByRole('dialog', { name: 'Xác nhận cách quản lý tồn' }).getByRole('button', { name: 'Xác nhận lưu cách quản lý tồn' }).click();
  await expect(page.getByRole('button', { name: 'Đang lưu cách quản lý tồn...' })).toBeDisabled();
  await expect(settingsDraft).toBeDisabled();
  const quantity = page.locator('[data-recipe-line="0"] input:visible');
  await expect(quantity).toBeEnabled();
  await quantity.fill('700');
  await expect(quantity).toHaveValue('700');
  await expect(page.getByRole('button', { name: 'Lưu công thức' })).toBeEnabled();
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toBeDisabled();
  releaseSettings();
  await expect(quantity).toHaveValue('700');
  await expect(page.getByRole('button', { name: 'Lưu công thức' })).toBeEnabled();
});

test('latest dirty intent supersedes selector and route overlaps', async ({ page }) => {
  const errors = [];
  const writes = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => { if (message.type() === 'error') errors.push(message.text()); });
  await page.route('**/api/admin/product-variants/12/recipe', (route) => route.fulfill({ json: fulfill({ variantId: 12, yieldQuantity: 1, active: true, items: [{ inventoryItemId: 8, quantity: 20 }] }) }));
  await page.route('**/api/admin/product-variants/12/inventory-settings', (route) => route.fulfill({ json: fulfill({ variantId: 12, inventoryMode: 'INGREDIENT' }) }));
  await page.route('**/api/admin/product-variants/12/inventory-capacity', (route) => route.fulfill({ json: fulfill({ variantId: 12, inventoryMode: 'INGREDIENT', availableServings: 0, limitingInventoryItemId: null, calculatedAt: '2026-08-24T08:00:00Z', variantPrice: 60000, ingredients: [], costStatus: 'NOT_APPLICABLE', recipeCostPerServing: null, foodCostPercent: null, missingCostItemCount: 0, missingCostItems: [] }) }));
  page.on('request', (request) => { if (request.method() === 'PUT') writes.push(request.url()); });

  await page.goto('/admin/recipes?variantId=11');
  const quantity = page.locator('[data-recipe-line="0"] input:visible');
  await quantity.fill('777');
  await page.getByLabel('Kích cỡ', { exact: true }).selectOption('12');
  await page.getByRole('link', { name: 'Tổng quan', exact: true }).first().evaluate((link) => link.click());
  const confirmation = page.getByRole('dialog', { name: 'Bỏ thay đổi chưa lưu?' });
  await expect(confirmation).toBeVisible();
  await confirmation.getByRole('button', { name: 'Bỏ thay đổi' }).click();
  await expect(page).toHaveURL(/\/admin\/inventory$/);
  expect(writes).toEqual([]);

  await page.goto('/admin/recipes?variantId=11');
  await quantity.fill('888');
  await page.getByRole('link', { name: 'Tổng quan', exact: true }).first().evaluate((link) => link.click());
  await expect(confirmation).toBeVisible();
  await page.getByLabel('Kích cỡ', { exact: true }).selectOption('12', { force: true });
  await confirmation.getByRole('button', { name: 'Hủy' }).click();
  await expect(page).toHaveURL(/\/admin\/recipes\?variantId=11$/);
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toHaveValue('11');
  await expect(quantity).toHaveValue('888');
  expect(writes).toEqual([]);
  expect(errors).toEqual([]);
});

test('non-ingredient capacity snapshots never become editable recipe rows', async ({ page }) => {
  const errors = [];
  page.on('pageerror', (error) => errors.push(error.message));
  for (const variant of ['13', '14', '15']) {
    await page.goto(`/admin/recipes?variantId=${variant}`);
    await expect(page.getByLabel('Kích cỡ', { exact: true })).toHaveValue(variant);
    await expect(page.getByText('Chưa có nguyên liệu.')).toBeVisible();
    await expect(page.getByLabel('Lượng Snapshot thành phẩm cho một mẻ')).toHaveCount(0);
    const summary = page.getByRole('region', { name: 'Năng lực và chi phí' });
    await expect(summary).toContainText(variant === '15' ? '— phần' : '4 phần');
    await expect(summary).toContainText('Snapshot thành phẩm');
    await expect(summary).toContainText('Chi phí');
    await expect(summary).not.toContainText('Chưa đủ dữ liệu');
  }
  expect(errors).toEqual([]);
});

test('guided inventory routes render and confirmation restores focus', async ({ page }) => {
  const errors = [];
  const expectedConflictLogs = [];
  const successfulGets = new Set();
  const writes = [];
  page.on('pageerror', (error) => errors.push(error.message));
  page.on('console', (message) => {
    if (message.type() !== 'error') return;
    if (message.text().includes('409 (Conflict)')) expectedConflictLogs.push(message.text());
    else errors.push(message.text());
  });
  page.on('response', (response) => { if (response.request().method() === 'GET' && response.ok()) successfulGets.add(new URL(response.url()).pathname); });
  page.on('request', (request) => { if (request.method() === 'PUT' && request.url().includes('/product-variants/11/')) writes.push({ path: new URL(request.url()).pathname, body: request.postDataJSON() }); });

  await page.goto('/admin/inventory');
  await expect(page.getByRole('heading', { name: 'Hôm nay cần làm gì?' })).toBeVisible();
  await expect(page.getByRole('navigation', { name: 'Quy trình tồn kho và giá vốn' })).toBeVisible();
  await expect(page.getByText('Ức gà')).toBeVisible();

  await page.goto('/admin/inventory/receipts');
  await expect(page.getByText('Thông tin phiếu')).toBeVisible();
  const approve = page.getByRole('button', { name: 'Duyệt', exact: true });
  await approve.focus();
  await approve.click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await page.evaluate(() => document.querySelector('.icon-btn')?.focus());
  await page.keyboard.press('Tab');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Hủy' })).toBeFocused();
  await page.evaluate(() => document.querySelector('.icon-btn')?.focus());
  await page.keyboard.press('Shift+Tab');
  await expect(page.getByRole('dialog').getByRole('button', { name: 'Duyệt phiếu' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(approve).toBeFocused();

  await page.goto('/admin/recipes?variantId=11');
  await expect(page.getByRole('heading', { name: 'Công thức cho 1 phần' })).toBeVisible();
  await expect(page.getByLabel('Món', { exact: true })).toHaveValue('1');
  await expect(page.getByLabel('Kích cỡ', { exact: true })).toHaveValue('11');
  await page.getByText('Cách tính số phần', { exact: true }).click();
  await expect(page.getByText('MIN(6) =')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Ảnh chụp năng lực đã lưu' })).toContainText('6 phần · 1 mặt hàng');
  await expect(page.getByRole('region', { name: 'Năng lực và chi phí' })).toContainText('6 phần');
  await page.getByRole('button', { name: 'Thêm nguyên liệu' }).click();
  const picker = page.getByRole('dialog', { name: 'Chọn nguyên liệu' });
  await expect(picker).toBeVisible();
  await expect(page.getByLabel('Tìm theo tên hoặc mã')).toBeFocused();
  await page.evaluate(() => document.querySelector('.icon-btn')?.focus());
  await expect(page.getByLabel('Tìm theo tên hoặc mã')).toBeFocused();
  await page.keyboard.press('Shift+Tab');
  await expect(picker.getByRole('button', { name: 'Đóng' })).toBeFocused();
  await expect(picker.getByText('Ức gà')).toHaveCount(0);
  await expect(picker.getByText('Gà rán làm sẵn')).toHaveCount(0);
  await picker.getByRole('button', { name: /Sốt tiêu/ }).click();
  await page.getByLabel('Số lượng (ml)').fill('50');
  await page.getByRole('button', { name: 'Thêm', exact: true }).click();
  await page.locator('[data-recipe-line="0"] input:visible').fill('600');
  await expect(page.getByText('Ước tính theo bản nháp công thức')).toBeVisible();
  await expect(page.getByRole('region', { name: 'Năng lực và chi phí' })).toContainText('3 phần');
  await page.getByRole('button', { name: 'Lưu công thức' }).click();
  const confirmation = page.getByRole('dialog', { name: 'Xác nhận lưu công thức' });
  await expect(confirmation).toBeVisible();
  await expect(confirmation).toContainText('Chỉ công thức gồm 2 dòng nguyên liệu sẽ được lưu.');
  await confirmation.getByRole('button', { name: 'Xác nhận lưu công thức' }).click();
  await expect(confirmation).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Lưu công thức' })).toBeDisabled();
  await expect.poll(() => writes.length).toBe(1);
  expect(writes).toEqual([
    { path: '/api/admin/product-variants/11/recipe', body: { yieldQuantity: 2, active: true, items: [{ inventoryItemId: 7, quantity: 600 }, { inventoryItemId: 8, quantity: 50 }], expectedUpdatedAt: '2026-08-24T08:00:00' } },
  ]);

  await page.getByRole('radio', { name: 'Đếm món làm sẵn' }).check();
  const settingsSave = page.getByRole('button', { name: 'Lưu cách quản lý tồn' });
  await settingsSave.click();
  const settingsConfirmation = page.getByRole('dialog', { name: 'Xác nhận cách quản lý tồn' });
  await settingsConfirmation.getByRole('button', { name: 'Xác nhận lưu cách quản lý tồn' }).click();
  await expect(settingsConfirmation).toHaveCount(0);
  await expect(page.getByText('Chưa thể bật quản lý thành phẩm: cần một mặt hàng kho thành phẩm đang hoạt động được liên kết với kích cỡ này.', { exact: true })).toBeVisible();
  await expect(settingsSave).toBeFocused();
  await expect.poll(() => writes.length).toBe(2);
  expect(writes[1]).toEqual({ path: '/api/admin/product-variants/11/inventory-settings', body: { inventoryMode: 'FINISHED_GOOD', expectedUpdatedAt: '2026-08-24T08:00:00' } });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);

  await page.goto('/admin/inventory/stock-counts');
  const count = page.getByText('#31 · 2026-08-24');
  await expect(count).toBeVisible();
  await count.click();
  await expect(page.getByRole('button', { name: 'Chưa đếm' })).toBeVisible();
  await expect(page.getByText('Đã đếm 0/1')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
  const reviewButton = page.getByRole('button', { name: 'Kiểm tra và duyệt' });
  await reviewButton.focus();
  await reviewButton.click();
  await expect(page.getByRole('alert')).toContainText('Nhập số lượng thực tế cho tất cả mặt hàng');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(reviewButton).toBeFocused();
  await page.getByLabel('Số đếm được của Ức gà').fill('900');
  await reviewButton.click();
  await expect(page.getByRole('alert')).toContainText('Nhập lý do cho mặt hàng có chênh lệch');
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await page.getByLabel('Lý do chênh lệch của Ức gà').fill('DAMAGE');
  await reviewButton.click();
  await expect(page.getByRole('dialog')).toBeVisible();
  await expect(page.getByRole('dialog')).toContainText('1 mặt hàng');

  for (const path of ['/api/admin/inventory/items', '/api/admin/inventory/receipts', '/api/admin/inventory/stock-counts', '/api/admin/product-variants/11/inventory-settings', '/api/admin/product-variants/11/inventory-capacity']) expect(successfulGets.has(path)).toBe(true);
  expect(expectedConflictLogs).toHaveLength(1);
  expect(errors).toEqual([]);
});
