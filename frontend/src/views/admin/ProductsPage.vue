<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { formatPrice } from '@/utils/format';

const adminStore = useAdminStore();
const searchTerm = ref('');

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
  discountPrice: null,
  image: '',
  description: '',
  inStock: true,
});

function openAdd() {
  editingId.value = null;
  form.value = {
    name: '',
    categoryId: adminStore.allCategories[0]?.id || null,
    price: 0,
    discountPrice: null,
    image: '',
    description: '',
    inStock: true,
  };
  showForm.value = true;
}

function openEdit(p) {
  editingId.value = p.id;
  form.value = {
    name: p.name,
    categoryId: p.categoryId,
    price: p.price,
    discountPrice: p.discountPrice,
    image: p.image,
    description: p.description,
    inStock: p.inStock,
  };
  showForm.value = true;
}

function save() {
  const payload = {
    name: form.value.name,
    categoryId: form.value.categoryId,
    price: form.value.price,
    imageUrl: form.value.image,
    description: form.value.description,
    status: form.value.inStock ? 'AVAILABLE' : 'UNAVAILABLE',
  };
  if (editingId.value) adminStore.updateProduct(editingId.value, payload);
  else adminStore.createProduct(payload);
  showForm.value = false;
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
      <div class="modal-content">
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
          <div class="grid-2">
            <div class="form-group">
              <label class="form-label">Trạng thái</label
              ><select v-model="form.inStock" class="form-select">
                <option :value="true">Còn hàng</option>
                <option :value="false">Hết hàng</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">Link ảnh</label
            ><input
              v-model="form.image"
              class="form-input"
              placeholder="URL hình ảnh"
            />
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
