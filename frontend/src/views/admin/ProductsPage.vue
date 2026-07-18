<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { useToastStore } from '@/stores/toast';
import { formatPrice } from '@/utils/format';
import { CLOUDINARY } from '@/utils/constants';

const adminStore = useAdminStore();
const toast = useToastStore();
const searchTerm = ref('');
const uploading = ref(false);
const uploadingGallery = ref(false);

onMounted(() => {
  adminStore.fetchProducts();
  adminStore.fetchCategories();
});

const showForm = ref(false);
const editingId = ref(null);
const form = ref({
  name: '',
  categoryId: null,
  basePrice: 0,
  image: '',
  description: '',
  status: 'AVAILABLE',
  availableFrom: '',
  availableTo: '',
  galleryImages: [],
});
const productVariants = ref([]);
const modifierGroup = ref({ name: '', minSelections: 0, maxSelections: 1 });
const modifierOption = ref({ groupId: null, name: '', price: 0 });
const comboVariantId = ref(null);
const comboQuantity = ref(1);

// Inline editing state
const editingGroupId = ref(null);
const editingGroupForm = ref({ name: '', minSelections: 0, maxSelections: 1 });
const editingOptionId = ref(null);
const editingOptionForm = ref({ name: '', price: 0 });

function openAdd() {
  editingId.value = null;
  form.value = {
    name: '',
    categoryId: adminStore.allCategories[0]?.id || null,
    basePrice: 0,
    image: '',
    description: '',
    status: 'AVAILABLE',
    galleryImages: [],
  };
  productVariants.value = [];
  showForm.value = true;
}

function openEdit(p) {
  editingId.value = p.id;
  form.value = {
    name: p.name,
    categoryId: p.categoryId,
    basePrice: p.basePrice,
    image: p.image,
    description: p.description,
    status: p.status,
    availableFrom: p.availableFrom || '',
    availableTo: p.availableTo || '',
    galleryImages: [...(p.galleryImages || [])],
  };
  productVariants.value = (p.variants || []).map((v) => ({ ...v }));
  editingGroupId.value = null;
  editingOptionId.value = null;
  showForm.value = true;
}

function validateImageFile(file) {
  if (!file) return false;
  if (!file.type.startsWith('image/')) {
    toast.error('Chỉ chấp nhận file ảnh (JPG, PNG, WebP...)');
    return false;
  }
  if (file.size > 10 * 1024 * 1024) {
    toast.error('Kích thước ảnh tối đa 10MB');
    return false;
  }
  return true;
}

async function uploadImage(file) {
  if (!validateImageFile(file)) return;
  uploading.value = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('upload_preset', CLOUDINARY.uploadPreset);
    const res = await fetch(CLOUDINARY.uploadUrl, { method: 'POST', body: fd });
    const data = await res.json();
    if (data.secure_url) form.value.image = data.secure_url;
    else toast.error('Upload ảnh thất bại');
  } catch {
    toast.error('Lỗi khi upload ảnh');
  } finally {
    uploading.value = false;
  }
}

function triggerUpload() {
  document.getElementById('image-upload-input').click();
}

async function addGalleryImages(e) {
  const files = e.target.files;
  if (!files.length) return;
  uploadingGallery.value = true;
  try {
    for (const file of files) {
      if (!validateImageFile(file)) continue;
      const fd = new FormData();
      fd.append('file', file);
      fd.append('upload_preset', CLOUDINARY.uploadPreset);
      const res = await fetch(CLOUDINARY.uploadUrl, { method: 'POST', body: fd });
      const data = await res.json();
      if (data.secure_url) form.value.galleryImages.push(data.secure_url);
    }
  } catch {
    toast.error('Lỗi khi upload ảnh gallery');
  } finally {
    uploadingGallery.value = false;
    e.target.value = '';
  }
}

function removeGallery(idx) {
  form.value.galleryImages.splice(idx, 1);
}

async function save() {
  if (!form.value.name.trim()) return toast.error('Nhập tên sản phẩm');
  if (Number(form.value.basePrice) < 0) return toast.error('Giá gốc không được âm');
  for (const v of productVariants.value) {
    if (!v.variantName?.trim()) return toast.error('Tên biến thể không được trống');
    if (Number(v.price) < 0) return toast.error('Giá biến thể không được âm');
    if (Number(v.quantityAvailable) < 0) return toast.error('Tồn kho không được âm');
  }
  const payload = {
    name: form.value.name,
    categoryId: form.value.categoryId,
    basePrice: form.value.basePrice,
    imageUrl: form.value.image,
    description: form.value.description,
    status: form.value.status,
    availableFrom: form.value.availableFrom || null,
    availableTo: form.value.availableTo || null,
    galleryImages: form.value.galleryImages,
  };
  try {
    if (editingId.value) {
      await adminStore.updateProduct(editingId.value, payload);
      await syncVariants(editingId.value);
    } else {
      const created = await adminStore.createProduct(payload);
      if (created) {
        const newId = created.productId;
        if (newId) await syncVariants(newId);
      }
    }
    showForm.value = false;
    toast.success(editingId.value ? 'Đã cập nhật sản phẩm' : 'Đã thêm sản phẩm mới');
  } catch (e) {
    toast.error(e.message || 'Không thể lưu sản phẩm');
  }
}

async function syncVariants(productId) {
  const existing = await adminStore.fetchVariants(productId);
  const existingIds = existing.map((v) => v.variantId);
  const keptIds = productVariants.value.filter((v) => v.variantId).map((v) => v.variantId);
  for (const id of existingIds) {
    if (!keptIds.includes(id)) {
      await adminStore.deleteVariant(id);
    }
  }
  for (const v of productVariants.value) {
    const data = {
      variantName: v.variantName,
      price: v.price || 0,
      quantityAvailable: v.quantityAvailable === '' || v.quantityAvailable === null || v.quantityAvailable === undefined ? null : Math.max(0, Number(v.quantityAvailable) || 0),
      isDefault: !!v.isDefault,
      status: v.status || 'AVAILABLE',
    };
    if (v.variantId) {
      await adminStore.updateVariant(v.variantId, data);
    } else {
      await adminStore.createVariant(productId, data);
    }
  }
  const updated = await adminStore.fetchVariants(productId);
  productVariants.value = (updated || []).map((v) => ({ ...v }));
}

function addVariant() {
  productVariants.value.push({
    variantId: null,
    variantName: '',
    price: 0,
    quantityAvailable: null,
    isDefault: false,
    status: 'AVAILABLE',
  });
}

function removeVariant(idx) {
  productVariants.value.splice(idx, 1);
}

function setDefaultVariant(idx) {
  productVariants.value.forEach((v, i) => {
    v.isDefault = i === idx;
  });
}

// Modifier Groups
async function addModifierGroup() {
  if (!editingId.value || !modifierGroup.value.name.trim()) return;
  if (modifierGroup.value.minSelections < 0 || modifierGroup.value.maxSelections < modifierGroup.value.minSelections) {
    return toast.error('min/max selections không hợp lệ');
  }
  if (modifierGroup.value.maxSelections > 50) {
    return toast.error('Tối đa 50 selections');
  }
  try {
    await adminStore.createModifierGroup(editingId.value, modifierGroup.value);
    await adminStore.fetchProducts();
    modifierGroup.value = { name: '', minSelections: 0, maxSelections: 1 };
    toast.success('Đã thêm nhóm topping');
  } catch (e) {
    toast.error(e.message || 'Không thể thêm nhóm topping');
  }
}

function startEditGroup(group) {
  editingGroupId.value = group.modifierGroupId;
  editingGroupForm.value = { name: group.name, minSelections: group.minSelections, maxSelections: group.maxSelections };
}

function cancelEditGroup() {
  editingGroupId.value = null;
}

async function saveEditGroup(groupId) {
  if (!editingGroupForm.value.name.trim()) return toast.error('Tên nhóm không được trống');
  if (editingGroupForm.value.minSelections < 0 || editingGroupForm.value.maxSelections < editingGroupForm.value.minSelections) {
    return toast.error('min/max selections không hợp lệ');
  }
  try {
    await adminStore.updateModifierGroup(groupId, editingGroupForm.value);
    editingGroupId.value = null;
    toast.success('Đã cập nhật nhóm topping');
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật');
  }
}

async function toggleGroupActive(group) {
  try {
    await adminStore.updateModifierGroup(group.modifierGroupId, { isActive: !group.isActive });
    await adminStore.fetchProducts();
    toast.success(group.isActive ? 'Đã tắt nhóm topping' : 'Đã kích hoạt nhóm topping');
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật');
  }
}

async function deleteModifierGroup(group) {
  if (!confirm(`Xóa nhóm "${group.name}"?`)) return;
  try {
    await adminStore.deleteModifierGroup(group.modifierGroupId);
    toast.success('Đã xóa nhóm topping');
  } catch (e) {
    if (e.message && e.message.includes('referenced')) {
      toast.error('Nhóm đã có đơn sử dụng, đang chuyển sang tắt kích hoạt');
      await adminStore.fetchProducts();
    } else {
      toast.error(e.message || 'Không thể xóa nhóm topping');
    }
  }
}

// Modifier Options
async function addModifierOption() {
  if (!modifierOption.value.groupId || !modifierOption.value.name.trim()) return;
  if (Number(modifierOption.value.price) < 0) return toast.error('Giá topping không được âm');
  try {
    await adminStore.createModifierOption(modifierOption.value.groupId, modifierOption.value);
    await adminStore.fetchProducts();
    modifierOption.value = { groupId: null, name: '', price: 0 };
    toast.success('Đã thêm topping');
  } catch (e) {
    toast.error(e.message || 'Không thể thêm topping');
  }
}

function startEditOption(opt, groupId) {
  editingOptionId.value = opt.modifierOptionId;
  editingOptionForm.value = { name: opt.name, price: opt.price };
}

function cancelEditOption() {
  editingOptionId.value = null;
}

async function saveEditOption(groupId, optionId) {
  if (!editingOptionForm.value.name.trim()) return toast.error('Tên topping không được trống');
  if (Number(editingOptionForm.value.price) < 0) return toast.error('Giá topping không được âm');
  try {
    await adminStore.updateModifierOption(groupId, optionId, editingOptionForm.value);
    editingOptionId.value = null;
    toast.success('Đã cập nhật topping');
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật');
  }
}

async function toggleOptionActive(opt, groupId) {
  try {
    await adminStore.updateModifierOption(groupId, opt.modifierOptionId, { isActive: !opt.isActive });
    await adminStore.fetchProducts();
    toast.success(opt.isActive ? 'Đã tắt topping' : 'Đã kích hoạt topping');
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật');
  }
}

async function deleteModifierOption(opt, groupId) {
  if (!confirm(`Xóa topping "${opt.name}"?`)) return;
  try {
    await adminStore.deleteModifierOption(groupId, opt.modifierOptionId);
    toast.success('Đã xóa topping');
  } catch (e) {
    if (e.message && e.message.includes('referenced')) {
      toast.error('Topping đã có đơn sử dụng, đang chuyển sang tắt kích hoạt');
      await adminStore.fetchProducts();
    } else {
      toast.error(e.message || 'Không thể xóa topping');
    }
  }
}

// Combo Items
async function addComboItem() {
  if (!editingId.value || !comboVariantId.value || Number(comboQuantity.value) < 1) return;
  try {
    await adminStore.saveCombo(editingId.value, { isActive: true });
    await adminStore.createComboItem(editingId.value, { variantId: comboVariantId.value, quantity: comboQuantity.value });
    await adminStore.fetchProducts();
    comboVariantId.value = null;
    comboQuantity.value = 1;
    toast.success('Đã thêm thành phần combo');
  } catch (e) {
    toast.error(e.message || 'Không thể thêm thành phần combo');
  }
}

async function deleteComboItem(comboItemId) {
  if (!confirm('Xóa thành phần combo?')) return;
  try {
    await adminStore.deleteComboItem(comboItemId);
    toast.success('Đã xóa thành phần combo');
  } catch (e) {
    toast.error(e.message || 'Không thể xóa');
  }
}

async function toggleComboActive(productId, isActive) {
  try {
    await adminStore.updateCombo(productId, { isActive: !isActive });
    await adminStore.fetchProducts();
    toast.success(isActive ? 'Đã tắt combo' : 'Đã kích hoạt combo');
  } catch (e) {
    toast.error(e.message || 'Không thể cập nhật combo');
  }
}

async function remove(id) {
  if (!confirm('Xóa sản phẩm?')) return;
  try {
    await adminStore.deleteProduct(id);
    toast.success('Đã xóa sản phẩm');
  } catch (e) {
    toast.error(e.message || 'Không thể xóa sản phẩm đang được sử dụng');
  }
}

const comboVariants = computed(() => adminStore.allProducts.flatMap(product => product.variants.map(variant => ({ ...variant, label: `${product.name} - ${variant.variantName}` }))));

const filtered = computed(() => {
  const list = adminStore.allProducts;
  if (!searchTerm.value) return list;
  const q = searchTerm.value.toLowerCase();
  return list.filter((p) => p.name.toLowerCase().includes(q));
});

const currentProduct = computed(() => adminStore.allProducts.find(p => p.id === editingId.value));
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý sản phẩm</h1>
      <button class="btn btn-primary" @click="openAdd">
        <i class="bi bi-plus-lg"></i> Thêm sản phẩm
      </button>
    </div>
    <div class="card card-flat">
      <div class="search-box" style="max-width: 320px; margin-bottom: 16px">
        <i class="bi bi-search"></i
        ><input
          v-model="searchTerm"
          class="form-input"
          placeholder="Tìm sản phẩm..."
        />
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>Tên</th>
              <th>Danh mục</th>
              <th>Giá gốc</th>
              <th>Biến thể</th>
              <th>Tồn kho</th>
              <th>Trạng thái</th>
              <th>Ảnh</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in filtered" :key="p.id">
              <td>
                <img
                  :src="p.image"
                  style="
                    width: 40px;
                    height: 40px;
                    border-radius: 8px;
                    object-fit: cover;
                  "
                />
              </td>
              <td>
                <strong>{{ p.name }}</strong>
              </td>
              <td>
                {{
                  adminStore.allCategories.find((c) => c.id === p.categoryId)
                    ?.name || '-'
                }}
              </td>
              <td>{{ formatPrice(p.basePrice) }}</td>
              <td>
                <span v-if="p.variants?.length" class="badge badge-info">
                  {{ p.variants.length }} biến thể
                </span>
                <span v-else class="text-muted">0</span>
              </td>
              <td>
                <strong>{{ (p.variants || []).some(v => v.quantityAvailable === null || v.quantityAvailable === undefined) ? 'Không giới hạn' : (p.variants || []).reduce((sum, v) => sum + (Number(v.quantityAvailable) || 0), 0) }}</strong>
              </td>
              <td>
                <span
                  :class="'badge badge-' + (p.status === 'AVAILABLE' ? 'success' : 'danger')"
                >{{ p.status === 'AVAILABLE' ? 'Còn hàng' : 'Hết hàng' }}</span>
              </td>
              <td>
                <span v-if="p.galleryImages?.length" class="badge badge-info">
                  {{ p.galleryImages.length }} ảnh
                </span>
                <span v-else class="text-muted">0</span>
              </td>
              <td>
                <button class="btn btn-sm btn-ghost" @click="openEdit(p)">
                  <i class="bi bi-pencil"></i></button
                ><button
                  class="btn btn-sm btn-ghost"
                  style="color: var(--red-active)"
                  @click="remove(p.id)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content" style="max-width: 720px; max-height: 90vh; overflow-y: auto;">
        <div class="modal-header">
          <h3 class="modal-title">
            {{ editingId ? 'Sửa sản phẩm' : 'Thêm sản phẩm' }}
          </h3>
          <button class="modal-close" @click="showForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="save">
          <div class="form-group">
            <label class="form-label">Tên</label
            ><input v-model="form.name" class="form-input" required />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Danh mục</label
              ><select v-model="form.categoryId" class="form-select">
                <option
                  v-for="c in adminStore.allCategories"
                  :key="c.id"
                  :value="c.id"
                >
                  {{ c.name }}
                </option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Giá gốc (basePrice)</label
              ><input
                v-model.number="form.basePrice"
                type="number"
                min="0"
                class="form-input"
                required
              />
            </div>
          </div>
           <div class="form-group">
             <label class="form-label">Trạng thái</label
             ><select v-model="form.status" class="form-select">
               <option value="AVAILABLE">Còn hàng</option>
               <option value="UNAVAILABLE">Hết hàng</option>
             </select>
           </div>
           <div class="grid-2">
             <div class="form-group"><label class="form-label">Bán từ</label><input v-model="form.availableFrom" type="time" class="form-input" /></div>
             <div class="form-group"><label class="form-label">Bán đến</label><input v-model="form.availableTo" type="time" class="form-input" /></div>
           </div>

           <div class="form-group">
            <label class="form-label">Ảnh chính</label>
            <div class="upload-area" @click="triggerUpload">
              <input
                id="image-upload-input"
                type="file"
                accept="image/*"
                style="display: none"
                @change="uploadImage($event.target.files[0])"
              />
              <div v-if="uploading" class="upload-placeholder">
                <i class="bi bi-cloud-upload"></i>
                <span>Đang tải lên...</span>
              </div>
              <div v-else-if="form.image" class="upload-preview">
                <img :src="form.image" />
                <button
                  type="button"
                  class="btn btn-sm btn-ghost upload-remove"
                  @click.stop="form.image = ''"
                >
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <div v-else class="upload-placeholder">
                <i class="bi bi-cloud-upload"></i>
                <span>Nhấn để upload ảnh (JPG, PNG, WebP, tối đa 10MB)</span>
              </div>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Ảnh gallery</label>
            <div class="gallery-grid">
              <div
                v-for="(url, idx) in form.galleryImages"
                :key="idx"
                class="gallery-item"
              >
                <img :src="url" />
                <button
                  type="button"
                  class="btn btn-sm btn-ghost gallery-remove"
                  @click="removeGallery(idx)"
                >
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <label
                class="gallery-add"
                :class="{ uploading: uploadingGallery }"
              >
                <i class="bi bi-plus-lg"></i>
                <input
                  type="file"
                  accept="image/*"
                  multiple
                  style="display: none"
                  @change="addGalleryImages"
                />
              </label>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label"
              >Biến thể (Size, Combo...)
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                style="margin-left: 8px"
                @click="addVariant"
              >
                <i class="bi bi-plus-lg"></i> Thêm
              </button>
            </label>
            <div
              v-for="(v, idx) in productVariants"
              :key="idx"
              class="option-row"
            >
              <input
                v-model="v.variantName"
                class="form-input"
                placeholder="Tên (vd: Size L)"
                style="flex: 2"
              />
              <input
                v-model.number="v.price"
                type="number"
                min="0"
                class="form-input"
                placeholder="Giá"
                style="flex: 1"
              />
              <input
                v-model.number="v.quantityAvailable"
                type="number"
                min="0"
                class="form-input"
                placeholder="Trống = không giới hạn"
                style="flex: 1"
              />
              <select v-model="v.status" class="form-select" style="flex: 1">
                <option value="AVAILABLE">Còn bán</option>
                <option value="UNAVAILABLE">Ngừng bán</option>
              </select>
              <label class="option-stock">
                <input type="checkbox" :checked="v.isDefault" @change="setDefaultVariant(idx)" />
                Mặc định
              </label>
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                style="color: var(--red-active)"
                @click="removeVariant(idx)"
              >
                <i class="bi bi-trash3"></i>
              </button>
            </div>
          </div>

          <!-- Modifier Groups Section -->
          <div v-if="editingId && currentProduct" class="form-group">
            <label class="form-label">Nhóm topping</label>
            
            <!-- Existing groups -->
            <div v-for="group in currentProduct.modifierGroups" :key="group.modifierGroupId" class="modifier-section">
              <div class="modifier-section-header">
                <template v-if="editingGroupId === group.modifierGroupId">
                  <input v-model="editingGroupForm.name" class="form-input" placeholder="Tên nhóm" style="flex: 2" />
                  <input v-model.number="editingGroupForm.minSelections" type="number" min="0" class="form-input" placeholder="Tối thiểu" style="flex: 1" />
                  <input v-model.number="editingGroupForm.maxSelections" type="number" min="0" class="form-input" placeholder="Tối đa" style="flex: 1" />
                  <button type="button" class="btn btn-sm btn-outline" @click="saveEditGroup(group.modifierGroupId)">Lưu</button>
                  <button type="button" class="btn btn-sm btn-ghost" @click="cancelEditGroup">Hủy</button>
                </template>
                <template v-else>
                  <span class="modifier-group-name">{{ group.name }} <span class="text-muted">({{ group.minSelections }}-{{ group.maxSelections }})</span></span>
                  <label class="toggle-switch" :aria-label="`Kích hoạt ${group.name}`">
                    <input type="checkbox" :checked="group.isActive" @change="toggleGroupActive(group)" />
                    <span class="toggle-slider"></span>
                  </label>
                  <button type="button" class="btn btn-sm btn-ghost" @click="startEditGroup(group)"><i class="bi bi-pencil"></i></button>
                  <button type="button" class="btn btn-sm btn-ghost" style="color: var(--red-active)" @click="deleteModifierGroup(group)"><i class="bi bi-trash3"></i></button>
                </template>
              </div>
              
              <!-- Options under this group -->
              <div class="modifier-options">
                <div v-for="opt in group.options" :key="opt.modifierOptionId" class="option-row" style="margin-top: 4px; margin-left: 16px;">
                  <template v-if="editingOptionId === opt.modifierOptionId">
                    <input v-model="editingOptionForm.name" class="form-input" placeholder="Tên topping" style="flex: 2" />
                    <input v-model.number="editingOptionForm.price" type="number" min="0" class="form-input" placeholder="Giá" style="flex: 1" />
                    <button type="button" class="btn btn-sm btn-outline" @click="saveEditOption(group.modifierGroupId, opt.modifierOptionId)">Lưu</button>
                    <button type="button" class="btn btn-sm btn-ghost" @click="cancelEditOption">Hủy</button>
                  </template>
                  <template v-else>
                    <span>{{ opt.name }} <span class="text-muted">{{ formatPrice(opt.price) }}</span></span>
                    <label class="toggle-switch" :aria-label="`Kích hoạt ${opt.name}`">
                      <input type="checkbox" :checked="opt.isActive" @change="toggleOptionActive(opt, group.modifierGroupId)" />
                      <span class="toggle-slider"></span>
                    </label>
                    <button type="button" class="btn btn-sm btn-ghost" @click="startEditOption(opt, group.modifierGroupId)"><i class="bi bi-pencil"></i></button>
                    <button type="button" class="btn btn-sm btn-ghost" style="color: var(--red-active)" @click="deleteModifierOption(opt, group.modifierGroupId)"><i class="bi bi-trash3"></i></button>
                  </template>
                </div>
              </div>
            </div>

            <!-- Add new group -->
            <div class="option-row">
              <input v-model="modifierGroup.name" class="form-input" placeholder="Tên nhóm mới" style="flex: 2" />
              <input v-model.number="modifierGroup.minSelections" type="number" min="0" class="form-input" placeholder="Tối thiểu" style="flex: 1" />
              <input v-model.number="modifierGroup.maxSelections" type="number" min="0" class="form-input" placeholder="Tối đa" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addModifierGroup">Thêm nhóm</button>
            </div>

            <!-- Add new option -->
            <div class="option-row" style="margin-top: 8px">
              <select v-model="modifierOption.groupId" class="form-select" style="flex: 1">
                <option :value="null">Chọn nhóm</option>
                <option v-for="group in currentProduct.modifierGroups" :key="group.modifierGroupId" :value="group.modifierGroupId">{{ group.name }}</option>
              </select>
              <input v-model="modifierOption.name" class="form-input" placeholder="Tên topping" style="flex: 2" />
              <input v-model.number="modifierOption.price" type="number" min="0" class="form-input" placeholder="Giá" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addModifierOption">Thêm topping</button>
            </div>
          </div>

          <!-- Combo Items Section -->
          <div v-if="editingId && currentProduct" class="form-group">
            <div style="display: flex; align-items: center; gap: 8px; margin-bottom: 8px;">
              <label class="form-label" style="margin: 0;">Thành phần combo cố định</label>
              <label v-if="currentProduct.combo" class="toggle-switch" aria-label="Kích hoạt combo">
                <input type="checkbox" :checked="currentProduct.combo.isActive" @change="toggleComboActive(editingId, currentProduct.combo.isActive)" />
                <span class="toggle-slider"></span>
              </label>
            </div>

            <!-- Existing combo items -->
            <div v-if="currentProduct.combo?.items?.length" class="combo-items-list">
              <div v-for="item in currentProduct.combo.items" :key="item.comboItemId" class="option-row">
                <span style="flex: 3">{{ item.productName || ('SP #' + item.productId) }} - {{ item.variantName || ('VT #' + item.variantId) }}</span>
                <span style="flex: 1">x{{ item.quantity }}</span>
                <button type="button" class="btn btn-sm btn-ghost" style="color: var(--red-active)" @click="deleteComboItem(item.comboItemId)"><i class="bi bi-trash3"></i></button>
              </div>
            </div>

            <!-- Add combo item -->
            <div class="option-row" style="margin-top: 8px">
              <select v-model="comboVariantId" class="form-select" style="flex: 2">
                <option :value="null">Chọn biến thể</option>
                <option v-for="variant in comboVariants" :key="variant.variantId" :value="variant.variantId">{{ variant.label }}</option>
              </select>
              <input v-model.number="comboQuantity" type="number" min="1" class="form-input" placeholder="SL" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addComboItem">Thêm</button>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Mô tả</label
            ><textarea
              v-model="form.description"
              class="form-textarea"
              rows="3"
            ></textarea>
          </div>
          <div class="modal-footer">
            <button
              type="button"
              class="btn btn-outline"
              @click="showForm = false"
            >
              Hủy</button
            ><button type="submit" class="btn btn-primary">
              {{ editingId ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-area {
  border: 1.5px dashed var(--border);
  border-radius: var(--radius);
  padding: 16px;
  text-align: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  background: var(--surface);
}
.upload-area:hover {
  border-color: var(--primary);
  background: var(--primary-50);
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-light);
  font-size: 14px;
}
.upload-placeholder i { font-size: 32px; }
.upload-preview { position: relative; display: inline-block; }
.upload-preview img {
  max-height: 160px;
  border-radius: var(--radius-sm);
  object-fit: cover;
}
.upload-remove {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--red-active);
  color: #fff;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gallery-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
.gallery-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  overflow: hidden;
}
.gallery-item img { width: 100%; height: 100%; object-fit: cover; }
.gallery-remove {
  position: absolute;
  top: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.55);
  color: #fff;
  border-radius: 50%;
  width: 20px;
  height: 20px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
}
.gallery-add {
  width: 80px;
  height: 80px;
  border: 1.5px dashed var(--border);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-light);
  font-size: 24px;
  transition: all var(--transition-fast);
  background: var(--surface);
}
.gallery-add:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-50);
}
.gallery-add.uploading { opacity: 0.5; pointer-events: none; }
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  padding: 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
}
.option-stock {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
  color: var(--text-mid);
}
.modifier-section {
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  padding: 10px;
  margin-bottom: 8px;
  background: var(--surface);
}
.modifier-section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}
.modifier-group-name {
  font-weight: 600;
  flex: 1;
}
.modifier-options {
  margin-top: 4px;
}
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 42px;
  height: 24px;
}
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider {
  position: absolute;
  inset: 0;
  cursor: pointer;
  border-radius: 99px;
  background: var(--border);
}
.toggle-slider::before {
  content: '';
  position: absolute;
  width: 18px;
  height: 18px;
  left: 3px;
  bottom: 3px;
  border-radius: 50%;
  background: #fff;
  transition: transform var(--transition-fast);
}
.toggle-switch input:checked + .toggle-slider { background: var(--role-accent, var(--primary)); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(18px); }
.combo-items-list {
  margin-bottom: 8px;
}
</style>
