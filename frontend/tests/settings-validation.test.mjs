import assert from 'node:assert/strict';
import test from 'node:test';
import {
  validateStore,
  validateHours,
  validateFees,
  validateDelivery,
  validateInventory,
  buildSettingsPayload,
  SCOPE_KEYS,
} from '../src/utils/settingsValidation.js';

test('validateStore requires a non-blank store name', () => {
  assert.deepEqual(validateStore('', '0901', 'addr', ''), { name: 'Tên cửa hàng là bắt buộc' });
  assert.deepEqual(validateStore('   ', '0901', 'addr', ''), { name: 'Tên cửa hàng là bắt buộc' });
  assert.deepEqual(validateStore(undefined, undefined, undefined, undefined), { name: 'Tên cửa hàng là bắt buộc' });
  assert.deepEqual(validateStore('FastGuy', '', '', ''), {});
});

test('validateStore logo is optional but must be an http(s) URL when present', () => {
  assert.deepEqual(validateStore('FastGuy', '', '', ''), {});
  assert.deepEqual(validateStore('FastGuy', '', '', 'https://cdn.example.com/logo.png'), {});
  assert.deepEqual(validateStore('FastGuy', '', '', 'http://cdn.example.com/logo.png'), {});
  assert.deepEqual(validateStore('FastGuy', '', '', 'not-a-url'), { logo: 'Logo URL không hợp lệ' });
  assert.deepEqual(validateStore('FastGuy', '', '', 'ftp://cdn.example.com/logo.png'), { logo: 'Logo URL không hợp lệ' });
  assert.deepEqual(validateStore('FastGuy', '', '', 'https://'), { logo: 'Logo URL không hợp lệ' });
  assert.deepEqual(validateStore('FastGuy', '', '', 'https://example.com/a b.png'), { logo: 'Logo URL không hợp lệ' });
});

test('validateHours requires both valid HH:MM times', () => {
  assert.deepEqual(validateHours('08:00', '22:00'), {});
  assert.deepEqual(validateHours('00:00', '23:59'), {});
  assert.deepEqual(validateHours('', ''), { open: 'Giờ mở cửa là bắt buộc', close: 'Giờ đóng cửa là bắt buộc' });
  assert.deepEqual(validateHours('08:00', ''), { close: 'Giờ đóng cửa là bắt buộc' });
  assert.deepEqual(validateHours('', '22:00'), { open: 'Giờ mở cửa là bắt buộc' });
  assert.deepEqual(validateHours('8:00', '22:00'), { open: 'Giờ mở cửa không hợp lệ (HH:MM)' });
  assert.deepEqual(validateHours('25:00', '22:00'), { open: 'Giờ mở cửa không hợp lệ (HH:MM)' });
  assert.deepEqual(validateHours('08:60', '22:00'), { open: 'Giờ mở cửa không hợp lệ (HH:MM)' });
});

test('validateHours accepts same-day, equal and overnight pairs', () => {
  assert.deepEqual(validateHours('08:00', '22:00'), {});
  assert.deepEqual(validateHours('12:00', '12:00'), {});
  assert.deepEqual(validateHours('23:00', '03:00'), {});
  assert.deepEqual(validateHours('00:00', '00:00'), {});
});

test('validateFees accepts non-negative fees and tax within 0..100', () => {
  assert.deepEqual(validateFees({ service_fee: 0, tax_rate: 0, delivery_fee: 0, min_order_amount: 0 }), {});
  assert.deepEqual(validateFees({ service_fee: 5000, tax_rate: 8, delivery_fee: 15000, min_order_amount: 20000 }), {});
  assert.deepEqual(validateFees({ service_fee: 1000, tax_rate: 100, delivery_fee: 1000, min_order_amount: 1000 }), {});
});

test('validateFees rejects negatives, non-numeric values and tax out of range', () => {
  assert.deepEqual(validateFees({ service_fee: -1, tax_rate: 0, delivery_fee: 0, min_order_amount: 0 }), { service_fee: 'Phí dịch vụ phải là số không âm' });
  assert.deepEqual(validateFees({ service_fee: 0, tax_rate: 0, delivery_fee: -5, min_order_amount: 0 }), { delivery_fee: 'Phí giao hàng phải là số không âm' });
  assert.deepEqual(validateFees({ service_fee: 0, tax_rate: 0, delivery_fee: 0, min_order_amount: -1 }), { min_order_amount: 'Đơn tối thiểu phải là số không âm' });
  assert.deepEqual(validateFees({ service_fee: 'abc', tax_rate: 0, delivery_fee: 0, min_order_amount: 0 }), { service_fee: 'Phí dịch vụ phải là số không âm' });
  assert.deepEqual(validateFees({ service_fee: '', tax_rate: 0, delivery_fee: 0, min_order_amount: 0 }), { service_fee: 'Phí dịch vụ phải là số không âm' });
  assert.deepEqual(validateFees({ service_fee: 0, tax_rate: 101, delivery_fee: 0, min_order_amount: 0 }), { tax_rate: 'Thuế phải từ 0 đến 100' });
  assert.deepEqual(validateFees({ service_fee: 0, tax_rate: -1, delivery_fee: 0, min_order_amount: 0 }), { tax_rate: 'Thuế phải từ 0 đến 100' });
});

test('validateDelivery accepts integers from 10 to 180', () => {
  assert.deepEqual(validateDelivery(30), {});
  assert.deepEqual(validateDelivery(10), {});
  assert.deepEqual(validateDelivery(180), {});
  assert.deepEqual(validateDelivery('30'), {});
});

test('validateDelivery rejects out-of-range, non-integer and empty values', () => {
  const msg = 'Thời gian giao phải là số nguyên từ 10 đến 180 phút';
  assert.deepEqual(validateDelivery(9), { delivery: msg });
  assert.deepEqual(validateDelivery(181), { delivery: msg });
  assert.deepEqual(validateDelivery(30.5), { delivery: msg });
  assert.deepEqual(validateDelivery(''), { delivery: msg });
  assert.deepEqual(validateDelivery(null), { delivery: msg });
  assert.deepEqual(validateDelivery('abc'), { delivery: msg });
});

test('validateInventory accepts integer low-stock threshold from 1 through 1000', () => {
  assert.deepEqual(validateInventory(1), {});
  assert.deepEqual(validateInventory(5), {});
  assert.deepEqual(validateInventory('1000'), {});
});

test('validateInventory rejects empty, fractional and out-of-range thresholds', () => {
  const message = 'Ngưỡng sắp hết phải là số nguyên từ 1 đến 1000';
  for (const value of ['', null, 0, 1001, 1.5, 'abc']) {
    assert.deepEqual(validateInventory(value), { low_stock_threshold: message });
  }
});

test('buildSettingsPayload sends only persisted inventory threshold', () => {
  assert.deepEqual(buildSettingsPayload('inventory', { low_stock_threshold: '7', delivery_fee: 15000 }), {
    payload: { low_stock_threshold: 7 },
    errors: {},
  });
  assert.deepEqual(SCOPE_KEYS.inventory, ['low_stock_threshold']);
});

test('buildSettingsPayload store group trims values and validates name/logo', () => {
  const result = buildSettingsPayload('store', { store_name: '  FastGuy  ', store_phone: '0901234567', store_address: '  Địa chỉ  ', store_logo: '  https://example.com/l.png  ' });
  assert.deepEqual(result.errors, {});
  assert.deepEqual(result.payload, { store_name: 'FastGuy', store_phone: '0901234567', store_address: 'Địa chỉ', store_logo: 'https://example.com/l.png' });
  const bad = buildSettingsPayload('store', { store_name: '', store_phone: '', store_address: '', store_logo: 'bad' });
  assert.ok(bad.errors.name);
  assert.ok(bad.errors.logo);
});

test('buildSettingsPayload sends only group keys for hours, fees, delivery', () => {
  const hours = buildSettingsPayload('hours', { business_open_time: '08:00', business_close_time: '22:00' });
  assert.deepEqual(hours.payload, { business_open_time: '08:00', business_close_time: '22:00' });
  assert.deepEqual(hours.errors, {});
  const fees = buildSettingsPayload('fees', { service_fee: '1000', tax_rate: '8', delivery_fee: '15000', min_order_amount: '20000' });
  assert.deepEqual(fees.payload, { service_fee: 1000, tax_rate: 8, delivery_fee: 15000, min_order_amount: 20000 });
  assert.deepEqual(fees.errors, {});
  const delivery = buildSettingsPayload('delivery', { estimated_delivery_minutes: '45' });
  assert.deepEqual(delivery.payload, { estimated_delivery_minutes: 45 });
  assert.deepEqual(delivery.errors, {});
});

test('buildSettingsPayload returns empty payload for read-only and unknown groups', () => {
  for (const scope of ['payment', 'ghn', 'unknown']) {
    const result = buildSettingsPayload(scope, { anything: 1 });
    assert.deepEqual(result.payload, {});
    assert.deepEqual(result.errors, {});
  }
});

test('SCOPE_KEYS cover exactly the editable settings groups', () => {
  assert.deepEqual(Object.keys(SCOPE_KEYS).sort(), ['delivery', 'fees', 'hours', 'inventory', 'store']);
  assert.deepEqual(SCOPE_KEYS.store, ['store_name', 'store_phone', 'store_address', 'store_logo']);
  assert.deepEqual(SCOPE_KEYS.hours, ['business_open_time', 'business_close_time']);
  assert.deepEqual(SCOPE_KEYS.fees, ['service_fee', 'tax_rate', 'delivery_fee', 'min_order_amount']);
  assert.deepEqual(SCOPE_KEYS.delivery, ['estimated_delivery_minutes']);
});
