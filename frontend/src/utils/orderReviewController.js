export function uniqueReviewProducts(items) {
  const products = new Map();
  for (const item of items || []) {
    const productId = Number(item.productId);
    if (Number.isSafeInteger(productId) && productId > 0 && !products.has(productId)) products.set(productId, item);
  }
  return [...products.values()];
}

const newState = () => ({ status: 'idle', review: null, form: { rating: 5, comment: '' }, submitting: false, error: '' });

export function createOrderReviewController({ getByOrder, create }) {
  let orderId = null;
  let loadGeneration = 0;
  let lifecycleGeneration = 0;
  let stopped = false;
  const states = new Map();

  function applyReviews(reviews) {
    for (const review of reviews || []) {
      const state = states.get(Number(review.productId));
      if (state) Object.assign(state, { status: 'reviewed', review, error: '' });
    }
  }

  const controller = {
    products: [],
    loading: false,
    loadError: '',
    stateFor(productId) {
      return states.get(Number(productId));
    },
    initialize(nextOrderId, items) {
      const numericOrderId = Number(nextOrderId);
      const products = uniqueReviewProducts(items);
      if (orderId !== numericOrderId) {
        orderId = numericOrderId;
        states.clear();
        lifecycleGeneration += 1;
      }
      controller.products = products;
      const productIds = new Set(products.map(product => Number(product.productId)));
      for (const productId of productIds) if (!states.has(productId)) states.set(productId, newState());
      for (const productId of states.keys()) if (!productIds.has(productId)) states.delete(productId);
    },
    async load(nextOrderId, items) {
      controller.initialize(nextOrderId, items);
      if (stopped) return { ignored: true };
      const generation = ++loadGeneration;
      const lifecycle = lifecycleGeneration;
      controller.loading = true;
      controller.loadError = '';
      try {
        const data = await getByOrder(orderId);
        if (stopped || generation !== loadGeneration || lifecycle !== lifecycleGeneration) return { ignored: true };
        applyReviews(data?.reviews);
        return { reviews: data?.reviews || [] };
      } catch (error) {
        if (stopped || generation !== loadGeneration || lifecycle !== lifecycleGeneration) return { ignored: true };
        controller.loadError = error.message || 'Không thể tải trạng thái đánh giá';
        return { error: controller.loadError };
      } finally {
        if (!stopped && generation === loadGeneration && lifecycle === lifecycleGeneration) controller.loading = false;
      }
    },
    async submit(productId) {
      const numericProductId = Number(productId);
      const state = states.get(numericProductId);
      if (stopped || !state || state.submitting || state.review) return { ignored: true };
      const lifecycle = lifecycleGeneration;
      state.submitting = true;
      state.error = '';
      state.status = 'submitting';
      try {
        const payload = {
          orderId,
          productId: numericProductId,
          rating: Number(state.form.rating),
          comment: state.form.comment.trim() || null,
        };
        const review = await create(payload);
        if (stopped || lifecycle !== lifecycleGeneration || states.get(numericProductId) !== state) return { ignored: true };
        state.review = review;
        state.status = 'success';
        return { review };
      } catch (error) {
        if (stopped || lifecycle !== lifecycleGeneration || states.get(numericProductId) !== state) return { ignored: true };
        if (error.status === 409) {
          const result = await controller.load(orderId, controller.products);
          if (stopped || lifecycle !== lifecycleGeneration || states.get(numericProductId) !== state) return { ignored: true };
          if (state.review) return { review: state.review, reloaded: true };
          state.status = 'error';
          state.error = result.error || 'Đánh giá đã tồn tại nhưng chưa thể đồng bộ';
          return { error: state.error };
        }
        state.status = 'error';
        state.error = error.message || 'Không thể gửi đánh giá';
        return { error: state.error };
      } finally {
        if (!stopped && lifecycle === lifecycleGeneration && states.get(numericProductId) === state) state.submitting = false;
      }
    },
    stop() {
      stopped = true;
      loadGeneration += 1;
      lifecycleGeneration += 1;
      controller.loading = false;
    },
  };
  return controller;
}
