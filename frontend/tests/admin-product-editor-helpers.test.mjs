import assert from 'node:assert/strict';
import test from 'node:test';
import { reactive } from 'vue';
import {
  buildProductPayload,
  cloneProductState,

  createProductDraft,
  createVariantDraft,
  isCurrentEditorRequest,
  isValidProductId,
  nextEnabledSectionIndex,
  normalizeProductScope,
  normalizeProductDetail,
  sectionDirty,
  validateGeneral,
  validateModifierGroup,
  validateModifierOption,
  validateVariant,
  variantPayload,
  withProductSlice,
} from '../src/utils/adminProductEditor.js';

test('cloneProductState clones Vue reactive product state without sharing nested values', () => {
  const source = reactive({ galleryImages: ['one.jpg'], variants: [{ price: 10000 }] });
  const clone = cloneProductState(source);
  clone.galleryImages.push('two.jpg');
  clone.variants[0].price = 20000;
  assert.deepEqual(source.galleryImages, ['one.jpg']);
  assert.equal(source.variants[0].price, 10000);
});

test('createProductDraft returns independent editor defaults', () => {
  const first = createProductDraft();
  const second = createProductDraft();
  assert.deepEqual(first, {
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
  });
  first.galleryImages.push('image');
  assert.deepEqual(second.galleryImages, []);
});

test('normalizeProductDetail defaults nullable collections and preserves numeric and null values', () => {
  assert.deepEqual(normalizeProductDetail(null), createProductDraft());
  const result = normalizeProductDetail({
    productId: 7,
    name: null,
    categoryId: 3,
    basePrice: 12500.5,
    imageUrl: null,
    description: null,
    status: null,
    availableFrom: null,
    availableTo: null,
    galleryImages: null,
    variants: [{ variantId: 9, price: 10000, originalPrice: null, quantityAvailable: null }],
    modifierGroups: null,
  });
  assert.equal(result.id, 7);
  assert.equal(result.basePrice, 12500.5);
  assert.equal(result.image, '');
  assert.equal(result.availableFrom, '');
  assert.deepEqual(result.galleryImages, []);
  assert.equal(result.variants[0].originalPrice, null);
  assert.equal(result.variants[0].quantityAvailable, null);
  assert.deepEqual(result.modifierGroups, []);
  assert.equal(result.isNew, false);
  assert.equal(result.spiceLevel, 0);
});

test('product update payload preserves exact homepage product controls', () => {
  const payload = buildProductPayload({ name: ' Burger ', categoryId: 2, basePrice: '12000', image: '', description: '', status: 'AVAILABLE', availableFrom: '', availableTo: '', galleryImages: [], isNew: true, spiceLevel: 3 });
  assert.equal(payload.isNew, true);
  assert.equal(payload.spiceLevel, 3);
});

test('isValidProductId accepts positive integer IDs only', () => {
  for (const value of [1, 42, '1', '0042']) assert.equal(isValidProductId(value), true);
  for (const value of [null, undefined, '', 0, -1, 1.5, '1.5', '1x', ' 1', '1 ', true, false, [], [1], {}, NaN, Infinity, -Infinity]) {
    assert.equal(isValidProductId(value), false);
  }
});

test('sectionDirty compares deterministically regardless of object key order', () => {
  assert.equal(sectionDirty({ b: 2, a: { d: 4, c: 3 } }, { a: { c: 3, d: 4 }, b: 2 }), false);
  assert.equal(sectionDirty({ a: [1, 2] }, { a: [2, 1] }), true);
  assert.equal(sectionDirty({ value: null }, { value: 0 }), true);
});

test('sectionDirty preserves values JSON normally collapses', () => {
  assert.equal(sectionDirty({}, { value: undefined }), true);
  assert.equal(sectionDirty({ value: null }, { value: NaN }), true);
  assert.equal(sectionDirty({ value: 0 }, { value: -0 }), true);
  assert.equal(sectionDirty({ value: NaN }, { value: NaN }), false);
});

test('validateGeneral returns field errors for required and invalid values', () => {
  assert.deepEqual(validateGeneral({ name: ' ', categoryId: null, basePrice: -1, availableFrom: '10:00', availableTo: '' }), {
    name: 'Nhập tên sản phẩm',
    categoryId: 'Chọn danh mục hợp lệ',
    basePrice: 'Giá gốc không được âm',
    availableTo: 'Nhập đầy đủ giờ bắt đầu và kết thúc',
  });
  assert.deepEqual(validateGeneral({ name: 'Burger', categoryId: 2, basePrice: 0, availableFrom: '10:00', availableTo: '09:00' }), {
    availableTo: 'Giờ kết thúc phải sau giờ bắt đầu',
  });
  assert.deepEqual(validateGeneral({ name: 'Burger', categoryId: '2', basePrice: 0, availableFrom: '', availableTo: '' }), {});
  for (const categoryId of [0, -1, 1.5, '1.5', ' 1', true, [], [1], {}, NaN, Infinity]) {
    assert.equal(validateGeneral({ name: 'Burger', categoryId, basePrice: 0 }).categoryId, 'Chọn danh mục hợp lệ');
  }
});

test('editor request accepts only current mounted route identity', () => {
  const current = { generation: 4, routeKey: 'AdminProductEdit:7' };
  assert.equal(isCurrentEditorRequest(current, 4, 'AdminProductEdit:7', false), true);
  assert.equal(isCurrentEditorRequest(current, 3, 'AdminProductEdit:7', false), false);
  assert.equal(isCurrentEditorRequest(current, 4, 'AdminProductEdit:8', false), false);
  assert.equal(isCurrentEditorRequest(current, 4, 'AdminProductEdit:7', true), false);
});

test('tab navigation wraps and skips disabled sections', () => {
  const sections = [{ disabled: false }, { disabled: false }, { disabled: true }, { disabled: false }];
  assert.equal(nextEnabledSectionIndex(sections, 1, 'ArrowRight'), 3);
  assert.equal(nextEnabledSectionIndex(sections, 3, 'ArrowRight'), 0);
  assert.equal(nextEnabledSectionIndex(sections, 0, 'ArrowLeft'), 3);
  assert.equal(nextEnabledSectionIndex(sections, 3, 'Home'), 0);
  assert.equal(nextEnabledSectionIndex(sections, 0, 'End'), 3);
  assert.equal(nextEnabledSectionIndex(sections, 1, 'Enter'), 1);
});

test('validateVariant preserves unlimited stock and validates entered numbers', () => {
  assert.deepEqual(validateVariant({ variantName: ' ', price: -1, originalPrice: -2, quantityAvailable: -3 }), {
    variantName: 'Tên biến thể không được trống',
    price: 'Giá biến thể không được âm',
    originalPrice: 'Giá gốc không được âm',
    quantityAvailable: 'Tồn kho không được âm',
  });
  assert.deepEqual(validateVariant({ variantName: 'Mặc định', price: 0, originalPrice: null, quantityAvailable: null }), {});
});

test('createVariantDraft returns independent unbounded draft rows', () => {
  const first = createVariantDraft();
  const second = createVariantDraft();
  assert.deepEqual(first, {
    variantId: null,
    variantName: '',
    price: 0,
    originalPrice: null,
    sku: '',
    quantityAvailable: null,
    isDefault: false,
    status: 'AVAILABLE',
  });
  first.sku = 'SKU-A';
  assert.equal(second.sku, '');
});

test('variantPayload keeps null quantity unlimited and omits blank original price', () => {
  assert.deepEqual(variantPayload({ variantName: ' L ', price: 0, originalPrice: null, sku: '', quantityAvailable: null, isDefault: false, status: 'AVAILABLE' }), {
    variantName: 'L',
    price: 0,
    status: 'AVAILABLE',
    quantityAvailable: null,
    sku: '',
    isDefault: false,
  });
  assert.equal(variantPayload({ variantName: 'M', price: '10000', originalPrice: 12000, quantityAvailable: '', status: 'UNAVAILABLE' }).originalPrice, 12000);
  assert.equal(variantPayload({ variantName: 'M', price: 10000, originalPrice: null, quantityAvailable: '3' }).quantityAvailable, 3);
  assert.equal('originalPrice' in variantPayload({ variantName: 'M', price: 10000, originalPrice: '' }), false);
});

test('variantPayload treats blank original price as no-change for updates', () => {
  const persisted = { variantId: 3, variantName: 'M', price: 10000, originalPrice: 12000, sku: 'SKU', quantityAvailable: 5, isDefault: false, status: 'AVAILABLE' };
  assert.equal(variantPayload({ ...persisted, originalPrice: '' }).originalPrice, undefined);
  assert.equal(variantPayload({ ...persisted, originalPrice: null }).originalPrice, undefined);
  assert.equal(variantPayload(persisted).originalPrice, 12000);
});

test('validateModifierGroup requires name and ordered non-negative limits', () => {
  assert.deepEqual(validateModifierGroup({ name: ' ', minSelections: -1, maxSelections: 0 }), {
    name: 'Nhập tên nhóm tùy chọn',
    minSelections: 'Tối thiểu không được âm',
  });
  assert.deepEqual(validateModifierGroup({ name: 'Topping', minSelections: 3, maxSelections: 1 }), {
    maxSelections: 'Tối đa phải lớn hơn hoặc bằng tối thiểu',
  });
  assert.deepEqual(validateModifierGroup({ name: 'Topping', minSelections: 0, maxSelections: 1 }), {});
});

test('validateModifierOption requires name and non-negative price', () => {
  assert.deepEqual(validateModifierOption({ name: '', price: -1 }), { name: 'Nhập tên tùy chọn', price: 'Giá tùy chọn không được âm' });
  assert.deepEqual(validateModifierOption({ name: 'Thêm phô mai', price: 5000 }), {});
});


test('normalizeProductScope always returns an array for canonical slice reloads', () => {
  assert.deepEqual(normalizeProductScope(['general', 'media']), ['general', 'media']);
  assert.deepEqual(normalizeProductScope(null), ['general', 'media']);
});





test('withProductSlice copies only the affected slice and preserves other sections', () => {
  const current = { name: 'Giữ tên', categoryId: 1, variants: [{ variantId: 1, price: 5000 }] };
  const canonical = { name: 'Tên mới', categoryId: 9, variants: [{ variantId: 2, price: 7000 }] };
  const sliced = withProductSlice(current, canonical, ['variants']);
  assert.equal(sliced.name, 'Giữ tên');
  assert.equal(sliced.categoryId, 1);
  assert.deepEqual(sliced.variants, canonical.variants);
  assert.equal(sliced.variants === canonical.variants, false);
  assert.deepEqual(current.variants, [{ variantId: 1, price: 5000 }]);
  const combined = withProductSlice(current, canonical, ['general']);
  assert.equal(combined.name, 'Tên mới');
  assert.deepEqual(combined.variants, current.variants);
});
