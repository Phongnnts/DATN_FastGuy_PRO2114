import assert from 'node:assert/strict';
import test from 'node:test';
import { addressModeState, createShippingValidationState, legacyHierarchyState, runValidatedShippingSubmit, shippingFieldError } from '../src/utils/shippingFormValidation.js';

const validValues = { recipientName: 'An', phone: '0912345678', district: 1, ward: '001', street: '12345' };

test('shipping field validation preserves recipient and street minimum lengths', () => {
  assert.equal(shippingFieldError('recipientName', { ...validValues, recipientName: 'A' }), 'Tên người nhận phải có ít nhất 2 ký tự');
  assert.equal(shippingFieldError('street', { ...validValues, street: '1234' }), 'Địa chỉ cụ thể phải có ít nhất 5 ký tự');
  assert.equal(shippingFieldError('recipientName', validValues), '');
  assert.equal(shippingFieldError('street', validValues), '');
});

test('invalid submit touches every field and does not invoke persistence', async () => {
  const state = createShippingValidationState();
  let calls = 0;
  const submitted = await runValidatedShippingSubmit(state, { recipientName: '', phone: '', district: null, ward: null, street: '' }, async () => { calls += 1; });
  assert.equal(submitted, false);
  assert.equal(calls, 0);
  assert.deepEqual(state.touched, { recipientName: true, phone: true, district: true, ward: true, street: true });
});

test('address validation requires province without changing district and ward wording', () => {
  assert.equal(shippingFieldError('province', { ...validValues, province: null }), 'Vui lòng chọn tỉnh/thành phố');
  assert.equal(shippingFieldError('district', { ...validValues, district: null }), 'Vui lòng chọn quận/huyện');
  assert.equal(shippingFieldError('ward', { ...validValues, ward: null }), 'Vui lòng chọn phường/xã');
});

test('legacy hierarchy fallback preserves saved ids and labels when lookup fails', () => {
  const saved = { ghnProvinceId: 202, ghnDistrictId: 1450, ghnWardCode: '21010', provinceName: 'TP. Hồ Chí Minh', districtName: 'Quận 1', wardName: 'Bến Nghé' };
  assert.deepEqual(legacyHierarchyState(saved), { province: 202, district: 1450, ward: '21010', provinceName: 'TP. Hồ Chí Minh', districtName: 'Quận 1', wardName: 'Bến Nghé' });
});

test('checkout mode transitions clear manual state and restore selected saved state', () => {
  const saved = { addressId: 7, recipientName: 'An', phone: '0912345678', street: '12345', ghnProvinceId: 202, ghnDistrictId: 1450, ghnWardCode: '21010' };
  assert.deepEqual(addressModeState('manual'), { useNewAddress: true, selectedAddressId: null, recipientName: '', phone: '', street: '', province: null, district: null, ward: null });
  assert.deepEqual(addressModeState('saved', saved), { useNewAddress: false, selectedAddressId: 7, recipientName: 'An', phone: '0912345678', street: '12345', province: 202, district: 1450, ward: '21010' });
});

test('dependent resets immediately revalidate only touched district and ward fields', () => {
  const state = createShippingValidationState();
  state.touch('district', validValues);
  state.touch('ward', validValues);
  state.resetDependents(['district', 'ward'], { ...validValues, district: null, ward: null });
  assert.equal(state.errors.district, 'Vui lòng chọn quận/huyện');
  assert.equal(state.errors.ward, 'Vui lòng chọn phường/xã');
  state.reset();
  assert.deepEqual(state.errors, { recipientName: '', phone: '', district: '', ward: '', street: '' });
  assert.deepEqual(state.touched, { recipientName: false, phone: false, district: false, ward: false, street: false });
});
