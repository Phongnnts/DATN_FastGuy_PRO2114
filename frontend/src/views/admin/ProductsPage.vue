<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';
import { CLOUDINARY } from '@/utils/constants';

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
  price: 0,
  image: '',
  description: '',
  inStock: true,
  galleryImages: [],
});
const productOptions = ref([]);

function openAdd() {
  editingId.value = null;
  form.value = {
    name: '',
    categoryId: adminStore.allCategories[0]?.id || null,
    price: 0,
    image: '',
    description: '',
    inStock: true,
    galleryImages: [],
  };
  productOptions.value = [];
  showForm.value = true;
}

function openEdit(p) {
  editingId.value = p.id;
  form.value = {
    name: p.name,
    categoryId: p.categoryId,
    price: p.price,
    image: p.image,
    description: p.description,
    inStock: p.inStock,
    galleryImages: [...(p.galleryImages || [])],
  };
  productOptions.value = (p.options || []).map((o) => ({ ...o }));
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
  const payload = {
    name: form.value.name,
    categoryId: form.value.categoryId,
    price: form.value.price,
    imageUrl: form.value.image,
    description: form.value.description,
    status: form.value.inStock ? 'AVAILABLE' : 'UNAVAILABLE',
    galleryImages: form.value.galleryImages,
  };
  if (editingId.value) {
    await adminStore.updateProduct(editingId.value, payload);
    await syncOptions(editingId.value);
  } else {
    const created = await adminStore.createProduct(payload);
    if (created) {
      const newId = created.productId;
      if (newId) await syncOptions(newId);
    }
  }
  showForm.value = false;
}

async function syncOptions(productId) {
  const existing = await adminStore.fetchOptions(productId);
  const existingIds = existing.map((o) => o.optionId);
  const keptIds = productOptions.value.filter((o) => o.optionId).map((o) => o.optionId);
  for (const id of existingIds) {
    if (!keptIds.includes(id)) {
      await adminStore.deleteOption(id);
    }
  }
  for (const opt of productOptions.value) {
    const data = {
      optionName: opt.optionName,
      extraPrice: opt.extraPrice || 0,
      stockControlled: !!opt.stockControlled,
      quantityAvailable: opt.stockControlled ? (opt.quantityAvailable || 0) : null,
    };
    if (opt.optionId) {
      await adminStore.updateOption(opt.optionId, data);
    } else {
      await adminStore.createOption(productId, data);
    }
  }
  const updated = await adminStore.fetchOptions(productId);
  productOptions.value = (updated || []).map((o) => ({ ...o }));
}

function addOption() {
  productOptions.value.push({
    optionId: null,
    optionName: '',
    extraPrice: 0,
    stockControlled: false,
    quantityAvailable: null,
  });
}

function removeOption(idx) {
  productOptions.value.splice(idx, 1);
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
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th></th>
              <th>Tên</th>
              <th>Danh mục</th>
              <th>Giá</th>
              <th>Giá KM</th>
              <th>Tồn</th>
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
              <td>{{ formatPrice(p.price) }}</td>
              <td>
                {{ p.discountPrice ? formatPrice(p.discountPrice) : '-' }}
              </td>
              <td>
                <i
                  :class="
                    p.inStock
                      ? 'bi bi-check-circle-fill'
                      : 'bi bi-x-circle-fill'
                  "
                  :style="{
                    color: p.inStock ? '#4CAF50' : 'var(--red-active)',
                  }"
                ></i>
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
              <label class="form-label">Giá</label
              ><input
                v-model.number="form.price"
                type="number"
                class="form-input"
                required
              />
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Trạng thái</label
            ><select v-model="form.inStock" class="form-select">
              <option :value="true">Còn hàng</option>
              <option :value="false">Hết hàng</option>
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
              >Tùy chọn (Size, Combo...)
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                style="margin-left: 8px"
                @click="addOption"
              >
                <i class="bi bi-plus-lg"></i> Thêm
              </button>
            </label>
            <div
              v-for="(opt, idx) in productOptions"
              :key="idx"
              class="option-row"
            >
              <input
                v-model="opt.optionName"
                class="form-input"
                placeholder="Tên (vd: Size L)"
                style="flex: 2"
              />
              <input
                v-model.number="opt.extraPrice"
                type="number"
                class="form-input"
                placeholder="Giá thêm"
                style="flex: 1"
              />
              <label class="option-stock">
                <input type="checkbox" v-model="opt.stockControlled" />
                Kiểm kho
              </label>
              <input
                v-if="opt.stockControlled"
                v-model.number="opt.quantityAvailable"
                type="number"
                class="form-input"
                placeholder="SL"
                style="width: 60px"
              />
              <button
                type="button"
                class="btn btn-sm btn-ghost"
                style="color: var(--red-active)"
                @click="removeOption(idx)"
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
</style>
