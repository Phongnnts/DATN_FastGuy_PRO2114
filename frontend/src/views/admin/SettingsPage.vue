<script setup>
import { ref, onMounted } from 'vue';
import { adminApi } from '@/api';
import { useToastStore } from '@/stores/toast';

const toast = useToastStore();

const form = ref({ business_open_time: '08:00', business_close_time: '22:00', service_fee: 0 });
const loading = ref(true);
const saving = ref(false);

async function load() {
  loading.value = true;
  try {
    const data = await adminApi.getSettings();
    form.value = {
      business_open_time: data.business_open_time || '08:00',
      business_close_time: data.business_close_time || '22:00',
      service_fee: Number(data.service_fee || 0),
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
    await adminApi.updateSettings({ ...form.value, service_fee: Number(form.value.service_fee) });
    toast.success('Đã lưu cài đặt');
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
    <form v-else class="card card-flat" style="max-width:560px" @submit.prevent="save">
      <div class="form-group">
        <label class="form-label">Giờ mở cửa</label>
        <input v-model="form.business_open_time" class="form-input" type="time" required>
      </div>
      <div class="form-group">
        <label class="form-label">Giờ đóng cửa</label>
        <input v-model="form.business_close_time" class="form-input" type="time" required>
      </div>
      <div class="form-group">
        <label class="form-label">Phí dịch vụ</label>
        <input v-model.number="form.service_fee" class="form-input" type="number" min="0" step="1" required>
      </div>
      <button class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button>
    </form>
  </div>
</template>
