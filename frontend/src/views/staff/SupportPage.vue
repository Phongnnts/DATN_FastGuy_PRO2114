<script setup>
import { computed, onMounted, ref } from 'vue';
import { supportApi } from '@/api';
import { formatDate } from '@/utils/format';

const tickets = ref([]);
const all = ref(false);
const error = ref('');
const editing = ref(null);
const saving = ref(false);
const statusOptions = ['OPEN', 'PROCESSING', 'RESOLVED'];
const labels = { OPEN: 'Mới', PROCESSING: 'Đang xử lý', RESOLVED: 'Đã giải quyết', MISSING_ITEM: 'Thiếu món', COLD_FOOD: 'Đồ ăn nguội', WRONG_ITEM: 'Sai món', LATE_DELIVERY: 'Giao trễ', OTHER: 'Khác' };
const visibleTickets = computed(() => tickets.value);

function statusClass(status) { return status === 'RESOLVED' ? 'badge-success' : status === 'PROCESSING' ? 'badge-info' : 'badge-warning'; }
async function load() { try { tickets.value = await supportApi.getStaff(all.value); } catch (e) { error.value = e.message; } }
function openEdit(ticket) { editing.value = { ...ticket, resolution: ticket.resolution || '' }; }
async function save() { saving.value = true; error.value = ''; try { const updated = await supportApi.update(editing.value.ticketId, { status: editing.value.status, resolution: editing.value.resolution }); const index = tickets.value.findIndex((ticket) => ticket.ticketId === updated.ticketId); if (index >= 0) tickets.value[index] = updated; editing.value = null; if (!all.value && updated.status === 'RESOLVED') tickets.value = tickets.value.filter((ticket) => ticket.ticketId !== updated.ticketId); } catch (e) { error.value = e.message; } finally { saving.value = false; } }
onMounted(load);
</script>

<template>
  <div class="support-page">
    <div class="page-header"><div><h1>Yêu cầu hỗ trợ</h1><p>Xử lý phản hồi của khách hàng.</p></div><label class="filter"><input v-model="all" type="checkbox" @change="load" /> Hiện cả đã giải quyết</label></div>
    <p v-if="error && !editing" class="form-error">{{ error }}</p>
    <div class="card card-flat"><div v-if="visibleTickets.length" class="table-wrapper"><table class="table"><thead><tr><th>Mã</th><th>Khách hàng</th><th>Vấn đề</th><th>Đơn hàng</th><th>Trạng thái</th><th>Thời gian</th><th></th></tr></thead><tbody><tr v-for="ticket in visibleTickets" :key="ticket.ticketId"><td>#{{ ticket.ticketId }}</td><td>{{ ticket.customerName || `User #${ticket.userId}` }}</td><td><strong>{{ ticket.subject }}</strong><span class="category">{{ labels[ticket.category] }}</span></td><td>{{ ticket.orderCode || '—' }}</td><td><span class="badge" :class="statusClass(ticket.status)">{{ labels[ticket.status] }}</span></td><td>{{ formatDate(ticket.createdAt) }}</td><td><button class="btn btn-outline btn-sm" @click="openEdit(ticket)">Xử lý</button></td></tr></tbody></table></div><div v-else class="empty-state">Không có yêu cầu hỗ trợ.</div></div>
    <div v-if="editing" class="modal-overlay" @click.self="editing = null"><form class="modal-content" @submit.prevent="save"><div class="modal-header"><h2>#{{ editing.ticketId }} · {{ editing.subject }}</h2><button type="button" class="btn btn-ghost" @click="editing = null"><i class="bi bi-x-lg"></i></button></div><p class="description">{{ editing.description }}</p><div class="form-group"><label class="form-label">Trạng thái</label><select v-model="editing.status" class="form-select"><option v-for="status in statusOptions" :key="status" :value="status">{{ labels[status] }}</option></select></div><div class="form-group"><label class="form-label">Phản hồi khách hàng</label><textarea v-model.trim="editing.resolution" class="form-textarea" maxlength="2000" :required="editing.status === 'RESOLVED'" placeholder="Nội dung phản hồi hoặc hướng xử lý"></textarea></div><p v-if="error" class="form-error">{{ error }}</p><button class="btn btn-primary submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cập nhật' }}</button></form></div>
  </div>
</template>

<style scoped>
.support-page { padding:8px 0; }.page-header { display:flex; justify-content:space-between; align-items:center; gap:16px; margin-bottom:24px; }.page-header h1 { margin:0 0 6px; }.page-header p,.category { margin:0; color:var(--text-mid); font-size:13px; }.filter { font-size:14px; color:var(--text-mid); }.category { display:block; margin-top:4px; }.empty-state { padding:48px; text-align:center; color:var(--text-mid); }.modal-header { display:flex; justify-content:space-between; align-items:center; gap:12px; margin-bottom:16px; }.modal-header h2 { margin:0; font-size:18px; }.description { padding:12px; background:var(--surface); border-radius:var(--radius-sm); white-space:pre-wrap; color:var(--text-mid); font-size:14px; }.submit { width:100%; }@media(max-width:700px){.page-header{align-items:flex-start;flex-direction:column}.table th:nth-child(4),.table td:nth-child(4),.table th:nth-child(6),.table td:nth-child(6){display:none}}
</style>
