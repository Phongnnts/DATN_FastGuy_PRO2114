<script setup>
import { computed, onMounted, ref } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';

const toast = useToast();
const adminStore = useAdminStore();
const showForm = ref(false);
const editingId = ref(null);
const deletingId = ref(null);
const form = ref({ name: '', description: '' });
const search = ref('');
const filter = ref('all');
const sort = ref('name-asc');
const loading = ref(true);
const saving = ref(false);
const error = ref('');

const categories = computed(() => adminStore.allCategories || []);
const stats = computed(() => ({
  total: categories.value.length,
  used: categories.value.filter(category => Number(category.productCount) > 0).length,
  empty: categories.value.filter(category => Number(category.productCount) === 0).length,
  products: categories.value.reduce((total, category) => total + Number(category.productCount || 0), 0)
}));
const visibleCategories = computed(() => {
  const term = search.value.trim().toLocaleLowerCase('vi');
  return categories.value
    .filter(category => filter.value === 'all' || (filter.value === 'used' ? Number(category.productCount) > 0 : Number(category.productCount) === 0))
    .filter(category => !term || `${category.name} ${category.description || ''}`.toLocaleLowerCase('vi').includes(term))
    .sort((a, b) => {
      if (sort.value === 'count-desc') return Number(b.productCount || 0) - Number(a.productCount || 0);
      if (sort.value === 'count-asc') return Number(a.productCount || 0) - Number(b.productCount || 0);
      return a.name.localeCompare(b.name, 'vi') * (sort.value === 'name-desc' ? -1 : 1);
    });
});
const hasQuery = computed(() => search.value || filter.value !== 'all' || sort.value !== 'name-asc');

onMounted(load);

async function load() {
  loading.value = true;
  error.value = '';
  try { await adminStore.fetchCategories(); }
  catch (e) { error.value = e.message || 'Không thể tải danh mục'; }
  finally { loading.value = false; }
}

function reset() { search.value = ''; filter.value = 'all'; sort.value = 'name-asc'; }
function openAdd() { editingId.value = null; form.value = { name: '', description: '' }; showForm.value = true; }
function openEdit(category) { editingId.value = category.id; form.value = { name: category.name, description: category.description || '' }; showForm.value = true; }
function closeForm() { if (!saving.value) showForm.value = false; }

async function save() {
  if (!form.value.name.trim()) return toast.error('Nhập tên danh mục');
  saving.value = true;
  try {
    const data = { name: form.value.name.trim(), description: form.value.description.trim() };
    if (editingId.value) await adminStore.updateCategory(editingId.value, data);
    else await adminStore.createCategory(data);
    toast.success(editingId.value ? 'Đã cập nhật danh mục' : 'Đã thêm danh mục');
    showForm.value = false;
  } catch (e) { toast.error(e.message || 'Không thể lưu danh mục'); }
  finally { saving.value = false; }
}

async function remove(category) {
  if (!confirm(`Xóa danh mục "${category.name}"?`)) return;
  deletingId.value = category.id;
  try {
    await adminStore.deleteCategory(category.id);
    toast.success('Đã xóa danh mục');
  } catch (e) { toast.error(e.message || 'Không thể xóa danh mục có sản phẩm'); }
  finally { deletingId.value = null; }
}
</script>

<template>
  <main class="categories-page">
    <header class="page-header">
      <div><span class="eyebrow">Thực đơn</span><h1>Quản lý danh mục</h1><p>Tổ chức và kiểm soát danh mục sản phẩm.</p></div>
      <button class="btn btn-primary add-button" type="button" @click="openAdd"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm danh mục</button>
    </header>

    <section class="stats-grid" aria-label="Thống kê danh mục">
      <article class="stat-card stat-total"><span class="stat-icon"><i class="bi bi-grid" aria-hidden="true"></i></span><div><span>Tổng danh mục</span><strong>{{ stats.total }}</strong></div></article>
      <article class="stat-card stat-used"><span class="stat-icon"><i class="bi bi-check2-circle" aria-hidden="true"></i></span><div><span>Có sản phẩm</span><strong>{{ stats.used }}</strong></div></article>
      <article class="stat-card stat-empty"><span class="stat-icon"><i class="bi bi-inbox" aria-hidden="true"></i></span><div><span>Danh mục trống</span><strong>{{ stats.empty }}</strong></div></article>
      <article class="stat-card stat-products"><span class="stat-icon"><i class="bi bi-box-seam" aria-hidden="true"></i></span><div><span>Tổng sản phẩm</span><strong>{{ stats.products }}</strong></div></article>
    </section>

    <section class="content-card" aria-labelledby="category-list-title">
      <div class="card-heading"><div><h2 id="category-list-title">Danh sách danh mục</h2><p>{{ visibleCategories.length }} / {{ stats.total }} danh mục</p></div></div>
      <div class="toolbar">
        <label class="search-box"><span class="sr-only">Tìm danh mục</span><i class="bi bi-search" aria-hidden="true"></i><input v-model="search" type="search" placeholder="Tìm theo tên hoặc mô tả..."></label>
        <label><span class="sr-only">Lọc trạng thái</span><select v-model="filter" class="control"><option value="all">Tất cả trạng thái</option><option value="used">Có sản phẩm</option><option value="empty">Danh mục trống</option></select></label>
        <label><span class="sr-only">Sắp xếp</span><select v-model="sort" class="control"><option value="name-asc">Tên A–Z</option><option value="name-desc">Tên Z–A</option><option value="count-desc">Nhiều sản phẩm nhất</option><option value="count-asc">Ít sản phẩm nhất</option></select></label>
        <button class="reset-button" type="button" :disabled="!hasQuery" @click="reset"><i class="bi bi-arrow-counterclockwise" aria-hidden="true"></i> Đặt lại</button>
      </div>

      <div v-if="loading" class="state" role="status"><span class="spinner"></span><h3>Đang tải danh mục</h3><p>Vui lòng chờ trong giây lát.</p></div>
      <div v-else-if="error" class="state error-state" role="alert"><i class="bi bi-exclamation-triangle" aria-hidden="true"></i><h3>Không thể tải dữ liệu</h3><p>{{ error }}</p><button class="btn btn-outline" type="button" @click="load">Thử lại</button></div>
      <div v-else-if="visibleCategories.length" class="table-wrapper">
        <table class="category-table"><thead><tr><th scope="col">Danh mục</th><th scope="col">Mô tả</th><th scope="col">Sản phẩm</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr></thead><tbody><tr v-for="category in visibleCategories" :key="category.id"><td><div class="category-name"><span class="category-avatar">{{ category.name.charAt(0).toUpperCase() }}</span><div><strong>{{ category.name }}</strong><small>#{{ category.id }}</small></div></div></td><td class="description">{{ category.description || 'Chưa có mô tả' }}</td><td><span :class="['status-badge', Number(category.productCount) ? 'is-used' : 'is-empty']">{{ category.productCount || 0 }} sản phẩm</span></td><td><div class="actions"><button class="icon-button" type="button" :aria-label="`Sửa ${category.name}`" :disabled="deletingId === category.id" @click="openEdit(category)"><i class="bi bi-pencil" aria-hidden="true"></i></button><button class="icon-button danger" type="button" :aria-label="`Xóa ${category.name}`" :disabled="deletingId !== null" @click="remove(category)"><span v-if="deletingId === category.id" class="button-spinner"></span><i v-else class="bi bi-trash3" aria-hidden="true"></i></button></div></td></tr></tbody></table>
      </div>
      <div v-else class="state empty-state"><i class="bi bi-tags" aria-hidden="true"></i><h3>{{ categories.length ? 'Không tìm thấy danh mục' : 'Chưa có danh mục' }}</h3><p>{{ categories.length ? 'Thử thay đổi từ khóa hoặc bộ lọc.' : 'Tạo danh mục đầu tiên để phân loại sản phẩm.' }}</p><button v-if="categories.length" class="btn btn-outline" type="button" @click="reset">Xóa bộ lọc</button><button v-else class="btn btn-primary" type="button" @click="openAdd">Thêm danh mục</button></div>
    </section>

    <div v-if="showForm" class="modal-overlay" @click.self="closeForm" @keydown.esc="closeForm"><div class="modal" role="dialog" aria-modal="true" aria-labelledby="category-modal-title"><div class="modal-header"><div><span class="eyebrow">{{ editingId ? 'Chỉnh sửa' : 'Tạo mới' }}</span><h2 id="category-modal-title" class="modal-title">{{ editingId ? 'Sửa danh mục' : 'Thêm danh mục' }}</h2></div><button class="modal-close" type="button" aria-label="Đóng" :disabled="saving" @click="closeForm"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div><form class="modal-body" @submit.prevent="save"><div class="form-group"><label class="form-label" for="category-name">Tên danh mục <span aria-hidden="true">*</span></label><input id="category-name" v-model="form.name" class="form-input" maxlength="100" required autofocus placeholder="Ví dụ: Cà phê"><small>Tối đa 100 ký tự</small></div><div class="form-group"><label class="form-label" for="category-description">Mô tả</label><textarea id="category-description" v-model="form.description" class="form-textarea" rows="4" maxlength="500" placeholder="Mô tả ngắn về danh mục"></textarea><small>{{ form.description.length }}/500 ký tự</small></div><div class="modal-footer"><button type="button" class="btn btn-ghost" :disabled="saving" @click="closeForm">Hủy</button><button type="submit" class="btn btn-primary" :disabled="saving"><span v-if="saving" class="button-spinner"></span>{{ saving ? 'Đang lưu...' : editingId ? 'Lưu thay đổi' : 'Tạo danh mục' }}</button></div></form></div></div>
  </main>
</template>

<style scoped>
.categories-page { color: #172033; }
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 24px; }
.page-header h1 { margin: 3px 0 6px; font-size: clamp(26px, 3vw, 34px); line-height: 1.15; letter-spacing: -.7px; }
.page-header p, .card-heading p { margin: 0; color: #68748a; font-size: 14px; }
.eyebrow { color: #e85d2a; font-size: 11px; font-weight: 800; letter-spacing: 1.4px; text-transform: uppercase; }
.add-button { min-height: 44px; box-shadow: 0 8px 18px rgba(232, 93, 42, .2); }
.stats-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 16px; margin-bottom: 20px; }
.stat-card { display: flex; align-items: center; gap: 14px; min-height: 112px; padding: 20px; overflow: hidden; border: 1px solid #e8ebf1; border-radius: 16px; background: #fff; box-shadow: 0 4px 16px rgba(24, 32, 51, .04); }
.stat-icon { display: grid; flex: 0 0 48px; height: 48px; place-items: center; border-radius: 14px; font-size: 21px; }
.stat-card span:not(.stat-icon) { display: block; margin-bottom: 3px; color: #68748a; font-size: 12px; font-weight: 700; }
.stat-card strong { font-size: 27px; line-height: 1; }
.stat-total .stat-icon { color: #3157c8; background: #edf2ff; }.stat-used .stat-icon { color: #14805e; background: #e9f8f2; }.stat-empty .stat-icon { color: #b87512; background: #fff6df; }.stat-products .stat-icon { color: #cf4d29; background: #fff0eb; }
.content-card { overflow: hidden; border: 1px solid #e5e9f0; border-radius: 18px; background: #fff; box-shadow: 0 8px 30px rgba(24, 32, 51, .06); }
.card-heading { display: flex; justify-content: space-between; padding: 22px 24px 16px; }.card-heading h2 { margin: 0 0 4px; font-size: 18px; }
.toolbar { display: grid; grid-template-columns: minmax(220px, 1fr) auto auto auto; gap: 10px; padding: 0 24px 20px; border-bottom: 1px solid #edf0f4; }
.search-box { position: relative; }.search-box i { position: absolute; top: 50%; left: 14px; color: #8b95a7; transform: translateY(-50%); }.search-box input, .control { width: 100%; height: 42px; border: 1px solid #dce1e9; border-radius: 10px; color: #253047; background: #fff; outline: none; }.search-box input { padding: 0 14px 0 40px; }.control { min-width: 160px; padding: 0 34px 0 12px; }
.search-box input:focus, .control:focus, .icon-button:focus-visible, .reset-button:focus-visible { border-color: #e85d2a; box-shadow: 0 0 0 3px rgba(232, 93, 42, .13); }
.reset-button { height: 42px; padding: 0 12px; border: 0; border-radius: 10px; color: #526077; background: #f3f5f8; font-weight: 700; cursor: pointer; }.reset-button:disabled { opacity: .45; cursor: not-allowed; }
.table-wrapper { overflow-x: auto; }.category-table { width: 100%; border-collapse: collapse; }.category-table th { padding: 12px 24px; color: #7a8598; background: #fafbfc; font-size: 11px; letter-spacing: .6px; text-align: left; text-transform: uppercase; }.category-table td { padding: 15px 24px; border-top: 1px solid #edf0f4; vertical-align: middle; }.category-table tbody tr { transition: background .15s; }.category-table tbody tr:hover { background: #fcfaf8; }
.category-name { display: flex; align-items: center; gap: 12px; min-width: 180px; }.category-avatar { display: grid; flex: 0 0 38px; height: 38px; place-items: center; border-radius: 11px; color: #d95328; background: #fff0eb; font-weight: 800; }.category-name strong, .category-name small { display: block; }.category-name small { margin-top: 3px; color: #97a0af; font-size: 11px; }.description { max-width: 420px; color: #657086; }
.status-badge { display: inline-flex; padding: 5px 9px; border-radius: 20px; font-size: 12px; font-weight: 700; white-space: nowrap; }.status-badge.is-used { color: #137153; background: #eaf8f3; }.status-badge.is-empty { color: #8a6470; background: #f5f0f2; }
.actions { display: flex; justify-content: flex-end; gap: 6px; }.icon-button { display: grid; width: 36px; height: 36px; place-items: center; border: 1px solid #e0e4ea; border-radius: 9px; color: #59667c; background: #fff; cursor: pointer; }.icon-button:hover { color: #3157c8; border-color: #bdc9eb; background: #f4f7ff; }.icon-button.danger:hover { color: #c63e3e; border-color: #f0bcbc; background: #fff4f4; }.icon-button:disabled { opacity: .5; cursor: not-allowed; }
.state { display: flex; min-height: 300px; padding: 40px 20px; align-items: center; justify-content: center; flex-direction: column; text-align: center; }.state > i { margin-bottom: 12px; color: #a5adba; font-size: 38px; }.state h3 { margin: 0 0 6px; font-size: 17px; }.state p { max-width: 420px; margin: 0 0 18px; color: #788398; }.error-state > i { color: #d54b45; }
.spinner, .button-spinner { display: inline-block; border: 2px solid #dfe3e9; border-top-color: #e85d2a; border-radius: 50%; animation: spin .7s linear infinite; }.spinner { width: 34px; height: 34px; margin-bottom: 16px; }.button-spinner { width: 15px; height: 15px; margin-right: 7px; vertical-align: -2px; }.modal { border-radius: 18px; }.modal-header { align-items: flex-start; padding: 22px 24px; }.modal-title { margin-top: 3px; }.modal-body { padding: 22px 24px 0; }.form-group small { display: block; margin-top: 6px; color: #8993a4; font-size: 11px; }.form-textarea { resize: vertical; }.modal-footer { margin: 24px -24px 0; padding: 16px 24px; background: #fafbfc; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 1050px) { .stats-grid { grid-template-columns: repeat(2, 1fr); }.toolbar { grid-template-columns: 1fr 1fr; }.search-box { grid-column: 1 / -1; } }
@media (max-width: 640px) { .page-header { align-items: stretch; flex-direction: column; }.add-button { width: 100%; }.stats-grid { gap: 10px; }.stat-card { min-height: 96px; padding: 14px; }.stat-icon { flex-basis: 40px; height: 40px; }.stat-card strong { font-size: 22px; }.toolbar { grid-template-columns: 1fr; padding: 0 16px 16px; }.search-box { grid-column: auto; }.control { min-width: 0; }.card-heading { padding: 18px 16px 14px; }.category-table th, .category-table td { padding: 12px 16px; }.description { min-width: 220px; }.modal-overlay { padding: 12px; } }
@media (max-width: 420px) { .stats-grid { grid-template-columns: 1fr; }.stat-card { min-height: 82px; } }
@media (prefers-reduced-motion: reduce) { .spinner, .button-spinner { animation-duration: 1.5s; } }
</style>
