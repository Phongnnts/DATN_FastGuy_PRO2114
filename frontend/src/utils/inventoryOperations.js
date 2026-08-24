const DECIMAL_RE = /^\d+(\.\d{1,4})?$/;
const PACKAGING_UNITS = new Set(['bao', 'hộp', 'khay', 'thùng', 'chai']);
const STANDARD_UNITS = {
  G: [{ value: 'g', factor: 1 }, { value: 'kg', factor: 1000 }],
  ML: [{ value: 'ml', factor: 1 }, { value: 'L', factor: 1000 }],
  PIECE: [{ value: 'cái', factor: 1 }],
};
const PACKAGING_BY_BASE = {
  G: ['bao', 'hộp', 'khay', 'thùng'],
  ML: ['chai', 'hộp', 'thùng'],
  PIECE: ['hộp', 'khay', 'thùng'],
};

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

export function goodsReceiptUnitOptions(baseUnit) {
  return [...(STANDARD_UNITS[baseUnit] || []), ...(PACKAGING_BY_BASE[baseUnit] || []).map(value => ({ value, factor: null }))];
}

export function goodsReceiptContentUnits(baseUnit) {
  return STANDARD_UNITS[baseUnit] || [];
}

export function resolveGoodsReceiptLine(line, baseUnit) {
  const standard = (STANDARD_UNITS[baseUnit] || []).find(unit => unit.value === line?.purchaseUnit);
  if (standard) return { purchaseUnit: standard.value, conversionFactor: standard.factor };
  if (PACKAGING_UNITS.has(line?.purchaseUnit)) {
    const contentUnit = (STANDARD_UNITS[baseUnit] || []).find(unit => unit.value === line?.packageUnit);
    const contentQuantity = decimal(line?.packageQuantity);
    if (contentUnit && contentQuantity !== null) return { purchaseUnit: line.purchaseUnit, conversionFactor: contentQuantity * contentUnit.factor };
    const legacyFactor = decimal(line?.conversionFactor);
    return legacyFactor === null ? null : { purchaseUnit: line.purchaseUnit, conversionFactor: legacyFactor };
  }
  const legacyFactor = decimal(line?.conversionFactor);
  return String(line?.purchaseUnit ?? '').trim() && legacyFactor !== null
    ? { purchaseUnit: String(line.purchaseUnit).trim(), conversionFactor: legacyFactor }
    : null;
}

export function hydrateGoodsReceiptLine(line, item) {
  const factor = Number(line.conversionFactor);
  const known = goodsReceiptUnitOptions(item?.baseUnit).some(unit => unit.value === line.purchaseUnit);
  const standard = (STANDARD_UNITS[item?.baseUnit] || []).find(unit => unit.value === line.purchaseUnit && unit.factor === factor);
  if (standard) return { inventoryItemId: String(line.inventoryItemId), purchaseQuantity: String(line.purchaseQuantity), purchaseUnit: line.purchaseUnit, packageQuantity: '', packageUnit: '', purchaseUnitPrice: String(line.purchaseUnitPrice), legacyUnit: false };
  const contentUnits = STANDARD_UNITS[item?.baseUnit] || [];
  const contentUnit = contentUnits.find(unit => unit.factor === 1000 && factor >= 1000) || contentUnits[0];
  return {
    inventoryItemId: String(line.inventoryItemId), purchaseQuantity: String(line.purchaseQuantity), purchaseUnit: line.purchaseUnit,
    packageQuantity: String(factor / (contentUnit?.factor || 1)), packageUnit: contentUnit?.value || '', purchaseUnitPrice: String(line.purchaseUnitPrice), legacyUnit: !known,
  };
}

export function validateGoodsReceipt(form, itemsById = {}) {
  if (!form?.receivedAt) return { receivedAt: 'Chọn thời gian nhận' };
  if (!Array.isArray(form.items) || !form.items.length) return { items: 'Thêm ít nhất một mặt hàng' };
  const ids = form.items.map((line) => Number(line.inventoryItemId));
  if (new Set(ids).size !== ids.length) return { items: 'Mỗi mặt hàng chỉ được nhập một lần' };
  if (form.items.some((line) => !Number.isInteger(Number(line.inventoryItemId))
    || decimal(line.purchaseQuantity) === null || decimal(line.purchaseUnitPrice) === null
    || !resolveGoodsReceiptLine(line, itemsById?.[line.inventoryItemId]?.baseUnit))) {
    return { items: 'Hoàn tất mặt hàng, số lượng nhận, đơn vị mua và giá mua' };
  }
  return {};
}

export function buildGoodsReceiptPayload(form, itemsById = {}) {
  return {
    supplierName: String(form.supplierName ?? '').trim(),
    invoiceNumber: nullableText(form.invoiceNumber, 100),
    receivedAt: form.receivedAt,
    items: form.items.map((line) => {
      const conversion = resolveGoodsReceiptLine(line, itemsById?.[line.inventoryItemId]?.baseUnit);
      return { inventoryItemId: Number(line.inventoryItemId), purchaseQuantity: decimal(line.purchaseQuantity), ...conversion, purchaseUnitPrice: decimal(line.purchaseUnitPrice) };
    }),
  };
}

export function goodsReceiptPreview(line, item) {
  const quantity = decimal(line?.purchaseQuantity);
  const factor = resolveGoodsReceiptLine(line, item?.baseUnit)?.conversionFactor ?? decimal(line?.conversionFactor);
  const price = decimal(line?.purchaseUnitPrice);
  if (quantity === null || factor === null || price === null) return { baseQuantity: null, lineTotal: null, baseUnitCost: null };
  return { baseQuantity: quantity * factor, lineTotal: quantity * price, baseUnitCost: price / factor };
}

export function goodsReceiptTotal(lines, itemsById = {}) {
  return (lines || []).reduce((total, line) => total + (goodsReceiptPreview(line, itemsById?.[line.inventoryItemId]).lineTotal || 0), 0);
}

export function receiptCostWarning(line, item) {
  const currentCost = Number(item?.averageUnitCost);
  if (!(currentCost > 0)) return '';
  const conversion = resolveGoodsReceiptLine(line, item?.baseUnit);
  const price = decimal(line?.purchaseUnitPrice);
  const converted = conversion && price !== null ? price / conversion.conversionFactor : null;
  return converted > currentCost * 5 ? 'Đơn giá quy đổi cao bất thường so với giá vốn hiện tại. Hãy kiểm tra lại đơn vị mua.' : '';
}

export function filterGoodsReceipts(receipts, filters = {}) {
  return (receipts || []).filter(receipt => {
    const date = String(receipt.receivedAt || '').slice(0, 10);
    if (filters.fromDate && date < filters.fromDate) return false;
    if (filters.toDate && date > filters.toDate) return false;
    return !filters.status || filters.status === 'ALL' || receipt.status === filters.status;
  });
}

export function receiptDateSummary(receipts) {
  const approved = (receipts || []).filter(receipt => receipt.status === 'APPROVED');
  const ingredientIds = new Set(approved.flatMap(receipt => (receipt.items || []).map(line => Number(line.inventoryItemId))));
  return {
    receiptCount: (receipts || []).length,
    approvedCount: approved.length,
    ingredientCount: ingredientIds.size,
    approvedValue: approved.reduce((total, receipt) => total + (receipt.items || []).reduce((sum, line) => sum + Number(line.lineTotal || 0), 0), 0),
  };
}

export function receiptGroupsByDate(receipts) {
  const groups = new Map();
  for (const receipt of [...(receipts || [])].sort((a, b) => String(b.receivedAt).localeCompare(String(a.receivedAt)))) {
    const date = String(receipt.receivedAt || '').slice(0, 10);
    if (!groups.has(date)) groups.set(date, []);
    groups.get(date).push(receipt);
  }
  return [...groups].map(([date, values]) => ({ date, receipts: values }));
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
