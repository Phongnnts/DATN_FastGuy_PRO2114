<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { CLOUDINARY } from '@/utils/constants';
import { useToast } from '@/stores/toast';

const toast = useToast();
const adminStore = useAdminStore();
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
  showForm.value = true;
}

async function uploadImage(file) {
  if (!file) return;
  uploading.value = true;
  try {
    const fd = new FormData();
    fd.append('file', file);
    fd.append('upload_preset', CLOUDINARY.uploadPreset);
    const res = await fetch(CLOUDINARY.uploadUrl, { method: 'POST', body: fd });
    const data = await res.json();
    if (data.secure_url) form.value.image = data.secure_url;
  } catch {
    // ignore
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
      const fd = new FormData();
      fd.append('file', file);
      fd.append('upload_preset', CLOUDINARY.uploadPreset);
      const res = await fetch(CLOUDINARY.uploadUrl, { method: 'POST', body: fd });
      const data = await res.json();
      if (data.secure_url) form.value.galleryImages.push(data.secure_url);
    }
  } catch {
    // ignore
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

async function addModifierGroup() {
  if (!editingId.value || !modifierGroup.value.name.trim()) return;
  await adminStore.createModifierGroup(editingId.value, modifierGroup.value);
  await adminStore.fetchProducts();
  modifierGroup.value = { name: '', minSelections: 0, maxSelections: 1 };
}

async function addModifierOption() {
  if (!modifierOption.value.groupId || !modifierOption.value.name.trim()) return;
  await adminStore.createModifierOption(modifierOption.value.groupId, modifierOption.value);
  await adminStore.fetchProducts();
  modifierOption.value = { groupId: null, name: '', price: 0 };
}

async function addComboItem() {
  if (!editingId.value || !comboVariantId.value || Number(comboQuantity.value) < 1) return;
  await adminStore.saveCombo(editingId.value, { isActive: true });
  await adminStore.createComboItem(editingId.value, { variantId: comboVariantId.value, quantity: comboQuantity.value });
  comboVariantId.value = null;
  comboQuantity.value = 1;
}

async function saveModifierGroup(group) {
  await adminStore.updateModifierGroup(group.modifierGroupId, {
    name: group.name,
    minSelections: group.minSelections,
    maxSelections: group.maxSelections,
    isActive: group.isActive,
  });
}

async function deleteModifierGroup(group) {
  await adminStore.deleteModifierGroup(group.modifierGroupId);
}

async function saveModifierOption(group, option) {
  await adminStore.updateModifierOption(group.modifierGroupId, option.modifierOptionId, {
    name: option.name,
    price: option.price,
    isActive: option.isActive,
  });
}

async function deleteModifierOption(group, option) {
  await adminStore.deleteModifierOption(group.modifierGroupId, option.modifierOptionId);
}

async function setComboActive(combo) {
  await adminStore.updateCombo(editingId.value, { isActive: combo.isActive });
}

async function deleteComboItem(item) {
  await adminStore.deleteComboItem(editingId.value, item.comboItemId);
}

async function remove(id) {
  if (!confirm('Xóa sản phẩm?')) return;
  try {
    await adminStore.deleteProduct(id);
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
      <div class="modal-content" style="max-width: 720px">
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
                <span>Nhấn để upload ảnh</span>
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

          <div v-if="editingId" class="form-group">
            <label class="form-label">Nhóm topping</label>
            <div class="option-row">
              <input v-model="modifierGroup.name" class="form-input" placeholder="Tên nhóm" style="flex: 2" />
              <input v-model.number="modifierGroup.minSelections" type="number" min="0" class="form-input" placeholder="Tối thiểu" style="flex: 1" />
              <input v-model.number="modifierGroup.maxSelections" type="number" min="0" class="form-input" placeholder="Tối đa" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addModifierGroup">Thêm</button>
            </div>
            <div v-for="group in adminStore.allProducts.find(p => p.id === editingId)?.modifierGroups || []" :key="group.modifierGroupId" style="margin-top:8px; padding:8px; border:1px solid var(--border-light); border-radius:var(--radius-sm)">
              <div class="option-row" style="border:none; padding:0; margin:0">
                <input v-model="group.name" class="form-input" style="flex:2" />
                <input v-model.number="group.minSelections" type="number" min="0" class="form-input" style="flex:1" />
                <input v-model.number="group.maxSelections" type="number" min="0" class="form-input" style="flex:1" />
                <label class="option-stock"><input type="checkbox" v-model="group.isActive" @change="saveModifierGroup(group)" /> Bật</label>
                <button type="button" class="btn btn-sm btn-ghost" @click="saveModifierGroup(group)"><i class="bi bi-check-lg"></i></button>
                <button type="button" class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="deleteModifierGroup(group)"><i class="bi bi-trash3"></i></button>
              </div>
              <div v-for="option in group.options || []" :key="option.modifierOptionId" class="option-row" style="margin-top:4px; border:none; padding:0 0 0 16px; margin:4px 0 0 0">
                <input v-model="option.name" class="form-input" style="flex:2" />
                <input v-model.number="option.price" type="number" min="0" class="form-input" style="flex:1" />
                <label class="option-stock"><input type="checkbox" v-model="option.isActive" @change="saveModifierOption(group, option)" /> Bật</label>
                <button type="button" class="btn btn-sm btn-ghost" @click="saveModifierOption(group, option)"><i class="bi bi-check-lg"></i></button>
                <button type="button" class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="deleteModifierOption(group, option)"><i class="bi bi-trash3"></i></button>
              </div>
            </div>
            <div class="option-row" style="margin-top:8px">
              <select v-model="modifierOption.groupId" class="form-select" style="flex: 1"><option :value="null">Chọn nhóm</option><option v-for="group in adminStore.allProducts.find(p => p.id === editingId)?.modifierGroups || []" :key="group.modifierGroupId" :value="group.modifierGroupId">{{ group.name }}</option></select>
              <input v-model="modifierOption.name" class="form-input" placeholder="Tên topping" style="flex: 2" />
              <input v-model.number="modifierOption.price" type="number" min="0" class="form-input" placeholder="Giá" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addModifierOption">Thêm</button>
            </div>
          </div>

          <div v-if="editingId" class="form-group">
            <label class="form-label">Thành phần combo cố định</label>
            <div class="option-row">
              <select v-model="comboVariantId" class="form-select" style="flex: 2"><option :value="null">Chọn biến thể</option><option v-for="variant in comboVariants" :key="variant.variantId" :value="variant.variantId">{{ variant.label }}</option></select>
              <input v-model.number="comboQuantity" type="number" min="1" class="form-input" placeholder="SL" style="flex: 1" />
              <button type="button" class="btn btn-sm btn-outline" @click="addComboItem">Thêm</button>
            </div>
            <div v-for="item in adminStore.allProducts.find(p => p.id === editingId)?.combo?.items || []" :key="item.comboItemId" class="option-row" style="margin-top:4px">
              <span style="flex:2; font-size:13px">{{ comboVariants.find(v => v.variantId === item.variantId)?.label || ('Variant #' + item.variantId) }}</span>
              <span style="flex:1; font-size:13px">x{{ item.quantity }}</span>
              <button type="button" class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="deleteComboItem(item)"><i class="bi bi-trash3"></i></button>
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
</style>
