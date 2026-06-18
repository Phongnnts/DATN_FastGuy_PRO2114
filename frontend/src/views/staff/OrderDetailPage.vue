<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useStaffStore } from '@/stores/staff'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'

const route = useRoute()
const staffStore = useStaffStore()

const order = ref(null)

onMounted(async () => {
  order.value = await staffStore.fetchOrderById(route.params.id)
})

async function confirmOrder() {
  if (!order.value) return
  await staffStore.updateOrderStatus(order.value.id, 'CONFIRMED')
  order.value.status = 'CONFIRMED'
}

async function cancelOrder() {
  if (!order.value) return
  if (!confirm('Hủy đơn hàng này?')) return
  await staffStore.updateOrderStatus(order.value.id, 'CANCELLED')
  order.value.status = 'CANCELLED'
}

function printInvoice() { window.print() }
</script>

<template>
  <div v-if="order">
    <div class="page-header">
      <div style="display:flex;align-items:center;gap:12px">
        <h1>Đơn hàng {{ order.orderCode }}</h1>
        <OrderStatusBadge :status="order.status" />
      </div>
      <div class="no-print" style="display:flex;gap:8px">
        <button class="btn btn-sm btn-outline" @click="printInvoice"><i class="bi bi-printer"></i> In hóa đơn</button>
      </div>
    </div>
    <div class="grid-2">
      <div class="card">
        <h3 style="font-weight:700;margin-bottom:16px">Thông tin đơn hàng</h3>
        <div class="info-row"><span>Khách hàng</span><span>{{ order.customerName }}</span></div>
        <div class="info-row"><span>Địa chỉ</span><span>{{ order.shippingAddress }}</span></div>
        <div class="info-row"><span>Phương thức</span><span>{{ order.paymentMethod }}</span></div>
        <div class="info-row"><span>Thanh toán</span><span>{{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</span></div>
        <div class="info-row"><span>Ghi chú</span><span>{{ order.note || 'Không có' }}</span></div>
      </div>
      <div class="card">
        <h3 style="font-weight:700;margin-bottom:16px">Thao tác</h3>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          <button v-if="order.status === 'PENDING'" class="btn btn-primary" @click="confirmOrder"><i class="bi bi-check-lg"></i> Xác nhận đơn</button>
          <button v-if="order.status === 'PENDING'" class="btn btn-outline" style="border-color:var(--red-active);color:var(--red-active)" @click="cancelOrder"><i class="bi bi-x-lg"></i> Hủy đơn</button>
          <span v-if="order.status === 'CONFIRMED'" class="badge badge-success">Đã xác nhận</span>
          <span v-if="order.status === 'CANCELLED'" class="badge badge-error">Đã hủy</span>
        </div>
      </div>
    </div>
    <div class="card mt-3">
      <h3 style="font-weight:700;margin-bottom:16px">Sản phẩm</h3>
      <table class="table">
        <thead><tr><th></th><th>Sản phẩm</th><th>Đơn giá</th><th>Số lượng</th><th>Thành tiền</th></tr></thead>
        <tbody>
          <tr v-for="item in order.items" :key="item.productId">
            <td><img :src="item.image" style="width:48px;height:48px;border-radius:8px;object-fit:cover" /></td>
            <td>{{ item.productName }}</td>
            <td>{{ formatPrice(item.price) }}</td>
            <td>{{ item.quantity }}</td>
            <td><strong>{{ formatPrice(item.price * item.quantity) }}</strong></td>
          </tr>
        </tbody>
      </table>
      <div class="order-totals" style="margin-top:16px;text-align:right">
        <div>Tạm tính: {{ formatPrice(order.subtotal) }}</div>
        <div>Phí ship: Miễn phí</div>
        <div style="font-size:20px;font-weight:800;margin-top:8px">Tổng: {{ formatPrice(order.total) }}</div>
      </div>
    </div>
    <div class="card mt-3">
      <h3 style="font-weight:700;margin-bottom:16px">Lịch sử trạng thái</h3>
      <OrderTimeline :history="order.statusHistory" />
    </div>
  </div>
  <div v-else class="empty-state" style="padding:60px 0">
    <i class="bi bi-box"></i>
    <h3>Không tìm thấy đơn hàng</h3>
  </div>
</template>

<style scoped>
.info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--border); font-size: 14px; }
.info-row span:last-child { font-weight: 600; text-align: right; max-width: 60%; }
.badge-success { background: #E8F5E9; color: #2E7D32; padding: 4px 12px; border-radius: 99px; font-size: 13px; font-weight: 600; }
.badge-error { background: #FFEBEE; color: #C62828; padding: 4px 12px; border-radius: 99px; font-size: 13px; font-weight: 600; }
</style>
