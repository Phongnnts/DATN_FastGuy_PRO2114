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
import { useToast } from '@/stores/toast'

const toast = useToast()
const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const productStore = useProductStore()
const order = ref(null)
const loading = ref(true)
const loadError = ref('')
const orderReview = ref(null)
const reviewForm = ref({ rating: 5, comment: '' })
const cancelForm = ref({ reason: '' })
const submitting = ref(false)
const cancelling = ref(false)
const showReviewForm = ref(false)
const showCancelForm = ref(false)
const reordering = ref(false)
const justCreated = ref(route.query.created === '1')

const isDelivered = computed(() => order.value?.status === 'DELIVERED')
const isCancelled = computed(() => order.value?.status === 'CANCELLED')
const canCancel = computed(() => order.value?.status === 'PENDING' || order.value?.status === 'WAITING_STOCK_CONFIRM')
const waitingStockConfirm = computed(() => order.value?.status === 'WAITING_STOCK_CONFIRM')
const paymentLabel = computed(() => ({ PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán', PENDING: 'Đang xử lý', FAILED: 'Thanh toán thất bại', REFUNDED: 'Đã hoàn tiền' })[order.value?.paymentStatus] || order.value?.paymentStatus || 'Chưa thanh toán')
const refundLabel = computed(() => ({ PENDING: 'Đang xử lý', PROCESSING: 'Đang hoàn tiền', COMPLETED: 'Đã hoàn tiền', FAILED: 'Hoàn tiền thất bại' })[order.value?.refundStatus] || order.value?.refundStatus)
let statusPolling = null

onMounted(loadOrder)

async function loadOrder() {
  loading.value = true
  loadError.value = ''
  try {
    const data = await orderApi.getById(route.params.id)
    if (data) {
      order.value = {
        id: data.orderId,
        orderCode: data.orderCode,
        status: data.status,
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
      }
    }
    if (data && data.status === 'DELIVERED') {
      await loadReview(data.orderId)
    }
    startStatusPolling()
  } catch (e) {
    loadError.value = e.message || 'Không thể tải chi tiết đơn hàng'
  } finally {
    loading.value = false
  }
}

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
    orderReview.value = data.reviewed === true ? data.review : null
  } catch {}
}

async function submitReview() {
  if (!order.value) return
  submitting.value = true
  try {
    await reviewApi.create({
      orderId: order.value.id,
      rating: Number(reviewForm.value.rating),
      comment: reviewForm.value.comment.trim() || null,
    })
    await loadReview(order.value.id)
    showReviewForm.value = false
  } catch (e) {
    toast.error(e.message || 'Không thể gửi đánh giá')
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
    if (unavailable.length) toast.error(`Không thể thêm: ${unavailable.join(', ')}. Tùy chọn cũ có thể không còn khả dụng.`)
    if (unavailable.length < order.value.items.length) router.push('/cart')
  } finally {
    reordering.value = false
  }
}

async function cancelOrder() {
  if (!canCancel.value || !order.value) return;
  cancelling.value = true;
  try {
    await orderApi.cancel(order.value.id, { reason: cancelForm.value.reason });
    order.value.status = 'CANCELLED';
    order.value.cancelledBy = 'CUSTOMER';
    if (order.value.statusHistory) {
      order.value.statusHistory.push({
        status: 'CANCELLED',
        time: new Date().toISOString(),
        note: cancelForm.value.reason || 'Khách hủy',
      });
    }
    showCancelForm.value = false;
  } catch (e) {
    toast.error(e.message || 'Không thể hủy đơn hàng');
  } finally {
    cancelling.value = false;
  }
}
</script>

<template>
  <div v-if="loading" class="empty-state detail-state" role="status"><i class="bi bi-arrow-repeat spin" aria-hidden="true"></i><h3>Đang tải đơn hàng...</h3></div>
  <div v-else-if="loadError" class="empty-state detail-state" role="alert"><i class="bi bi-exclamation-circle" aria-hidden="true"></i><h3>{{ loadError }}</h3><button class="btn btn-primary" @click="loadOrder">Thử lại</button></div>
  <div class="order-detail-page" v-else-if="order">
    <div class="card">
      <div v-if="justCreated" class="order-success">
        <i class="bi bi-check-circle-fill"></i> Đặt đơn thành công. Mã đơn: <strong>{{ order.orderCode }}</strong>
      </div>
      <div class="detail-header">
        <div>
          <h3>Đơn hàng {{ order.orderCode }}</h3>
          <p style="color:var(--text-mid);font-size:14px">{{ formatDate(order.createdAt) }}</p>
        </div>
        <OrderStatusBadge :status="order.status" />
      </div>
      <div class="detail-section">
        <h4>Thông tin giao hàng</h4>
        <p><i class="bi bi-geo-alt"></i> {{ order.shippingAddress }}</p>
        <p v-if="order.note"><i class="bi bi-chat-dots"></i> Ghi chú: {{ order.note }}</p>
        <p><i class="bi bi-credit-card"></i> {{ order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : 'Thanh toán PayOS' }}</p>
        <p><i class="bi bi-receipt"></i> Trạng thái thanh toán: {{ paymentLabel }}</p>
      </div>
      <div class="detail-section">
        <h4>Sản phẩm</h4>
        <div v-for="(item, index) in order.items" :key="`${item.productId}-${item.variantId || 'default'}-${index}`" class="detail-item">
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
        <div class="detail-summary-row"><span>Tạm tính</span><span>{{ formatPrice(order.subtotal) }}</span></div>
        <div class="detail-summary-row"><span>Phí giao hàng</span><span>{{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Miễn phí' }}</span></div>
        <div v-if="order.discount > 0" class="detail-summary-row" style="color:var(--red-active)"><span>Giảm giá</span><span>-{{ formatPrice(order.discount) }}</span></div>
        <div class="detail-summary-row detail-total"><span>Tổng cộng</span><span>{{ formatPrice(order.total) }}</span></div>
      </div>
      <div class="detail-section">
        <h4>Trạng thái đơn hàng</h4>
        <OrderTimeline :history="order.statusHistory" />
      </div>

      <div v-if="waitingStockConfirm" class="detail-section" style="text-align:center;padding:24px">
        <i class="bi bi-hourglass-split" style="color:var(--primary);font-size:48px"></i>
        <h4 style="margin-top:8px">Đang chờ cửa hàng xác nhận tồn kho</h4>
        <p style="color:var(--text-mid);font-size:13px">Cửa hàng sẽ kiểm tra và xác nhận đơn hàng của bạn</p>
      </div>

      <div v-if="order.paymentMethod === 'BANK_TRANSFER' && order.checkoutUrl && !isCancelled && order.paymentStatus !== 'PAID'" class="detail-section" style="text-align:center;padding:24px">
        <h4>Thanh toán PayOS</h4>
        <p style="font-size:20px;font-weight:800">{{ formatPrice(order.total) }}</p>
        <a :href="order.checkoutUrl" class="btn btn-primary">Mở trang thanh toán</a>
      </div>

      <div v-if="isDelivered" class="detail-section">
        <h4>Đánh giá đơn hàng</h4>
        <div v-if="orderReview" class="review-done">
          <StarRating :modelValue="orderReview.rating" readonly :size="18" />
          <p v-if="orderReview.comment" class="review-done-comment">{{ orderReview.comment }}</p>
          <span class="badge badge-success">Đã đánh giá</span>
        </div>
        <div v-else-if="showReviewForm" class="review-form-block">
          <StarRating v-model="reviewForm.rating" :size="24" />
          <textarea v-model="reviewForm.comment" class="form-textarea" rows="3" maxlength="1000" placeholder="Chia sẻ cảm nhận về đơn hàng..."></textarea>
          <div class="review-form-actions">
            <button class="btn btn-sm btn-ghost" @click="showReviewForm = false">Hủy</button>
            <button class="btn btn-sm btn-primary" :disabled="submitting" @click="submitReview">
              {{ submitting ? 'Đang gửi...' : 'Gửi đánh giá' }}
            </button>
          </div>
        </div>
        <div v-else>
          <button class="btn btn-outline" @click="showReviewForm = true">
            <i class="bi bi-star"></i> Đánh giá đơn hàng
          </button>
        </div>
      </div>

      <div v-if="isCancelled" class="detail-section">
        <h4>Thông tin hủy đơn</h4>
        <p v-if="order.failureReason"><i class="bi bi-chat-left-text"></i> Lý do: {{ order.failureReason }}</p>
        <p v-if="order.cancelledBy"><i class="bi bi-person-x"></i> Người hủy: {{ order.cancelledBy === 'STAFF' ? 'Nhân viên' : 'Bạn' }}</p>
        <p v-if="order.refundStatus"><i class="bi bi-arrow-return-left"></i> Hoàn tiền: {{ refundLabel }}{{ order.refundNote ? ' - ' + order.refundNote : '' }}</p>
        <p v-if="order.refundAmount !== null"><i class="bi bi-cash-stack"></i> Số tiền hoàn: {{ formatPrice(order.refundAmount) }}</p>
        <p v-if="order.refundedAt"><i class="bi bi-calendar-check"></i> Ngày hoàn: {{ formatDate(order.refundedAt) }}</p>
      </div>
      <div v-if="isDelivered || isCancelled" style="margin-top:16px">
        <button class="btn btn-primary" :disabled="reordering" @click="reorder"><i class="bi bi-cart-plus"></i> {{ reordering ? 'Đang thêm...' : 'Đặt lại đơn' }}</button>
      </div>
      <div v-if="canCancel && !showCancelForm" style="margin-top:16px">
        <button class="btn btn-outline" style="border-color:var(--red-active);color:var(--red-active)" @click="showCancelForm = true">
          <i class="bi bi-x-lg"></i> Hủy đơn hàng
        </button>
      </div>
      <div v-if="showCancelForm" class="detail-section">
        <div class="form-group">
          <label class="form-label">Lý do hủy</label>
          <textarea v-model="cancelForm.reason" class="form-textarea" rows="3" maxlength="500" placeholder="Nhập lý do hủy..."></textarea>
        </div>
        <div style="display:flex;gap:8px;justify-content:flex-end;margin-top:12px">
          <button class="btn btn-outline" @click="showCancelForm = false">Quay lại</button>
          <button class="btn btn-danger" :disabled="!cancelForm.reason.trim() || cancelling" @click="cancelOrder">{{ cancelling ? 'Đang hủy...' : 'Xác nhận hủy' }}</button>
        </div>
      </div>
    </div>
  </div>
  <div v-else class="empty-state" style="padding:60px 0">
    <i class="bi bi-box"></i>
    <h3>{{ loadError || 'Không tìm thấy đơn hàng' }}</h3>
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
.detail-state { min-height: 320px; }.spin { animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) { .order-detail-page { padding: 16px 0; }.detail-header { gap: 12px; }.detail-item { align-items: flex-start; flex-wrap: wrap; }.detail-item-info { min-width: calc(100% - 76px); }.detail-item-total { margin-left: auto; }.review-form-actions, .detail-section .btn { width: 100%; }.review-form-actions .btn { flex: 1; } }
</style>
