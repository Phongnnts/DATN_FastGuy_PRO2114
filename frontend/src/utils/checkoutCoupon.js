export function createCouponController(state, verifyCoupon) {
  let generation = 0;

  function clearResult() {
    state.applied = null;
    state.discount = 0;
    state.error = '';
  }

  function invalidate() {
    generation += 1;
    state.verifying = false;
    clearResult();
  }

  async function verify(subtotal, shippingFee) {
    const code = state.code.trim();
    if (!code) return;
    const requestGeneration = ++generation;
    const requestSubtotal = Number(subtotal) || 0;
    const requestShippingFee = Number(shippingFee) || 0;
    state.verifying = true;
    clearResult();
    try {
      const result = await verifyCoupon(code, requestSubtotal, requestShippingFee);
      if (requestGeneration !== generation) return;
      if (result.valid) {
        state.applied = result;
        state.discount = result.discount;
      } else {
        state.error = result.message || 'Mã không hợp lệ';
      }
    } catch {
      if (requestGeneration === generation) state.error = 'Lỗi kiểm tra mã';
    } finally {
      if (requestGeneration === generation) state.verifying = false;
    }
  }

  function remove() {
    invalidate();
    state.code = '';
  }

  return { verify, invalidate, remove };
}
