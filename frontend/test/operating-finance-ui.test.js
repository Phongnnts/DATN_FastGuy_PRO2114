import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const read = path => readFile(new URL(`../${path}`, import.meta.url), 'utf8');

test('admin finance API uses exact OpenAPI endpoints and retire body', async () => {
  const source = await read('src/api/admin.js');
  for (const fragment of ["client.get('/admin/operating-expenses')", "client.post('/admin/operating-expenses', data)", 'client.put(`/admin/operating-expenses/${id}`, data)', 'client.delete(`/admin/operating-expenses/${id}`)', "client.get('/admin/fixed-assets')", "client.post('/admin/fixed-assets', data)", 'client.put(`/admin/fixed-assets/${id}`, data)', "client.put(`/admin/fixed-assets/${id}/retire`, { expectedStatus: 'ACTIVE' })", "client.get('/admin/reports/operating-profit', { params })"]) assert.match(source, new RegExp(fragment.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
});

test('finance pages are routed and linked without fixed asset delete', async () => {
  const [router, layout, expenses, assets] = await Promise.all([read('src/router/index.js'), read('src/layouts/AdminLayout.vue'), read('src/views/admin/OperatingExpensesPage.vue'), read('src/views/admin/FixedAssetsPage.vue')]);
  assert.match(router, /AdminOperatingExpenses/); assert.match(router, /AdminFixedAssets/);
  assert.match(layout, /Chi phí vận hành/); assert.match(layout, /Tài sản cố định/);
  for (const field of ['expenseDate', 'category', 'description', 'amount']) assert.match(expenses, new RegExp(field));
  for (const field of ['assetName', 'acquisitionCost', 'salvageValue', 'depreciationStartDate', 'usefulLifeMonths']) assert.match(assets, new RegExp(field));
  assert.match(expenses, /role="dialog"/); assert.match(assets, /retireFixedAsset/); assert.doesNotMatch(assets, /deleteFixedAsset/);
});

test('operating report uses exact labels and incomplete costs are not zero', async () => {
  const source = await read('src/views/admin/ReportsPage.vue');
  for (const label of ['Doanh thu thuần', 'COGS', 'Lợi nhuận gộp', 'Chi phí vận hành', 'Lợi nhuận trước khấu hao (mô phỏng)', 'Khấu hao', 'Lợi nhuận hoạt động']) assert.match(source, new RegExp(label.replace(/[()]/g, '\\$&')));
  assert.match(source, /costComplete/); assert.match(source, /Chưa đầy đủ/); assert.match(source, /mô phỏng quản trị sinh viên, không phải kế toán thuế/);
});

test('checkout cutoff clock refreshes and rechecks immediately before submit', async () => {
  const [checkout, cutoff] = await Promise.all([read('src/views/user/CheckoutPage.vue'), read('src/utils/orderCutoff.js')]);
  assert.match(checkout, /cutoffNow/); assert.match(checkout, /setInterval\([\s\S]*30000/); assert.match(checkout, /clearInterval\(cutoffTimer\)/);
  assert.match(checkout, /await storeApi\.getConfig\(\)[\s\S]*isPastOrderCutoff/); assert.match(checkout, /STORE_CLOSED/);
  assert.match(cutoff, /split\(':', 2\)/);
});

test('finance dialogs implement keyboard containment and fixed asset inline validation', async () => {
  const [expenses, assets] = await Promise.all([read('src/views/admin/OperatingExpensesPage.vue'), read('src/views/admin/FixedAssetsPage.vue')]);
  for (const page of [expenses, assets]) {
    assert.match(page, /document\.body\.style\.overflow/); assert.match(page, /event\.key === 'Escape'/); assert.match(page, /event\.key !== 'Tab'/); assert.match(page, /@keydown=/);
  }
  assert.match(assets, /salvageValue[\s\S]*acquisitionCost/); assert.match(assets, /aria-describedby="asset-error"/); assert.match(assets, /id="asset-error"[\s\S]*role="alert"/);
});

test('reports update full and operating reports independently', async () => {
  const source = await read('src/views/admin/ReportsPage.vue');
  assert.match(source, /Promise\.allSettled/); assert.match(source, /reportWarning/); assert.match(source, /financeWarning/);
});

test('admin weekly schedule prevents stale overwrite and tabs use roving tabindex', async () => {
  const source = await read('src/views/admin/ShiftsPage.vue');
  assert.match(source, /loadGeneration/); assert.match(source, /baseline/); assert.match(source, /await adminApi\.getShiftWeek/); assert.match(source, /Lịch tuần đã được thay đổi/);
  assert.match(source, /if \(saving\.value\) return/); assert.match(source, /:tabindex="tab ===/); assert.match(source, /ArrowLeft/); assert.match(source, /ArrowRight/); assert.match(source, /Home/); assert.match(source, /End/);
});

test('staff schedule countdown states truth and refreshes at zero', async () => {
  const source = await read('src/views/staff/StaffShiftsPage.vue');
  assert.match(source, /Kết thúc theo lịch sau/); assert.doesNotMatch(source, /Tự động sau/); assert.match(source, /crossedShiftEnd/); assert.match(source, /load\(\)/);
});
