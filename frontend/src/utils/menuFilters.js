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
