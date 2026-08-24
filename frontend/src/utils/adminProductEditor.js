import { toRaw } from 'vue';

export function cloneProductState(value) {
  return structuredClone(toRaw(value));
}

function stableValue(value) {
  if (value === undefined) return ['undefined'];
  if (value === null) return ['null'];
  if (typeof value === 'number') {
    if (Number.isNaN(value)) return ['number', 'NaN'];
    if (Object.is(value, -0)) return ['number', '-0'];
    return ['number', String(value)];
  }
  if (Array.isArray(value)) return ['array', value.map(stableValue)];
  if (typeof value === 'object') {
    return ['object', Object.keys(value).sort().map((key) => [key, stableValue(value[key])])];
  }
  return [typeof value, value];
}

export function createProductDraft() {
  return {
    id: null,
    name: '',
    categoryId: null,
    basePrice: 0,
    image: '',
    description: '',
    status: 'AVAILABLE',
    availableFrom: '',
    availableTo: '',
    galleryImages: [],
    variants: [],
    modifierGroups: [],
    isNew: false,
    spiceLevel: 0,
  };
}

export function normalizeProductDetail(raw) {
  if (!raw || typeof raw !== 'object') return createProductDraft();
  const draft = createProductDraft();
  return {
    ...draft,
    ...raw,
    id: raw.productId ?? raw.id ?? null,
    name: raw.name ?? '',
    categoryId: raw.categoryId ?? null,
    basePrice: raw.basePrice ?? 0,
    image: raw.imageUrl ?? raw.image ?? '',
    description: raw.description ?? '',
    status: raw.status ?? 'AVAILABLE',
    availableFrom: raw.availableFrom ?? '',
    availableTo: raw.availableTo ?? '',
    galleryImages: Array.isArray(raw.galleryImages) ? [...raw.galleryImages] : [],
    variants: Array.isArray(raw.variants) ? raw.variants.map((variant) => ({ ...variant })) : [],
    modifierGroups: Array.isArray(raw.modifierGroups) ? raw.modifierGroups.map((group) => ({ ...group })) : [],
    isNew: raw.isNew ?? false,
    spiceLevel: raw.spiceLevel ?? 0,
  };
}

export function isValidProductId(value) {
  if (typeof value === 'number') return Number.isSafeInteger(value) && value > 0;
  if (typeof value !== 'string' || !/^\d+$/.test(value)) return false;
  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0;
}

export function isCurrentEditorRequest(request, generation, routeKey, stopped) {
  return !stopped && request.generation === generation && request.routeKey === routeKey;
}

export function nextEnabledSectionIndex(sections, currentIndex, key) {
  const enabled = sections.map((section, index) => section.disabled ? -1 : index).filter((index) => index >= 0);
  if (!enabled.length || !['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(key)) return currentIndex;
  if (key === 'Home') return enabled[0];
  if (key === 'End') return enabled[enabled.length - 1];
  const position = enabled.indexOf(currentIndex);
  const step = key === 'ArrowRight' ? 1 : -1;
  return enabled[(position + step + enabled.length) % enabled.length];
}

export function sectionDirty(snapshot, value) {
  return JSON.stringify(stableValue(snapshot)) !== JSON.stringify(stableValue(value));
}

export function validateGeneral(form = {}) {
  const errors = {};
  if (!String(form.name ?? '').trim()) errors.name = 'Nhập tên sản phẩm';
  if (!isValidProductId(form.categoryId)) errors.categoryId = 'Chọn danh mục hợp lệ';
  if (form.basePrice === '' || form.basePrice === null || form.basePrice === undefined || !Number.isFinite(Number(form.basePrice)) || Number(form.basePrice) < 0) errors.basePrice = 'Giá gốc không được âm';
  const spiceLevel = Number(form.spiceLevel ?? 0);
  if (!Number.isInteger(spiceLevel) || spiceLevel < 0 || spiceLevel > 3) errors.spiceLevel = 'Chọn mức độ cay hợp lệ';
  const hasFrom = Boolean(form.availableFrom);
  const hasTo = Boolean(form.availableTo);
  if (hasFrom !== hasTo) errors[hasFrom ? 'availableTo' : 'availableFrom'] = 'Nhập đầy đủ giờ bắt đầu và kết thúc';
  else if (hasFrom && form.availableFrom >= form.availableTo) errors.availableTo = 'Giờ kết thúc phải sau giờ bắt đầu';
  return errors;
}

export function buildProductPayload(draft = {}) {
  return { name: String(draft.name ?? '').trim(), categoryId: draft.categoryId, basePrice: Number(draft.basePrice), imageUrl: draft.image, description: draft.description, status: draft.status, availableFrom: draft.availableFrom || null, availableTo: draft.availableTo || null, galleryImages: draft.galleryImages, isNew: Boolean(draft.isNew), spiceLevel: Number(draft.spiceLevel) };
}

export function validateVariant(variant = {}) {
  const errors = {};
  if (!String(variant.variantName ?? '').trim()) errors.variantName = 'Tên biến thể không được trống';
  if (variant.price === '' || variant.price === null || variant.price === undefined || !Number.isFinite(Number(variant.price)) || Number(variant.price) < 0) errors.price = 'Giá biến thể không được âm';
  if (variant.originalPrice !== null && variant.originalPrice !== undefined && variant.originalPrice !== '' && (!Number.isFinite(Number(variant.originalPrice)) || Number(variant.originalPrice) < 0)) errors.originalPrice = 'Giá gốc không được âm';
  return errors;
}

export function createVariantDraft() {
  return {
    variantId: null,
    variantName: '',
    price: 0,
    originalPrice: null,
    sku: '',
    isDefault: false,
    status: 'AVAILABLE',
  };
}

export function variantPayload(variant = {}) {
  const payload = {
    variantName: String(variant.variantName ?? '').trim(),
    price: Number(variant.price) || 0,
    status: variant.status === 'UNAVAILABLE' ? 'UNAVAILABLE' : 'AVAILABLE',
    sku: variant.sku ?? '',
    isDefault: Boolean(variant.isDefault),
  };
  if (variant.originalPrice !== '' && variant.originalPrice !== null && variant.originalPrice !== undefined) payload.originalPrice = Number(variant.originalPrice);
  return payload;
}

export function validateModifierGroup(group = {}) {
  const errors = {};
  if (!String(group.name ?? '').trim()) errors.name = 'Nhập tên nhóm tùy chọn';
  const min = Number(group.minSelections);
  const max = Number(group.maxSelections);
  if (!Number.isFinite(min) || min < 0) errors.minSelections = 'Tối thiểu không được âm';
  if (!Number.isFinite(max) || max < 0) errors.maxSelections = 'Tối đa không được âm';
  if (!errors.minSelections && !errors.maxSelections && max < min) errors.maxSelections = 'Tối đa phải lớn hơn hoặc bằng tối thiểu';
  return errors;
}

export function validateModifierOption(option = {}) {
  const errors = {};
  if (!String(option.name ?? '').trim()) errors.name = 'Nhập tên tùy chọn';
  if (option.price === '' || option.price === null || option.price === undefined || !Number.isFinite(Number(option.price)) || Number(option.price) < 0) errors.price = 'Giá tùy chọn không được âm';
  return errors;
}

export function normalizeProductScope(scope) {
  if (Array.isArray(scope)) return scope;
  return scope ? [scope] : ['general', 'media'];
}

export function withProductSlice(target, source, scope) {
  const next = { ...target };
  if (scope.includes('general')) {
    next.name = source.name;
    next.categoryId = source.categoryId;
    next.basePrice = source.basePrice;
    next.description = source.description;
    next.status = source.status;
    next.availableFrom = source.availableFrom;
    next.availableTo = source.availableTo;
    next.isNew = source.isNew;
    next.spiceLevel = source.spiceLevel;
  }
  if (scope.includes('media')) {
    next.image = source.image;
    next.galleryImages = cloneProductState(source.galleryImages);
  }
  if (scope.includes('variants')) next.variants = cloneProductState(source.variants);
  if (scope.includes('modifiers')) next.modifierGroups = cloneProductState(source.modifierGroups);
  return next;
}
