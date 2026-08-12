const CONFLICT_MESSAGE = 'Tồn kho đã thay đổi. Đã cập nhật số lượng hiện tại, vui lòng kiểm tra và gửi lại.';

export function adjustmentState(operation, rawQuantity, currentQuantity) {
  if (rawQuantity === '') return { projectedQuantity: null, canSubmit: false };
  const quantity = Number(rawQuantity);
  if (!Number.isInteger(quantity)) return { projectedQuantity: null, canSubmit: false };
  const projectedQuantity = operation === 'INCREASE'
    ? currentQuantity + quantity
    : operation === 'DECREASE'
      ? currentQuantity - quantity
      : quantity;
  const validQuantity = operation === 'SET' ? quantity >= 0 : quantity > 0;
  return { projectedQuantity, canSubmit: validQuantity && projectedQuantity >= 0 && projectedQuantity !== currentQuantity };
}

export function nextOperationIndex(key, currentIndex, count) {
  if (key === 'ArrowRight') return (currentIndex + 1) % count;
  if (key === 'ArrowLeft') return (currentIndex - 1 + count) % count;
  if (key === 'Home') return 0;
  if (key === 'End') return count - 1;
  return null;
}

export function nextFocusIndex(currentIndex, count, backwards) {
  if (backwards && currentIndex === 0) return count - 1;
  if (!backwards && currentIndex === count - 1) return 0;
  return null;
}

export async function submitAdjustment(mutate, payload) {
  try {
    const result = await mutate(payload);
    if (result?.changed === false) {
      return { close: false, currentQuantity: result.currentQuantity, error: 'Tồn kho không thay đổi' };
    }
    return { close: true, currentQuantity: result?.currentQuantity, error: '' };
  } catch (error) {
    if (error.status === 409) {
      return { close: false, currentQuantity: error.data?.currentQuantity, error: CONFLICT_MESSAGE };
    }
    throw error;
  }
}
