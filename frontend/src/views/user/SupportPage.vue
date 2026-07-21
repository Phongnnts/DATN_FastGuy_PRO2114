<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { orderApi, supportApi } from '@/api';
import { formatDate } from '@/utils/format';

const tickets = ref([]);
const orders = ref([]);
const loading = ref(true);
const ordersLoading = ref(false);
const listError = ref('');
const orderError = ref('');
const formError = ref('');
const showForm = ref(false);
const saving = ref(false);
const subjectInput = ref(null);
const modal = ref(null);
const success = ref('');
let modalTrigger = null;
const form = ref({ subject: '', category: 'OTHER', description: '', orderId: '' });
const categories = { MISSING_ITEM: 'Thiếu món', COLD_FOOD: 'Đồ ăn nguội', WRONG_ITEM: 'Sai món', LATE_DELIVERY: 'Giao trễ', OTHER: 'Khác' };

function statusClass(status) { return status === 'RESOLVED' ? 'badge-success' : status === 'PROCESSING' ? 'badge-info' : 'badge-warning'; }
function statusLabel(status) { return { OPEN: 'Mới', PROCESSING: 'Đang xử lý', RESOLVED: 'Đã giải quyết' }[status] || status; }
async function loadTickets() { loading.value = true; listError.value = ''; try { tickets.value = await supportApi.getMine() || []; } catch (e) { listError.value = e.message; } finally { loading.value = false; } }
async function loadOrders() { ordersLoading.value = true; orderError.value = ''; try { orders.value = await orderApi.getAll() || []; } catch (e) { orderError.value = 'Không thể tải đơn hàng. Bạn vẫn có thể gửi yêu cầu không liên kết đơn.'; } finally { ordersLoading.value = false; } }
async function openForm(event) { modalTrigger = event?.currentTarget || document.activeElement; showForm.value = true; formError.value = ''; success.value = ''; await nextTick(); subjectInput.value?.focus(); }
function closeForm() { if (!saving.value) { showForm.value = false; nextTick(() => modalTrigger?.focus()); } }
function handleKeydown(event) { if (!showForm.value) return; if (event.key === 'Escape') return closeForm(); if (event.key !== 'Tab') return; const focusable = [...modal.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled)')]; const first = focusable[0]; const last = focusable.at(-1); if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); } else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); } }
async function submit() { if (!form.value.subject.trim() || !form.value.description.trim()) return; saving.value = true; formError.value = ''; try { const ticket = await supportApi.create({ ...form.value, orderId: form.value.orderId ? Number(form.value.orderId) : null }); tickets.value.unshift(ticket); form.value = { subject: '', category: 'OTHER', description: '', orderId: '' }; showForm.value = false; success.value = 'Yêu cầu hỗ trợ đã được gửi thành công.'; nextTick(() => modalTrigger?.focus()); } catch (e) { formError.value = e.message; } finally { saving.value = false; } }
watch(showForm, open => { document.body.style.overflow = open ? 'hidden' : ''; });
onMounted(() => { loadTickets(); loadOrders(); document.addEventListener('keydown', handleKeydown); });
onBeforeUnmount(() => { document.removeEventListener('keydown', handleKeydown); document.body.style.overflow = ''; });
</script>

<template>
  <main class="support-page">
    <div class="page-header"><div><h1>Hỗ trợ khách hàng</h1><p>Theo dõi và gửi yêu cầu hỗ trợ cho FastGuy.</p></div><button class="btn btn-primary" @click="openForm"><i class="bi bi-plus-lg"></i>Tạo yêu cầu</button></div>
    <p v-if="success" class="success-message" role="status" aria-live="polite">{{ success }}</p>
    <div v-if="loading" class="card state" role="status"><span class="spinner"></span><p>Đang tải yêu cầu...</p></div>
    <div v-else-if="listError" class="card state" role="alert"><i class="bi bi-exclamation-circle"></i><h3>Không thể tải yêu cầu</h3><p>{{ listError }}</p><button class="btn btn-outline" @click="loadTickets">Thử lại</button></div>
    <section v-else-if="tickets.length" class="ticket-list" aria-label="Danh sách yêu cầu hỗ trợ">
      <article v-for="ticket in tickets" :key="ticket.ticketId" class="card ticket">
        <div class="ticket-top"><div><strong>#{{ ticket.ticketId }} · {{ ticket.subject }}</strong><div class="meta"><span>{{ categories[ticket.category] }}</span><RouterLink v-if="ticket.orderId" :to="{ name: 'UserOrderDetail', params: { id: ticket.orderId } }">{{ ticket.orderCode || `Đơn #${ticket.orderId}` }}</RouterLink><span>{{ formatDate(ticket.createdAt) }}</span></div></div><span class="badge" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span></div>
        <p class="description">{{ ticket.description }}</p><div v-if="ticket.resolution" class="resolution"><strong>Phản hồi từ FastGuy</strong><span>{{ ticket.resolution }}</span></div>
      </article>
    </section>
    <div v-else class="card state"><i class="bi bi-headset"></i><h3>Chưa có yêu cầu hỗ trợ</h3><p>Khi cần trợ giúp, hãy tạo yêu cầu mới.</p><button class="btn btn-outline" @click="openForm">Tạo yêu cầu đầu tiên</button></div>
    <div v-if="showForm" class="modal-overlay" @click.self="closeForm" @keydown.esc="closeForm">
      <form ref="modal" class="modal-content" role="dialog" aria-modal="true" aria-labelledby="support-title" @submit.prevent="submit">
        <div class="modal-header"><div><h2 id="support-title">Tạo yêu cầu hỗ trợ</h2><p>Thông tin rõ ràng giúp chúng tôi xử lý nhanh hơn.</p></div><button type="button" class="btn btn-ghost" aria-label="Đóng" @click="closeForm"><i class="bi bi-x-lg"></i></button></div>
        <div class="form-group"><div class="label-row"><label for="subject" class="form-label">Tiêu đề</label><span>{{ form.subject.length }}/255</span></div><input id="subject" ref="subjectInput" v-model.trim="form.subject" class="form-input" maxlength="255" required aria-describedby="subject-help" placeholder="Ví dụ: Đơn hàng bị thiếu món" /><small id="subject-help">Tóm tắt ngắn vấn đề cần hỗ trợ.</small></div>
        <div class="form-group"><label for="category" class="form-label">Loại vấn đề</label><select id="category" v-model="form.category" class="form-select"><option v-for="(label, key) in categories" :key="key" :value="key">{{ label }}</option></select></div>
        <div class="form-group"><label for="order" class="form-label">Đơn hàng liên quan <span>(không bắt buộc)</span></label><select id="order" v-model="form.orderId" class="form-select" :disabled="ordersLoading"><option value="">{{ ordersLoading ? 'Đang tải đơn hàng...' : 'Không liên kết đơn hàng' }}</option><option v-for="order in orders" :key="order.orderId" :value="order.orderId">{{ order.orderCode }}</option></select><small v-if="orderError" class="field-warning">{{ orderError }}</small></div>
        <div class="form-group"><div class="label-row"><label for="description" class="form-label">Chi tiết</label><span>{{ form.description.length }}/2000</span></div><textarea id="description" v-model.trim="form.description" class="form-textarea" maxlength="2000" required rows="6" placeholder="Mô tả tình trạng, thời điểm và mong muốn hỗ trợ"></textarea></div>
        <p v-if="formError" class="form-error" role="alert">{{ formError }}</p><div class="actions"><button type="button" class="btn btn-outline" :disabled="saving" @click="closeForm">Hủy</button><button class="btn btn-primary" :disabled="saving || !form.subject.trim() || !form.description.trim()">{{ saving ? 'Đang gửi...' : 'Gửi yêu cầu' }}</button></div>
      </form>
    </div>
  </main>
</template>

<style scoped>
.support-page{padding:32px 0}.success-message{padding:12px 16px;border:1px solid #a7f3d0;border-radius:var(--radius-sm);background:#ecfdf5;color:#047857;font-weight:600}.page-header,.ticket-top,.modal-header,.actions,.label-row{display:flex;justify-content:space-between;gap:16px}.page-header{align-items:center;margin-bottom:24px}.page-header h1,.modal-header h2{margin:0 0 6px}.page-header p,.modal-header p,.meta,.form-group small,.label-row span{margin:0;color:var(--text-mid);font-size:13px}.ticket-list{display:grid;gap:14px}.ticket{padding:20px}.ticket-top{align-items:flex-start}.meta{display:flex;flex-wrap:wrap;gap:6px 14px;margin-top:6px}.meta a{color:var(--primary);font-weight:600}.description{margin:16px 0 0;white-space:pre-wrap;color:var(--text-mid)}.resolution{display:grid;gap:5px;margin-top:16px;padding:14px;border-radius:var(--radius-sm);background:var(--primary-50);font-size:14px}.state{text-align:center;padding:52px 20px}.state i{font-size:32px;color:var(--primary)}.state h3{margin:12px 0 6px}.state p{margin:0 0 16px;color:var(--text-mid)}.modal-header{align-items:flex-start;margin-bottom:20px}.modal-header h2{font-size:21px}.label-row{align-items:center}.form-label span{font-weight:400;color:var(--text-mid)}.field-warning{display:block;margin-top:6px;color:var(--warning,#9a6700)!important}.actions{justify-content:flex-end;margin-top:20px}.spinner{display:inline-block;width:28px;height:28px;border:3px solid var(--border);border-top-color:var(--primary);border-radius:50%;animation:spin .7s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}@media(max-width:600px){.page-header{align-items:flex-start;flex-direction:column}.ticket-top{flex-direction:column}.actions .btn{flex:1}}
</style>
