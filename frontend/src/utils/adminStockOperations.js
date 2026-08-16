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
