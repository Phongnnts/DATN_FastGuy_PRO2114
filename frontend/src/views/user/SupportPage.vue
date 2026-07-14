<script setup>
import { onMounted, ref } from 'vue';
import { orderApi, supportApi } from '@/api';
import { formatDate } from '@/utils/format';

const tickets = ref([]);
const orders = ref([]);
const showForm = ref(false);
const saving = ref(false);
const error = ref('');
const form = ref({ subject: '', category: 'OTHER', description: '', orderId: '' });
const categories = {
  MISSING_ITEM: 'Thiếu món',
  COLD_FOOD: 'Đồ ăn nguội',
  WRONG_ITEM: 'Sai món',
  LATE_DELIVERY: 'Giao trễ',
  OTHER: 'Khác',
};

function statusClass(status) {
  return status === 'RESOLVED' ? 'badge-success' : status === 'PROCESSING' ? 'badge-info' : 'badge-warning';
}

function statusLabel(status) {
  return { OPEN: 'Mới', PROCESSING: 'Đang xử lý', RESOLVED: 'Đã giải quyết' }[status] || status;
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

async function submit() {
  saving.value = true;
  error.value = '';
  try {
    const ticket = await supportApi.create({ ...form.value, orderId: form.value.orderId ? Number(form.value.orderId) : null });
    tickets.value.unshift(ticket);
    form.value = { subject: '', category: 'OTHER', description: '', orderId: '' };
    showForm.value = false;
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}

onMounted(load);
</script>

<template>
  <div class="support-page">
    <div class="page-header">
      <div><h1>Hỗ trợ đơn hàng</h1><p>Gửi yêu cầu, chúng tôi sẽ phản hồi sớm nhất.</p></div>
      <button class="btn btn-primary" @click="showForm = true"><i class="bi bi-plus-lg"></i>Tạo yêu cầu</button>
    </div>
    <p v-if="error && !showForm" class="form-error">{{ error }}</p>
    <div v-if="tickets.length" class="ticket-list">
      <article v-for="ticket in tickets" :key="ticket.ticketId" class="card ticket">
        <div class="ticket-top"><div><strong>#{{ ticket.ticketId }} · {{ ticket.subject }}</strong><div class="meta">{{ categories[ticket.category] }}<span v-if="ticket.orderCode"> · {{ ticket.orderCode }}</span> · {{ formatDate(ticket.createdAt) }}</div></div><span class="badge" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span></div>
        <p>{{ ticket.description }}</p>
        <div v-if="ticket.resolution" class="resolution"><strong>Phản hồi</strong><span>{{ ticket.resolution }}</span></div>
      </article>
    </div>
    <div v-else class="card empty-state"><i class="bi bi-headset"></i><h3>Chưa có yêu cầu hỗ trợ</h3><p>Tạo yêu cầu nếu bạn cần chúng tôi giúp đỡ.</p></div>
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <form class="modal-content" @submit.prevent="submit">
        <div class="modal-header"><h2>Tạo yêu cầu hỗ trợ</h2><button type="button" class="btn btn-ghost" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="form-group"><label class="form-label">Tiêu đề</label><input v-model.trim="form.subject" class="form-input" maxlength="255" required placeholder="Mô tả ngắn vấn đề" /></div>
        <div class="form-group"><label class="form-label">Loại vấn đề</label><select v-model="form.category" class="form-select"><option v-for="(label, key) in categories" :key="key" :value="key">{{ label }}</option></select></div>
        <div class="form-group"><label class="form-label">Đơn hàng</label><select v-model="form.orderId" class="form-select"><option value="">Không liên kết đơn hàng</option><option v-for="order in orders" :key="order.orderId" :value="order.orderId">{{ order.orderCode }}</option></select></div>
        <div class="form-group"><label class="form-label">Chi tiết</label><textarea v-model.trim="form.description" class="form-textarea" maxlength="2000" required placeholder="Hãy cho chúng tôi biết chi tiết vấn đề"></textarea></div>
        <p v-if="error" class="form-error">{{ error }}</p><button class="btn btn-primary submit" :disabled="saving">{{ saving ? 'Đang gửi...' : 'Gửi yêu cầu' }}</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.support-page { padding: 32px 0; }.page-header { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; }.page-header h1 { margin:0 0 6px; font-size:26px; }.page-header p,.meta { margin:0; color:var(--text-mid); font-size:13px; }.ticket-list { display:grid; gap:12px; }.ticket { padding:18px; }.ticket-top { display:flex; justify-content:space-between; gap:16px; }.ticket p { margin:14px 0 0; white-space:pre-wrap; color:var(--text-mid); }.resolution { display:grid; gap:4px; margin-top:14px; padding:12px; border-radius:var(--radius-sm); background:var(--primary-50); font-size:14px; }.modal-header { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }.modal-header h2 { margin:0; font-size:20px; }.submit { width:100%; }.empty-state { text-align:center; padding:52px 20px; }.empty-state i { font-size:30px; color:var(--primary); }.empty-state h3 { margin:12px 0 6px; }.empty-state p { margin:0; color:var(--text-mid); }@media (max-width:600px) { .page-header { align-items:flex-start; flex-direction:column; } }
</style>
