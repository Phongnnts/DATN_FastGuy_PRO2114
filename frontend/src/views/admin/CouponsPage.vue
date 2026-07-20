<script setup>
import { computed, onMounted, ref } from 'vue';
import couponApi from '@/api/coupon';
import { useToast } from '@/stores/toast';

const toast = useToast();
const coupons = ref([]);
const loading = ref(true);
const error = ref('');
const showModal = ref(false);
const editing = ref(null);
const submitting = ref(false);
const form = ref(defaultForm());

function defaultForm() {
  return { code: '', type: 'PERCENT', value: 0, minOrder: 0, maxDiscount: null, maxUses: 0, expiresAt: '', isPublic: true };
}

const valueLabel = computed(() => form.value.type === 'PERCENT' ? 'Phần trăm giảm' : form.value.type === 'FIXED' ? 'Giá trị giảm' : 'Giá trị');

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

function validate() {
  if (!/^[A-Z0-9_-]{3,50}$/.test(form.value.code.trim())) return 'Mã gồm 3-50 ký tự: chữ, số, - hoặc _';
  if (form.value.type === 'PERCENT' && (form.value.value <= 0 || form.value.value > 100)) return 'Phần trăm giảm phải từ 1 đến 100';
  if (form.value.type === 'FIXED' && form.value.value <= 0) return 'Giá trị giảm phải lớn hơn 0';
  if (form.value.minOrder < 0 || form.value.maxUses < 0 || form.value.maxDiscount < 0) return 'Giá trị không được âm';
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
      minOrder: Number(form.value.minOrder || 0),
      maxUses: Number(form.value.maxUses || 0),
      maxDiscount: form.value.type === 'PERCENT' && form.value.maxDiscount !== null && form.value.maxDiscount !== '' ? Number(form.value.maxDiscount) : null,
      expiresAt: form.value.expiresAt || null,
    };
    if (editing.value) await couponApi.update(editing.value.couponId, payload);
    else await couponApi.create(payload);
    showModal.value = false;
    await load();
  } catch (e) {
    toast.error(e.message || 'Không thể lưu mã giảm giá');
  } finally {
    submitting.value = false;
  }
}

async function remove(coupon) {
  if (!coupon.canDelete) return toast.error('Mã đã có lịch sử nhận hoặc sử dụng, chỉ có thể tắt kích hoạt');
  if (!confirm(`Xóa mã ${coupon.code}?`)) return;
  try {
    await couponApi.delete(coupon.couponId);
    await load();
  } catch (e) {
    toast.error(e.message || 'Không thể xóa mã giảm giá');
  }
}

async function toggle(coupon, field) {
  const previous = coupon[field];
  coupon[field] = !previous;
  try {
    await couponApi.update(coupon.couponId, { [field]: coupon[field] });
  } catch (e) {
    coupon[field] = previous;
    toast.error(e.message || 'Không thể cập nhật mã giảm giá');
  }
}

function typeLabel(type) {
  return { PERCENT: '%', FIXED: 'Tiền mặt', FREE_SHIPPING: 'Miễn phí ship' }[type] || type;
}

function valueText(coupon) {
  if (coupon.type === 'PERCENT') return `${coupon.value}%`;
  if (coupon.type === 'FREE_SHIPPING') return 'Phí giao hàng';
  return `${Number(coupon.value || 0).toLocaleString('vi-VN')}đ`;
}

onMounted(load);
</script>

<template>
  <div class="coupons-page">
    <div class="page-header">
      <div><h1>Mã giảm giá</h1><p>Quản lý ưu đãi cho khách hàng.</p></div>
      <button class="btn btn-primary" @click="openCreate"><i class="bi bi-plus-lg"></i> Tạo mã</button>
    </div>

    <div v-if="loading" class="admin-state"><span class="spinner"></span> Đang tải mã giảm giá...</div>
    <div v-else-if="error" class="admin-state admin-error"><span>{{ error }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <div v-else-if="coupons.length === 0" class="empty-state"><i class="bi bi-ticket-perforated"></i><h3>Chưa có mã giảm giá</h3><p>Tạo mã đầu tiên để bắt đầu chương trình ưu đãi.</p></div>

    <div v-else class="table-wrapper">
      <table class="table">
        <thead><tr><th scope="col">Mã</th><th scope="col">Loại</th><th scope="col">Giá trị</th><th scope="col">Đơn tối thiểu</th><th scope="col">Đã dùng</th><th scope="col">Giảm tối đa</th><th scope="col">Hết hạn</th><th scope="col">Kích hoạt</th><th scope="col">Công khai</th><th scope="col"><span class="sr-only">Thao tác</span></th></tr></thead>
        <tbody>
          <tr v-for="coupon in coupons" :key="coupon.couponId">
            <td><strong>{{ coupon.code }}</strong></td><td>{{ typeLabel(coupon.type) }}</td><td>{{ valueText(coupon) }}</td>
            <td>{{ Number(coupon.minOrder || 0).toLocaleString('vi-VN') }}đ</td><td>{{ coupon.usedCount }}/{{ coupon.maxUses || '∞' }}</td>
            <td>{{ coupon.maxDiscount ? `${Number(coupon.maxDiscount).toLocaleString('vi-VN')}đ` : '—' }}</td><td>{{ coupon.expiresAt ? new Date(coupon.expiresAt).toLocaleDateString('vi-VN') : '—' }}</td>
            <td><label class="toggle-switch" :aria-label="`Kích hoạt ${coupon.code}`"><input type="checkbox" :checked="coupon.isActive" @change="toggle(coupon, 'isActive')"><span class="toggle-slider"></span></label></td>
            <td><label class="toggle-switch" :aria-label="`Công khai ${coupon.code}`"><input type="checkbox" :checked="coupon.isPublic" @change="toggle(coupon, 'isPublic')"><span class="toggle-slider"></span></label></td>
            <td class="actions"><button class="btn btn-sm btn-ghost" :aria-label="`Sửa ${coupon.code}`" @click="openEdit(coupon)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost text-danger" :disabled="!coupon.canDelete" :title="coupon.canDelete ? 'Xóa mã' : 'Mã đã có lịch sử, hãy tắt kích hoạt'" :aria-label="`Xóa ${coupon.code}`" @click="remove(coupon)"><i class="bi bi-trash"></i></button></td>
          </tr>
        </tbody>
      </table>
    </div>

    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal" role="dialog" aria-modal="true" aria-labelledby="coupon-modal-title">
        <div class="modal-header"><h2 id="coupon-modal-title" class="modal-title">{{ editing ? 'Sửa mã giảm giá' : 'Tạo mã giảm giá' }}</h2><button class="modal-close" aria-label="Đóng" @click="showModal = false"><i class="bi bi-x-lg"></i></button></div>
        <form class="modal-body" @submit.prevent="save">
          <div class="form-group"><label class="form-label" for="coupon-code">Mã *</label><input id="coupon-code" v-model="form.code" class="form-input" placeholder="VD: GIAM10" maxlength="50" @input="form.code = form.code.toUpperCase()"></div>
          <div class="form-group"><label class="form-label" for="coupon-type">Loại</label><select id="coupon-type" v-model="form.type" class="form-select"><option value="PERCENT">Giảm theo phần trăm</option><option value="FIXED">Giảm tiền mặt</option><option value="FREE_SHIPPING">Miễn phí vận chuyển</option></select></div>
          <div v-if="form.type !== 'FREE_SHIPPING'" class="form-group"><label class="form-label" for="coupon-value">{{ valueLabel }} *</label><input id="coupon-value" v-model.number="form.value" type="number" class="form-input" :min="form.type === 'PERCENT' ? 1 : 0" :max="form.type === 'PERCENT' ? 100 : undefined"></div>
          <div class="form-group"><label class="form-label" for="coupon-min-order">Đơn tối thiểu</label><input id="coupon-min-order" v-model.number="form.minOrder" type="number" class="form-input" min="0"></div>
          <div v-if="form.type === 'PERCENT'" class="form-group"><label class="form-label" for="coupon-max-discount">Giảm tối đa</label><input id="coupon-max-discount" v-model.number="form.maxDiscount" type="number" class="form-input" min="0" placeholder="Không giới hạn"></div>
          <div class="form-group"><label class="form-label" for="coupon-max-uses">Số lần dùng tối đa</label><input id="coupon-max-uses" v-model.number="form.maxUses" type="number" class="form-input" min="0"><small>Nhập 0 để không giới hạn.</small></div>
          <div class="form-group"><label class="form-label" for="coupon-expiry">Hết hạn</label><input id="coupon-expiry" v-model="form.expiresAt" type="datetime-local" class="form-input"><small>Để trống nếu không giới hạn thời hạn.</small></div>
          <label class="checkbox-label"><input v-model="form.isPublic" type="checkbox"> Hiển thị công khai cho khách hàng</label>
          <div class="modal-footer"><button type="button" class="btn btn-ghost" @click="showModal = false">Hủy</button><button type="submit" class="btn btn-primary" :disabled="submitting">{{ submitting ? 'Đang lưu...' : 'Lưu mã' }}</button></div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.coupons-page { padding: 0; }
.page-header p { margin: 4px 0 0; color: var(--text-mid); font-size: 14px; }
.actions { display: flex; gap: 4px; white-space: nowrap; }
.admin-state { display: flex; align-items: center; justify-content: center; gap: 10px; min-height: 180px; color: var(--text-mid); }
.admin-error { flex-direction: column; color: var(--red-active); }
.toggle-switch { position: relative; display: inline-block; width: 42px; height: 24px; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: var(--border); border-radius: var(--radius-full); transition: background var(--transition-fast); }
.toggle-slider::before { content: ''; position: absolute; height: 18px; width: 18px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: transform var(--transition-fast); box-shadow: var(--shadow-xs); }
.toggle-switch input:checked + .toggle-slider { background: var(--role-accent, var(--primary)); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(18px); }
.toggle-switch input:focus-visible + .toggle-slider { outline: 3px solid var(--role-soft, var(--primary-50)); outline-offset: 2px; }
.checkbox-label { display: flex; align-items: center; gap: 8px; cursor: pointer; color: var(--text-mid); }
.checkbox-label input { accent-color: var(--role-accent, var(--primary)); }
small { display: block; margin-top: 5px; color: var(--text-mid); font-size: 12px; }
</style>
