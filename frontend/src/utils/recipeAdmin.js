import { buildRecipePayload, formatQuantity } from './inventoryItem.js';

const UNIT_LABELS = { G: 'g', ML: 'ml', PIECE: 'cái' };

export function formatInventoryQuantity(value, unit) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return '—';
  if (unit === 'G' && Math.abs(amount) >= 1000) return `${formatQuantity(amount / 1000)} kg`;
  if (unit === 'ML' && Math.abs(amount) >= 1000) return `${formatQuantity(amount / 1000)} L`;
  return `${formatQuantity(amount)} ${UNIT_LABELS[unit] || unit || ''}`.trim();
}

export function formatReferenceCost(value, unit) {
  const cost = Number(value);
  if (!Number.isFinite(cost)) return 'Chưa có';
  const factor = unit === 'G' || unit === 'ML' ? 1000 : 1;
  const suffix = unit === 'G' ? 'kg' : unit === 'ML' ? 'L' : 'cái';
  return `${Math.round(cost * factor).toLocaleString('vi-VN')} ₫/${suffix}`;
}

export function buildInventorySettingsPayload(inventoryMode, expectedUpdatedAt) {
  return { inventoryMode, expectedUpdatedAt };
}

export function presentRecipeDraft(draft, inventoryItems, variantPrice) {
  const byId = new Map((inventoryItems || []).map((item) => [Number(item.inventoryItemId), item]));
  const yieldQuantity = Number(draft?.yieldQuantity);
  const missingCostItems = [];
  let totalCost = 0;
  const lines = (draft?.items || []).map((line) => {
    const item = byId.get(Number(line.inventoryItemId)) || {};
    const requiredPerServing = Number(line.quantity) > 0 && yieldQuantity > 0 ? Number(line.quantity) / yieldQuantity : null;
    const availableQuantity = Number(item.availableQuantity);
    const averageUnitCost = item.averageUnitCost == null ? null : Number(item.averageUnitCost);
    const availableServings = requiredPerServing > 0 && Number.isFinite(availableQuantity) ? Math.floor(availableQuantity / requiredPerServing) : null;
    const costAvailable = Number.isFinite(averageUnitCost) && averageUnitCost > 0;
    const costPerServing = costAvailable && requiredPerServing != null ? averageUnitCost * requiredPerServing : null;
    if (!costAvailable) missingCostItems.push({ inventoryItemId: Number(line.inventoryItemId), name: item.name || 'Mặt hàng chưa xác định' });
    else totalCost += costPerServing || 0;
    return { ...item, inventoryItemId: Number(line.inventoryItemId), requiredPerServing, availableServings, costPerServing, costAvailable, limiting: false };
  });
  const finite = lines.filter((line) => line.availableServings != null);
  const availableServings = finite.length ? Math.min(...finite.map((line) => line.availableServings)) : null;
  const limiting = finite.find((line) => line.availableServings === availableServings);
  if (limiting) limiting.limiting = true;
  const complete = lines.length > 0 && missingCostItems.length === 0;
  const recipeCostPerServing = complete ? Number(totalCost.toFixed(4)) : null;
  const price = Number(variantPrice);
  return {
    lines,
    availableServings,
    limitingInventoryItemId: limiting?.inventoryItemId ?? null,
    costStatus: complete ? 'COMPLETE' : 'INCOMPLETE',
    recipeCostPerServing,
    foodCostPercent: complete && price > 0 ? Number(((recipeCostPerServing / price) * 100).toFixed(2)) : null,
    missingCostItemCount: missingCostItems.length,
    missingCostItems,
  };
}

export function isRecipeDraftDirty(draft, baseline) {
  if (!baseline) return false;
  return JSON.stringify(buildRecipePayload(draft)) !== JSON.stringify(baseline.recipe);
}

export function isInventorySettingsDirty(inventoryMode, baseline) {
  return Boolean(baseline) && inventoryMode !== baseline.inventoryMode;
}
