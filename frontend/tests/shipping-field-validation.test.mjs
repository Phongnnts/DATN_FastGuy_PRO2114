import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const checkout = readFileSync(new URL('../src/views/user/CheckoutPage.vue', import.meta.url), 'utf8');
const addresses = readFileSync(new URL('../src/views/user/AddressesPage.vue', import.meta.url), 'utf8');

test('shipping forms share exact field prompts and local validation messages', () => {
  for (const source of [checkout, addresses]) assert.match(source, /FormField/);
  for (const text of ['Họ tên người nhận', 'Chọn quận\/huyện', 'Chọn phường\/xã', 'VD: 123 Nguyễn Huệ', 'Số điện thoại nhận hàng']) assert.match(checkout, new RegExp(text));
});

test('address edit reloads hierarchy even when the province id is unchanged', () => {
  assert.match(addresses, /await loadEditAddressHierarchy\(addr\)/);
  assert.match(addresses, /shippingApi\.getDistricts\(provinceId\)/);
  assert.match(addresses, /shippingApi\.getWards\(districtId\)/);
});

test('shipping validation runs on blur, change, and before persistence', () => {
  for (const source of [checkout, addresses]) {
    assert.match(source, /@blur=/);
    assert.match(source, /validateShippingForm/);
  }
  assert.match(checkout, /runValidatedShippingSubmit\(shippingValidation, shippingValues\(\), executePlaceOrder\)/);
  assert.match(addresses, /runValidatedShippingSubmit\(shippingValidation, shippingValues\(\), persistAddress\)/);
  assert.match(addresses, /<form novalidate @submit\.prevent="saveAddress">/);
  assert.ok((addresses.match(/shippingValidation\.reset\(\)/g) || []).length >= 4);
});
