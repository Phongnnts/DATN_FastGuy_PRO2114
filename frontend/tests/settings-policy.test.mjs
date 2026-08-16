import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const page = read('../src/views/admin/SettingsPage.vue');
const helper = read('../src/utils/settingsValidation.js');
const adminApi = read('../src/api/admin.js');
const orderApi = read('../src/api/order.js');

const TAB_LABELS = ['Cửa hàng', 'Giờ hoạt động', 'Phí & thuế', 'Giao hàng', 'Tồn kho', 'Thanh toán', 'Vận chuyển GHN'];
const GHN_KEYS = ['ghn_from_district_id', 'ghn_from_ward_code', 'default_service_type_id', 'default_weight', 'default_length', 'default_width', 'default_height'];

test('settings page defines seven grouped tabs with accessible roving tablist', () => {
  for (const label of TAB_LABELS) assert.ok(page.includes(label), `missing tab label ${label}`);
  assert.match(page, /role="tablist"/);
  assert.match(page, /role="tab"/);
  assert.match(page, /role="tabpanel"/);
  assert.match(page, /aria-selected/);
  assert.match(page, /:tabindex="activeTab === tab\.id \? 0 : -1"/);
  assert.match(page, /tabindex="0"/);
  assert.match(page, /@keydown="handleTabKeydown\(\$event, index\)"/);
  assert.match(page, /visually-hidden/);
  for (const key of ['ArrowLeft', 'ArrowRight', 'Home', 'End']) assert.match(page, new RegExp(key));
});

test('settings page loads settings exactly once via getSettings', () => {
  assert.equal((page.match(/adminApi\.getSettings\(\)/g) || []).length, 1);
  assert.match(page, /tabErrors\.value = \{ store: \{\}, hours: \{\}, fees: \{\}, delivery: \{\}, inventory: \{\} \}/);
});

test('each editable tab saves only its group payload through updateSettings', () => {
  assert.match(page, /const \{ payload, errors \} = buildSettingsPayload\(scope, form\.value\)/);
  assert.match(page, /adminApi\.updateSettings\(payload\)/);
  assert.match(page, /if \(saving\.value\) return/);
  assert.match(page, /:disabled="saving"/);
  assert.match(page, /tabErrors\.value\[scope\] = errors/);
  for (const scope of ['store', 'hours', 'fees', 'delivery', 'inventory']) {
    assert.match(page, new RegExp(`saveTab\\('${scope}'\\)`));
  }
  assert.doesNotMatch(page, /saveTab\('ghn'\)/);
  assert.doesNotMatch(page, /saveTab\('payment'\)/);
});

test('inventory tab edits persisted low-stock threshold with accessible error', () => {
  assert.match(page, /id: 'inventory'/);
  assert.match(page, /low_stock_threshold: 5/);
  assert.match(page, /form\.value\.low_stock_threshold = Number/);
  assert.match(page, /saveTab\('inventory'\)/);
  assert.match(page, /for="settings-low-stock-threshold"/);
  assert.match(page, /id="settings-low-stock-threshold"/);
  assert.match(page, /fieldError\('inventory', 'low_stock_threshold'\)/);
  assert.match(page, /id="settings-low-stock-error"/);
  assert.match(page, /:aria-invalid="Boolean\(fieldError\('inventory', 'low_stock_threshold'\)\)"/);
  assert.match(page, /settings-low-stock-help settings-low-stock-error/);
  assert.match(page, /role="alert"/);
});

test('payment tab calls orderApi.getPaymentCapabilities and renders COD + BANK_TRANSFER', () => {
  assert.match(page, /orderApi\.getPaymentCapabilities\(\)/);
  assert.match(page, /'COD'/);
  assert.match(page, /'BANK_TRANSFER'/);
  assert.match(page, /Đang bật/);
  assert.match(page, /Đã tắt/);
  assert.match(orderApi, /getPaymentCapabilities\(\)/);
});

test('payment tab offers retry for failed capabilities fetch', () => {
  assert.match(page, /v-if="paymentError"/);
  assert.match(page, /@click="loadPaymentCapabilities"/);
});

test('GHN tab is read-only: renders all keys and never submits them', () => {
  for (const key of GHN_KEYS) assert.ok(page.includes(key), `missing ghn key ${key}`);
  const ghnBlock = page.slice(page.lastIndexOf('Vận chuyển GHN'));
  assert.match(ghnBlock, /readonly/);
  assert.doesNotMatch(ghnBlock, /updateSettings/);
  assert.doesNotMatch(ghnBlock, /v-model/);
});

test('validation helper is imported and wired into tab save flow', () => {
  assert.match(page, /settingsValidation/);
  assert.match(page, /nextEnabledSectionIndex/);
  for (const fn of ['validateStore', 'validateHours', 'validateFees', 'validateDelivery', 'buildSettingsPayload']) {
    assert.match(helper, new RegExp(`export function ${fn}`));
  }
  assert.match(helper, /export const SCOPE_KEYS/);
  assert.match(adminApi, /getSettings\(\)/);
  assert.match(adminApi, /updateSettings\(data\)/);
});
