function searchableText(product) {
  return [product.name, product.categoryName, product.sku, ...(product.variants || []).flatMap((variant) => [variant.variantName, variant.sku])].filter(Boolean).join(' ').toLocaleLowerCase('vi');
}

export function productTypes(products) {
  return [...new Set(products.map((product) => product.productType).filter(Boolean))].sort();
}

export function filterProducts(products, filters = {}) {
  const query = (filters.query || '').trim().toLocaleLowerCase('vi');
  return products.filter((product) => (!query || searchableText(product).includes(query))
    && (!filters.categoryId || String(product.categoryId) === String(filters.categoryId))
    && (!filters.productType || product.productType === filters.productType)
    && (!filters.status || product.status === filters.status));
}

export function catalogAvailabilityPresentation(product) {
  return product?.status === 'AVAILABLE'
    ? { label: 'Đang hiển thị', tone: 'success' }
    : { label: 'Đang ẩn', tone: 'secondary' };
}

export function catalogCounts(products) {
  return {
    total: products.length,
    available: products.filter((product) => product.status === 'AVAILABLE').length,
    outOfStock: products.filter((product) => product.inStock === false).length,
    discounted: products.filter((product) => Number(product.discountPrice) > 0).length,
  };
}

export function paginateProducts(products, requestedPage, pageSize) {
  const pageCount = Math.max(1, Math.ceil(products.length / pageSize));
  const page = Math.min(Math.max(1, Number(requestedPage) || 1), pageCount);
  const start = products.length ? (page - 1) * pageSize : 0;
  const end = Math.min(start + pageSize, products.length);
  return { page, pageCount, start, end, items: products.slice(start, end) };
}
