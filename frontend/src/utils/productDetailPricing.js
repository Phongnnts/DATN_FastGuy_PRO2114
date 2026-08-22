export function resolveProductDetailPricing(product = {}, selectedVariant = null) {
  const source = selectedVariant || product;
  const price = Number(source.price ?? product.price) || 0;
  const legacyDiscountPrice = Number(source.discountPrice);
  const currentPrice = price > 0 && legacyDiscountPrice > 0 && legacyDiscountPrice < price ? legacyDiscountPrice : price;
  const originalPrice = Number(selectedVariant?.originalPrice ?? product.originalPrice);
  const crossedPrice = originalPrice > currentPrice ? originalPrice : (currentPrice < price ? price : null);
  if (!crossedPrice) return { currentPrice, crossedPrice: null, discountPercent: null };
  const realDiscountPercent = Number(selectedVariant?.discountPercent ?? (selectedVariant && Number(selectedVariant.price) !== Number(product.price) ? null : product.discountPercent));
  const discountPercent = realDiscountPercent > 0 ? Math.round(realDiscountPercent) : Math.round((1 - currentPrice / crossedPrice) * 100);
  return { currentPrice, crossedPrice, discountPercent };
}
