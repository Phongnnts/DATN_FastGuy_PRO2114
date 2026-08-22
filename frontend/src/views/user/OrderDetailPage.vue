<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { formatPrice, formatDate } from '@/utils/format'
import { createReorderController, reorderItemKey } from '@/utils/reorderPlanner'
import { createOrderReviewController } from '@/utils/orderReviewController'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import StarRating from '@/components/common/StarRating.vue'
import { orderApi, reviewApi } from '@/api'
import { useCartStore } from '@/stores/cart'
import { useProductStore } from '@/stores/product'
import { useToast } from '@/stores/toast'

const toast = useToast()
const route = useRoute()
const cart = useCartStore()
const productStore = useProductStore()
const order = ref(null)
const loading = ref(true)
const loadError = ref('')
const cancelForm = ref({ reason: '' })
const cancelling = ref(false)
const showCancelForm = ref(false)
const reordering = ref(false)
const reorderResult = ref(null)
const reviewVersion = ref(0)
const justCreated = ref(route.query.created === '1')
let pollTimer = null
let loadingOrder = false
let loadedReviewOrderId = null
let stopped = false

const isDelivered = computed(() => order.value?.status === 'DELIVERED')
const isCancelled = computed(() => order.value?.status === 'CANCELLED')
const canCancel = computed(() => order.value?.status === 'PENDING')
const paymentLabel = computed(() => ({ PAID: 'Đã thanh toán', UNPAID: 'Chưa thanh toán', PENDING: 'Đang xử lý', FAILED: 'Thanh toán thất bại', REFUNDED: 'Đã hoàn tiền' })[order.value?.paymentStatus] || order.value?.paymentStatus || 'Chưa thanh toán')
const refundLabel = computed(() => ({ PENDING: 'Đang xử lý', PROCESSING: 'Đang hoàn tiền', COMPLETED: 'Đã hoàn tiền', FAILED: 'Hoàn tiền thất bại' })[order.value?.refundStatus] || order.value?.refundStatus)
const reviewController = createOrderReviewController({ getByOrder: reviewApi.getByOrder, create: reviewApi.create })
const reviewProducts = computed(() => { reviewVersion.value; return reviewController.products })

onMounted(async () => {
  await loadOrder()
  pollTimer = setInterval(() => {
    if (order.value && !['DELIVERED', 'CANCELLED', 'RETURNED_TO_STORE'].includes(order.value.status)) loadOrder(true)
  }, 30000)
})
onBeforeUnmount(() => {
  stopped = true
  reviewController.stop()
  clearInterval(pollTimer)
})

async function loadOrder(silent = false) {
  if (loadingOrder) return
  loadingOrder = true
  if (!silent) loading.value = true
  if (!silent) loadError.value = ''
  try {
    const data = await orderApi.getById(route.params.id)
    if (stopped) return
    if (data) {
      order.value = {
        id: data.orderId,
        orderCode: data.orderCode,
        status: data.status,
        items: (data.items || []).map(i => ({
          orderItemId: i.orderItemId,
          productId: i.productId,
          variantId: i.variantId || null,
          productName: i.productName,
          variantName: i.variantName || '',
          price: i.unitPrice || 0,
          quantity: i.quantity,
          totalPrice: i.totalPrice || 0,
          image: i.image || '',
          modifiers: Array.isArray(i.modifiers) ? i.modifiers : [],
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
        statusHistory: (data.statusHistory || [{ status: data.status, time: data.createdAt }]).map(entry => ({ status: entry.status, time: entry.time || entry.timestamp })),
        cancelledBy: data.cancelledBy || null,
        refundStatus: data.refundStatus || null,
        refundAmount: data.refundAmount ?? null,
        refundedAt: data.refundedAt || null,
        refundNote: data.refundNote || '',
        failureReason: data.status === 'CANCELLED' ? data.failureReason || '' : '',
        retryScheduledAt: data.retryScheduledAt || null,
        checkoutUrl: data.checkoutUrl || null,
      }
      if (data.status === 'DELIVERED') {
        reviewController.initialize(data.orderId, order.value.items)
        reviewVersion.value += 1
        if (loadedReviewOrderId !== data.orderId) {
          const result = await reviewController.load(data.orderId, order.value.items)
          if (!result?.ignored && !result?.error) loadedReviewOrderId = data.orderId
          reviewVersion.value += 1
        }
      }
    }
  } catch (e) {
    if (!stopped && !silent) loadError.value = e.message || 'Không thể tải chi tiết đơn hàng'
  } finally {
    loadingOrder = false
    if (!stopped && !silent) loading.value = false
  }
}

function reviewState(productId) {
  reviewVersion.value
  return reviewController.stateFor(productId)
}

function editReview(productId, editing) {
  const state = reviewController.stateFor(productId)
  if (!state || state.review || state.submitting) return
  state.status = editing ? 'editing' : 'idle'
  state.error = ''
  reviewVersion.value += 1
}

async function submitReview(productId) {
  const pending = reviewController.submit(productId)
  reviewVersion.value += 1
  const result = await pending
  reviewVersion.value += 1
  return result
}

async function retryReviews() {
  if (!order.value || reviewController.loading) return
  await reviewController.load(order.value.id, order.value.items)
  if (!reviewController.loadError) loadedReviewOrderId = order.value.id
  reviewVersion.value += 1
}

const reorderController = createReorderController({
  fetchProduct: productId => productStore.fetchById(productId),
  addItem: (productId, variantId, quantity, modifiers) => cart.addItem(productId, variantId, quantity, modifiers),
})

async function reorder() {
  if (!order.value || reordering.value) return
  reordering.value = true
  reorderResult.value = null
  try {
    const result = await reorderController.run(order.value.items)
    if (result.ignored) return
    reorderResult.value = result
    if (result.kind === 'success') toast.success(result.message)
    else toast.error(result.message)
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
      <div v-if="order.status === 'DELIVERY_FAILED'" class="detail-section" aria-live="polite">
        <h4>Thông tin giao hàng</h4>
        <p>Giao chưa thành công, cửa hàng đang xử lý</p>
        <p v-if="order.retryScheduledAt">Dự kiến giao lại {{ formatDate(order.retryScheduledAt) }}</p>
      </div>
      <div class="detail-section">
        <h4>Sản phẩm</h4>
        <div v-for="(item, index) in order.items" :key="reorderItemKey(item, index)" class="detail-item">
          <img :src="item.image" :alt="item.productName" class="detail-item-img" />
          <div class="detail-item-info">
            <div class="detail-item-name">{{ item.productName }}</div>
            <div v-if="item.variantName" class="item-variant" style="font-size:12px;color:var(--text-mid)">Kích cỡ: {{ item.variantName }}</div>
            <ul v-if="item.modifiers.length" class="item-modifiers" aria-label="Tùy chọn món">
              <li v-for="modifier in item.modifiers" :key="`${modifier.groupId}:${modifier.modifierOptionId}`">
                {{ modifier.groupName ? `${modifier.groupName}: ` : '' }}{{ modifier.name }}
              </li>
            </ul>
            <div class="detail-item-price">{{ formatPrice(item.price) }}</div>
          </div>
          <div class="detail-item-qty">x{{ item.quantity }}</div>
          <div class="detail-item-total">{{ formatPrice(item.price * item.quantity) }}</div>
        </div>
      </div>
      <div class="detail-summary">
        <div class="detail-summary-row"><span>Tạm tính</span><span>{{ formatPrice(order.subtotal) }}</span></div>
        <div class="detail-summary-row"><span>Phí giao hàng</span><span>{{ formatPrice(order.shippingFee) }}</span></div>
        <div v-if="order.discount > 0" class="detail-summary-row" style="color:var(--red-active)"><span>Giảm giá</span><span>-{{ formatPrice(order.discount) }}</span></div>
        <div class="detail-summary-row detail-total"><span>Tổng cộng</span><span>{{ formatPrice(order.total) }}</span></div>
      </div>
      <div class="detail-section">
        <h4>Trạng thái đơn hàng</h4>
        <OrderTimeline :history="order.statusHistory" />
      </div>

      <div v-if="order.paymentMethod === 'BANK_TRANSFER' && order.checkoutUrl && !isCancelled && order.paymentStatus !== 'PAID'" class="detail-section" style="text-align:center;padding:24px">
        <h4>Thanh toán PayOS</h4>
        <p style="font-size:20px;font-weight:800">{{ formatPrice(order.total) }}</p>
        <a :href="order.checkoutUrl" class="btn btn-primary">Mở trang thanh toán</a>
      </div>

      <div v-if="isDelivered" class="detail-section">
        <h4>Đánh giá sản phẩm</h4>
        <div v-if="reviewController.loadError" class="review-section-error" role="alert">
          <span>{{ reviewController.loadError }}</span>
          <button type="button" class="btn btn-sm btn-outline" :disabled="reviewController.loading" @click="retryReviews">{{ reviewController.loading ? 'Đang thử lại...' : 'Thử lại' }}</button>
        </div>
        <p v-else-if="reviewController.loading" role="status" aria-live="polite">Đang tải trạng thái đánh giá...</p>
        <article v-for="product in reviewProducts" :key="product.productId" class="product-review">
          <div class="product-review-heading">
            <img :src="product.image" :alt="product.productName" class="detail-item-img" />
            <strong>{{ product.productName }}</strong>
          </div>
          <div v-if="reviewState(product.productId)?.review" class="review-done" role="status" aria-live="polite">
            <StarRating :model-value="reviewState(product.productId).review.rating" readonly :size="18" />
            <p v-if="reviewState(product.productId).review.comment">{{ reviewState(product.productId).review.comment }}</p>
            <span class="badge badge-success">Đã đánh giá</span>
          </div>
          <form v-else-if="reviewState(product.productId)?.status === 'editing' || reviewState(product.productId)?.status === 'error' || reviewState(product.productId)?.status === 'submitting'" class="review-form-block" @submit.prevent="submitReview(product.productId)">
            <span :id="`review-rating-label-${product.productId}`" class="form-label">Số sao</span>
            <StarRating v-model="reviewState(product.productId).form.rating" :size="24" :label="`Số sao cho ${product.productName}`" @update:model-value="reviewVersion += 1" />
            <label class="form-label" :for="`review-comment-${product.productId}`">Nhận xét</label>
            <textarea :id="`review-comment-${product.productId}`" v-model="reviewState(product.productId).form.comment" :aria-describedby="`review-counter-${product.productId}`" class="form-textarea" rows="3" maxlength="1000" placeholder="Chia sẻ cảm nhận về sản phẩm..."></textarea>
            <span :id="`review-counter-${product.productId}`" class="review-counter">Còn {{ 1000 - reviewState(product.productId).form.comment.length }} ký tự</span>
            <p v-if="reviewState(product.productId).submitting" role="status" aria-live="polite">Đang gửi đánh giá...</p>
            <p v-if="reviewState(product.productId).error" class="review-error" role="alert">{{ reviewState(product.productId).error }}</p>
            <div class="review-form-actions">
              <button type="button" class="btn btn-sm btn-ghost" :disabled="reviewState(product.productId).submitting" @click="editReview(product.productId, false)">Hủy</button>
              <button type="submit" class="btn btn-sm btn-primary" :disabled="reviewState(product.productId).submitting">{{ reviewState(product.productId).submitting ? 'Đang gửi...' : 'Gửi đánh giá' }}</button>
            </div>
          </form>
          <button v-else type="button" class="btn btn-outline" @click="editReview(product.productId, true)"><i class="bi bi-star" aria-hidden="true"></i> Đánh giá sản phẩm</button>
        </article>
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
        <button class="btn btn-primary" :disabled="reordering" @click="reorder"><i class="bi bi-cart-plus" aria-hidden="true"></i> {{ reordering ? 'Đang thêm...' : 'Đặt lại đơn' }}</button>
        <p v-if="reorderResult" class="reorder-result" role="status" aria-live="polite" aria-atomic="true">{{ reorderResult.message }}</p>
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
.item-modifiers { margin: var(--space-1) 0 0; padding-left: var(--space-4); color: var(--text-mid); font-size: 12px; }
.detail-item-price { font-size: 13px; color: var(--text-mid); }
.detail-item-qty { font-size: 14px; color: var(--text-mid); }
.detail-item-total { font-size: 14px; font-weight: 600; min-width: 80px; text-align: right; }
.detail-summary { border-top: 1px solid var(--border-light); padding: 20px 0; }
.detail-summary-row { display: flex; justify-content: space-between; font-size: 14px; padding: 6px 0; }
.detail-total { font-size: 18px; font-weight: 800; border-top: 1px solid var(--border-light); padding-top: 12px; margin-top: 8px; }
.product-review { padding: 16px 0; border-bottom: 1px solid var(--border-light); }
.product-review:last-child { border-bottom: 0; }
.product-review-heading, .review-done { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.product-review-heading { margin-bottom: 12px; }
.review-form-block { display: flex; flex-direction: column; gap: 10px; }
.review-form-actions { display: flex; justify-content: flex-end; gap: 8px; }
.review-form-actions .btn, .review-section-error .btn, .product-review > .btn { min-height: 44px; }
.review-section-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px; border-radius: var(--radius-sm); background: #fef2f2; color: var(--red-active); }
.review-counter { align-self: flex-end; color: var(--text-mid); font-size: 12px; }
.review-error { color: var(--red-active) !important; }
.detail-state { min-height: 320px; }.spin { animation: spin 1s linear infinite; } @keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 640px) { .order-detail-page { padding: 16px 0; }.detail-header { gap: 12px; }.detail-item { align-items: flex-start; flex-wrap: wrap; }.detail-item-info { min-width: calc(100% - 76px); }.detail-item-total { margin-left: auto; }.review-form-actions, .detail-section .btn { width: 100%; }.review-form-actions .btn { flex: 1; } }
</style>
