const defaults = {
  price: 'ALL',
  min: '',
  max: '',
  availability: 'ALL',
  discounted: false,
  bestSeller: false,
};

export function createMenuFilterDraft(filters = {}) {
  return { ...defaults, ...filters };
}

export function applyMenuFilterDraft(draft) {
  return createMenuFilterDraft(draft);
}

export function menuFilterCount(filters) {
  return Number(filters.price !== 'ALL')
    + Number(filters.availability !== 'ALL')
    + Number(Boolean(filters.discounted))
    + Number(Boolean(filters.bestSeller));
}

export function paginationRange(page, size, total) {
  if (!total) return { from: 0, to: 0 };
  const from = Math.min((page - 1) * size + 1, total);
  return { from, to: Math.min(page * size, total) };
}

function normalize(value) {
  return String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLocaleLowerCase('vi');
}

export function buildMenuCategoryGroups(categories, total) {
  const items = categories.map(item => ({ id: item.id, name: item.name, count: Number(item.productCount || item.count || 0) }));
  const rice = items.filter(item => /^com(?:\s|$)/.test(normalize(item.name)));
  const groups = [{ key: 'all', name: 'Tất cả', count: Number(total || 0), categoryIds: [], children: [] }];
  items.filter(item => !rice.includes(item)).forEach(item => groups.push({ key: String(item.id), name: item.name, count: item.count, categoryIds: [item.id], children: [] }));
  if (rice.length) groups.push({ key: 'rice', name: 'Cơm', count: rice.reduce((sum, item) => sum + item.count, 0), categoryIds: rice.map(item => item.id), children: rice });
  return groups;
}

export function quickFilterParams(filters = []) {
  return {
    ...(filters.includes('bestSeller') ? { sold: 1 } : {}),
    ...(filters.includes('discounted') ? { discounted: true } : {}),
    ...(filters.includes('under40') ? { maxPrice: 40000 } : {}),
  };
}

export function matchesMenuDiscovery(product, filter) {
  const phrase = filter === 'officeCombo' ? 'van phong' : filter === 'studentCombo' ? 'sinh vien' : '';
  return Boolean(phrase && normalize(`${product.name} ${product.description}`).includes(phrase));
}
