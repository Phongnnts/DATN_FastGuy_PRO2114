<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { staffApi } from '@/api';
import { formatPrice } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { useToast } from '@/stores/toast';

const toast = useToast();
const route = useRoute();
const staffStore = useStaffStore();
const order = ref(null);
const loading = ref(true);
const loadError = ref('');
const saving = ref(false);
const showCancelModal = ref(false);
const cancelReason = ref('');
const selectedShipperId = ref(null);
const shippers = ref([]);

const pendingPayment = computed(() => order.value?.paymentMethod === 'BANK_TRANSFER' && order.value?.paymentStatus !== 'PAID');
const canCancel = computed(() => ['WAITING_STOCK_CONFIRM', 'PENDING', 'CONFIRMED', 'PREPARING'].includes(order.value?.status));
const assignedShipper = computed(() => order.value?.shipperName || '');

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    order.value = await staffStore.fetchOrderById(route.params.id);
    if (!order.value) loadError.value = 'Không tìm thấy đơn hàng';
    if (order.value?.status === 'READY' && !order.value.shipperId) await loadShippers();
  } catch (e) { loadError.value = e.message || 'Không thể tải đơn hàng'; }
  finally { loading.value = false; }
}
async function loadShippers() { try { const data = await staffApi.getAvailableShippers(); shippers.value = Array.isArray(data) ? data : []; } catch { shippers.value = []; } }
async function updateStatus(status, reason = null) {
  if (!order.value || saving.value) return;
  saving.value = true;
  try { await staffStore.updateOrderStatus(order.value.id, status, reason); await load(); }
  catch (e) { toast.error(e.message || 'Không thể cập nhật trạng thái'); }
  finally { saving.value = false; }
}
async function assignShipper() {
  if (!order.value || !selectedShipperId.value || saving.value) return;
  saving.value = true;
  try { await staffApi.assignShipper(order.value.id, selectedShipperId.value); await load(); }
  catch (e) { toast.error(e.message || 'Không thể gán shipper'); }
  finally { saving.value = false; }
}
function openCancelModal() { cancelReason.value = ''; showCancelModal.value = true; }
async function cancelOrder() { if (!cancelReason.value.trim()) return; await updateStatus('CANCELLED', cancelReason.value.trim()); showCancelModal.value = false; }
function printInvoice() { window.print(); }
onMounted(load);
</script>

<template>
  <div v-if="loading" class="staff-state"><span class="spinner"></span> Đang tải đơn hàng...</div>
  <div v-else-if="loadError" class="staff-state staff-error"><span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
  <div v-else-if="order">
    <div class="page-header"><div class="title-wrap"><h1>Đơn hàng {{ order.orderCode }}</h1><OrderStatusBadge :status="order.status" /></div><button class="btn btn-sm btn-outline no-print" @click="printInvoice"><i class="bi bi-printer"></i> In hóa đơn</button></div>
    <div class="grid-2"><section class="card"><h3>Thông tin đơn hàng</h3><div class="info-row"><span>Khách hàng</span><strong>{{ order.customerName }}</strong></div><div class="info-row"><span>Địa chỉ</span><strong>{{ order.shippingAddress }}</strong></div><div class="info-row"><span>Phương thức</span><strong>{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }}</strong></div><div class="info-row"><span>Thanh toán</span><strong :class="{ paid: order.paymentStatus === 'PAID' }">{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</strong></div><div class="info-row"><span>Ghi chú</span><strong>{{ order.note || 'Không có' }}</strong></div><template v-if="order.status === 'CANCELLED'"><div class="info-row"><span>Người hủy</span><strong>{{ order.cancelledBy === 'STAFF' ? 'Nhân viên' : 'Khách hàng' }}</strong></div><div v-if="order.failureReason" class="info-row"><span>Lý do</span><strong class="danger">{{ order.failureReason }}</strong></div><div v-if="order.refundStatus" class="info-row"><span>Hoàn tiền</span><strong>{{ order.refundStatus }}{{ order.refundNote ? ` · ${order.refundNote}` : '' }}</strong></div></template></section>
      <section class="card"><h3>Thao tác</h3><div class="actions"><button v-if="order.status === 'WAITING_STOCK_CONFIRM'" class="btn btn-primary" :disabled="saving" @click="updateStatus('PENDING')"><i class="bi bi-check-lg"></i> Xác nhận tồn kho</button><button v-if="order.status === 'PENDING' && !pendingPayment" class="btn btn-primary" :disabled="saving" @click="updateStatus('CONFIRMED')"><i class="bi bi-check-lg"></i> Xác nhận</button><p v-if="pendingPayment" class="payment-wait"><i class="bi bi-hourglass-split"></i> Chờ khách thanh toán PayOS</p><button v-if="order.status === 'CONFIRMED'" class="btn btn-primary" :disabled="saving" @click="updateStatus('PREPARING')"><i class="bi bi-fire"></i> Bắt đầu chế biến</button><button v-if="order.status === 'PREPARING'" class="btn btn-success" :disabled="saving" @click="updateStatus('READY')"><i class="bi bi-check2-all"></i> Hoàn thành</button><button v-if="canCancel" class="btn btn-outline danger-button" :disabled="saving" @click="openCancelModal"><i class="bi bi-x-lg"></i> Hủy đơn</button><span v-if="order.status === 'READY'" class="badge badge-success">Sẵn sàng giao</span></div>
        <div v-if="order.status === 'READY'" class="shipper-panel"><h4>Giao shipper</h4><p v-if="assignedShipper" class="assigned"><i class="bi bi-person-check"></i> {{ assignedShipper }}<span v-if="order.assignedAt"> · Đã gán</span></p><div v-else class="shipper-form"><select v-model="selectedShipperId" class="form-select" aria-label="Chọn shipper" :disabled="saving"><option :value="null">Chọn shipper</option><option v-for="shipper in shippers" :key="shipper.id" :value="shipper.id">{{ shipper.fullName }} · {{ shipper.phone }}</option></select><button class="btn btn-primary btn-sm" :disabled="saving || !selectedShipperId" @click="assignShipper">{{ saving ? 'Đang gán...' : 'Gán' }}</button></div></div>
      </section></div>
    <section class="card mt-3"><h3>Sản phẩm</h3><div class="table-wrapper"><table class="table"><thead><tr><th></th><th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr></thead><tbody><tr v-for="item in order.items" :key="`${item.productId}-${item.variantId}`"><td><img :src="item.image" :alt="item.productName" class="item-image"></td><td>{{ item.productName }}</td><td>{{ item.variantName || 'Mặc định' }}</td><td>{{ formatPrice(item.price) }}</td><td>{{ item.quantity }}</td><td><strong>{{ formatPrice(item.price * item.quantity) }}</strong></td></tr></tbody></table></div><div class="order-totals"><div>Tạm tính: {{ formatPrice(order.subtotal) }}</div><div>Phí ship: {{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Miễn phí' }}</div><strong>Tổng: {{ formatPrice(order.total) }}</strong></div></section>
    <section class="card mt-3"><h3>Lịch sử trạng thái</h3><OrderTimeline :history="order.statusHistory" /></section>
    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false"><div class="modal" role="dialog" aria-modal="true" aria-labelledby="cancel-title"><div class="modal-header"><h2 id="cancel-title" class="modal-title">Hủy đơn hàng</h2><button class="modal-close" aria-label="Đóng" :disabled="saving" @click="showCancelModal = false"><i class="bi bi-x-lg"></i></button></div><div class="modal-body"><label class="form-label" for="cancel-reason">Lý do hủy *</label><textarea id="cancel-reason" v-model="cancelReason" class="form-textarea" placeholder="Nhập lý do hủy đơn" rows="3" maxlength="500"></textarea><div class="modal-footer"><button class="btn btn-ghost" :disabled="saving" @click="showCancelModal = false">Quay lại</button><button class="btn btn-danger" :disabled="saving || !cancelReason.trim()" @click="cancelOrder">{{ saving ? 'Đang hủy...' : 'Xác nhận hủy' }}</button></div></div></div></div>
  </div>
</template>

<style scoped>
.staff-state { display:flex; align-items:center; justify-content:center; gap:10px; min-height:240px; color:var(--text-mid); }.staff-error { flex-direction:column; color:var(--red-active); }.title-wrap { display:flex; align-items:center; gap:12px; flex-wrap:wrap; }.card h3 { margin-bottom:16px; font-size:16px; }.info-row { display:flex; justify-content:space-between; gap:16px; padding:10px 0; border-bottom:1px solid var(--border-light); font-size:14px; }.info-row span { color:var(--text-mid); }.info-row strong { max-width:62%; text-align:right; }.paid { color:#15803d; }.danger { color:var(--red-active); }.actions { display:flex; flex-wrap:wrap; gap:8px; }.danger-button { color:var(--red-active); border-color:var(--red-active); }.payment-wait { width:100%; margin:0; padding:10px 12px; border-radius:var(--radius-sm); background:#fff7ed; color:#c2410c; font-size:13px; }.shipper-panel { margin-top:18px; padding-top:16px; border-top:1px solid var(--border-light); }.shipper-panel h4 { margin-bottom:8px; font-size:14px; }.assigned { margin:0; color:#15803d; font-size:14px; font-weight:600; }.shipper-form { display:flex; gap:8px; }.item-image { width:48px; height:48px; border-radius:8px; object-fit:cover; }.order-totals { margin-top:16px; color:var(--text-mid); line-height:1.9; text-align:right; }.order-totals strong { display:block; color:var(--text-dark); font-size:19px; }@media print { .topbar,.sidebar,.no-print,.actions,.shipper-panel { display:none!important; }.main-content { margin-left:0!important; }.card { box-shadow:none; } }@media (max-width:640px) { .shipper-form { flex-direction:column; }.info-row strong { max-width:58%; } }
</style>
