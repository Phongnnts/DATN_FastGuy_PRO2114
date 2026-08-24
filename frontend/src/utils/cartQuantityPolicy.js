export const MAX_PRODUCT_QUANTITY = 20;
export const PRODUCT_QUANTITY_LIMIT_MESSAGE = 'Mỗi sản phẩm chỉ được đặt tối đa 20 cái để đảm bảo đơn hàng hợp lệ.';

export function productQuantityInCart(items, productId, excludedKey = null) {
  return (items || []).filter(item => Number(item.productId) === Number(productId) && item.key !== excludedKey).reduce((sum, item) => sum + Number(item.quantity || 0), 0);
}

export function validateProductQuantity(items, productId, quantity, excludedKey = null) {
  const total = productQuantityInCart(items, productId, excludedKey) + Number(quantity || 0);
  return { allowed: total <= MAX_PRODUCT_QUANTITY, total, maximum: MAX_PRODUCT_QUANTITY };
}
