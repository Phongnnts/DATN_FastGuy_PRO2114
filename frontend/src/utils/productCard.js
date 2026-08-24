export function canDirectAddProduct(product) {
  const variant = product?.defaultVariant;
  const stock = variant?.quantityAvailable;
  return Boolean(product?.cardDataComplete === true
    && product?.productType === 'SIMPLE'
    && product?.inStock
    && product?.isAvailableNow !== false
    && Array.isArray(product?.variants)
    && product.variants.length === 1
    && variant?.variantId
    && variant.status === 'AVAILABLE'
    && variant.availabilityStatus !== 'OUT_OF_STOCK'
    && variant.availabilityStatus !== 'SUSPENDED'
    && (stock == null || Number(stock) > 0));
}
