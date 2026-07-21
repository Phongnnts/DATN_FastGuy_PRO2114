<script setup>
import { computed, onMounted, ref } from 'vue';
import { supportApi } from '@/api';
import { formatDate } from '@/utils/format';

const tickets = ref([]);
const all = ref(false);
const loading = ref(true);
const error = ref('');
const editing = ref(null);
const saving = ref(false);
const labels = { OPEN: 'Mới', PROCESSING: 'Đang xử lý', RESOLVED: 'Đã giải quyết', MISSING_ITEM: 'Thiếu món', COLD_FOOD: 'Đồ ăn nguội', WRONG_ITEM: 'Sai món', LATE_DELIVERY: 'Giao trễ', OTHER: 'Khác' };
const statusOptions = computed(() => editing.value?.originalStatus === 'OPEN' ? ['OPEN', 'PROCESSING'] : editing.value?.originalStatus === 'PROCESSING' ? ['PROCESSING', 'RESOLVED'] : ['RESOLVED']);
function statusClass(status) { return status === 'RESOLVED' ? 'badge-success' : status === 'PROCESSING' ? 'badge-info' : 'badge-warning'; }
async function load() { loading.value = true; error.value = ''; try { tickets.value = await supportApi.getStaff(all.value) || []; } catch (e) { error.value = e.message; } finally { loading.value = false; } }
function openEdit(ticket) { editing.value = { ...ticket, originalStatus: ticket.status, resolution: ticket.status === 'RESOLVED' ? ticket.resolution || '' : '' }; error.value = ''; }
function closeEdit() { if (!saving.value) editing.value = null; }
async function save() { saving.value = true; error.value = ''; const ticketId = editing.value.ticketId; try { const payload = { status: editing.value.status, resolution: editing.value.status === 'RESOLVED' ? editing.value.resolution : null }; const updated = await supportApi.update(ticketId, payload); const index = tickets.value.findIndex((ticket) => ticket.ticketId === updated.ticketId); if (index >= 0) tickets.value[index] = updated; editing.value = null; if (!all.value && updated.status === 'RESOLVED') tickets.value = tickets.value.filter((ticket) => ticket.ticketId !== updated.ticketId); } catch (e) { error.value = e.message; } finally { saving.value = false; } }
onMounted(load);
</script>

<template>
  <main class="support-page"><div class="page-header"><div><h1>Yêu cầu hỗ trợ</h1><p>Xử lý phản hồi của khách hàng.</p></div><label class="filter"><input v-model="all" type="checkbox" @change="load" /> Hiện cả đã giải quyết</label></div>
    <div v-if="loading" class="card state" role="status">Đang tải yêu cầu...</div><div v-else-if="error && !editing" class="card state" role="alert"><p>{{ error }}</p><button class="btn btn-outline" @click="load">Thử lại</button></div>
    <div v-else class="card card-flat"><div v-if="tickets.length" class="table-wrapper"><table class="table"><thead><tr><th>Mã</th><th>Khách hàng</th><th>Vấn đề</th><th>Đơn hàng</th><th>Trạng thái</th><th>Thời gian</th><th></th></tr></thead><tbody><tr v-for="ticket in tickets" :key="ticket.ticketId"><td>#{{ ticket.ticketId }}</td><td>{{ ticket.customerName || `User #${ticket.userId}` }}</td><td><strong>{{ ticket.subject }}</strong><span class="category">{{ labels[ticket.category] }}</span></td><td><RouterLink v-if="ticket.orderId" :to="{ name: 'StaffOrderDetail', params: { id: ticket.orderId } }">{{ ticket.orderCode }}</RouterLink><span v-else>—</span></td><td><span class="badge" :class="statusClass(ticket.status)">{{ labels[ticket.status] }}</span></td><td>{{ formatDate(ticket.createdAt) }}</td><td><button class="btn btn-outline btn-sm" @click="openEdit(ticket)">{{ ticket.status === 'RESOLVED' ? 'Xem' : 'Xử lý' }}</button></td></tr></tbody></table></div><div v-else class="state">Không có yêu cầu hỗ trợ.</div></div>
    <div v-if="editing" class="modal-overlay" @click.self="closeEdit"><form class="modal-content" role="dialog" aria-modal="true" aria-labelledby="edit-title" @submit.prevent="save"><div class="modal-header"><h2 id="edit-title">#{{ editing.ticketId }} · {{ editing.subject }}</h2><button type="button" class="btn btn-ghost" :disabled="saving" aria-label="Đóng" @click="closeEdit"><i class="bi bi-x-lg"></i></button></div><p class="description">{{ editing.description }}</p><div class="form-group"><label for="status" class="form-label">Trạng thái</label><select id="status" v-model="editing.status" class="form-select" :disabled="editing.status === 'RESOLVED'"><option v-for="status in statusOptions" :key="status" :value="status">{{ labels[status] }}</option></select></div><div class="form-group"><div class="label-row"><label for="resolution" class="form-label">Phản hồi khách hàng</label><span>{{ editing.resolution.length }}/2000</span></div><textarea id="resolution" v-model.trim="editing.resolution" class="form-textarea" maxlength="2000" :required="editing.status === 'RESOLVED'" :disabled="editing.status !== 'RESOLVED'" placeholder="Nội dung giải quyết"></textarea></div><p v-if="error" class="form-error" role="alert">{{ error }}</p><button v-if="editing.resolvedAt == null" class="btn btn-primary submit" :disabled="saving || (editing.status === 'RESOLVED' && !editing.resolution.trim())">{{ saving ? 'Đang lưu...' : 'Lưu cập nhật' }}</button></form></div>
  </main>
</template>

<style scoped>
.support-page{padding:8px 0}.page-header,.modal-header,.label-row{display:flex;justify-content:space-between;align-items:center;gap:16px}.page-header{margin-bottom:24px}.page-header h1{margin:0 0 6px}.page-header p,.category,.label-row span{margin:0;color:var(--text-mid);font-size:13px}.filter{font-size:14px;color:var(--text-mid)}.category{display:block;margin-top:4px}.state{padding:48px;text-align:center;color:var(--text-mid)}.modal-header{margin-bottom:16px}.modal-header h2{margin:0;font-size:18px}.description{padding:12px;background:var(--surface);border-radius:var(--radius-sm);white-space:pre-wrap;color:var(--text-mid);font-size:14px}.submit{width:100%}@media(max-width:700px){.page-header{align-items:flex-start;flex-direction:column}.table th:nth-child(4),.table td:nth-child(4),.table th:nth-child(6),.table td:nth-child(6){display:none}}
</style>
