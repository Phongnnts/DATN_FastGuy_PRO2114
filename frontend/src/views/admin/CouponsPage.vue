<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import couponApi from '@/api/coupon';
import { useToast } from '@/stores/toast';

const toast = useToast();
const coupons = ref([]);
const loading = ref(true);
const error = ref('');
const showModal = ref(false);
const editing = ref(null);
const submitting = ref(false);
const pendingAction = ref('');
const searchTerm = ref('');
const typeFilter = ref('');
const statusFilter = ref('');
const publicFilter = ref('');
const sortBy = ref('expiry-asc');
const currentPage = ref(1);
const pageSize = 10;
const form = ref(defaultForm());

function defaultForm() {
  return { code: '', type: 'PERCENT', value: 0, minOrder: 0, maxDiscount: null, maxUses: 0, expiresAt: '', isPublic: true };
}

const valueLabel = computed(() => form.value.type === 'PERCENT' ? 'Phần trăm giảm' : 'Giá trị giảm');
const stats = computed(() => ({
  total: coupons.value.length,
  active: coupons.value.filter((coupon) => couponStatus(coupon) === 'ACTIVE').length,
  public: coupons.value.filter((coupon) => coupon.isPublic).length,
  used: coupons.value.reduce((sum, coupon) => sum + Number(coupon.usedCount || 0), 0),
}));
const hasFilters = computed(() => searchTerm.value || typeFilter.value || statusFilter.value || publicFilter.value || sortBy.value !== 'expiry-asc');
const filtered = computed(() => {
  const query = searchTerm.value.trim().toLocaleLowerCase('vi');
  return coupons.value.filter((coupon) => {
    if (query && !String(coupon.code || '').toLocaleLowerCase('vi').includes(query)) return false;
    if (typeFilter.value && coupon.type !== typeFilter.value) return false;
    if (statusFilter.value && couponStatus(coupon) !== statusFilter.value) return false;
    if (publicFilter.value && String(coupon.isPublic) !== publicFilter.value) return false;
    return true;
  }).sort((a, b) => {
    if (sortBy.value === 'code-asc') return String(a.code).localeCompare(String(b.code), 'vi');
    if (sortBy.value === 'used-desc') return Number(b.usedCount || 0) - Number(a.usedCount || 0);
    const aTime = a.expiresAt ? new Date(a.expiresAt).getTime() : Infinity;
    const bTime = b.expiresAt ? new Date(b.expiresAt).getTime() : Infinity;
    return sortBy.value === 'expiry-desc' ? bTime - aTime : aTime - bTime;
  });
});
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => filtered.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize));

watch([searchTerm, typeFilter, statusFilter, publicFilter, sortBy], () => { currentPage.value = 1; });
watch(totalPages, (pages) => { if (currentPage.value > pages) currentPage.value = pages; });

async function load() {
  loading.value = true;
  error.value = '';
  try {
    coupons.value = await couponApi.getAll() || [];
  } catch (e) {
    coupons.value = [];
    error.value = e.message || 'Không thể tải mã giảm giá';
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  editing.value = null;
  form.value = defaultForm();
  showModal.value = true;
}

function openEdit(coupon) {
  editing.value = coupon;
  form.value = {
    code: coupon.code,
    type: coupon.type,
    value: Number(coupon.value || 0),
    minOrder: Number(coupon.minOrder || 0),
    maxDiscount: coupon.maxDiscount == null ? null : Number(coupon.maxDiscount),
    maxUses: Number(coupon.maxUses || 0),
    expiresAt: coupon.expiresAt ? coupon.expiresAt.replace(' ', 'T').substring(0, 16) : '',
    isPublic: coupon.isPublic !== false,
  };
  showModal.value = true;
}

function closeModal() {
  if (!submitting.value) showModal.value = false;
}

function validate() {
  const values = [form.value.value, form.value.minOrder, form.value.maxUses];
  if (form.value.maxDiscount !== null && form.value.maxDiscount !== '') values.push(form.value.maxDiscount);
  if (!/^[A-Z0-9_-]{3,50}$/.test(form.value.code.trim())) return 'Mã gồm 3-50 ký tự: chữ, số, - hoặc _';
  if (values.some((value) => !Number.isFinite(Number(value)))) return 'Các giá trị số phải hợp lệ';
  if (form.value.type === 'PERCENT' && (Number(form.value.value) <= 0 || Number(form.value.value) > 100)) return 'Phần trăm giảm phải từ 1 đến 100';
  if (form.value.type === 'FIXED' && Number(form.value.value) <= 0) return 'Giá trị giảm phải lớn hơn 0';
  if ([form.value.minOrder, form.value.maxUses, form.value.maxDiscount].some((value) => value !== null && value !== '' && Number(value) < 0)) return 'Giá trị không được âm';
  if (!editing.value && form.value.expiresAt && new Date(form.value.expiresAt).getTime() <= Date.now()) return 'Thời gian hết hạn phải ở tương lai';
  if (editing.value && Number(form.value.maxUses) > 0 && Number(form.value.maxUses) < Number(editing.value.usedCount || 0)) return `Số lần dùng tối đa không thể thấp hơn ${editing.value.usedCount || 0}`;
  return '';
}

async function save() {
  const message = validate();
  if (message) return toast.error(message);
  submitting.value = true;
  try {
    const payload = {
      ...form.value,
      code: form.value.code.trim().toUpperCase(),
      value: form.value.type === 'FREE_SHIPPING' ? 0 : Number(form.value.value),
      minOrder: Number(form.value.minOrder),
      maxUses: Number(form.value.maxUses),
      maxDiscount: form.value.type === 'PERCENT' && form.value.maxDiscount !== null && form.value.maxDiscount !== '' ? Number(form.value.maxDiscount) : null,
      expiresAt: form.value.expiresAt || null,
    };
    if (editing.value) await couponApi.update(editing.value.couponId, payload);
    else await couponApi.create(payload);
    toast.success(editing.value ? 'Đã cập nhật mã giảm giá' : 'Đã tạo mã giảm giá');
    showModal.value = false;
    await load();
  } catch (e) {
    toast.error(e.message || 'Không thể lưu mã giảm giá');
  } finally {
    submitting.value = false;
  }
}

async function remove(coupon) {
  if (!coupon.canDelete) return toast.error('Mã đã có lịch sử, chỉ có thể tắt kích hoạt');
  if (!confirm(`Xóa mã ${coupon.code}?`)) return;
  pendingAction.value = `delete-${coupon.couponId}`;
  try {
    await couponApi.delete(coupon.couponId);
    toast.success('Đã xóa mã giảm giá');
    await load();
  } catch (e) {
    toast.error(e.message || 'Không thể xóa mã giảm giá');
  } finally {
    pendingAction.value = '';
  }
}

async function toggle(coupon, field) {
  const key = `${field}-${coupon.couponId}`;
  if (pendingAction.value) return;
  const previous = coupon[field];
  pendingAction.value = key;
  coupon[field] = !previous;
  try {
    await couponApi.update(coupon.couponId, { [field]: coupon[field] });
    toast.success(field === 'isActive' ? 'Đã cập nhật trạng thái mã' : 'Đã cập nhật hiển thị mã');
  } catch (e) {
    coupon[field] = previous;
    toast.error(e.message || 'Không thể cập nhật mã giảm giá');
  } finally {
    pendingAction.value = '';
  }
}

function resetFilters() {
  searchTerm.value = '';
  typeFilter.value = '';
  statusFilter.value = '';
  publicFilter.value = '';
  sortBy.value = 'expiry-asc';
}

function couponStatus(coupon) {
  if (!coupon.isActive) return 'INACTIVE';
  if (coupon.expiresAt && new Date(coupon.expiresAt).getTime() <= Date.now()) return 'EXPIRED';
  return 'ACTIVE';
}

function statusLabel(coupon) {
  return { ACTIVE: 'Hoạt động', INACTIVE: 'Đã tắt', EXPIRED: 'Hết hạn' }[couponStatus(coupon)];
}

function typeLabel(type) {
  return { PERCENT: 'Phần trăm', FIXED: 'Tiền mặt', FREE_SHIPPING: 'Miễn phí ship' }[type] || type;
}

function valueText(coupon) {
  if (coupon.type === 'PERCENT') return `${coupon.value}%`;
  if (coupon.type === 'FREE_SHIPPING') return 'Phí giao hàng';
  return `${Number(coupon.value || 0).toLocaleString('vi-VN')}đ`;
}

onMounted(load);
</script>

<template>
  <main class="coupons-page">
    <header class="page-heading"><div><p class="eyebrow">Khuyến mãi</p><h1>Quản lý mã giảm giá</h1><p>Theo dõi ưu đãi, lượt sử dụng và phạm vi hiển thị.</p></div><button class="btn btn-primary" @click="openCreate"><i class="bi bi-plus-lg"></i> Tạo mã</button></header>

    <section class="stats" aria-label="Tổng quan mã giảm giá">
      <article><span class="stat-icon violet"><i class="bi bi-ticket-perforated"></i></span><div><small>Tổng mã</small><strong>{{ stats.total }}</strong></div></article>
      <article><span class="stat-icon green"><i class="bi bi-check-circle"></i></span><div><small>Đang hoạt động</small><strong>{{ stats.active }}</strong></div></article>
      <article><span class="stat-icon blue"><i class="bi bi-eye"></i></span><div><small>Đang công khai</small><strong>{{ stats.public }}</strong></div></article>
      <article><span class="stat-icon amber"><i class="bi bi-bag-check"></i></span><div><small>Tổng lượt dùng</small><strong>{{ stats.used }}</strong></div></article>
    </section>

    <section class="panel">
      <div class="filters">
        <label class="search-box"><i class="bi bi-search"></i><input v-model="searchTerm" aria-label="Tìm theo mã" placeholder="Tìm theo mã giảm giá"><button v-if="searchTerm" type="button" aria-label="Xóa tìm kiếm" @click="searchTerm = ''"><i class="bi bi-x-circle-fill"></i></button></label>
        <select v-model="typeFilter" class="form-select" aria-label="Lọc loại"><option value="">Mọi loại</option><option value="PERCENT">Phần trăm</option><option value="FIXED">Tiền mặt</option><option value="FREE_SHIPPING">Miễn phí ship</option></select>
        <select v-model="statusFilter" class="form-select" aria-label="Lọc trạng thái"><option value="">Mọi trạng thái</option><option value="ACTIVE">Hoạt động</option><option value="INACTIVE">Đã tắt</option><option value="EXPIRED">Hết hạn</option></select>
        <select v-model="publicFilter" class="form-select" aria-label="Lọc hiển thị"><option value="">Mọi hiển thị</option><option value="true">Công khai</option><option value="false">Riêng tư</option></select>
        <select v-model="sortBy" class="form-select" aria-label="Sắp xếp"><option value="expiry-asc">Sắp hết hạn</option><option value="expiry-desc">Hết hạn xa nhất</option><option value="used-desc">Dùng nhiều nhất</option><option value="code-asc">Mã A–Z</option></select>
      </div>
      <div class="filter-summary"><span>{{ filtered.length }} kết quả</span><button v-if="hasFilters" class="reset-button" @click="resetFilters"><i class="bi bi-x-circle"></i> Đặt lại bộ lọc</button></div>

      <div v-if="loading" class="state" role="status"><span class="spinner"></span><strong>Đang tải mã giảm giá...</strong></div>
      <div v-else-if="error" class="state error" role="alert"><i class="bi bi-exclamation-circle"></i><strong>{{ error }}</strong><button class="btn btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="!filtered.length" class="state"><i class="bi bi-ticket-detailed"></i><strong>{{ coupons.length ? 'Không tìm thấy mã phù hợp' : 'Chưa có mã giảm giá' }}</strong><span>{{ coupons.length ? 'Thử thay đổi hoặc đặt lại bộ lọc.' : 'Tạo mã đầu tiên để bắt đầu chương trình ưu đãi.' }}</span><button v-if="coupons.length" class="btn btn-outline" @click="resetFilters">Đặt lại bộ lọc</button></div>
      <template v-else>
        <div class="table-wrapper"><table class="table"><thead><tr><th>Mã ưu đãi</th><th>Loại</th><th>Giá trị</th><th>Điều kiện</th><th>Usage</th><th>Hết hạn</th><th>Trạng thái</th><th>Công khai</th><th><span class="sr-only">Thao tác</span></th></tr></thead><tbody><tr v-for="coupon in paged" :key="coupon.couponId" :class="{ muted: couponStatus(coupon) !== 'ACTIVE' }">
          <td><strong class="coupon-code">{{ coupon.code }}</strong></td><td><span class="type-pill">{{ typeLabel(coupon.type) }}</span></td><td><strong>{{ valueText(coupon) }}</strong><small v-if="coupon.maxDiscount">Tối đa {{ Number(coupon.maxDiscount).toLocaleString('vi-VN') }}đ</small></td><td>Từ {{ Number(coupon.minOrder || 0).toLocaleString('vi-VN') }}đ</td>
          <td><div class="usage"><span><b>{{ coupon.usedCount || 0 }}</b> / {{ coupon.maxUses || '∞' }}</span><div><i :style="{ width: `${coupon.maxUses ? Math.min(100, Number(coupon.usedCount || 0) / Number(coupon.maxUses) * 100) : 0}%` }"></i></div></div></td>
          <td>{{ coupon.expiresAt ? new Date(coupon.expiresAt).toLocaleString('vi-VN', { dateStyle: 'short', timeStyle: 'short' }) : 'Không giới hạn' }}</td>
          <td><button class="status-pill" :class="couponStatus(coupon).toLowerCase()" :disabled="!!pendingAction" @click="toggle(coupon, 'isActive')"><span v-if="pendingAction === `isActive-${coupon.couponId}`" class="mini-spinner"></span><i v-else></i>{{ statusLabel(coupon) }}</button></td>
          <td><label class="toggle-switch" :aria-label="`Công khai ${coupon.code}`"><input type="checkbox" :checked="coupon.isPublic" :disabled="!!pendingAction" @change="toggle(coupon, 'isPublic')"><span class="toggle-slider"></span></label></td>
          <td><div class="actions"><button class="icon-button edit" :disabled="!!pendingAction" :aria-label="`Sửa ${coupon.code}`" @click="openEdit(coupon)"><i class="bi bi-pencil-square"></i></button><button class="icon-button delete" :disabled="!coupon.canDelete || !!pendingAction" :title="coupon.canDelete ? 'Xóa mã' : 'Mã đã có lịch sử, hãy tắt kích hoạt'" :aria-label="`Xóa ${coupon.code}`" @click="remove(coupon)"><span v-if="pendingAction === `delete-${coupon.couponId}`" class="mini-spinner"></span><i v-else class="bi bi-trash3"></i></button></div></td>
        </tr></tbody></table></div>
        <footer class="table-footer"><span>Hiển thị {{ (currentPage - 1) * pageSize + 1 }}–{{ Math.min(currentPage * pageSize, filtered.length) }} / {{ filtered.length }} mã</span><div class="pagination"><button :disabled="currentPage === 1" aria-label="Trang trước" @click="currentPage--"><i class="bi bi-chevron-left"></i></button><span>Trang {{ currentPage }} / {{ totalPages }}</span><button :disabled="currentPage === totalPages" aria-label="Trang sau" @click="currentPage++"><i class="bi bi-chevron-right"></i></button></div></footer>
      </template>
    </section>

    <div v-if="showModal" class="modal-overlay" @click.self="closeModal" @keydown.esc="closeModal">
      <div class="modal coupon-modal" role="dialog" aria-modal="true" aria-labelledby="coupon-modal-title">
        <div class="modal-accent"></div><div class="modal-header"><div><span class="modal-icon"><i class="bi bi-ticket-perforated"></i></span><div><h2 id="coupon-modal-title" class="modal-title">{{ editing ? 'Chỉnh sửa mã' : 'Tạo mã giảm giá' }}</h2><p>{{ editing ? 'Cập nhật điều kiện và phạm vi áp dụng.' : 'Thiết lập ưu đãi mới cho khách hàng.' }}</p></div></div><button class="modal-close" :disabled="submitting" aria-label="Đóng" @click="closeModal"><i class="bi bi-x-lg"></i></button></div>
        <form class="modal-body" @submit.prevent="save"><div class="form-grid">
          <div class="form-group full"><label class="form-label" for="coupon-code">Mã giảm giá *</label><input id="coupon-code" v-model="form.code" class="form-input" placeholder="VD: GIAM10" maxlength="50" required :disabled="editing && !editing.canDelete" @input="form.code = form.code.toUpperCase()"><small v-if="editing && !editing.canDelete">Không thể đổi do mã đã có lịch sử.</small></div>
          <div class="form-group"><label class="form-label" for="coupon-type">Loại ưu đãi *</label><select id="coupon-type" v-model="form.type" class="form-select" :disabled="editing && !editing.canDelete"><option value="PERCENT">Giảm theo phần trăm</option><option value="FIXED">Giảm tiền mặt</option><option value="FREE_SHIPPING">Miễn phí vận chuyển</option></select></div>
          <div v-if="form.type !== 'FREE_SHIPPING'" class="form-group"><label class="form-label" for="coupon-value">{{ valueLabel }} *</label><input id="coupon-value" v-model.number="form.value" type="number" class="form-input" :min="form.type === 'PERCENT' ? 1 : 0" :max="form.type === 'PERCENT' ? 100 : undefined" required :disabled="editing && !editing.canDelete"></div>
          <div class="form-group"><label class="form-label" for="coupon-min-order">Đơn tối thiểu</label><input id="coupon-min-order" v-model.number="form.minOrder" type="number" class="form-input" min="0" required></div>
          <div v-if="form.type === 'PERCENT'" class="form-group"><label class="form-label" for="coupon-max-discount">Giảm tối đa</label><input id="coupon-max-discount" v-model.number="form.maxDiscount" type="number" class="form-input" min="0" placeholder="Không giới hạn"></div>
          <div class="form-group"><label class="form-label" for="coupon-max-uses">Số lần dùng tối đa</label><input id="coupon-max-uses" v-model.number="form.maxUses" type="number" class="form-input" min="0" required><small>0 là không giới hạn{{ editing ? ` · Đã dùng ${editing.usedCount || 0}` : '' }}.</small></div>
          <div class="form-group"><label class="form-label" for="coupon-expiry">Thời gian hết hạn</label><input id="coupon-expiry" v-model="form.expiresAt" type="datetime-local" class="form-input"><small>Để trống nếu không giới hạn.</small></div>
          <label class="checkbox-label full"><input v-model="form.isPublic" type="checkbox"> Hiển thị công khai cho khách hàng</label>
        </div><div class="modal-footer"><button type="button" class="btn btn-ghost" :disabled="submitting" @click="closeModal">Hủy</button><button type="submit" class="btn btn-primary" :disabled="submitting"><span v-if="submitting" class="mini-spinner"></span>{{ submitting ? 'Đang lưu...' : editing ? 'Lưu thay đổi' : 'Tạo mã' }}</button></div></form>
      </div>
    </div>
  </main>
</template>

<style scoped>
.coupons-page { display: grid; gap: 22px; }.page-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; }.page-heading h1 { margin: 2px 0 4px; font-size: 28px; }.page-heading p { color: var(--text-mid); font-size: 14px; }.eyebrow { color: var(--role-admin) !important; font-size: 11px !important; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; }.stats article { display: flex; align-items: center; gap: 14px; padding: 18px; border: 1px solid var(--border); border-radius: var(--radius); background: var(--bg-card); box-shadow: var(--shadow-xs); }.stat-icon { display: grid; width: 44px; height: 44px; flex: 0 0 44px; place-items: center; border-radius: 11px; font-size: 19px; }.violet { color: #7c3aed; background: #ede9fe; }.green { color: #047857; background: #d1fae5; }.blue { color: #1d4ed8; background: #dbeafe; }.amber { color: #b45309; background: #fef3c7; }.stats small,.stats strong { display: block; }.stats small { margin-bottom: 3px; color: var(--text-mid); font-size: 12px; }.stats strong { font-size: 22px; }
.panel { overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-lg); background: var(--bg-card); box-shadow: var(--shadow-sm); }.filters { display: grid; grid-template-columns: minmax(240px, 2fr) repeat(4, minmax(130px, 1fr)); gap: 10px; padding: 16px 16px 10px; }.search-box { display: flex; align-items: center; height: 42px; padding: 0 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); color: var(--text-mid); background: var(--surface); }.search-box:focus-within { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(212,118,74,.12); }.search-box input { width: 100%; padding: 0 9px; border: 0; outline: 0; background: transparent; font: inherit; }.search-box button { color: var(--text-mid); }.filter-summary { display: flex; align-items: center; justify-content: space-between; padding: 0 16px 14px; color: var(--text-mid); font-size: 12px; }.reset-button { color: var(--role-admin); font-size: 12px; }
.table-wrapper { overflow-x: auto; border-top: 1px solid var(--border); }.table { min-width: 1080px; }.table th { color: var(--text-mid); font-size: 11px; letter-spacing: .04em; text-transform: uppercase; }.table td { vertical-align: middle; }.table tr.muted { opacity: .67; }.coupon-code { color: var(--role-admin); letter-spacing: .03em; }.table td small { display: block; margin-top: 3px; color: var(--text-mid); font-size: 10px; }.type-pill { padding: 5px 8px; border-radius: 7px; color: #5b21b6; background: #ede9fe; font-size: 11px; font-weight: 700; }.usage { min-width: 85px; font-size: 11px; }.usage > div { width: 78px; height: 4px; margin-top: 6px; overflow: hidden; border-radius: 4px; background: var(--border); }.usage i { display: block; height: 100%; background: var(--primary); }.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 6px 9px; border-radius: 20px; font-size: 11px; font-weight: 700; }.status-pill i { width: 7px; height: 7px; border-radius: 50%; }.status-pill.active { color: #047857; background: #d1fae5; }.status-pill.active i { background: #10b981; }.status-pill.inactive { color: #b91c1c; background: #fee2e2; }.status-pill.inactive i { background: #ef4444; }.status-pill.expired { color: #92400e; background: #fef3c7; }.status-pill.expired i { background: #f59e0b; }.toggle-switch { position: relative; display: inline-block; width: 40px; height: 22px; }.toggle-switch input { width: 0; height: 0; opacity: 0; }.toggle-slider { position: absolute; inset: 0; border-radius: 22px; background: var(--border); cursor: pointer; transition: .2s; }.toggle-slider::before { position: absolute; bottom: 3px; left: 3px; width: 16px; height: 16px; border-radius: 50%; background: white; box-shadow: var(--shadow-xs); content: ''; transition: .2s; }.toggle-switch input:checked + .toggle-slider { background: var(--primary); }.toggle-switch input:checked + .toggle-slider::before { transform: translateX(18px); }.toggle-switch input:focus-visible + .toggle-slider { outline: 3px solid var(--primary-light); }.actions { display: flex; justify-content: flex-end; gap: 4px; }.icon-button { display: grid; width: 32px; height: 32px; place-items: center; border-radius: 8px; }.icon-button.edit { color: #2563eb; }.icon-button.delete { color: #dc2626; }.icon-button:hover { background: var(--surface); }.icon-button:disabled,.status-pill:disabled,.toggle-switch input:disabled + .toggle-slider { opacity: .45; cursor: wait; }
.table-footer { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; color: var(--text-mid); font-size: 12px; }.pagination { display: flex; align-items: center; gap: 10px; }.pagination button { display: grid; width: 32px; height: 32px; place-items: center; border: 1px solid var(--border); border-radius: 7px; }.pagination button:disabled { opacity: .4; cursor: not-allowed; }.state { display: flex; min-height: 280px; flex-direction: column; align-items: center; justify-content: center; gap: 10px; padding: 32px; color: var(--text-mid); text-align: center; }.state > i { color: var(--text-light); font-size: 36px; }.state.error > i { color: var(--red-active); }.spinner { width: 30px; height: 30px; border: 3px solid var(--border); border-top-color: var(--role-admin); border-radius: 50%; animation: spin .8s linear infinite; }.mini-spinner { width: 14px; height: 14px; border: 2px solid currentColor; border-top-color: transparent; border-radius: 50%; animation: spin .7s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }
.coupon-modal { max-width: 700px; overflow: hidden; }.modal-accent { height: 5px; background: linear-gradient(90deg, var(--primary), #f59e0b, #7c3aed); }.modal-header > div { display: flex; align-items: center; gap: 12px; }.modal-header p { margin: 3px 0 0; color: var(--text-mid); font-size: 12px; }.modal-icon { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 12px; color: var(--primary-dark); background: var(--primary-light); font-size: 19px; }.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }.form-grid .full { grid-column: 1 / -1; }.form-group small { display: block; margin-top: 5px; color: var(--text-mid); font-size: 11px; }.checkbox-label { display: flex; align-items: center; gap: 8px; padding: 6px 0 12px; color: var(--text-mid); cursor: pointer; }.checkbox-label input { accent-color: var(--primary); }.modal-footer { margin: 8px -24px -24px; padding: 16px 24px; background: var(--surface); }.modal-footer .btn-primary { min-width: 135px; }.modal-close:disabled { opacity: .4; cursor: not-allowed; }
@media (max-width: 1100px) { .stats { grid-template-columns: repeat(2, 1fr); }.filters { grid-template-columns: repeat(2, 1fr); }.search-box { grid-column: 1 / -1; } }@media (max-width: 640px) { .page-heading { align-items: flex-start; flex-direction: column; }.page-heading .btn { width: 100%; justify-content: center; }.stats { grid-template-columns: 1fr 1fr; gap: 9px; }.stats article { padding: 12px; }.filters,.form-grid { grid-template-columns: 1fr; }.search-box,.form-grid .full { grid-column: auto; }.table-footer { align-items: flex-start; flex-direction: column; gap: 10px; }.modal-footer { margin: 8px -16px -16px; padding: 14px 16px; } }
</style>
