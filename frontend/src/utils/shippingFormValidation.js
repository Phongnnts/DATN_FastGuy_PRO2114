import { required, validPhone } from './formValidation.js';

const fields = ['recipientName', 'phone', 'district', 'ward', 'street'];
const blankState = (value, activeFields = fields) => Object.fromEntries(activeFields.map(field => [field, value]));

export function legacyHierarchyState(address) {
  return { province: address.ghnProvinceId || null, district: address.ghnDistrictId || null, ward: address.ghnWardCode || null, provinceName: address.provinceName || '', districtName: address.districtName || '', wardName: address.wardName || '' };
}

export function addressModeState(mode, address = {}) {
  if (mode === 'manual') return { useNewAddress: true, selectedAddressId: null, recipientName: '', phone: '', street: '', province: null, district: null, ward: null };
  return { useNewAddress: false, selectedAddressId: address.addressId || null, recipientName: address.recipientName || '', phone: address.phone || '', street: address.street || '', province: address.ghnProvinceId || null, district: address.ghnDistrictId || null, ward: address.ghnWardCode || null };
}

export function shippingFieldError(field, values) {
  const value = values[field];
  if (field === 'recipientName') {
    if (!required(value)) return 'Vui lòng nhập tên người nhận';
    return value.trim().length < 2 ? 'Tên người nhận phải có ít nhất 2 ký tự' : '';
  }
  if (field === 'phone') return !required(value) ? 'Vui lòng nhập số điện thoại' : !validPhone(value) ? 'Số điện thoại không hợp lệ' : '';
  if (field === 'province') return value ? '' : 'Vui lòng chọn tỉnh/thành phố';
  if (field === 'district') return value ? '' : 'Vui lòng chọn quận/huyện';
  if (field === 'ward') return value ? '' : 'Vui lòng chọn phường/xã';
  if (field === 'street') {
    if (!required(value)) return 'Vui lòng nhập địa chỉ cụ thể';
    return value.trim().length < 5 ? 'Địa chỉ cụ thể phải có ít nhất 5 ký tự' : '';
  }
  return '';
}

export async function runValidatedShippingSubmit(validation, values, operation) {
  if (!validation.validateAll(values)) return false;
  await operation();
  return true;
}

export function createShippingValidationState(options = {}) {
  const activeFields = options.includeProvince ? ['recipientName', 'phone', 'province', 'district', 'ward', 'street'] : fields;
  const touched = blankState(false, activeFields);
  const errors = blankState('', activeFields);
  const validate = (field, values) => { errors[field] = shippingFieldError(field, values); return !errors[field]; };
  return {
    touched,
    errors,
    touch(field, values) { touched[field] = true; return validate(field, values); },
    update(field, values) { return !touched[field] && !errors[field] ? true : validate(field, values); },
    validateAll(values) { let valid = true; for (const field of activeFields) { touched[field] = true; if (!validate(field, values)) valid = false; } return valid; },
    resetDependents(dependents, values) { for (const field of dependents) if (touched[field]) validate(field, values); },
    reset() { Object.assign(touched, blankState(false, activeFields)); Object.assign(errors, blankState('', activeFields)); },
  };
}
