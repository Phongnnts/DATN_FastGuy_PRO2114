const active = value => value?.status !== 'INACTIVE' && value?.isActive !== false;
const safePositiveInteger = value => Number.isSafeInteger(value) && value > 0;
const id = value => Number(value);
const itemReason = (item, reason) => `${item.productName || `Sản phẩm ${item.productId}`}: ${reason}`;

function resolveModifiers(product, snapshots) {
  const groups = (product.modifierGroups || []).filter(active);
  const groupIds = groups.map(group => id(group.modifierGroupId));
  if (groupIds.some(value => !safePositiveInteger(value)) || new Set(groupIds).size !== groupIds.length) return null;

  const options = new Map();
  for (const group of groups) {
    for (const option of (group.options || []).filter(active)) {
      const groupId = id(group.modifierGroupId);
      const optionGroupId = id(option.groupId ?? groupId);
      const optionId = id(option.modifierOptionId);
      const key = `${optionGroupId}:${optionId}`;
      if (optionGroupId !== groupId || !safePositiveInteger(optionId) || options.has(key)) return null;
      options.set(key, { ...option, groupId, groupName: option.groupName || group.name || '' });
    }
  }

  const snapshotKeys = snapshots.map(snapshot => `${id(snapshot.groupId)}:${id(snapshot.modifierOptionId)}`);
  const snapshotOptionIds = snapshots.map(snapshot => id(snapshot.modifierOptionId));
  if (new Set(snapshotKeys).size !== snapshotKeys.length || new Set(snapshotOptionIds).size !== snapshotOptionIds.length) return null;
  const resolved = snapshotKeys.map(key => options.get(key));
  if (resolved.some(option => !option)) return null;

  for (const group of groups) {
    const count = resolved.filter(option => option.groupId === id(group.modifierGroupId)).length;
    const min = Number(group.minSelections ?? 0);
    const max = Number(group.maxSelections ?? Number.MAX_SAFE_INTEGER);
    if (!Number.isSafeInteger(min) || !Number.isSafeInteger(max) || min < 0 || max < min || count < min || count > max) return null;
  }

  return resolved.map(option => ({
    modifierOptionId: id(option.modifierOptionId),
    groupId: id(option.groupId),
    groupName: option.groupName,
    name: option.name || '',
    price: Number(option.price),
  }));
}

export async function planReorder(items, fetchProduct) {
  const invalid = [];
  const candidates = [];
  const products = new Map();

  for (const item of items) {
    if (!safePositiveInteger(item.quantity)) {
      invalid.push(itemReason(item, 'số lượng không hợp lệ'));
      continue;
    }
    let product;
    try {
      if (!products.has(item.productId)) products.set(item.productId, await fetchProduct(item.productId));
      product = products.get(item.productId);
    } catch (error) {
      invalid.push(itemReason(item, error.message || 'không thể tải sản phẩm'));
      continue;
    }
    if (!product || product.isAvailable !== true || product.inStock !== true || product.isAvailableNow !== true) {
      invalid.push(itemReason(item, 'sản phẩm không còn khả dụng'));
      continue;
    }
    const variant = (product.variants || []).find(value => id(value.variantId) === id(item.variantId));
    const stock = variant?.quantityAvailable === null || variant?.quantityAvailable === undefined ? null : Number(variant.quantityAvailable);
    if (!variant || variant.status !== 'AVAILABLE' || (stock !== null && (!Number.isFinite(stock) || stock < 0))) {
      invalid.push(itemReason(item, 'phiên bản hoặc tồn kho không còn khả dụng'));
      continue;
    }
    const modifiers = resolveModifiers(product, Array.isArray(item.modifiers) ? item.modifiers : []);
    if (!modifiers || modifiers.some(option => !Number.isFinite(option.price))) {
      invalid.push(itemReason(item, 'tùy chọn không còn khả dụng'));
      continue;
    }
    candidates.push({
      productId: id(item.productId),
      variantId: id(item.variantId),
      quantity: item.quantity,
      productName: item.productName || product.name || '',
      modifiers,
      stock,
    });
  }

  const aggregate = new Map();
  for (const candidate of candidates) {
    const optionIds = candidate.modifiers.map(option => option.modifierOptionId).sort((a, b) => a - b);
    const key = `${candidate.productId}:${candidate.variantId}:${optionIds.join(',')}`;
    const existing = aggregate.get(key);
    if (existing) existing.quantity += candidate.quantity;
    else aggregate.set(key, { ...candidate });
  }

  const entries = [];
  for (const candidate of aggregate.values()) {
    if (!safePositiveInteger(candidate.quantity) || (candidate.stock !== null && candidate.quantity > candidate.stock)) {
      invalid.push(itemReason(candidate, 'số lượng vượt quá tồn kho'));
      continue;
    }
    const { stock, ...entry } = candidate;
    entries.push(entry);
  }
  return { entries, invalid };
}

export async function executeReorderPlan(entries, addItem) {
  const failed = [];
  let added = 0;
  for (const entry of entries) {
    try {
      await addItem(entry.productId, entry.variantId, entry.quantity, entry.modifiers);
      added += 1;
    } catch (error) {
      failed.push(itemReason(entry, error.message || 'không thể thêm vào giỏ'));
    }
  }
  return { added, failed };
}

function resultMessage(added, failed) {
  if (added > 0 && failed.length === 0) return { kind: 'success', message: `Đã thêm ${added} món vào giỏ hàng.` };
  if (added > 0) return { kind: 'partial', message: `Đã thêm ${added} món. Không thể thêm ${failed.join('; ')}.` };
  return { kind: 'error', message: `Không thể thêm ${failed.join('; ')}.` };
}

export function reorderItemKey(item, index) {
  const identity = `${item.productId}:${item.variantId}:${item.quantity}:${(item.modifiers || []).map(modifier => `${modifier.groupId}:${modifier.modifierOptionId}`).sort().join(',')}`;
  return item.orderItemId === null || item.orderItemId === undefined ? `${index}:${identity}` : `id:${item.orderItemId}:${identity}`;
}

export function createReorderController({ fetchProduct, addItem }) {
  let running = false;
  return {
    async run(items) {
      if (running) return { ignored: true };
      running = true;
      try {
        const plan = await planReorder(items, fetchProduct);
        const execution = await executeReorderPlan(plan.entries, addItem);
        const failed = [...plan.invalid, ...execution.failed];
        return { ...execution, failed, ...resultMessage(execution.added, failed) };
      } finally {
        running = false;
      }
    },
  };
}
