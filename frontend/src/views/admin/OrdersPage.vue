<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAdminStore } from '@/stores/admin';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';

const toast = useToast();
const router = useRouter();
const adminStore = useAdminStore();
const searchTerm = ref('');
const activeStatus = ref('');
const currentPage = ref(1);
const pageSize = 10;
const refundOrder = ref(null);
const refundForm = ref({ refundAmount: 0, refundNote: '', status: 'REFUNDED' });
const refunding = ref(false);
const filterFromDate = ref('');
const filterToDate = ref('');

const statusFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'REFUND_PENDING', label: 'Cần hoàn tiền' },
  { key: 'WAITING_STOCK_CONFIRM', label: 'Chờ xác nhận tồn' },
  { key: 'PENDING', label: 'Chờ xác nhận' },
  { key: 'CONFIRMED', label: 'Đã xác nhận' },
  { key: 'PREPARING', label: 'Đang chế biến' },
  { key: 'READY', label: 'Sẵn sàng giao' },
  { key: 'PICKED_UP', label: 'Đang giao' },
  { key: 'DELIVERED', label: 'Đã giao' },
  { key: 'CANCELLED', label: 'Đã hủy' },
];

async function loadOrders() {
  const params = {};
  if (filterFromDate.value) params.fromDate = filterFromDate.value;
  if (filterToDate.value) params.toDate = filterToDate.value;
  await adminStore.fetchOrders(params);
}
function clearDateFilter() {
  filterFromDate.value = '';
  filterToDate.value = '';
  loadOrders();
}
onMounted(loadOrders);

const filtered = computed(() => {
  let list = adminStore.allOrders;
  if (activeStatus.value === 'REFUND_PENDING') {
    list = list.filter((o) => o.status === 'CANCELLED' && o.refundStatus === 'PENDING');
  } else if (activeStatus.value) {
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
  const msg = refundForm.value.status === 'REFUNDED'
    ? `Xác nhận hoàn tiền ${formatPrice(refundForm.value.refundAmount)} cho đơn ${refundOrder.value.orderCode}?`
    : `Xác nhận từ chối hoàn tiền cho đơn ${refundOrder.value.orderCode}?`;
  if (!confirm(msg)) return;
  refunding.value = true;
  try {
    await adminApi.updateRefund(refundOrder.value.orderId, { ...refundForm.value, refundAmount: Number(refundForm.value.refundAmount) });
    toast.success(refundForm.value.status === 'REFUNDED' ? 'Đã xử lý hoàn tiền thành công' : 'Đã từ chối hoàn tiền');
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
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:end;margin-bottom:16px">
        <div class="search-box" style="max-width:320px">
          <i class="bi bi-search"></i>
          <input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn..." />
        </div>
        <div style="display:flex;gap:8px;align-items:end">
          <div class="form-group" style="margin:0">
            <label class="form-label" style="font-size:11px">Từ ngày</label>
            <input v-model="filterFromDate" type="date" class="form-input" style="width:auto" @change="loadOrders">
          </div>
          <div class="form-group" style="margin:0">
            <label class="form-label" style="font-size:11px">Đến ngày</label>
            <input v-model="filterToDate" type="date" class="form-input" style="width:auto" @change="loadOrders">
          </div>
          <button v-if="filterFromDate || filterToDate" class="btn btn-sm btn-outline" @click="clearDateFilter"><i class="bi bi-x-lg"></i></button>
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
              <td><strong class="order-link" @click="router.push(`/admin/orders/${o.orderId}`)">{{ o.orderCode }}</strong></td>
              <td>{{ o.customerName || 'Khách' }}</td>
              <td>{{ o.itemCount || 0 }}</td>
              <td>{{ formatPrice(o.finalAmount || o.total) }}</td>
              <td>{{ o.paymentMethod === 'BANK_TRANSFER' ? `PayOS · ${o.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chờ thanh toán'}` : 'COD' }}</td>
              <td>{{ formatDate(o.createdAt) }}</td>
              <td><OrderStatusBadge :status="o.status" /></td>
              <td>
                <template v-if="o.status === 'CANCELLED'">
                  <div v-if="o.cancelledBy" class="text-muted" style="font-size:12px">Hủy bởi: {{ o.cancelledBy === 'STAFF' ? 'Nhân viên' : o.cancelledBy === 'SHIPPER' ? 'Shipper' : 'Khách' }}</div>
                  <div v-if="o.failureReason" style="color:var(--red-active);font-size:12px">{{ o.failureReason }}</div>
                  <div v-if="o.refundStatus === 'REFUNDED'" style="margin-top:4px">
                    <span class="refund-badge refund-done"><i class="bi bi-check-circle-fill"></i> Đã hoàn {{ formatPrice(o.refundAmount) }}</span>
                  </div>
                  <div v-else-if="o.refundStatus === 'REJECTED'" style="margin-top:4px">
                    <span class="refund-badge refund-rejected"><i class="bi bi-x-circle-fill"></i> Từ chối hoàn tiền</span>
                  </div>
                  <div v-else-if="o.refundStatus === 'PENDING'" style="margin-top:4px">
                    <span class="refund-badge refund-pending"><i class="bi bi-clock-fill"></i> Chờ hoàn tiền</span>
                    <button class="btn btn-sm btn-primary" style="margin-top:6px;display:block;width:100%" @click="openRefund(o)">
                      <i class="bi bi-arrow-return-left"></i> Xử lý hoàn tiền
                    </button>
                  </div>
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
      <form class="modal" style="max-width:520px" @submit.prevent="saveRefund">
        <div class="modal-header">
          <h3><i class="bi bi-arrow-return-left"></i> Hoàn tiền {{ refundOrder.orderCode }}</h3>
          <button type="button" class="btn btn-sm btn-outline" @click="refundOrder = null"><i class="bi bi-x-lg"></i></button>
        </div>
        <div class="modal-body">
          <div class="refund-order-info">
            <div class="refund-info-row"><span>Khách hàng:</span><strong>{{ refundOrder.customerName || 'Khách' }}</strong></div>
            <div class="refund-info-row"><span>Tổng tiền đơn:</span><strong class="text-primary">{{ formatPrice(refundOrder.finalAmount) }}</strong></div>
            <div class="refund-info-row"><span>Thanh toán:</span><span>{{ refundOrder.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }} · {{ refundOrder.paymentStatus }}</span></div>
            <div class="refund-info-row" v-if="refundOrder.failureReason"><span>Lý do hủy:</span><span class="text-red">{{ refundOrder.failureReason }}</span></div>
          </div>
          <div class="form-group">
            <label class="form-label">Hành động</label>
            <select v-model="refundForm.status" class="form-select">
              <option value="REFUNDED">Hoàn tiền (Đã chuyển khoản thủ công)</option>
              <option value="REJECTED">Từ chối hoàn tiền</option>
            </select>
          </div>
          <div class="form-group" v-if="refundForm.status === 'REFUNDED'">
            <label class="form-label">Số tiền hoàn (tối đa {{ formatPrice(refundOrder.finalAmount) }})</label>
            <input v-model.number="refundForm.refundAmount" class="form-input" type="number" min="0" :max="Number(refundOrder.finalAmount)" required>
          </div>
          <div class="form-group">
            <label class="form-label">Ghi chú</label>
            <textarea v-model="refundForm.refundNote" class="form-input" rows="3" :placeholder="refundForm.status === 'REFUNDED' ? 'VD: Đã chuyển khoản TK ..., STK ..., nội dung CK ...' : 'Lý do từ chối hoàn tiền'"></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline" @click="refundOrder = null">Hủy</button>
          <button class="btn" :class="refundForm.status === 'REFUNDED' ? 'btn-primary' : 'btn-danger'" :disabled="refunding">
            <i :class="refundForm.status === 'REFUNDED' ? 'bi bi-check-lg' : 'bi bi-x-lg'"></i>
            {{ refunding ? 'Đang xử lý...' : (refundForm.status === 'REFUNDED' ? 'Xác nhận hoàn tiền' : 'Từ chối') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.refund-order-info {
  background: var(--bg-secondary, #f8f9fa);
  border: 1px solid var(--border, #e5e7eb);
  border-radius: var(--radius-sm, 6px);
  padding: 12px 16px;
  margin-bottom: 16px;
}
.refund-info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 0;
  font-size: 14px;
}
.refund-info-row span:first-child { color: var(--text-mid, #6b7280); }
.refund-info-row + .refund-info-row { border-top: 1px solid var(--border, #e5e7eb); }
.text-primary { color: var(--primary, #2563eb); }
.text-red { color: var(--red-active, #ef4444); }
.refund-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}
.refund-pending { background: #fef3c7; color: #92400e; }
.refund-done { background: #d1fae5; color: #065f46; }
.refund-rejected { background: #fee2e2; color: #991b1b; }
.order-link { cursor: pointer; color: var(--primary, #2563eb); }
.order-link:hover { text-decoration: underline; }
</style>
