export function canDirectAddProduct(product) {
  const variant = product?.defaultVariant;
  const stock = variant?.quantityAvailable;
  return Boolean(product?.inStock
    && product?.isAvailableNow !== false
    && variant?.variantId
    && variant.status === 'AVAILABLE'
    && (stock == null || Number(stock) > 0)
    && Array.isArray(product?.modifierGroups)
    && !product.modifierGroups.some((group) => Number(group.minSelections) > 0));
}
