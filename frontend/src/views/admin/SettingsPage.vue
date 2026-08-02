<script setup>
import { ref, onMounted } from 'vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const toast = useToast();
const form = ref({
  store_name: 'FastGuy', store_phone: '', store_address: '', store_logo: '',
  business_open_time: '08:00', business_close_time: '22:00',
  service_fee: 0, tax_rate: 0, delivery_fee: 0, min_order_amount: 0,
  estimated_delivery_minutes: 30,
});
const loading = ref(true);
const saving = ref(false);
const success = ref('');

async function load() {
  loading.value = true;
  try {
    const data = await adminApi.getSettings();
    form.value = {
      store_name: data.store_name || 'FastGuy',
      store_phone: data.store_phone || '',
      store_address: data.store_address || '',
      store_logo: data.store_logo || '',
      business_open_time: data.business_open_time || '08:00',
      business_close_time: data.business_close_time || '22:00',
      service_fee: Number(data.service_fee || 0),
      tax_rate: Number(data.tax_rate || 0),
      delivery_fee: Number(data.delivery_fee || 0),
      min_order_amount: Number(data.min_order_amount || 0),
      estimated_delivery_minutes: Number(data.estimated_delivery_minutes || 30),
    };
  } catch (e) {
    toast.error(e.message);
  } finally {
    loading.value = false;
  }
}

async function save() {
  saving.value = true;
  try {
    const payload = {};
    for (const [key, val] of Object.entries(form.value)) {
      payload[key] = typeof val === 'number' ? Number(val) : val;
    }
    await adminApi.updateSettings(payload);
    success.value = 'Đã lưu cài đặt';
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    toast.error(e.message);
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div>
    <div class="page-header"><h1>Cài đặt cửa hàng</h1></div>
    <div v-if="loading" class="card card-flat text-center" style="padding:32px"><div class="spinner"></div></div>
    <form v-else class="card card-flat" style="max-width:640px" @submit.prevent="save">
      <div v-if="success" style="padding:10px 14px;margin-bottom:16px;border-radius:var(--radius);background:#dcfce7;color:#166534;font-size:14px">{{ success }}</div>

      <h3 style="font-size:15px;margin-bottom:12px"><i class="bi bi-shop"></i> Thông tin cửa hàng</h3>
      <div class="form-group"><label class="form-label">Tên cửa hàng</label><input v-model="form.store_name" class="form-input" required></div>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
        <div class="form-group"><label class="form-label">Số điện thoại</label><input v-model="form.store_phone" class="form-input" type="tel"></div>
        <div class="form-group"><label class="form-label">Logo URL</label><input v-model="form.store_logo" class="form-input" placeholder="https://..."></div>
      </div>
      <div class="form-group"><label class="form-label">Địa chỉ</label><input v-model="form.store_address" class="form-input"></div>

      <hr style="margin:20px 0;border:none;border-top:1px solid var(--border)">

      <h3 style="font-size:15px;margin-bottom:12px"><i class="bi bi-clock"></i> Giờ hoạt động</h3>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
        <div class="form-group"><label class="form-label">Giờ mở cửa</label><input v-model="form.business_open_time" class="form-input" type="time" required></div>
        <div class="form-group"><label class="form-label">Giờ đóng cửa</label><input v-model="form.business_close_time" class="form-input" type="time" required></div>
      </div>

      <hr style="margin:20px 0;border:none;border-top:1px solid var(--border)">

      <h3 style="font-size:15px;margin-bottom:12px"><i class="bi bi-cash-stack"></i> Phí & Thuế</h3>
      <div style="display:grid;grid-template-columns:1fr 1fr;gap:12px">
        <div class="form-group"><label class="form-label">Phí dịch vụ (VNĐ)</label><input v-model.number="form.service_fee" class="form-input" type="number" min="0"></div>
        <div class="form-group"><label class="form-label">Thuế (%)</label><input v-model.number="form.tax_rate" class="form-input" type="number" min="0" max="100" step="0.1"></div>
        <div class="form-group"><label class="form-label">Phí ship mặc định (VNĐ)</label><input v-model.number="form.delivery_fee" class="form-input" type="number" min="0"></div>
        <div class="form-group"><label class="form-label">Đơn tối thiểu (VNĐ)</label><input v-model.number="form.min_order_amount" class="form-input" type="number" min="0"></div>
      </div>

      <hr style="margin:20px 0;border:none;border-top:1px solid var(--border)">

      <h3 style="font-size:15px;margin-bottom:12px"><i class="bi bi-truck"></i> Giao hàng</h3>
      <div class="form-group" style="max-width:280px"><label class="form-label">Thời gian giao ước tính (phút)</label><input v-model.number="form.estimated_delivery_minutes" class="form-input" type="number" min="10" max="180"></div>

      <div style="margin-top:20px"><button class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
    </form>
  </div>
</template>
