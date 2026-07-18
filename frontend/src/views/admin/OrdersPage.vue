<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { useToastStore } from '@/stores/toast';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { adminApi } from '@/api';

const toast = useToastStore();

const adminStore = useAdminStore();
const searchTerm = ref('');
const activeStatus = ref('');
const currentPage = ref(1);
const pageSize = 10;
const refundOrder = ref(null);
const refundForm = ref({ refundAmount: 0, refundNote: '', status: 'REFUNDED' });
const refunding = ref(false);

const statusFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'WAITING_STOCK_CONFIRM', label: 'Chờ xác nhận tồn' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

onMounted(() => adminStore.fetchOrders());

const filtered = computed(() => {
  let list = adminStore.allOrders;
  if (activeStatus.value) {
    list = list.filter((o) => o.status === activeStatus.value);
  }
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase();
    list = list.filter((o) => (o.orderCode || '').toLowerCase().includes(q));
  }
  return list;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filtered.value.slice(start, start + pageSize);
});

function setStatus(s) {
  activeStatus.value = s;
  currentPage.value = 1;
}

function openRefund(order) {
  refundOrder.value = order;
  refundForm.value = { refundAmount: Number(order.finalAmount || 0), refundNote: '', status: 'REFUNDED' };
}

async function saveRefund() {
  refunding.value = true;
  try {
    await adminApi.updateRefund(refundOrder.value.orderId, { ...refundForm.value, refundAmount: Number(refundForm.value.refundAmount) });
    refundOrder.value = null;
    await adminStore.fetchOrders();
  } catch (e) {
    toast.error(e.message);
  } finally {
    refunding.value = false;
  }
}
</script>

<template>
  <div>
    <div class="page-header"><h1>Quản lý đơn hàng</h1></div>
    <div class="card card-flat">
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin-bottom:16px">
        <div class="search-box" style="max-width:320px">
          <i class="bi bi-search"></i>
          <input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn..." />
        </div>
      </div>
      <div class="tabs" style="margin-bottom:16px">
        <button v-for="f in statusFilters" :key="f.key" class="tab" :class="{ active: activeStatus === f.key }" @click="setStatus(f.key)">{{ f.label }}</button>
      </div>
      <div class="table-wrapper">
        <table class="table">
          <thead>
            <tr>
              <th>Mã đơn</th>
              <th>Khách hàng</th>
              <th>SP</th>
              <th>Tổng</th>
              <th>Thanh toán</th>
              <th>Ngày</th>
              <th>Trạng thái</th>
              <th>Ghi chú</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="o in paged" :key="o.id || o.orderId">
              <td><strong>{{ o.orderCode }}</strong></td>
              <td>{{ o.customerName || 'Khách' }}</td>
              <td>{{ o.itemCount || 0 }}</td>
              <td>{{ formatPrice(o.finalAmount || o.total) }}</td>
              <td>{{ o.paymentMethod === 'BANK_TRANSFER' ? `PayOS · ${o.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chờ thanh toán'}` : 'COD' }}</td>
              <td>{{ formatDate(o.createdAt) }}</td>
              <td><OrderStatusBadge :status="o.status" /></td>
              <td>
                <template v-if="o.status === 'CANCELLED'">
                  <div v-if="o.cancelledBy">Hủy bởi: {{ o.cancelledBy === 'STAFF' ? 'Nhân viên' : 'Khách' }}</div>
                  <div v-if="o.failureReason" style="color:var(--red-active)">{{ o.failureReason }}</div>
                  <div v-if="o.refundStatus">Hoàn tiền: {{ o.refundStatus }}</div>
                  <button v-if="o.refundStatus === 'PENDING'" class="btn btn-sm btn-primary" style="margin-top:6px" @click="openRefund(o)">Xử lý hoàn tiền</button>
                </template>
                <span v-else class="text-muted">—</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="totalPages > 1" class="pagination" style="margin-top:16px;justify-content:center">
        <button :disabled="currentPage <= 1" @click="currentPage--"><i class="bi bi-chevron-left"></i></button>
        <span style="padding:0 12px;font-size:14px">{{ currentPage }} / {{ totalPages }}</span>
        <button :disabled="currentPage >= totalPages" @click="currentPage++"><i class="bi bi-chevron-right"></i></button>
      </div>
    </div>

    <div v-if="refundOrder" class="modal-overlay" @click.self="refundOrder = null">
      <form class="modal" style="max-width:500px" @submit.prevent="saveRefund">
        <div class="modal-header">
          <h3>Hoàn tiền {{ refundOrder.orderCode }}</h3>
          <button type="button" class="btn btn-sm btn-outline" @click="refundOrder = null"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Trạng thái</label>
            <select v-model="refundForm.status" class="form-select">
              <option value="REFUNDED">REFUNDED</option>
              <option value="REJECTED">REJECTED</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Số tiền hoàn</label>
            <input v-model.number="refundForm.refundAmount" class="form-input" type="number" min="0" :max="refundOrder.finalAmount" required>
          </div>
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <textarea v-model="refundForm.refundNote" class="form-input" rows="3"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="refundOrder = null">Hủy</button>
          <button class="btn btn-primary" :disabled="refunding">{{ refunding ? 'Đang lưu...' : 'Xác nhận' }}</button>
        </div>
      </form>
    </div>
  </div>
</template>
