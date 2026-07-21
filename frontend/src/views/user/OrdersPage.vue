<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'

const router = useRouter()
const route = useRoute()
const orderStore = useOrderStore()
const activeTab = ref(route.query.status === 'DELIVERED' ? 'delivered' : 'pending')
const search = ref('')
const page = ref(1)
const pageSize = 6
const tabs = [
  { key: 'pending', label: 'Chờ xử lý', statuses: ['WAITING_STOCK_CONFIRM', 'PENDING'] },
  { key: 'processing', label: 'Đang xử lý', statuses: ['CONFIRMED', 'PREPARING', 'READY'] },
  { key: 'delivering', label: 'Đang giao', statuses: ['PICKED_UP'] },
  { key: 'delivered', label: 'Đã giao', statuses: ['DELIVERED'] },
  { key: 'cancelled', label: 'Đã hủy', statuses: ['CANCELLED'] },
]

const filteredOrders = computed(() => {
  const tab = tabs.find(item => item.key === activeTab.value)
  const term = search.value.trim().toLocaleLowerCase('vi')
  return orderStore.userOrders.filter(order => tab.statuses.includes(order.status) && (!term || order.orderCode?.toLocaleLowerCase('vi').includes(term) || order.items?.some(item => item.productName?.toLocaleLowerCase('vi').includes(term))))
})
const pageCount = computed(() => Math.max(1, Math.ceil(filteredOrders.value.length / pageSize)))
const visibleOrders = computed(() => filteredOrders.value.slice((page.value - 1) * pageSize, page.value * pageSize))

watch([activeTab, search], () => { page.value = 1 })
onMounted(() => orderStore.fetchOrders())
</script>

<template>
  <section class="orders-page card" aria-labelledby="orders-title">
    <h1 id="orders-title">Đơn hàng của tôi</h1>
    <div class="toolbar">
      <div class="tabs" role="tablist" aria-label="Lọc đơn hàng">
        <button v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active: activeTab === tab.key }" role="tab" :aria-selected="activeTab === tab.key" @click="activeTab = tab.key">{{ tab.label }}</button>
      </div>
      <label class="search"><span class="sr-only">Tìm đơn hàng</span><input v-model="search" class="form-input" type="search" placeholder="Mã đơn hoặc sản phẩm" /></label>
    </div>
    <div v-if="orderStore.loading" class="state" role="status"><i class="bi bi-arrow-repeat spin" aria-hidden="true"></i><span>Đang tải đơn hàng...</span></div>
    <div v-else-if="orderStore.error" class="state error" role="alert"><span>{{ orderStore.error }}</span><button class="btn btn-outline" @click="orderStore.fetchOrders()">Thử lại</button></div>
    <div v-else-if="!visibleOrders.length" class="state"><i class="bi bi-box" aria-hidden="true"></i><h2>Không có đơn hàng phù hợp</h2><router-link to="/menu" class="btn btn-primary">Đặt hàng ngay</router-link></div>
    <div v-else class="orders-list">
      <article v-for="order in visibleOrders" :key="order.id" class="order-card">
        <div class="order-card-header"><div><strong>{{ order.orderCode }}</strong><time :datetime="order.createdAt">{{ formatDate(order.createdAt) }}</time></div><OrderStatusBadge :status="order.status" /></div>
        <div class="order-card-items">
          <img v-for="(item, index) in order.items.slice(0, 3)" :key="`${item.productId}-${item.variantId || 'default'}-${index}`" :src="item.image" :alt="item.productName" />
          <span v-if="order.items.length > 3">+{{ order.items.length - 3 }}</span>
        </div>
        <div class="order-card-footer"><div><strong>{{ formatPrice(order.total) }}</strong><small>{{ order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chờ thanh toán' }}</small></div><button class="btn btn-sm btn-outline" :aria-label="`Xem chi tiết đơn ${order.orderCode}`" @click="router.push(`/account/orders/${order.id}`)">Xem chi tiết</button></div>
      </article>
      <nav v-if="pageCount > 1" class="pagination" aria-label="Phân trang đơn hàng"><button class="btn btn-sm btn-outline" :disabled="page === 1" @click="page--">Trước</button><span>Trang {{ page }}/{{ pageCount }}</span><button class="btn btn-sm btn-outline" :disabled="page === pageCount" @click="page++">Sau</button></nav>
    </div>
  </section>
</template>

<style scoped>
.orders-page { margin: 32px 0; padding: 24px; }
h1 { font-size: 20px; margin-bottom: 20px; }
.toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 20px; }
.tabs { display: flex; gap: 4px; overflow-x: auto; }
.tab { white-space: nowrap; border: 0; border-bottom: 2px solid transparent; padding: 10px 12px; background: transparent; cursor: pointer; }
.tab.active { color: var(--primary); border-color: var(--primary); font-weight: 700; }
.tab:focus-visible, .order-card button:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }
.search { width: min(260px, 100%); }
.orders-list { display: grid; gap: 12px; }
.order-card { border: 1px solid var(--border-light); border-radius: var(--radius); padding: 18px; }
.order-card-header, .order-card-footer { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
.order-card-header time, .order-card-footer small { display: block; margin-top: 3px; color: var(--text-light); font-size: 12px; }
.order-card-items { display: flex; gap: 8px; margin: 14px 0; }
.order-card-items img, .order-card-items span { width: 48px; height: 48px; border-radius: var(--radius-sm); object-fit: cover; }
.order-card-items span { display: grid; place-items: center; background: var(--surface); }
.state { min-height: 220px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; text-align: center; }
.state i { font-size: 32px; }.state h2 { font-size: 16px; }.error { color: #b91c1c; }
.pagination { display: flex; align-items: center; justify-content: center; gap: 12px; margin-top: 8px; }
.spin { animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } }
.sr-only { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0,0,0,0); }
@media (max-width: 720px) { .orders-page { margin: 16px 0; padding: 16px; }.toolbar { align-items: stretch; flex-direction: column; }.search { width: 100%; }.order-card-footer { align-items: center; } }
</style>
