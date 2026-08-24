export function cartStockLimit(item) {
  if (item?.inventoryMode === 'UNTRACKED') return null;
  if (item?.remainingServings != null) return Number(item.remainingServings);
  if (item?.inventoryMode === 'INGREDIENT' || item?.inventoryMode === 'FINISHED_GOOD' || item?.inventoryMode === 'SUSPENDED') {
    return 0;
  }
  return item?.quantityAvailable == null ? null : Number(item.quantityAvailable);
}
