const emptyDistribution = () => ({ 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 });
const integer = (value, minimum, maximum = Number.MAX_SAFE_INTEGER) => Number.isFinite(Number(value)) ? Math.min(maximum, Math.max(minimum, Math.trunc(Number(value)))) : minimum;

export function normalizeReviewPage(raw = {}) {
  const distribution = emptyDistribution();
  for (let rating = 1; rating <= 5; rating += 1) distribution[rating] = integer(raw.ratingDistribution?.[rating], 0);
  return {
    items: Array.isArray(raw.items) ? raw.items : [],
    total: integer(raw.total, 0),
    page: integer(raw.page, 1),
    size: integer(raw.size, 1, 50),
    averageRating: Number.isFinite(Number(raw.averageRating)) ? Math.min(5, Math.max(0, Number(raw.averageRating))) : 0,
    reviewCount: integer(raw.reviewCount, 0),
    ratingDistribution: distribution,
  };
}

export function createReviewPageController({ requestPage, applyState, size = 10 }) {
  let generation = 0;
  let stopped = false;
  let pendingRequest = Promise.resolve();
  let retryTarget = null;
  let state = { page: 1, size, data: null, loading: false, initialError: '', refreshError: '' };
  const publish = () => applyState({ ...state });

  async function load(productId, { reset = false, page = state.page } = {}) {
    const requestGeneration = ++generation;
    const targetPage = reset ? 1 : page;
    retryTarget = { productId, page: targetPage };
    if (reset) state = { ...state, page: 1, data: null };
    state = { ...state, loading: true, initialError: '', refreshError: '' };
    publish();
    try {
      const data = normalizeReviewPage(await requestPage(productId, { page: targetPage, size: state.size }));
      if (stopped || requestGeneration !== generation) return;
      state = { ...state, page: data.page, data, loading: false };
      publish();
    } catch (error) {
      if (stopped || requestGeneration !== generation) return;
      const message = error.message || 'Không thể tải đánh giá';
      state = state.data
        ? { ...state, loading: false, refreshError: message }
        : { ...state, loading: false, initialError: message };
      publish();
    }
  }

  function startLoad(productId, options) {
    pendingRequest = load(productId, options);
    return pendingRequest;
  }

  function goToPage(page, productId) {
    const total = state.data?.total || 0;
    if (!Number.isInteger(page) || page < 1 || (page - 1) * state.size >= total) return false;
    startLoad(productId, { page });
    return true;
  }

  function retry() {
    if (!retryTarget) return false;
    startLoad(retryTarget.productId, { page: retryTarget.page });
    return true;
  }

  function stop() {
    stopped = true;
    generation += 1;
  }

  return { load: startLoad, goToPage, retry, pending: () => pendingRequest, snapshot: () => ({ ...state }), stop };
}
