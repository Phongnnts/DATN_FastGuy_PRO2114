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

export function validateStockCount(lines) {
  if (!Array.isArray(lines) || !lines.length) return { items: 'Phiếu không có mặt hàng cần kiểm' };
  if (lines.some((line) => decimal(line.actualQuantity, true) === null)) return { items: 'Nhập số lượng thực tế cho tất cả mặt hàng' };
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
  const quantity = Number((Number(line.actualQuantity) - Number(line.theoreticalQuantity)).toFixed(4));
  return { quantity, cost: Number((quantity * Number(unitCost || 0)).toFixed(4)) };
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

function localDate(date) {
  const year = date.getFullYear();
  return `${year}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function dateRangeForDays(days, now = new Date()) {
  const from = new Date(now);
  from.setDate(from.getDate() - Math.max(0, days - 1));
  return { fromDate: localDate(from), toDate: localDate(now) };
}
