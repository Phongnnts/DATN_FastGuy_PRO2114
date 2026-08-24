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

export function customerAvailability(variant) {
  const status = variant?.availabilityStatus;
  if (status === 'LOW_STOCK') {
    const servings = Number(variant?.remainingServings);
    return {
      status,
      remainingServings: servings,
      label: Number.isInteger(servings) && servings >= 1 && servings <= 3 ? `Chỉ còn ${servings} phần` : 'Sắp hết',
      available: true,
    };
  }
  if (status === 'OUT_OF_STOCK' || status === 'SUSPENDED') return { status, remainingServings: null, label: 'Tạm hết', available: false };
  if (status === 'IN_STOCK') {
    const servings = Number(variant?.remainingServings);
    return Number.isInteger(servings) && servings > 0
      ? { status, remainingServings: servings, label: `Còn ${servings} phần`, available: true }
      : { status, remainingServings: null, label: 'Còn hàng', available: true };
  }
  if (status === 'UNTRACKED') return { status, remainingServings: null, label: 'Còn hàng', available: true };
  const quantity = variant?.quantityAvailable;
  const managed = quantity !== null && quantity !== undefined;
  const available = variant?.status !== 'UNAVAILABLE' && !(managed && Number(quantity) <= 0);
  return { status: available ? 'IN_STOCK' : 'OUT_OF_STOCK', remainingServings: null, label: available ? 'Còn hàng' : 'Tạm hết', available };
}
