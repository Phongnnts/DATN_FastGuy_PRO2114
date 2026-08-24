const DECIMAL_RE = /^\d+(\.\d{1,4})?$/;

function decimal(value, allowZero = false) {
  const text = String(value ?? '').trim().replace(',', '.');
  if (!DECIMAL_RE.test(text)) return null;
  const number = Number(text);
  return Number.isFinite(number) && (allowZero ? number >= 0 : number > 0) ? number : null;
}

function nullableText(value, max) {
  const text = String(value ?? '').trim();
  return text ? text.slice(0, max) : null;
}

export function validateGoodsReceipt(form) {
  if (!String(form?.supplierName ?? '').trim()) return { supplierName: 'Nhập nhà cung cấp' };
  if (!form?.receivedAt) return { receivedAt: 'Chọn thời gian nhận' };
  if (!Array.isArray(form.items) || !form.items.length) return { items: 'Thêm ít nhất một mặt hàng' };
  const ids = form.items.map((line) => Number(line.inventoryItemId));
  if (new Set(ids).size !== ids.length) return { items: 'Mỗi mặt hàng chỉ được nhập một lần' };
  if (form.items.some((line) => !Number.isInteger(Number(line.inventoryItemId)) || !String(line.purchaseUnit ?? '').trim()
    || decimal(line.purchaseQuantity) === null || decimal(line.conversionFactor) === null || decimal(line.purchaseUnitPrice) === null)) {
    return { items: 'Hoàn tất mặt hàng, số lượng, đơn vị, quy đổi và đơn giá' };
  }
  return {};
}

export function buildGoodsReceiptPayload(form) {
  return {
    supplierName: String(form.supplierName).trim(),
    invoiceNumber: nullableText(form.invoiceNumber, 100),
    receivedAt: form.receivedAt,
    items: form.items.map((line) => ({
      inventoryItemId: Number(line.inventoryItemId),
      purchaseQuantity: decimal(line.purchaseQuantity),
      purchaseUnit: String(line.purchaseUnit).trim(),
      conversionFactor: decimal(line.conversionFactor),
      purchaseUnitPrice: decimal(line.purchaseUnitPrice),
    })),
  };
}

export function goodsReceiptPreview(line) {
  const quantity = decimal(line?.purchaseQuantity);
  const factor = decimal(line?.conversionFactor);
  const price = decimal(line?.purchaseUnitPrice);
  if (quantity === null || factor === null || price === null) return { baseQuantity: null, lineTotal: null, baseUnitCost: null };
  return { baseQuantity: quantity * factor, lineTotal: quantity * price, baseUnitCost: price / factor };
}

export function goodsReceiptTotal(lines) {
  return (lines || []).reduce((total, line) => total + (goodsReceiptPreview(line).lineTotal || 0), 0);
}

export function validateStockCount(lines) {
  if (!Array.isArray(lines) || !lines.length) return { items: 'Phiếu không có mặt hàng cần kiểm' };
  if (lines.some((line) => decimal(line.actualQuantity, true) === null)) return { items: 'Nhập số lượng thực tế cho tất cả mặt hàng' };
  if (lines.some((line) => stockCountVariance(line).quantity !== 0 && !String(line.reasonCode ?? '').trim())) return { items: 'Nhập lý do cho mặt hàng có chênh lệch' };
  return {};
}

export function buildStockCountPayload(lines) {
  return { items: lines.map((line) => ({
    inventoryItemId: Number(line.inventoryItemId),
    theoreticalQuantity: Number(line.theoreticalQuantity),
    actualQuantity: decimal(line.actualQuantity, true),
    reasonCode: nullableText(line.reasonCode, 50),
    note: nullableText(line.note, 500),
  })) };
}

export function stockCountVariance(line, unitCost = 0) {
  const actual = decimal(line?.actualQuantity, true);
  if (actual === null) return { quantity: null, cost: null };
  const quantity = Number((actual - Number(line.theoreticalQuantity)).toFixed(4));
  return { quantity, cost: Number((quantity * Number(unitCost || 0)).toFixed(4)) };
}

export function stockCountProgress(lines) {
  const total = lines?.length || 0;
  const counted = (lines || []).filter((line) => decimal(line.actualQuantity, true) !== null).length;
  return { counted, total, percent: total ? Math.round((counted / total) * 100) : 0 };
}

export function stockCountSummary(lines, itemsById) {
  return (lines || []).reduce((summary, line) => {
    const variance = stockCountVariance(line, itemsById?.[line.inventoryItemId]?.averageUnitCost);
    if (variance.quantity < 0) {
      summary.shortageItemCount += 1;
      summary.lossCost += Math.abs(variance.cost);
    } else if (variance.quantity > 0) summary.surplusItemCount += 1;
    return summary;
  }, { shortageItemCount: 0, surplusItemCount: 0, lossCost: 0 });
}

export function recipeCost(recipe, itemsById, variantPrice) {
  if (!recipe?.items?.length || !(Number(recipe.yieldQuantity) > 0)) return { cost: null, foodCostPercent: null };
  let total = 0;
  for (const line of recipe.items) {
    const unitCost = Number(itemsById?.[line.inventoryItemId]?.averageUnitCost);
    if (!Number.isFinite(unitCost)) return { cost: null, foodCostPercent: null };
    total += Number(line.quantity) * unitCost;
  }
  const cost = Number((total / Number(recipe.yieldQuantity)).toFixed(4));
  const price = Number(variantPrice);
  return { cost, foodCostPercent: Number.isFinite(price) && price > 0 ? Number(((cost / price) * 100).toFixed(2)) : null };
}

export function recipeLinePresentation(line, item, yieldQuantity) {
  const quantity = Number(line?.quantity);
  const yieldValue = Number(yieldQuantity);
  const amountPerServing = quantity > 0 && yieldValue > 0 ? quantity / yieldValue : null;
  const availableQuantity = Number.isFinite(Number(item?.availableQuantity)) ? Number(item.availableQuantity) : null;
  const unitCost = Number(item?.averageUnitCost);
  return {
    amountPerServing,
    availableQuantity,
    estimatedServings: amountPerServing > 0 && availableQuantity !== null ? Math.floor(availableQuantity / amountPerServing) : null,
    costPerServing: amountPerServing !== null && Number.isFinite(unitCost) ? amountPerServing * unitCost : null,
  };
}

function localDate(date) {
  const year = date.getFullYear();
  return `${year}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function dateRangeForDays(days, now = new Date()) {
  const from = new Date(now);
  from.setDate(from.getDate() - Math.max(0, days - 1));
  return { fromDate: localDate(from), toDate: localDate(now) };
}
