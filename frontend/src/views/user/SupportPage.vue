<script setup>
import { onMounted, ref } from 'vue';
import { orderApi, supportApi } from '@/api';
import { formatDate } from '@/utils/format';
import { useToast } from '@/utils/toast';

const toast = useToast();
const tickets = ref([]);
const orders = ref([]);
const showForm = ref(false);
const saving = ref(false);
const error = ref('');
const form = ref({ subject: '', category: 'OTHER', description: '', orderId: '' });
const categories = {
  MISSING_ITEM: 'Thieu mon',
  COLD_FOOD: 'Do an nguoi',
  WRONG_ITEM: 'Sai mon',
  LATE_DELIVERY: 'Giao tre',
  OTHER: 'Khac',
};
const categoryLabels = {
  MISSING_ITEM: 'Thieu mon',
  COLD_FOOD: 'Do an nguoi',
  WRONG_ITEM: 'Sai mon',
  LATE_DELIVERY: 'Giao tre',
  OTHER: 'Khac',
};
const statusLabels = {
  OPEN: 'Moi',
  PROCESSING: 'Dang xu ly',
  RESOLVED: 'Da giai quyet',
};

function statusClass(status) {
  return status === 'RESOLVED' ? 'badge-success' : status === 'PROCESSING' ? 'badge-info' : 'badge-warning';
}

function statusLabel(status) {
  return statusLabels[status] || status;
}

async function load() {
  try {
    const [ticketData, orderData] = await Promise.all([supportApi.getMine(), orderApi.getAll()]);
    tickets.value = ticketData || [];
    orders.value = orderData || [];
  } catch (e) {
    error.value = e.message;
  }
}

function validateForm() {
  if (!form.value.subject.trim()) {
    toast.error('Vui long nhap tieu de');
    return false;
  }
  if (form.value.subject.trim().length > 255) {
    toast.error('Tieu de toi da 255 ky tu');
    return false;
  }
  if (!form.value.description.trim()) {
    toast.error('Vui long nhap mo ta chi tiet');
    return false;
  }
  if (form.value.description.trim().length > 2000) {
    toast.error('Mo ta toi da 2000 ky tu');
    return false;
  }
  if (!form.value.category) {
    toast.error('Vui long chon loai van de');
    return false;
  }
  return true;
}

async function submit() {
  if (!validateForm()) return;
  saving.value = true;
  error.value = '';
  try {
    const ticket = await supportApi.create({ ...form.value, orderId: form.value.orderId ? Number(form.value.orderId) : null });
    tickets.value.unshift(ticket);
    form.value = { subject: '', category: 'OTHER', description: '', orderId: '' };
    showForm.value = false;
    toast.success('Gui yeu cau thanh cong!');
  } catch (e) {
    error.value = e.message;
    toast.error(e.message || 'Loi khi gui yeu cau');
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="support-page">
    <div class="page-header">
      <div><h1>Ho tro don hang</h1><p>Gui yeu cau, chung toi se phan hoi som nhat.</p></div>
      <button class="btn btn-primary" @click="showForm = true"><i class="bi bi-plus-lg"></i>Tao yeu cau</button>
    </div>
    <p v-if="error && !showForm" class="form-error">{{ error }}</p>
    <div v-if="tickets.length" class="ticket-list">
      <article v-for="ticket in tickets" :key="ticket.ticketId" class="card ticket">
        <div class="ticket-top">
          <div>
            <strong>#{{ ticket.ticketId }} &middot; {{ ticket.subject }}</strong>
            <div class="meta">
              {{ categoryLabels[ticket.category] || ticket.category }}
              <span v-if="ticket.orderCode"> &middot; {{ ticket.orderCode }}</span>
              &middot; {{ formatDate(ticket.createdAt) }}
            </div>
          </div>
          <span class="badge" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span>
        </div>
        <p>{{ ticket.description }}</p>
        <div v-if="ticket.resolution" class="resolution">
          <strong><i class="bi bi-reply"></i> Phan hoi cua nhan vien</strong>
          <span>{{ ticket.resolution }}</span>
          <small v-if="ticket.resolvedAt"> &middot; {{ formatDate(ticket.resolvedAt) }}</small>
        </div>
      </article>
    </div>
    <div v-else class="card empty-state"><i class="bi bi-headset"></i><h3>Chua co yeu cau ho tro</h3><p>Tao yeu cau neu ban can chung toi giup do.</p></div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <form class="modal-content" @submit.prevent="submit">
        <div class="modal-header"><h2>Tao yeu cau ho tro</h2><button type="button" class="btn btn-ghost" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="form-group">
          <label class="form-label">Tieu de</label>
          <input v-model.trim="form.subject" class="form-input" maxlength="255" required placeholder="Mo ta ngan van de" />
          <small class="field-hint">{{ form.subject.length }}/255 ky tu</small>
        </div>
        <div class="form-group">
          <label class="form-label">Loai van de</label>
          <select v-model="form.category" class="form-select" required>
            <option v-for="(label, key) in categories" :key="key" :value="key">{{ label }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Don hang</label>
          <select v-model="form.orderId" class="form-select">
            <option value="">Khong lien ket don hang</option>
            <option v-for="order in orders" :key="order.orderId" :value="order.orderId">{{ order.orderCode }}</option>
          </select>
        </div>
        <div class="form-group">
          <label class="form-label">Chi tiet</label>
          <textarea v-model.trim="form.description" class="form-textarea" maxlength="2000" required placeholder="Hay cho chung toi biet chi tiet van de"></textarea>
          <small class="field-hint">{{ form.description.length }}/2000 ky tu</small>
        </div>
        <p v-if="error" class="form-error">{{ error }}</p>
        <button class="btn btn-primary submit" :disabled="saving">{{ saving ? 'Dang gui...' : 'Gui yeu cau' }}</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.support-page { padding: 32px 0; }
.page-header { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; }
.page-header h1 { margin:0 0 6px; font-size:26px; }
.page-header p,.meta { margin:0; color:var(--text-mid); font-size:13px; }
.ticket-list { display:grid; gap:12px; }
.ticket { padding:18px; }
.ticket-top { display:flex; justify-content:space-between; gap:16px; }
.ticket p { margin:14px 0 0; white-space:pre-wrap; color:var(--text-mid); }
.resolution { display:grid; gap:4px; margin-top:14px; padding:12px; border-radius:var(--radius-sm); background:var(--primary-50); font-size:14px; }
.resolution strong { display:flex; align-items:center; gap:6px; }
.resolution small { color:var(--text-mid); font-size:12px; }
.field-hint { display:block; margin-top:4px; font-size:12px; color:var(--text-mid); }
.modal-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.modal-header h2 { margin:0; font-size:20px; }
.submit { width:100%; }
.empty-state { text-align:center; padding:52px 20px; }
.empty-state i { font-size:30px; color:var(--primary); }
.empty-state h3 { margin:12px 0 6px; }
.empty-state p { margin:0; color:var(--text-mid); }
@media (max-width:600px) { .page-header { align-items:flex-start; flex-direction:column; } }
</style>
