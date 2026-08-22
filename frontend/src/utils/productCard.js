export function canDirectAddProduct(product) {
  const variant = product?.defaultVariant;
  const stock = variant?.quantityAvailable;
  return Boolean(product?.cardDataComplete === true
    && product?.productType === 'SIMPLE'
    && product?.inStock
    && product?.isAvailableNow !== false
    && Array.isArray(product?.variants)
    && product.variants.length === 1
    && Array.isArray(product?.modifierGroups)
    && product.modifierGroups.length === 0
    && variant?.variantId
    && variant.status === 'AVAILABLE'
    && (stock == null || Number(stock) > 0));
}
