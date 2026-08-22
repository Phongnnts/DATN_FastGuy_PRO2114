function normalize(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLocaleLowerCase('vi');
}

export function menuSearchSuggestions(products, query, limit = 5) {
  const term = normalize(query).trim();
  if (!term) return [];
  return [...products]
    .map((product, index) => {
      const name = normalize(product.name);
      const description = normalize(product.description);
      const nameIndex = name.indexOf(term);
      const descriptionIndex = description.indexOf(term);
      return { product, index, rank: nameIndex === 0 ? 0 : nameIndex > 0 ? 1 : descriptionIndex >= 0 ? 2 : 3 };
    })
    .filter(item => item.rank < 3)
    .sort((a, b) => a.rank - b.rank || a.index - b.index)
    .slice(0, limit)
    .map(item => item.product);
}
