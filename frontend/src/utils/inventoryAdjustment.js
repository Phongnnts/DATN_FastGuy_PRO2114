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
