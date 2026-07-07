<script setup>
import { ref, onMounted } from 'vue';
import couponApi from '@/api/coupon';

const coupons = ref([]);
const loading = ref(true);
const showModal = ref(false);
const editing = ref(null);
const form = ref({ code: '', type: 'PERCENT', value: 0, minOrder: 0, maxDiscount: null, maxUses: 0, expiresAt: '', isPublic: true });
const submitting = ref(false);
const formError = ref('');

async function load() {
  loading.value = true;
  try { coupons.value = await couponApi.getAll() || []; } catch { coupons.value = []; }
  finally { loading.value = false; }
}

function openCreate() {
  editing.value = null;
  formError.value = '';
  form.value = { code: '', type: 'PERCENT', value: 0, minOrder: 0, maxDiscount: null, maxUses: 0, expiresAt: '' };
  showModal.value = true;
}

function openEdit(c) {
  editing.value = c;
  formError.value = '';
  form.value = {
    code: c.code,
    type: c.type,
    value: c.value,
    minOrder: c.minOrder || 0,
    maxDiscount: c.maxDiscount,
    maxUses: c.maxUses || 0,
    expiresAt: c.expiresAt ? c.expiresAt.replace(' ', 'T').substring(0, 16) : '',
    isPublic: c.isPublic !== false,
  };
  showModal.value = true;
}

async function save() {
  formError.value = validateForm();
  if (formError.value) return;
  submitting.value = true;
  try {
    const payload = { ...form.value, code: form.value.code.trim().toUpperCase(), value: Number(form.value.value) };
    if (editing.value) {
      await couponApi.update(editing.value.couponId, payload);
    } else {
      await couponApi.create(payload);
    }
    showModal.value = false;
    await load();
  } catch (e) {
    formError.value = e.response?.data?.message || e.message || 'Lưu mã giảm giá thất bại';
  } finally {
    submitting.value = false;
  }
}

function validateForm() {
  const code = form.value.code.trim();
  if (!/^[A-Z0-9_-]{3,30}$/.test(code)) return 'Mã chỉ gồm chữ hoa, số, _ hoặc -, dài 3-30 ký tự';
  if (!['PERCENT', 'FIXED', 'FREE_SHIPPING'].includes(form.value.type)) return 'Loại mã không hợp lệ';
  if (form.value.type === 'PERCENT' && (Number(form.value.value) <= 0 || Number(form.value.value) > 100)) return 'Phần trăm giảm phải từ 1 đến 100';
  if (form.value.type === 'FIXED' && Number(form.value.value) <= 0) return 'Số tiền giảm phải lớn hơn 0';
  if (form.value.type === 'FREE_SHIPPING') form.value.value = 0;
  if (Number(form.value.minOrder) < 0) return 'Đơn tối thiểu không được âm';
  if (form.value.maxDiscount !== null && form.value.maxDiscount !== '' && Number(form.value.maxDiscount) < 0) return 'Giảm tối đa không được âm';
  if (Number(form.value.maxUses) < 0) return 'Số lần dùng không được âm';
  return '';
}

async function remove(id) {
  if (!confirm('Xoá mã này?')) return;
  try {
    await couponApi.delete(id);
    await load();
  } catch (e) {
    alert(e.response?.data?.message || e.message);
  }
}

function typeLabel(t) {
  return { PERCENT: '%', FIXED: 'Tiền mặt', FREE_SHIPPING: 'Free ship' }[t] || t;
}

async function toggleActive(c) {
  try {
    await couponApi.update(c.couponId, { isActive: !c.isActive });
    c.isActive = !c.isActive;
  } catch (e) { alert(e.response?.data?.message || e.message); }
}

async function togglePublic(c) {
  try {
    await couponApi.update(c.couponId, { isPublic: !c.isPublic });
    c.isPublic = !c.isPublic;
  } catch (e) { alert(e.response?.data?.message || e.message); }
}

onMounted(load);
</script>

<template>
  <div class="coupons-page">
    <div class="page-header">
      <h3>Mã giảm giá</h3>
      <button class="btn btn-primary btn-sm" @click="openCreate"><i class="bi bi-plus-lg"></i> Tạo mã</button>
    </div>

    <div v-if="loading" class="text-center py-4"><div class="spinner"></div></div>

    <div v-else-if="coupons.length === 0" class="empty-state"><i class="bi bi-ticket-perforated"></i><h4>Chưa có mã giảm giá</h4></div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>Mã</th><th>Loại</th><th>Giá trị</th><th>Đơn tối thiểu</th><th>Đã dùng</th><th>Giới hạn</th><th>Hết hạn</th><th>Kích hoạt</th><th>Công khai</th><th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="c in coupons" :key="c.couponId">
          <td><strong>{{ c.code }}</strong></td>
          <td>{{ typeLabel(c.type) }}</td>
          <td>{{ c.type === 'PERCENT' ? c.value + '%' : c.value?.toLocaleString() + 'đ' }}</td>
          <td>{{ c.minOrder?.toLocaleString() }}đ</td>
          <td>{{ c.usedCount }}/{{ c.maxUses || '∞' }}</td>
          <td>{{ c.maxDiscount ? c.maxDiscount.toLocaleString() + 'đ' : '—' }}</td>
          <td>{{ c.expiresAt ? new Date(c.expiresAt).toLocaleDateString('vi') : '—' }}</td>
          <td>
            <label class="toggle-switch">
              <input type="checkbox" :checked="c.isActive" @change="toggleActive(c)">
              <span class="toggle-slider"></span>
            </label>
          </td>
          <td>
            <label class="toggle-switch">
              <input type="checkbox" :checked="c.isPublic" @change="togglePublic(c)">
              <span class="toggle-slider"></span>
            </label>
          </td>
          <td>
            <button class="btn btn-sm btn-ghost" @click="openEdit(c)"><i class="bi bi-pencil"></i></button>
            <button class="btn btn-sm btn-ghost text-danger" @click="remove(c.couponId)"><i class="bi bi-trash"></i></button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal" style="max-width: 500px">
        <div class="modal-header">
          <h4>{{ editing ? 'Sửa mã' : 'Tạo mã mới' }}</h4>
          <button class="btn btn-ghost" @click="showModal = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <p v-if="formError" class="form-error">{{ formError }}</p>
          <div class="form-group">
            <label>Mã *</label>
            <input v-model="form.code" class="form-input" placeholder="VD: GIAM10" maxlength="30" required @input="form.code = form.code.toUpperCase()">
          </div>
          <div class="form-group">
            <label>Loại</label>
            <select v-model="form.type" class="form-input">
              <option value="PERCENT">% giảm trên tổng đơn</option>
              <option value="FIXED">Giảm tiền mặt</option>
              <option value="FREE_SHIPPING">Free ship</option>
            </select>
          </div>
          <div class="form-group">
            <label>Giá trị</label>
            <input v-model.number="form.value" type="number" class="form-input" min="0">
          </div>
          <div class="form-group">
            <label>Đơn tối thiểu</label>
            <input v-model.number="form.minOrder" type="number" class="form-input" min="0">
          </div>
          <div class="form-group" v-if="form.type === 'PERCENT'">
            <label>Giảm tối đa (để trống nếu không giới hạn)</label>
            <input v-model.number="form.maxDiscount" type="number" class="form-input" min="0">
          </div>
          <div class="form-group">
            <label>Số lần dùng tối đa (0 = không giới hạn)</label>
            <input v-model.number="form.maxUses" type="number" class="form-input" min="0">
          </div>
          <div class="form-group">
            <label>Hết hạn</label>
            <input v-model="form.expiresAt" type="datetime-local" class="form-input">
          </div>
          <div class="form-group">
            <label class="checkbox-label">
              <input v-model="form.isPublic" type="checkbox">
              Hiển thị công khai cho khách hàng
            </label>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-ghost" @click="showModal = false">Huỷ</button>
          <button class="btn btn-primary" :disabled="submitting" @click="save">{{ submitting ? 'Đang lưu...' : 'Lưu' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.coupons-page { padding: 0; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.empty-state { text-align: center; padding: 60px 0; color: var(--text-mid); }
.empty-state i { font-size: 48px; margin-bottom: 16px; display: block; }
.toggle-switch { position: relative; display: inline-block; width: 40px; height: 22px; }
.toggle-switch input { opacity: 0; width: 0; height: 0; }
.toggle-slider { position: absolute; cursor: pointer; inset: 0; background: #ccc; border-radius: 22px; transition: .3s; }
.toggle-slider::before { content: ''; position: absolute; height: 16px; width: 16px; left: 3px; bottom: 3px; background: white; border-radius: 50%; transition: .3s; }
.toggle-switch input:checked + .toggle-slider { background: var(--primary); }
.toggle-switch input:checked + .toggle-slider::before { transform: translateX(18px); }
.checkbox-label { display: flex; align-items: center; gap: 8px; cursor: pointer; }
</style>
