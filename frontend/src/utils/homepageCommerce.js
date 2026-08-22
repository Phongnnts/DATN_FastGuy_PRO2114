const CHIP_FILTERS = [
  { key: 'QUICK', label: 'Ăn nhanh', matches: product => product.productType === 'SIMPLE' },
  { key: 'FULL', label: 'Ăn no', matches: product => Number(product.price) >= 100000 },
  { key: 'SPICY', label: 'Cay một chút', matches: product => Number(product.spiceLevel) > 0 },
  { key: 'PAIR', label: 'Cho 2 người', matches: product => product.productType === 'COMBO' },
  { key: 'GROUP', label: 'Cho nhóm', matches: product => product.productType === 'COMBO' },
  { key: 'UNDER_100', label: 'Dưới 100.000đ', matches: product => Number(product.price) < 100000 },
];

export function signatureHomepageProduct(products) {
  return Array.isArray(products) ? products[0] || null : null;
}

export function homepageRanking(products) {
  return [...products].sort((left, right) => Number(right.soldCount || 0) - Number(left.soldCount || 0) || Number(right.averageRating || 0) - Number(left.averageRating || 0));
}

export function homepageRecommendationChips(products) {
  return CHIP_FILTERS.filter(chip => products.some(chip.matches)).map(({ key, label }) => ({ key, label }));
}

export function homepageRecommendations(products, key) {
  const chip = CHIP_FILTERS.find(item => item.key === key);
  return chip ? products.filter(chip.matches).slice(0, 4) : [];
}

export function newHomepageProducts(products) {
  return products.filter(product => product.isNew === true).slice(0, 4);
}
