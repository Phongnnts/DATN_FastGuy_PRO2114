<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useShipperStore } from '@/stores/shipper'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'

const route = useRoute()
const router = useRouter()
const shipperStore = useShipperStore()

const delivery = ref(null)
const noteInput = ref('')

onMounted(async () => {
  delivery.value = await shipperStore.fetchDeliveryById(route.params.id)
})

async function accept() {
  if (!delivery.value) return
  await shipperStore.acceptDelivery(delivery.value.id)
  delivery.value.status = 'DELIVERING'
}

async function markDelivered() {
  if (!delivery.value) return
  await shipperStore.updateDeliveryStatus(delivery.value.id, 'DELIVERED')
  delivery.value.status = 'DELIVERED'
}

async function markFailed() {
  if (!delivery.value) return
  const reason = prompt('Lý do giao thất bại:')
  await shipperStore.updateDeliveryStatus(delivery.value.id, 'FAILED', { failureReason: reason || '' })
  delivery.value.status = 'FAILED'
}

async function collectCOD() {
  if (!delivery.value) return
  await shipperStore.updateDeliveryStatus(delivery.value.id, 'DELIVERED', { collectedCOD: delivery.value.total })
  delivery.value.status = 'DELIVERED'
}

async function removeDelivery() {
  if (!delivery.value) return
  if (!confirm('Xóa đơn hàng này khỏi danh sách của bạn?')) return
  await shipperStore.removeDelivery(delivery.value.id)
  router.push('/shipper')
}

async function addNote() {
  if (!noteInput.value || !delivery.value) return
  await shipperStore.saveDeliveryNote(delivery.value.id, noteInput.value)
  noteInput.value = ''
}
</script>

<template>
  <div v-if="delivery">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:12px">
      <h3 style="font-size:18px;font-weight:700">{{ delivery.orderCode }}</h3>
      <span class="badge" :class="delivery.status === 'READY' ? 'badge-warning' : 'badge-info'">{{ delivery.status === 'READY' ? 'Sẵn sàng' : 'Đang giao' }}</span>
    </div>
    <div class="card mb-2">
      <div style="display:flex;align-items:center;gap:10px;margin-bottom:8px">
        <i class="bi bi-person-circle" style="font-size:24px;color:var(--primary)"></i>
        <div>
          <div style="font-weight:600">{{ delivery.customerName }}</div>
          <div style="font-size:13px;color:var(--text-mid)">{{ delivery.customerPhone }}</div>
        </div>
      </div>
      <div style="font-size:14px;margin-bottom:4px"><i class="bi bi-geo-alt" style="color:var(--primary)"></i> {{ delivery.address }}</div>
      <div v-if="delivery.note" style="font-size:13px;color:var(--text-mid);background:var(--bg);padding:8px;border-radius:var(--radius-sm);margin-top:8px">
        <i class="bi bi-chat-dots"></i> {{ delivery.note }}
      </div>
    </div>
    <div class="card mb-2">
      <h4 style="font-weight:600;font-size:14px;margin-bottom:8px">Đơn hàng</h4>
      <div v-for="item in delivery.items" :key="item.name" style="display:flex;justify-content:space-between;font-size:14px;padding:4px 0;border-bottom:1px solid var(--border)">
        <span>{{ item.name }} x{{ item.quantity }}</span>
      </div>
      <div style="display:flex;justify-content:space-between;font-size:16px;font-weight:700;margin-top:8px;padding-top:8px;border-top:2px solid var(--border)">
        <span>Tổng</span>
        <span>{{ formatPrice(delivery.total) }}</span>
      </div>
      <div style="font-size:13px;color:var(--text-mid);margin-top:4px">
        {{ delivery.paymentMethod === 'COD' ? 'Thu hộ COD' : 'Đã thanh toán online' }}
      </div>
    </div>
    <div class="card mb-2">
      <h4 style="font-weight:600;font-size:14px;margin-bottom:8px">Thao tác</h4>
      <div style="display:flex;flex-direction:column;gap:8px">
        <button v-if="delivery.status === 'READY'" class="btn btn-primary" @click="accept" style="background:var(--primary);border-color:var(--primary)"><i class="bi bi-check-lg"></i> Nhận đơn</button>
        <button v-if="delivery.status === 'DELIVERING'" class="btn" @click="markDelivered" style="background:#4CAF50;color:#fff"><i class="bi bi-check-circle"></i> Giao thành công</button>
        <button v-if="delivery.status === 'DELIVERING' && delivery.paymentMethod === 'COD'" class="btn btn-outline" @click="collectCOD"><i class="bi bi-cash"></i> Thu COD ({{ formatPrice(delivery.total) }})</button>
        <button v-if="delivery.status === 'DELIVERING'" class="btn btn-outline" @click="markFailed" style="border-color:var(--red-active);color:var(--red-active)"><i class="bi bi-x-circle"></i> Giao thất bại</button>
        <button v-if="delivery.status === 'READY' || delivery.status === 'DELIVERING'" class="btn btn-outline" @click="removeDelivery" style="border-color:var(--orange);color:var(--orange)"><i class="bi bi-x-square"></i> Xóa tạm</button>
      </div>
    </div>
    <div class="card">
      <h4 style="font-weight:600;font-size:14px;margin-bottom:8px">Ghi chú giao hàng</h4>
      <div v-if="delivery.notes?.length" class="notes-list">
        <div v-for="(n, i) in delivery.notes" :key="i" style="background:var(--bg);padding:8px 12px;border-radius:var(--radius-sm);margin-bottom:6px;font-size:13px">
          <div>{{ n.content }}</div>
          <div style="font-size:11px;color:var(--text-light);margin-top:2px">{{ formatDate(n.createdAt) }}</div>
        </div>
      </div>
      <div style="display:flex;gap:8px;margin-top:8px">
        <input v-model="noteInput" class="form-input" placeholder="Thêm ghi chú..." @keyup.enter="addNote" />
        <button class="btn btn-sm btn-primary" @click="addNote" style="background:var(--primary);border:none"><i class="bi bi-send"></i></button>
      </div>
    </div>
  </div>
  <div v-else style="text-align:center;padding:40px;color:var(--text-mid)">Không tìm thấy đơn giao</div>
</template>
