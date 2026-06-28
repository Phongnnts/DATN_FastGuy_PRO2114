<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import OrderTimeline from '@/components/common/OrderTimeline.vue';

const route = useRoute();
const staffStore = useStaffStore();

const order = ref(null);
const showCancelModal = ref(false);
const cancelReason = ref('');

onMounted(async () => {
  order.value = await staffStore.fetchOrderById(route.params.id);
});

async function updateStatus(status) {
  if (!order.value) return;
  try {
    await staffStore.updateOrderStatus(order.value.id, status);
    order.value = await staffStore.fetchOrderById(route.params.id);
  } catch (e) {
    alert(e.message);
  }
}

async function confirmOrder() {
  await updateStatus('CONFIRMED');
}

async function startPreparing() {
  await updateStatus('PREPARING');
}

async function completeOrder() {
  await updateStatus('READY');
}

function openCancelModal() {
  cancelReason.value = '';
  showCancelModal.value = true;
}

async function cancelOrder() {
  if (!order.value) return;
  try {
    await staffStore.updateOrderStatus(order.value.id, 'CANCELLED', cancelReason.value);
    order.value = await staffStore.fetchOrderById(route.params.id);
    showCancelModal.value = false;
  } catch (e) {
    alert(e.message);
  }
}

function printInvoice() {
  window.print();
}
</script>

<template>
  <div v-if="order">
    <div class="page-header">
      <div style="display: flex; align-items: center; gap: 12px">
        <h1>Đơn hàng {{ order.orderCode }}</h1>
        <OrderStatusBadge :status="order.status" />
      </div>
      <div class="no-print" style="display: flex; gap: 8px">
        <button class="btn btn-sm btn-outline" @click="printInvoice">
          <i class="bi bi-printer"></i> In hóa đơn
        </button>
      </div>
    </div>
    <div class="grid-2">
      <div class="card">
        <h3 style="font-weight: 700; margin-bottom: 16px">
          Thông tin đơn hàng
        </h3>
        <div class="info-row">
          <span>Khách hàng</span><span>{{ order.customerName }}</span>
        </div>
        <div class="info-row">
          <span>Địa chỉ</span><span>{{ order.shippingAddress }}</span>
        </div>
        <div class="info-row">
          <span>Phương thức</span><span>{{ order.paymentMethod }}</span>
        </div>
        <div class="info-row">
          <span>Thanh toán</span
          ><span>{{
            order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán'
          }}</span>
        </div>
        <div class="info-row">
          <span>Ghi chú</span><span>{{ order.note || 'Không có' }}</span>
        </div>
      </div>
      <div class="card">
        <h3 style="font-weight: 700; margin-bottom: 16px">Thao tác</h3>
        <div style="display: flex; gap: 8px; flex-wrap: wrap">
          <button
            v-if="order.status === 'PENDING'"
            class="btn btn-primary"
            @click="confirmOrder"
          >
            <i class="bi bi-check-lg"></i> Xác nhận
          </button>
          <button
            v-if="order.status === 'CONFIRMED'"
            class="btn btn-primary"
            @click="startPreparing"
          >
            <i class="bi bi-fire"></i> Bắt đầu chế biến
          </button>
          <button
            v-if="order.status === 'PREPARING'"
            class="btn btn-success"
            @click="completeOrder"
          >
            <i class="bi bi-check2-all"></i> Hoàn thành
          </button>
          <button
            v-if="order.status !== 'READY'"
            class="btn btn-outline"
            style="border-color: var(--red-active); color: var(--red-active)"
            @click="openCancelModal"
          >
            <i class="bi bi-x-lg"></i> Hủy đơn
          </button>
          <span v-if="order.status === 'READY'" class="badge badge-success"
            >Đã sẵn sàng</span
          >
          <span v-if="order.status === 'CANCELLED'" class="badge badge-error"
            >Đã hủy</span
          >
        </div>
      </div>
    </div>
    <div class="card mt-3">
      <h3 style="font-weight: 700; margin-bottom: 16px">Sản phẩm</h3>
      <table class="table">
        <thead>
          <tr>
            <th></th>
            <th>Sản phẩm</th>
            <th>Phân loại</th>
            <th>Đơn giá</th>
            <th>Số lượng</th>
            <th>Thành tiền</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in order.items" :key="item.productId">
            <td>
              <img
                :src="item.image"
                style="
                  width: 48px;
                  height: 48px;
                  border-radius: 8px;
                  object-fit: cover;
                "
              />
            </td>
            <td>{{ item.productName }}</td>
            <td>{{ item.variantName || 'Mặc định' }}</td>
            <td>{{ formatPrice(item.price) }}</td>
            <td>{{ item.quantity }}</td>
            <td>
              <strong>{{ formatPrice(item.price * item.quantity) }}</strong>
            </td>
          </tr>
        </tbody>
      </table>
      <div class="order-totals" style="margin-top: 16px; text-align: right">
        <div>Tạm tính: {{ formatPrice(order.subtotal) }}</div>
        <div>Phí ship: {{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Miễn phí' }}</div>
        <div style="font-size: 20px; font-weight: 800; margin-top: 8px">
          Tổng: {{ formatPrice(order.total) }}
        </div>
      </div>
    </div>
    <div class="card mt-3">
      <h3 style="font-weight: 700; margin-bottom: 16px">Lịch sử trạng thái</h3>
      <OrderTimeline :history="order.statusHistory" />
    </div>

    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>Hủy đơn hàng</h3>
          <button class="btn btn-sm btn-outline" @click="showCancelModal = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Lý do hủy</label>
            <textarea
              v-model="cancelReason"
              class="form-textarea"
              placeholder="Nhập lý do hủy đơn..."
              rows="3"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showCancelModal = false">Quay lại</button>
          <button class="btn btn-danger" @click="cancelOrder" :disabled="!cancelReason.trim()">
            <i class="bi bi-x-lg"></i> Xác nhận hủy
          </button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="empty-state" style="padding: 60px 0">
    <i class="bi bi-box"></i>
    <h3>Không tìm thấy đơn hàng</h3>
  </div>
</template>

<style scoped>
.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
  font-size: 14px;
}
.info-row span:last-child {
  font-weight: 600;
  text-align: right;
  max-width: 60%;
}
.badge-success {
  background: #e8f5e9;
  color: #2e7d32;
  padding: 4px 12px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 600;
}
.badge-error {
  background: #ffebee;
  color: #c62828;
  padding: 4px 12px;
  border-radius: 99px;
  font-size: 13px;
  font-weight: 600;
}
.btn-success {
  background: #10b981;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-success:hover {
  background: #059669;
}
.btn-danger {
  background: #ef4444;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: var(--radius-sm);
  font-weight: 600;
  cursor: pointer;
}
.btn-danger:hover {
  background: #dc2626;
}
.btn-danger:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff;
  border-radius: var(--radius);
  width: 440px;
  max-width: 90vw;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
}
.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.modal-body {
  padding: 20px 24px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
</style>
