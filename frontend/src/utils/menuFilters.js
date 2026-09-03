const defaults = {
  price: 'ALL',
  min: '',
  max: '',
  availability: 'ALL',
  discounted: false,
  bestSeller: false,
};

export function createMenuFilterDraft(filters = {}) {
  return Object.fromEntries(Object.keys(defaults).map(key => [key, filters[key] ?? defaults[key]]));
}

export function applyMenuFilterDraft(draft) {
  return createMenuFilterDraft(draft);
}

const quickKeys = ['bestSeller', 'discounted', 'under40', 'available'];

export function resetMenuFilters() {
  return { ...createMenuFilterDraft(), quickFilters: [] };
}

export function createEffectiveMenuFilterDraft(state = {}) {
  const draft = createMenuFilterDraft(state);
  const quickFilters = state.quickFilters || [];
  if (quickFilters.includes('available')) draft.availability = 'AVAILABLE';
  if (quickFilters.includes('discounted')) draft.discounted = true;
  if (quickFilters.includes('bestSeller')) draft.bestSeller = true;
  if (quickFilters.includes('under40')) Object.assign(draft, { price: 'CUSTOM', min: '', max: '40000' });
  return draft;
}

export function applyEffectiveMenuFilterDraft(state, draft) {
  const previousDraft = createEffectiveMenuFilterDraft(state);
  const next = createMenuFilterDraft(draft);
  const quickFilters = (state.quickFilters || []).filter((key) => {
    if (key === 'available') return next.availability === previousDraft.availability;
    if (key === 'discounted') return next.discounted === previousDraft.discounted;
    if (key === 'bestSeller') return next.bestSeller === previousDraft.bestSeller;
    if (key === 'under40') return next.price === previousDraft.price && String(next.min) === String(previousDraft.min) && String(next.max) === String(previousDraft.max);
    return true;
  });
  if (quickFilters.includes('available')) next.availability = 'ALL';
  if (quickFilters.includes('discounted')) next.discounted = false;
  if (quickFilters.includes('bestSeller')) next.bestSeller = false;
  if (quickFilters.includes('under40')) Object.assign(next, { price: state.price || 'ALL', min: state.min || '', max: state.max || '' });
  return { ...next, quickFilters };
}

export function hydrateMenuFilterState(query = {}) {
  const state = resetMenuFilters();
  state.price = ['UNDER_30', '30_60', 'OVER_60', 'CUSTOM'].includes(query.price) ? query.price : 'ALL';
  state.min = String(query.min || '');
  state.max = String(query.max || '');
  state.availability = ['AVAILABLE', 'OUT_OF_STOCK', 'OUTSIDE_HOURS'].includes(query.availability) ? query.availability : 'ALL';
  state.discounted = query.discounted === 'true';
  state.bestSeller = query.bestSeller === 'true';
  state.quickFilters = [...new Set(String(query.quick || '').split(',').filter(key => quickKeys.includes(key)))];
  if (state.quickFilters.includes('available')) state.availability = 'ALL';
  if (state.quickFilters.includes('discounted')) state.discounted = false;
  if (state.quickFilters.includes('bestSeller')) state.bestSeller = false;
  return state;
}

export function removeMenuFilter(state, key) {
  const next = { ...state, quickFilters: [...(state.quickFilters || [])] };
  if (key.startsWith('quick:')) next.quickFilters = next.quickFilters.filter(item => item !== key.slice(6));
  if (key === 'price') Object.assign(next, { price: 'ALL', min: '', max: '' });
  if (key === 'availability') next.availability = 'ALL';
  if (key === 'discounted') next.discounted = false;
  if (key === 'bestSeller') next.bestSeller = false;
  return next;
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

export function buildMenuCategoryGroups(categories, total) {
  return [
    { key: 'all', name: 'Tất cả', count: Number(total || 0), categoryIds: [], children: [] },
    ...categories.map(item => ({ key: String(item.id), name: item.name, count: Number(item.productCount || item.count || 0), categoryIds: [item.id], children: [] })),
  ];
}

export function quickFilterParams(filters = []) {
  return {
    ...(filters.includes('bestSeller') ? { sold: 1 } : {}),
    ...(filters.includes('discounted') ? { discounted: true } : {}),
    ...(filters.includes('under40') ? { maxPrice: 40000 } : {}),
    ...(filters.includes('available') ? { availability: 'AVAILABLE' } : {}),
  };
}
