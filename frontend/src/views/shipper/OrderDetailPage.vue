<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice, formatDate } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const shipperStore = useShipperStore();
const order = ref(null);
const loading = ref(true);
const showCancelModal = ref(false);
const cancelReason = ref('');

onMounted(async () => {
  order.value = await shipperStore.fetchOrderById(route.params.id);
  loading.value = false;
});

async function pickUp() {
  if (!confirm('Xác nhận đã lấy hàng?')) return;
  try {
    await shipperStore.pickUpOrder(order.value.id);
    order.value.status = 'PICKED_UP';
  } catch (e) {
    alert(e.message);
  }
}

async function deliver() {
  if (!confirm('Xác nhận đã giao hàng thành công?')) return;
  try {
    await shipperStore.deliverOrder(order.value.id);
    order.value.status = 'DELIVERED';
  } catch (e) {
    alert(e.message);
  }
}

function openCancel() {
  cancelReason.value = '';
  showCancelModal.value = true;
}

async function confirmCancel() {
  try {
    await shipperStore.cancelOrder(order.value.id, cancelReason.value);
    order.value.status = 'CANCELLED';
    showCancelModal.value = false;
  } catch (e) {
    alert(e.message);
  }
}

function callCustomer() {
  if (order.value?.customerPhone) {
    window.location.href = `tel:${order.value.customerPhone}`;
  }
}
</script>

<template>
  <div v-if="loading" class="shipper-empty">Đang tải...</div>
  <div v-else-if="!order" class="shipper-empty">
    <i class="bi bi-box"></i>
    <p>Không tìm thấy đơn hàng</p>
  </div>
  <div v-else>
    <div class="detail-header">
      <div>
        <h3 style="font-size: 16px; font-weight: 700;">{{ order.orderCode }}</h3>
        <span style="font-size: 13px; color: var(--text-mid);">{{ formatDate(order.createdAt) }}</span>
      </div>
      <span :class="'status-badge status-' + order.status.toLowerCase()">
        {{ order.status === 'PICKED_UP' ? 'Đang giao' : order.status === 'DELIVERED' ? 'Đã giao' : order.status === 'READY' ? 'Sẵn sàng giao' : 'Đã hủy' }}
      </span>
    </div>

    <div class="info-card">
      <h4>Khách hàng</h4>
      <p><i class="bi bi-person"></i> {{ order.customerName }}</p>
      <p><i class="bi bi-telephone"></i> {{ order.customerPhone }}</p>
      <p><i class="bi bi-geo-alt"></i> {{ order.customerAddress }}</p>
      <button v-if="order.customerPhone" class="btn btn-sm btn-primary call-btn" @click="callCustomer">
        <i class="bi bi-telephone"></i> Gọi điện
      </button>
      <a :href="'https://www.google.com/maps/search/?api=1&query=' + encodeURIComponent(order.customerAddress)" target="_blank" class="btn btn-sm btn-outline map-btn">
        <i class="bi bi-map"></i> Xem bản đồ
      </a>
    </div>

    <div class="info-card" v-if="order.deliveryNote">
      <h4>Ghi chú</h4>
      <p>{{ order.deliveryNote }}</p>
    </div>

    <div class="info-card" v-if="order.items && order.items.length">
      <h4>Sản phẩm</h4>
      <div v-for="item in order.items" :key="item.productId" class="item-row">
        <span>{{ item.productName }}{{ item.variantName ? ' - ' + item.variantName : '' }}</span>
        <span>x{{ item.quantity }}</span>
      </div>
    </div>

    <div class="info-card">
      <h4>Thanh toán</h4>
      <div class="summary-row"><span>Phí ship</span><span>{{ formatPrice(order.shippingFee) }}</span></div>
      <div class="summary-row total"><span>Tổng cộng</span><span>{{ formatPrice(order.total) }}</span></div>
      <p style="font-size:13px;color:var(--text-mid);margin-top:8px;">
        {{ order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 'Đã thanh toán online' }}
      </p>
    </div>

    <div class="action-bar">
      <button v-if="order.status === 'READY'" class="btn btn-lg btn-primary action-btn" @click="pickUp">
        <i class="bi bi-box-seam"></i> Đã lấy hàng
      </button>
      <button v-if="order.status === 'PICKED_UP'" class="btn btn-lg btn-success action-btn" @click="deliver">
        <i class="bi bi-check2-all"></i> Đã giao thành công
      </button>
      <button v-if="order.status === 'READY' || order.status === 'PICKED_UP'" class="btn btn-outline cancel-btn" @click="openCancel">
        <i class="bi bi-x-lg"></i> Hủy đơn
      </button>
    </div>

    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>Hủy đơn hàng</h3>
          <button class="btn btn-sm btn-ghost" @click="showCancelModal = false"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Lý do hủy</label>
            <textarea v-model="cancelReason" class="form-textarea" rows="3" placeholder="Nhập lý do hủy..."></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" @click="showCancelModal = false">Quay lại</button>
          <button class="btn btn-danger" @click="confirmCancel" :disabled="!cancelReason.trim()">
            Xác nhận hủy
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.shipper-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--text-mid);
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.status-badge {
  padding: 4px 12px;
  border-radius: 99px;
  font-size: 12px;
  font-weight: 600;
}
.status-ready { background: #FEF3C7; color: #92400E; }
.status-picked_up { background: #DBEAFE; color: #1E40AF; }
.status-delivered { background: #D1FAE5; color: #065F46; }
.status-cancelled { background: #FEE2E2; color: #991B1B; }
.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 10px;
}
.info-card h4 {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-mid);
  margin-bottom: 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.info-card p {
  font-size: 14px;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.call-btn {
  margin-top: 8px;
  width: 100%;
}
.item-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.summary-row {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  padding: 4px 0;
}
.summary-row.total {
  font-size: 17px;
  font-weight: 800;
  border-top: 1px solid #eee;
  padding-top: 8px;
  margin-top: 4px;
}
.action-bar {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.action-btn {
  width: 100%;
  padding: 14px;
  font-size: 16px;
  border-radius: 12px;
}
.btn-success {
  background: #10B981;
  color: #fff;
  border: none;
  font-weight: 700;
  cursor: pointer;
}
.btn-success:hover { background: #059669; }
.cancel-btn {
  width: 100%;
  border-color: var(--red-active);
  color: var(--red-active);
  font-weight: 600;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff;
  border-radius: 16px;
  width: 360px;
  max-width: 90vw;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 20px 0;
}
.modal-header h3 { font-size: 17px; font-weight: 700; }
.modal-body { padding: 16px 20px; }
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px 20px;
}
.btn-danger {
  background: #EF4444;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
}
.btn-danger:disabled { opacity: 0.5; }
</style>
