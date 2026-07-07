<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { CLOUDINARY } from '@/utils/constants';

const adminStore = useAdminStore();
const searchTerm = ref('');
const uploading = ref(false);
const uploadingGallery = ref(false);
const submitting = ref(false);
const formError = ref('');

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
  galleryImages: [],
});
const productVariants = ref([]);

function openAdd() {
  editingId.value = null;
  formError.value = '';
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
  formError.value = '';
  form.value = {
    name: p.name,
    categoryId: p.categoryId,
    basePrice: p.basePrice,
    image: p.image,
    description: p.description,
    status: p.status,
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
  formError.value = validateForm();
  if (formError.value) return;
  submitting.value = true;
  try {
    const payload = {
      name: form.value.name.trim(),
      categoryId: form.value.categoryId,
      basePrice: Number(form.value.basePrice),
      imageUrl: form.value.image,
      description: form.value.description,
      status: form.value.status,
      galleryImages: form.value.galleryImages,
    };
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
    formError.value = e.message || 'Lưu sản phẩm thất bại';
  } finally {
    submitting.value = false;
  }
}

function validateForm() {
  if (form.value.name.trim().length < 2) return 'Tên sản phẩm phải từ 2 ký tự';
  if (!form.value.categoryId) return 'Vui lòng chọn danh mục';
  if (Number(form.value.basePrice) < 0) return 'Giá gốc không được âm';
  for (const v of productVariants.value) {
    if (!v.variantName?.trim()) return 'Tên biến thể không được trống';
    if (Number(v.price) < 0) return 'Giá biến thể không được âm';
  }
  return '';
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

function remove(id) {
  if (confirm('Xóa sản phẩm?')) adminStore.deleteProduct(id);
}

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
      <div v-if="adminStore.error" class="empty-state">
        <i class="bi bi-exclamation-triangle"></i>
        <h3>{{ adminStore.error }}</h3>
      </div>
      <div v-else-if="adminStore.loading" class="empty-state">
        <i class="bi bi-arrow-repeat spin"></i>
        <h3>Đang tải sản phẩm...</h3>
      </div>
      <div v-else-if="filtered.length === 0" class="empty-state">
        <i class="bi bi-box"></i>
        <h3>Chưa có sản phẩm</h3>
      </div>
      <div v-else class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>Tên</th>
              <th>Danh mục</th>
              <th>Giá gốc</th>
              <th>Biến thể</th>
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
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-group">
            <label class="form-label">Tên</label
            ><input v-model="form.name" class="form-input" minlength="2" maxlength="150" required />
          </div>
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Danh mục</label
              ><select v-model="form.categoryId" class="form-select" required>
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
                class="form-input"
                min="0"
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
                required
              />
              <input
                v-model.number="v.price"
                type="number"
                class="form-input"
                placeholder="Giá"
                style="flex: 1"
                min="0"
              />
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
            ><button type="submit" class="btn btn-primary" :disabled="submitting">
              {{ submitting ? 'Đang lưu...' : editingId ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-area {
  border: 2px dashed var(--border-color, #ddd);
  border-radius: 12px;
  padding: 12px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
  position: relative;
}
.upload-area:hover {
  border-color: var(--primary, #4f46e5);
}
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: #999;
  font-size: 14px;
}
.upload-placeholder i {
  font-size: 32px;
}
.upload-preview {
  position: relative;
  display: inline-block;
}
.upload-preview img {
  max-height: 160px;
  border-radius: 8px;
  object-fit: cover;
}
.upload-remove {
  position: absolute;
  top: -8px;
  right: -8px;
  background: var(--red-active, #dc2626);
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
  border-radius: 8px;
  overflow: hidden;
}
.gallery-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.gallery-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0, 0, 0, 0.6);
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
  border: 2px dashed var(--border-color, #ddd);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
  font-size: 24px;
  transition: border-color 0.2s;
}
.gallery-add:hover {
  border-color: var(--primary, #4f46e5);
  color: var(--primary, #4f46e5);
}
.gallery-add.uploading {
  opacity: 0.5;
  pointer-events: none;
}
.option-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.option-stock {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  white-space: nowrap;
  cursor: pointer;
}
.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--text-mid);
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
