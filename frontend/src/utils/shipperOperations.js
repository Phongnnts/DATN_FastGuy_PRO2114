export function acceptsShipperRequest({ requestGeneration, latestGeneration, requestMode, currentMode, stopped = false }) {
  return !stopped && requestGeneration === latestGeneration && (!requestMode || requestMode === currentMode);
}

export function isActiveShipperMode(routeName) {
  return routeName === 'ShipperOrders';
}

export function validateExactCod(value, total) {
  if (value === '' || value == null) return { valid: false, amount: null };
  const amount = Number(value);
  const expected = Number(total);
  return { valid: Number.isFinite(amount) && Number.isFinite(expected) && amount === expected, amount };
}

const failureLabels = {
  CUSTOMER_UNREACHABLE: 'Không liên hệ được khách hàng',
  INVALID_ADDRESS: 'Địa chỉ không hợp lệ',
  CUSTOMER_RESCHEDULED: 'Khách hẹn giao lại',
  CUSTOMER_REJECTED: 'Khách từ chối nhận hàng',
  SHIPPER_INCIDENT: 'Shipper gặp sự cố',
  PRODUCT_INCIDENT: 'Sản phẩm gặp sự cố',
};

export function deliveryFailureLabel(code) {
  return failureLabels[code] || code || 'Không rõ';
}

export function failureFocusTarget({ activeIndex, lastIndex, shiftKey }) {
  if (activeIndex < 0) return shiftKey ? lastIndex : 0;
  if (shiftKey && activeIndex === 0) return lastIndex;
  if (!shiftKey && activeIndex === lastIndex) return 0;
  return null;
}

export function failureRestoreTarget(opener, fallback) {
  return opener?.isConnected ? opener : fallback;
}
