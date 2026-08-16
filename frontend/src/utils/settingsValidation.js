const TIME_RE = /^([01]\d|2[0-3]):[0-5]\d$/;
const URL_RE = /^https?:\/\/\S+$/i;

export function validateStore(name, phone, address, logo) {
  const errors = {};
  if (!String(name ?? '').trim()) errors.name = 'Tên cửa hàng là bắt buộc';
  const logoValue = String(logo ?? '').trim();
  if (logoValue && !URL_RE.test(logoValue)) errors.logo = 'Logo URL không hợp lệ';
  return errors;
}

export function validateHours(open, close) {
  const errors = {};
  const openValue = String(open ?? '').trim();
  const closeValue = String(close ?? '').trim();
  if (!openValue) errors.open = 'Giờ mở cửa là bắt buộc';
  else if (!TIME_RE.test(openValue)) errors.open = 'Giờ mở cửa không hợp lệ (HH:MM)';
  if (!closeValue) errors.close = 'Giờ đóng cửa là bắt buộc';
  else if (!TIME_RE.test(closeValue)) errors.close = 'Giờ đóng cửa không hợp lệ (HH:MM)';
  return errors;
}

export function validateFees(values = {}) {
  const errors = {};
  const feeFields = [
    ['service_fee', 'Phí dịch vụ'],
    ['delivery_fee', 'Phí giao hàng'],
    ['min_order_amount', 'Đơn tối thiểu'],
  ];
  for (const [key, label] of feeFields) {
    const value = values[key];
    if (value === '' || value === null || value === undefined || !Number.isFinite(Number(value)) || Number(value) < 0) {
      errors[key] = `${label} phải là số không âm`;
    }
  }
  const tax = values.tax_rate;
  if (tax === '' || tax === null || tax === undefined || !Number.isFinite(Number(tax)) || Number(tax) < 0 || Number(tax) > 100) {
    errors.tax_rate = 'Thuế phải từ 0 đến 100';
  }
  return errors;
}

export function validateDelivery(minutes) {
  const errors = {};
  const value = Number(minutes);
  if (minutes === '' || minutes === null || minutes === undefined || !Number.isFinite(value) || !Number.isInteger(value) || value < 10 || value > 180) {
    errors.delivery = 'Thời gian giao phải là số nguyên từ 10 đến 180 phút';
  }
  return errors;
}

export function validateInventory(threshold) {
  const errors = {};
  const value = Number(threshold);
  if (threshold === '' || threshold === null || threshold === undefined || !Number.isInteger(value) || value < 1 || value > 1000) {
    errors.low_stock_threshold = 'Ngưỡng sắp hết phải là số nguyên từ 1 đến 1000';
  }
  return errors;
}

export const SCOPE_KEYS = {
  store: ['store_name', 'store_phone', 'store_address', 'store_logo'],
  hours: ['business_open_time', 'business_close_time'],
  fees: ['service_fee', 'tax_rate', 'delivery_fee', 'min_order_amount'],
  delivery: ['estimated_delivery_minutes'],
  inventory: ['low_stock_threshold'],
};

export function buildSettingsPayload(scopeKey, form = {}) {
  switch (scopeKey) {
    case 'store': {
      const payload = {
        store_name: String(form.store_name ?? '').trim(),
        store_phone: String(form.store_phone ?? '').trim(),
        store_address: String(form.store_address ?? '').trim(),
        store_logo: String(form.store_logo ?? '').trim(),
      };
      return { payload, errors: validateStore(form.store_name, form.store_phone, form.store_address, form.store_logo) };
    }
    case 'hours': {
      const open = String(form.business_open_time ?? '').trim();
      const close = String(form.business_close_time ?? '').trim();
      return { payload: { business_open_time: open, business_close_time: close }, errors: validateHours(open, close) };
    }
    case 'fees':
      return {
        payload: { service_fee: Number(form.service_fee), tax_rate: Number(form.tax_rate), delivery_fee: Number(form.delivery_fee), min_order_amount: Number(form.min_order_amount) },
        errors: validateFees(form),
      };
    case 'delivery':
      return { payload: { estimated_delivery_minutes: Number(form.estimated_delivery_minutes) }, errors: validateDelivery(form.estimated_delivery_minutes) };
    case 'inventory':
      return {
        payload: { low_stock_threshold: Number(form.low_stock_threshold) },
        errors: validateInventory(form.low_stock_threshold),
      };
    default:
      return { payload: {}, errors: {} };
  }
}
