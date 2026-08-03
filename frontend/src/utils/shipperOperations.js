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
