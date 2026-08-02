<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import { useToast } from '@/stores/toast';

const toast = useToast();
const route = useRoute();
const router = useRouter();
const order = ref(null);
const loading = ref(true);
const loadError = ref('');
const saving = ref(false);
const showCancelModal = ref(false);
const cancelReason = ref('');
const showNoteModal = ref(false);
const noteText = ref('');

const canCancel = computed(() => order.value && !['CANCELLED', 'DELIVERED'].includes(order.value.status));
const canPrint = computed(() => true);

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    order.value = await adminApi.getOrderById(route.params.id);
    if (!order.value) loadError.value = 'Không tìm thấy đơn hàng';
  } catch (e) {
    loadError.value = e.message || 'Không thể tải đơn hàng';
  } finally {
    loading.value = false;
  }
}

async function cancelOrder() {
  if (!cancelReason.value.trim()) return;
  saving.value = true;
  try {
    await adminApi.cancelOrder(order.value.orderId, { reason: cancelReason.value.trim() });
    toast.success('Đã hủy đơn hàng');
    showCancelModal.value = false;
    await load();
  } catch (e) { toast.error(e.message); }
  finally { saving.value = false; }
}

async function saveNote() {
  if (!noteText.value.trim()) return;
  saving.value = true;
  try {
    await adminApi.addOrderNote(order.value.orderId, noteText.value.trim());
    toast.success('Đã lưu ghi chú');
    showNoteModal.value = false;
    noteText.value = '';
    await load();
  } catch (e) { toast.error(e.message); }
  finally { saving.value = false; }
}

function printInvoice() { window.print(); }

onMounted(load);
</script>

<template>
  <div v-if="loading" style="display:flex;justify-content:center;padding:48px;color:var(--text-mid)">
    <span>Đang tải...</span>
  </div>
  <div v-else-if="loadError" style="text-align:center;padding:48px;color:var(--red-active)">
    <p>{{ loadError }}</p>
    <button class="btn btn-sm btn-outline" @click="router.back()">Quay lại</button>
  </div>
  <div v-else-if="order">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px;flex-wrap:wrap">
        <button class="btn btn-sm btn-outline no-print" @click="router.push('/admin/orders')"><i class="bi bi-arrow-left"></i></button>
        <h1>Đơn {{ order.orderCode }}</h1>
        <OrderStatusBadge :status="order.status" />
      </div>
      <div style="display:flex;gap:8px">
        <button v-if="canCancel" class="btn btn-sm btn-danger" @click="showCancelModal = true"><i class="bi bi-x-lg"></i> Hủy đơn</button>
        <button class="btn btn-sm btn-outline" @click="showNoteModal = true"><i class="bi bi-chat-left-text"></i> Ghi chú</button>
        <button class="btn btn-sm btn-outline no-print" @click="printInvoice"><i class="bi bi-printer"></i> In</button>
      </div>
    </div>

    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px">
      <section class="card">
        <h3>Thông tin đơn hàng</h3>
        <div class="info-row"><span>Khách hàng</span><strong>{{ order.customerName || 'Khách vãng lai' }}</strong></div>
        <div class="info-row"><span>SĐT</span><strong>{{ order.customerPhone }}</strong></div>
        <div class="info-row"><span>Địa chỉ</span><strong>{{ order.customerAddress }}</strong></div>
        <div class="info-row"><span>Ghi chú</span><strong>{{ order.deliveryNote || 'Không có' }}</strong></div>
        <div v-if="order.staffName" class="info-row"><span>Nhân viên xử lý</span><strong>{{ order.staffName }}</strong></div>
        <div v-if="order.shipperName" class="info-row"><span>Shipper</span><strong>{{ order.shipperName }}</strong></div>
        <template v-if="order.status === 'CANCELLED'">
          <hr style="margin:12px 0;border:none;border-top:1px solid var(--border)">
          <div class="info-row"><span>Hủy bởi</span><strong>{{ order.cancelledBy === 'STAFF' ? 'Nhân viên' : order.cancelledBy === 'ADMIN' ? 'Quản trị' : order.cancelledBy === 'SHIPPER' ? 'Shipper' : 'Khách' }}</strong></div>
          <div v-if="order.failureReason" class="info-row"><span>Lý do</span><strong style="color:var(--red-active)">{{ order.failureReason }}</strong></div>
          <div v-if="order.refundStatus" class="info-row"><span>Hoàn tiền</span><strong>{{ order.refundStatus }}{{ order.refundNote ? ` · ${order.refundNote}` : '' }}</strong></div>
        </template>
      </section>
      <section class="card">
        <h3>Thanh toán</h3>
        <div class="info-row"><span>Phương thức</span><strong>{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS (CK)' : 'COD' }}</strong></div>
        <div class="info-row"><span>Trạng thái</span><strong :style="{ color: order.paymentStatus === 'PAID' ? '#15803d' : '#c2410c' }">{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</strong></div>
        <div class="info-row"><span>Tạm tính</span><strong>{{ formatPrice(order.totalAmount) }}</strong></div>
        <div class="info-row"><span>Phí ship</span><strong>{{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Miễn phí' }}</strong></div>
        <div v-if="order.discountAmount > 0" class="info-row"><span>Giảm giá</span><strong style="color:#15803d">-{{ formatPrice(order.discountAmount) }}</strong></div>
        <div class="info-row"><span>Tổng cộng</span><strong style="font-size:18px">{{ formatPrice(order.finalAmount) }}</strong></div>
      </section>
    </div>

    <section class="card" style="margin-top:16px">
      <h3>Sản phẩm</h3>
      <div class="table-wrapper">
        <table class="table">
          <thead><tr><th>Sản phẩm</th><th>Phân loại</th><th>Đơn giá</th><th>SL</th><th>Thành tiền</th></tr></thead>
          <tbody>
            <tr v-for="(item, idx) in order.items" :key="idx">
              <td>{{ item.productName }}</td>
              <td>{{ item.variantName || 'Mặc định' }}</td>
              <td>{{ formatPrice(item.unitPrice) }}</td>
              <td>{{ item.quantity }}</td>
              <td><strong>{{ formatPrice(item.totalPrice) }}</strong></td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <section v-if="order.internalNote" class="card" style="margin-top:16px">
      <h3>Ghi chú nội bộ</h3>
      <div style="white-space:pre-wrap;font-size:14px;color:var(--text-mid)">{{ order.internalNote }}</div>
    </section>

    <section v-if="order.statusHistory && order.statusHistory.length" class="card" style="margin-top:16px">
      <h3>Lịch sử trạng thái</h3>
      <OrderTimeline :history="order.statusHistory" />
    </section>

    <div style="margin-top:12px;color:var(--text-mid);font-size:13px">
      Tạo: {{ formatDate(order.createdAt) }}
      <span v-if="order.confirmedAt"> · Xác nhận: {{ formatDate(order.confirmedAt) }}</span>
      <span v-if="order.deliveredAt"> · Giao: {{ formatDate(order.deliveredAt) }}</span>
    </div>

    <!-- Cancel Modal -->
    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
      <div class="modal" style="max-width:480px">
        <div class="modal-header"><h3>Hủy đơn {{ order.orderCode }}</h3><button class="btn btn-sm btn-outline" @click="showCancelModal = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div class="form-group"><label class="form-label">Lý do hủy *</label><textarea v-model="cancelReason" class="form-textarea" rows="3" placeholder="Nhập lý do hủy đơn" maxlength="500"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showCancelModal = false">Quay lại</button>
          <button class="btn btn-danger" :disabled="saving || !cancelReason.trim()" @click="cancelOrder">{{ saving ? 'Đang hủy...' : 'Xác nhận hủy' }}</button>
        </div>
      </div>
    </div>

    <!-- Note Modal -->
    <div v-if="showNoteModal" class="modal-overlay" @click.self="showNoteModal = false">
      <div class="modal" style="max-width:480px">
        <div class="modal-header"><h3>Thêm ghi chú</h3><button class="btn btn-sm btn-outline" @click="showNoteModal = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div class="form-group"><label class="form-label">Nội dung ghi chú</label><textarea v-model="noteText" class="form-textarea" rows="3" placeholder="Nhập ghi chú cho đơn hàng" maxlength="500"></textarea></div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showNoteModal = false">Hủy</button>
          <button class="btn btn-primary" :disabled="saving || !noteText.trim()" @click="saveNote">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card h3 { margin-bottom:12px; font-size:15px; }
.info-row { display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid var(--border-light); font-size:14px; }
.info-row span { color:var(--text-mid); }
.info-row strong { max-width:60%; text-align:right; }
@media (max-width:768px) { div[style*="grid-template-columns:1fr 1fr"] { grid-template-columns:1fr !important; } }
</style>
