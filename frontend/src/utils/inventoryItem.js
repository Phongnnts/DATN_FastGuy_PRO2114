const QTY_RE = /^-?\d+(\.\d+)?$/;
const INPUT_RE = /^\d+(\.\d{1,4})?$/;

export const INVENTORY_MODES = [
  { value: 'INGREDIENT', label: 'Nguyên liệu' },
  { value: 'FINISHED_GOOD', label: 'Thành phẩm' },
  { value: 'UNTRACKED', label: 'Không theo dõi' },
  { value: 'SUSPENDED', label: 'Tạm dừng bán' },
];

export function formatQuantity(value) {
  const text = value === null || value === undefined ? '' : String(value).trim();
  if (!QTY_RE.test(text)) return '—';
  const negative = text.startsWith('-');
  const [intPart, fracPart = ''] = (negative ? text.slice(1) : text).split('.');
  const frac = fracPart.slice(0, 4).replace(/0+$/, '');
  const grouped = intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.');
  return `${negative ? '-' : ''}${grouped}${frac ? `,${frac}` : ''}`;
}

export function parseQuantity(raw) {
  const text = String(raw ?? '').trim().replace(',', '.');
  if (!INPUT_RE.test(text)) return { ok: false, value: null };
  const value = Number(text);
  if (!Number.isFinite(value) || value <= 0 || value > 999999999999999.9999) return { ok: false, value: null };
  return { ok: true, value };
}

export function itemStockState(item) {
  if (!item || item.active === false) return 'INACTIVE';
  const available = Number(item.availableQuantity);
  if (!Number.isFinite(available)) return 'OUT';
  if (available <= 0) return 'OUT';
  const minimum = Number(item.minimumQuantity);
  if (Number.isFinite(minimum) && available <= minimum) return 'LOW';
  return 'OK';
}

export function inventoryKpis(items) {
  const list = Array.isArray(items) ? items : [];
  return {
    itemCount: list.length,
    belowMinimumCount: list.filter((item) => {
      if (itemStockState(item) === 'INACTIVE') return false;
      return Number(item.availableQuantity) <= Number(item.minimumQuantity);
    }).length,
  };
}

function cleanNote(note) {
  const text = String(note ?? '').trim();
  return text ? text.slice(0, 500) : null;
}

function parseNonNegative(raw) {
  const text = String(raw ?? '').trim().replace(',', '.');
  if (!/^\d+(\.\d{1,4})?$/.test(text)) return 0;
  const value = Number(text);
  return Number.isFinite(value) && value <= 999999999999999.9999 ? value : 0;
}

export function buildItemPayload(form) {
  return {
    inventoryCode: String(form.inventoryCode ?? '').trim().toUpperCase(),
    name: String(form.name ?? '').trim(),
    itemType: form.itemType,
    baseUnit: form.baseUnit,
    minimumQuantity: parseNonNegative(form.minimumQuantity),
    countFrequency: form.countFrequency,
    active: Boolean(form.active),
  };
}

export function buildAdjustmentPayload(item, form) {
  const parsed = parseQuantity(form.quantity);
  const signed = form.operation === 'DECREASE' ? -parsed.value : parsed.value;
  return {
    inventoryItemId: item.inventoryItemId,
    quantity: Number(signed.toFixed(4)),
    expectedOnHandQuantity: Number(item.onHandQuantity),
    reason: String(form.reason ?? '').trim(),
    note: cleanNote(form.note),
  };
}

export function validateRecipeForm(form) {
  const errors = {};
  if (!['INGREDIENT', 'FINISHED_GOOD', 'UNTRACKED', 'SUSPENDED'].includes(form.inventoryMode)) {
    errors.inventoryMode = 'Chọn chế độ kho';
  }
  if (!parseQuantity(form.yieldQuantity).ok) errors.yieldQuantity = 'Số phần đầu ra phải lớn hơn 0';
  const lines = Array.isArray(form.items) ? form.items : [];
  if (!lines.length) {
    errors.items = 'Thêm ít nhất một dòng nguyên liệu';
    return errors;
  }
  const seen = new Set();
  const lineErrors = {};
  lines.forEach((line, index) => {
    const id = Number(line.inventoryItemId);
    if (!Number.isInteger(id) || id <= 0) lineErrors[index] = 'Chọn mặt hàng';
    else if (seen.has(id)) lineErrors[index] = 'Mặt hàng đã được chọn';
    else seen.add(id);
    if (lineErrors[index] !== 'Chọn mặt hàng' && !parseQuantity(line.quantity).ok) {
      lineErrors[index] = 'Số lượng phải lớn hơn 0';
    }
  });
  if (Object.keys(lineErrors).length) errors.lines = lineErrors;
  return errors;
}

export function buildRecipePayload(form) {
  return {
    inventoryMode: form.inventoryMode,
    yieldQuantity: parseQuantity(form.yieldQuantity).value,
    active: Boolean(form.active),
    items: (form.items || []).map((line) => ({
      inventoryItemId: Number(line.inventoryItemId),
      quantity: parseQuantity(line.quantity).value,
    })),
  };
}

// ponytail: float division fine for demo magnitudes; switch to decimal strings when >2^53 servings needed
export function recipeServings(recipe, itemsById) {
  if (!recipe || !Array.isArray(recipe.items) || !recipe.items.length || !(Number(recipe.yieldQuantity) > 0)) {
    return { servings: 0, limitingItemId: null };
  }
  let servings = Infinity;
  let limitingItemId = recipe.items[0].inventoryItemId;
  for (const line of recipe.items) {
    const available = Number(itemsById?.[line.inventoryItemId]?.availableQuantity ?? 0);
    const possible = Math.floor((available * Number(recipe.yieldQuantity)) / Number(line.quantity));
    if (possible < servings) {
      servings = possible;
      limitingItemId = line.inventoryItemId;
    }
  }
  return { servings: Math.max(0, Math.min(servings, Number.MAX_SAFE_INTEGER)), limitingItemId };
}
