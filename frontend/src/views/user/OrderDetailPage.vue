<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import StarRating from '@/components/common/StarRating.vue'
import { orderApi, reviewApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import { useProductStore } from '@/stores/product'
import { useAuthStore } from '@/stores/auth'
import { useToast } from '@/utils/toast'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const productStore = useProductStore()
const auth = useAuthStore()
const toast = useToast()
const order = ref(null)
const loadError = ref('')
const orderReview = ref(null)
const reviewForm = ref({ rating: 5, comment: '' })
const cancelForm = ref({ reason: '' })
const submitting = ref(false)
const showReviewForm = ref(false)
const showCancelForm = ref(false)
const reordering = ref(false)
const justCreated = ref(route.query.created === '1')

const isDelivered = computed(() => order.value?.status === 'DELIVERED')
const isCancelled = computed(() => order.value?.status === 'CANCELLED')
const canCancel = computed(() => order.value?.allowedActions?.includes('CANCEL') || false)
const canReview = computed(() => order.value?.canReview === true && !orderReview.value)
const waitingStockConfirm = computed(() => order.value?.status === 'WAITING_STOCK_CONFIRM')
const isOwner = computed(() => order.value?.userId === auth.user?.id)
const showPayOS = computed(() => isOwner.value && order.value?.paymentMethod === 'BANK_TRANSFER' && order.value?.checkoutUrl && !isCancelled.value && order.value?.paymentStatus !== 'PAID')

let statusPolling = null

onMounted(async () => {
  try {
    const data = await orderApi.getById(route.params.id)
    if (data) {
      order.value = {
        id: data.orderId,
        orderCode: data.orderCode,
        status: data.status,
        userId: data.userId,
        items: (data.items || []).map(i => ({
          productId: i.productId,
          variantId: i.variantId || null,
          productName: i.productName,
          variantName: i.variantName || '',
          price: i.unitPrice || 0,
          quantity: i.quantity,
          totalPrice: i.totalPrice || 0,
          image: i.image || ''
        })),
        subtotal: data.totalAmount || 0,
        shippingFee: data.shippingFee || 0,
        discount: data.discountAmount || 0,
        total: data.finalAmount || 0,
        paymentMethod: data.paymentMethod,
        paymentStatus: data.paymentStatus,
        codCollectedAmount: data.codCollectedAmount != null ? Number(data.codCollectedAmount) : null,
        codCollectedAt: data.codCollectedAt || null,
        shippingAddress: data.customerAddress || '',
        note: data.deliveryNote || '',
        createdAt: data.createdAt,
        statusHistory: data.statusHistory || [{ status: data.status, time: data.createdAt, note: '' }],
        cancelledBy: data.cancelledBy || null,
        refundStatus: data.refundStatus || null,
        refundAmount: data.refundAmount ?? null,
        refundedAt: data.refundedAt || null,
        refundNote: data.refundNote || '',
        failureReason: data.failureReason || '',
        checkoutUrl: data.checkoutUrl || null,
        allowedActions: data.allowedActions || [],
        canReview: data.canReview || false,
      }
    }
    if (data && data.status === 'DELIVERED') {
      await loadReview(data.orderId)
    }
    startStatusPolling()
  } catch (e) {
    loadError.value = e.message || 'Khong the tai chi tiet don hang'
  }
})

onUnmounted(() => {
  if (statusPolling) { clearInterval(statusPolling); statusPolling = null }
})

function startStatusPolling() {
  if (!order.value || order.value.status !== 'WAITING_STOCK_CONFIRM') return
  statusPolling = setInterval(async () => {
    try {
      const data = await orderApi.getById(order.value.id)
      if (data && data.status !== 'WAITING_STOCK_CONFIRM') {
        order.value.status = data.status
        order.value.allowedActions = data.allowedActions || []
        order.value.canReview = data.canReview || false
        order.value.checkoutUrl = data.checkoutUrl || null
        order.value.statusHistory = data.statusHistory || order.value.statusHistory
        order.value.paymentStatus = data.paymentStatus
        if (order.value.checkoutUrl) window.location.assign(order.value.checkoutUrl)
        if (statusPolling) { clearInterval(statusPolling); statusPolling = null }
      }
    } catch {}
  }, 5000)
}

async function loadReview(orderId) {
  try {
    const data = await reviewApi.getByOrder(orderId)
    orderReview.value = data
  } catch {}
}

async function submitReview() {
  if (!order.value) return
  submitting.value = true
  try {
    await reviewApi.create({
      orderId: order.value.id,
      rating: Number(reviewForm.value.rating),
      comment: reviewForm.value.comment || '',
    })
    await loadReview(order.value.id)
    showReviewForm.value = false
    toast.success('Gui danh gia thanh cong!')
  } catch (e) {
    toast.error(e.message || 'Khong the gui danh gia')
  } finally {
    submitting.value = false
  }
}

async function reorder() {
  if (!order.value || reordering.value) return
  reordering.value = true
  const unavailable = []
  try {
    for (const item of order.value.items) {
      try {
        const product = await productStore.fetchById(item.productId)
        const variant = (product?.variants || []).find(v => v.variantId === item.variantId)
        const stock = variant?.quantityAvailable
        if (!variant || variant.status !== 'AVAILABLE' || (stock !== null && stock !== undefined && Number(stock) < item.quantity)) {
          unavailable.push(item.productName)
          continue
        }
        await cart.addItem(item.productId, item.variantId, item.quantity)
      } catch {
        unavailable.push(item.productName)
      }
    }
    if (unavailable.length) toast.warning(`Khong the them: ${unavailable.join(', ')}`)
    if (unavailable.length < order.value.items.length) router.push('/cart')
  } finally {
    reordering.value = false
  }
}

async function cancelOrder() {
  if (!canCancel.value || !order.value) return;
  try {
    await orderApi.cancel(order.value.id, { reason: cancelForm.value.reason });
    order.value.status = 'CANCELLED';
    order.value.cancelledBy = 'CUSTOMER';
    order.value.allowedActions = [];
    if (order.value.statusHistory) {
      order.value.statusHistory.push({
        status: 'CANCELLED',
        time: new Date().toISOString(),
        note: cancelForm.value.reason || 'Khach huy',
      });
    }
    showCancelForm.value = false;
    toast.success('Huy don hang thanh cong');
  } catch (e) {
    toast.error(e.message || 'Khong the huy don hang');
  }
}
</script>

<template>
  <div class="order-detail-page" v-if="order">
    <div class="card">
      <div v-if="justCreated" class="order-success">
        <i class="bi bi-check-circle-fill"></i> Dat don thanh cong. Ma don: <strong>{{ order.orderCode }}</strong>
      </div>
      <div class="detail-header">
        <div>
          <h3>Don hang {{ order.orderCode }}</h3>
          <p style="color:var(--text-mid);font-size:14px">{{ formatDate(order.createdAt) }}</p>
        </div>
        <OrderStatusBadge :status="order.status" />
      </div>
      <div class="detail-section">
        <h4>Thong tin giao hang</h4>
        <p><i class="bi bi-geo-alt"></i> {{ order.shippingAddress }}</p>
        <p v-if="order.note"><i class="bi bi-chat-dots"></i> Ghi chu: {{ order.note }}</p>
        <p><i class="bi bi-credit-card"></i> {{ order.paymentMethod === 'COD' ? 'Thanh toan khi nhan hang' : 'Thanh toan PayOS' }}</p>
        <p><i class="bi bi-receipt"></i> Trang thai: {{ order.paymentStatus === 'PAID' ? 'Da thanh toan' : 'Chua thanh toan' }}</p>
      </div>
      <div class="detail-section">
        <h4>San pham</h4>
        <div v-for="item in order.items" :key="item.productId" class="detail-item">
          <img :src="item.image" :alt="item.productName" class="detail-item-img" />
          <div class="detail-item-info">
            <div class="detail-item-name">{{ item.productName }}</div>
            <div v-if="item.variantName" class="item-variant" style="font-size:12px;color:var(--text-mid)">{{ item.variantName }}</div>
            <div class="detail-item-price">{{ formatPrice(item.price) }}</div>
          </div>
          <div class="detail-item-qty">x{{ item.quantity }}</div>
          <div class="detail-item-total">{{ formatPrice(item.price * item.quantity) }}</div>
        </div>
      </div>
      <div class="detail-summary">
        <div class="detail-summary-row"><span>Tam tinh</span><span>{{ formatPrice(order.subtotal) }}</span></div>
        <div class="detail-summary-row"><span>Phi giao hang</span><span>{{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Mien phi' }}</span></div>
        <div v-if="order.discount > 0" class="detail-summary-row" style="color:var(--red-active)"><span>Giam gia</span><span>-{{ formatPrice(order.discount) }}</span></div>
        <div class="detail-summary-row detail-total"><span>Tong cong</span><span>{{ formatPrice(order.total) }}</span></div>
      </div>
      <div class="detail-section">
        <h4>Trang thai don hang</h4>
        <OrderTimeline :history="order.statusHistory" />
      </div>

      <div v-if="waitingStockConfirm" class="detail-section" style="text-align:center;padding:24px">
        <i class="bi bi-hourglass-split" style="color:var(--primary);font-size:48px"></i>
        <h4 style="margin-top:8px">Dang cho cua hang xac nhan ton kho</h4>
        <p style="color:var(--text-mid);font-size:13px">Cua hang se kiem tra va xac nhan don hang cua ban</p>
      </div>

      <div v-if="showPayOS" class="detail-section" style="text-align:center;padding:24px">
        <h4>Thanh toan PayOS</h4>
        <p style="font-size:20px;font-weight:800">{{ formatPrice(order.total) }}</p>
        <a :href="order.checkoutUrl" class="btn btn-primary">Mo trang thanh toan</a>
      </div>

      <div v-if="isDelivered" class="detail-section">
        <h4>Danh gia don hang</h4>
        <div v-if="orderReview" class="review-done">
          <StarRating :modelValue="orderReview.rating" readonly :size="18" />
          <p class="review-done-comment">{{ orderReview.comment || 'Ngon, se ung ho tiep.' }}</p>
          <span class="badge badge-success">Da danh gia</span>
        </div>
        <div v-else-if="showReviewForm" class="review-form-block">
          <StarRating v-model="reviewForm.rating" :size="24" />
          <textarea v-model="reviewForm.comment" class="form-textarea" rows="3" maxlength="1000" placeholder="Chia se cam nhan ve don hang..."></textarea>
          <div class="review-form-actions">
            <button class="btn btn-sm btn-ghost" @click="showReviewForm = false">Huy</button>
            <button class="btn btn-sm btn-primary" :disabled="submitting" @click="submitReview">
              {{ submitting ? 'Dang gui...' : 'Gui danh gia' }}
            </button>
          </div>
        </div>
        <div v-else-if="canReview">
          <button class="btn btn-outline" @click="showReviewForm = true">
            <i class="bi bi-star"></i> Danh gia don hang
          </button>
        </div>
      </div>

      <div v-if="isCancelled" class="detail-section">
        <h4>Thong tin huy don</h4>
        <p v-if="order.failureReason"><i class="bi bi-chat-left-text"></i> Ly do: {{ order.failureReason }}</p>
        <p v-if="order.cancelledBy"><i class="bi bi-person-x"></i> Nguoi huy: {{ order.cancelledBy === 'STAFF' ? 'Nhan vien' : 'Ban' }}</p>
        <p v-if="order.refundStatus"><i class="bi bi-arrow-return-left"></i> Hoan tien: {{ order.refundStatus }}{{ order.refundNote ? ' - ' + order.refundNote : '' }}</p>
        <p v-if="order.refundAmount !== null"><i class="bi bi-cash-stack"></i> So tien hoan: {{ formatPrice(order.refundAmount) }}</p>
        <p v-if="order.refundedAt"><i class="bi bi-calendar-check"></i> Ngay hoan: {{ formatDate(order.refundedAt) }}</p>
      </div>
      <div v-if="isDelivered || isCancelled" style="margin-top:16px">
        <button class="btn btn-primary" :disabled="reordering" @click="reorder"><i class="bi bi-cart-plus"></i> {{ reordering ? 'Dang them...' : 'Dat lai don' }}</button>
      </div>
      <div v-if="canCancel && !showCancelForm" style="margin-top:16px">
        <button class="btn btn-outline" style="border-color:var(--red-active);color:var(--red-active)" @click="showCancelForm = true">
          <i class="bi bi-x-lg"></i> Huy don hang
        </button>
      </div>
      <div v-if="showCancelForm" class="detail-section">
        <div class="form-group">
          <label class="form-label">Ly do huy</label>
          <textarea v-model="cancelForm.reason" class="form-textarea" rows="3" maxlength="500" placeholder="Nhap ly do huy..."></textarea>
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px">
          <button class="btn btn-outline" @click="showCancelForm = false">Quay lai</button>
          <button class="btn btn-danger" :disabled="!cancelForm.reason.trim()" @click="cancelOrder">Xac nhan huy</button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="empty-state" style="padding:60px 0">
    <i class="bi bi-box"></i>
    <h3>{{ loadError || 'Khong tim thay don hang' }}</h3>
    <button v-if="loadError" class="btn btn-primary" @click="router.go(0)">Thu lai</button>
  </div>
</template>

<style scoped>
.order-detail-page { padding: 32px 0; }
.order-success { margin-bottom: 16px; padding: 10px 12px; border-radius: var(--radius-sm); background: #ecfdf5; color: #047857; font-size: 13px; font-weight: 600; }
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.detail-header h3 { font-size: 18px; font-weight: 700; }
.detail-section { border-top: 1px solid var(--border-light); padding: 20px 0; }
.detail-section h4 { font-size: 15px; font-weight: 700; margin-bottom: 12px; }
.detail-section p { font-size: 14px; color: var(--text-mid); margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid var(--border-light);
}
.detail-item:last-child { border-bottom: none; }
.detail-item-img { width: 52px; height: 52px; border-radius: var(--radius-sm); object-fit: cover; }
.detail-item-info { flex: 1; }
.detail-item-name { font-size: 14px; font-weight: 600; }
.detail-item-price { font-size: 13px; color: var(--text-mid); }
.detail-item-qty { font-size: 14px; color: var(--text-mid); }
.detail-item-total { font-size: 14px; font-weight: 600; min-width: 80px; text-align: right; }
.detail-summary { border-top: 1px solid var(--border-light); padding: 20px 0; }
.detail-summary-row { display: flex; justify-content: space-between; font-size: 14px; padding: 6px 0; }
.detail-total { font-size: 18px; font-weight: 800; border-top: 1px solid var(--border-light); padding-top: 12px; margin-top: 8px; }
.review-done { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.review-done-comment { font-size: 14px; color: var(--text-mid); margin: 0; }
.review-form-block { display: flex; flex-direction: column; gap: 10px; }
.review-form-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
