export function normalizePurchaseQuantity(value, stock) {
  const parsed = Math.floor(Number(value));
  const quantity = Number.isFinite(parsed) && parsed > 0 ? parsed : 1;
  const limited = Math.min(quantity, 20);
  return stock == null ? limited : Math.min(limited, Math.max(1, Number(stock) || 1));
}
