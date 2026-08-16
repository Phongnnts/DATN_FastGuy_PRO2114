export const DEFAULT_LOW_STOCK_THRESHOLD = 5;

export function normalizeLowStockThreshold(value) {
  const threshold = Number(value);
  return Number.isInteger(threshold) && threshold >= 1 && threshold <= 1000
    ? threshold
    : DEFAULT_LOW_STOCK_THRESHOLD;
}

export function stockState(quantity, thresholdValue) {
  if (quantity === null || quantity === undefined) return 'UNMANAGED';
  const stock = Number(quantity);
  if (!Number.isFinite(stock)) return 'UNKNOWN';
  const threshold = normalizeLowStockThreshold(thresholdValue);
  if (stock <= 0) return 'OUT';
  if (stock <= threshold) return 'LOW';
  return 'IN';
}

export function productStockSummary(product, thresholdValue) {
  const variants = Array.isArray(product?.variants) ? product.variants : [];
  const states = variants.map((variant) => ({
    quantity: variant.quantityAvailable,
    state: stockState(variant.quantityAvailable, thresholdValue),
  }));
  const managed = states.filter(({ state }) => !['UNMANAGED', 'UNKNOWN'].includes(state));
  const unknownSkus = states.filter(({ state }) => state === 'UNKNOWN').length;
  const outOfStockSkus = managed.filter(({ state }) => state === 'OUT').length;
  const status = product?.status && product.status !== 'AVAILABLE'
    ? 'UNAVAILABLE'
    : unknownSkus > 0
      ? 'UNKNOWN'
      : managed.length > 0 && outOfStockSkus === managed.length && states.every(({ state }) => state !== 'UNMANAGED')
        ? 'OUT'
        : 'AVAILABLE';
  return {
    total: states.some(({ state }) => ['UNMANAGED', 'UNKNOWN'].includes(state))
      ? null
      : managed.reduce((sum, { quantity }) => sum + Number(quantity), 0),
    outOfStockSkus,
    lowStockSkus: managed.filter(({ state }) => state === 'LOW').length,
    managedSkus: managed.length,
    unknownSkus,
    status,
  };
}
