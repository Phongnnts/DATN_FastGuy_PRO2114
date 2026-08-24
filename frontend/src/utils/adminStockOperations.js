import { normalizeLowStockThreshold, productStockSummary, stockState } from './stockPolicy.js';

export function createStockPageLoader(state) {
  let generation = 0;
  return {
    async load({ required, dashboard, errorMessage = 'Không thể tải dữ liệu' }) {
      const requestGeneration = ++generation;
      state.loading = true;
      state.error = '';
      if ('dashboardError' in state) state.dashboardError = '';
      const [requiredResults, dashboardResult] = await Promise.all([
        Promise.allSettled(required.map((load) => load())),
        Promise.resolve().then(dashboard).then(
          (value) => ({ status: 'fulfilled', value }),
          (reason) => ({ status: 'rejected', reason }),
        ),
      ]);
      if (requestGeneration !== generation) return;
      if (dashboardResult.status === 'fulfilled') {
        state.threshold = normalizeLowStockThreshold(dashboardResult.value?.lowStockThreshold);
      } else {
        if (state.threshold === null || state.threshold === undefined) state.threshold = normalizeLowStockThreshold();
        if ('dashboardError' in state) state.dashboardError = dashboardResult.reason?.message || 'Không thể cập nhật ngưỡng tồn kho';
      }
      const failure = requiredResults.find(({ status }) => status === 'rejected');
      state.error = failure ? failure.reason?.message || errorMessage : '';
      state.loading = false;
    },
    stop() {
      generation += 1;
    },
  };
}

export function createLatestCatalogFetcher({ load, map, commit }) {
  let generation = 0;
  let loadCurrent = load;
  const fetchLatest = async (...args) => {
    const requestGeneration = ++generation;
    const data = await loadCurrent(...args);
    const items = map(data);
    if (requestGeneration === generation) commit(items);
    return items;
  };
  fetchLatest.setLoad = (nextLoad) => { loadCurrent = nextLoad; };
  return fetchLatest;
}

export function inventoryRowsSummary(rows, threshold) {
  const managedRows = rows.filter((row) => ['OUT', 'LOW', 'IN'].includes(stockState(row.stock, threshold)));
  return {
    managedRows,
    outOfStockRows: managedRows.filter((row) => stockState(row.stock, threshold) === 'OUT'),
    lowStockRows: managedRows.filter((row) => stockState(row.stock, threshold) === 'LOW'),
    unmanagedRows: rows.filter((row) => stockState(row.stock, threshold) === 'UNMANAGED'),
    unknownRows: rows.filter((row) => stockState(row.stock, threshold) === 'UNKNOWN'),
    totalStock: managedRows.reduce((sum, row) => sum + Number(row.stock), 0),
  };
}

export function inventoryRowCanMutate(row, threshold) {
  return ['OUT', 'LOW', 'IN'].includes(stockState(row.stock, threshold));
}

export function inventoryMatchesStockFilter(row, filter, threshold) {
  if (filter === 'ALL') return true;
  if (filter === 'UNAVAILABLE') return row.status !== 'AVAILABLE' || row.productStatus !== 'AVAILABLE';
  return stockState(row.stock, threshold) === filter;
}

export function productMatchesStockFilter(product, filter, threshold) {
  if (!filter) return true;
  const summary = productStockSummary(product, threshold);
  if (filter === 'unlimited') return summary.total === null && summary.unknownSkus === 0;
  if (filter === 'out') return summary.outOfStockSkus > 0;
  if (filter === 'low') return summary.lowStockSkus > 0;
  if (filter === 'unknown') return summary.unknownSkus > 0;
  return summary.outOfStockSkus === 0 && summary.lowStockSkus === 0 && summary.unknownSkus === 0;
}

export function createCapacityPageLoader(state, loadCapacity) {
  let generation = 0;
  let lastSignature = '';
  return {
    async load(products) {
      const variants = (products || []).flatMap(product => product.variants || []).filter(variant => variant.variantId);
      const signature = variants.map(variant => variant.variantId).sort((a,b) => a-b).join(',');
      if (signature === lastSignature) return;
      lastSignature = signature;
      const request = ++generation;
      state.loading = true;
      state.error = '';
      const entries = await Promise.all(variants.map(async variant => {
        try { return [variant.variantId, await loadCapacity(variant.variantId)]; }
        catch (error) { return [variant.variantId, { error: error.message || 'Không thể tải tồn kho' }]; }
      }));
      if (request !== generation) return;
      state.values = Object.fromEntries(entries);
      state.loading = false;
    },
    stop() { generation += 1; lastSignature = ''; },
  };
}

export function variantCapacityPresentation(capacity) {
  if (!capacity || capacity.error) return { label: 'Không xác định', detail: capacity?.error || '', tone: 'secondary' };
  if (capacity.inventoryMode === 'SUSPENDED') return { label: 'Tạm ngừng bán', detail: '', tone: 'danger' };
  if (capacity.inventoryMode === 'UNTRACKED') return { label: 'Không theo dõi tồn', detail: '', tone: 'secondary' };
  const servings = Number(capacity.availableServings ?? 0);
  if (capacity.inventoryMode === 'FINISHED_GOOD') return { label: `Tồn thành phẩm: ${servings} phần`, detail: '', tone: servings > 0 ? 'success' : 'danger' };
  const limiting = (capacity.ingredients || []).find(item => item.limiting)?.name;
  return { label: servings === 0 ? 'Tạm hết' : servings <= 3 ? `Chỉ còn ${servings} phần` : `Có thể bán ${servings} phần`, detail: limiting ? `Giới hạn: ${limiting}` : '', tone: servings === 0 ? 'danger' : servings <= 3 ? 'warning' : 'success' };
}
