<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { formatPrice, formatDate } from '@/utils/format'
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue'
import OrderTimeline from '@/components/common/OrderTimeline.vue'
import StarRating from '@/components/common/StarRating.vue'
import { orderApi, reviewApi } from '@/api'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const reviewsByProduct = ref({})
const reviewForms = ref({})
const submitting = ref(false)

const isDelivered = computed(() => order.value?.status === 'DELIVERED')

onMounted(async () => {
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
        discount: 0,
        total: data.finalAmount || 0,
        paymentMethod: data.paymentMethod,
        paymentStatus: data.paymentStatus,
        shippingAddress: data.customerAddress || '',
        note: data.deliveryNote || '',
        createdAt: data.createdAt,
        statusHistory: data.statusHistory || [{ status: data.status, time: data.createdAt, note: '' }],
      }
    }
    if (data && data.status === 'DELIVERED') {
      await loadOrderReviews(data.orderId)
    }
  } catch {}
})

async function loadOrderReviews(orderId) {
  try {
    const list = await reviewApi.getByOrder(orderId)
    const map = {}
    for (const r of list) {
      map[r.productId] = r
    }
    reviewsByProduct.value = map
  } catch {}
}

function getReviewForm(productId) {
  if (!reviewForms.value[productId]) {
    reviewForms.value = { ...reviewForms.value, [productId]: { rating: 5, comment: '' } }
  }
  return reviewForms.value[productId]
}

async function submitReview(item) {
  submitting.value = true
  const form = reviewForms.value[item.productId]
  if (!form) return
  try {
    await reviewApi.create({
      orderId: order.value.id,
      productId: item.productId,
      rating: Number(form.rating),
      comment: form.comment || '',
    })
    await loadOrderReviews(order.value.id)
    reviewForms.value = { ...reviewForms.value, [item.productId]: { rating: 5, comment: '' } }
  } catch (e) {
    alert(e.message || 'Không thể gửi đánh giá')
  } finally {
    submitting.value = false
  }
}

async function cancelOrder() {
  if (!confirm('Bạn có chắc muốn hủy đơn hàng này?')) return;
  try {
    await orderApi.cancel(order.value.id);
    order.value.status = 'CANCELLED';
    if (order.value.statusHistory) {
      order.value.statusHistory.push({
        status: 'CANCELLED',
        time: new Date().toISOString(),
        note: 'Khách hủy',
      });
    }
  } catch (e) {
    alert(e.message || 'Không thể hủy đơn hàng');
  }
}
</script>

<template>
  <div class="order-detail-page" v-if="order">
    <div class="card">
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
        <p><i class="bi bi-credit-card"></i> {{ order.paymentMethod === 'COD' ? 'Thanh toán khi nhận hàng' : order.paymentMethod }}</p>
        <p><i class="bi bi-receipt"></i> Trạng thái: {{ order.paymentStatus === 'PAID' ? 'Đã thanh toán' : 'Chưa thanh toán' }}</p>
      </div>
      <div class="detail-section">
        <h4>Sản phẩm</h4>
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
      <div v-if="isDelivered" class="detail-section">
        <h4>Đánh giá sản phẩm</h4>
        <div v-for="item in order.items" :key="item.productId" class="review-item-block">
          <div class="review-item-header">
            <img :src="item.image" :alt="item.productName" class="review-item-img" />
            <span class="review-item-name">{{ item.productName }}</span>
          </div>
          <template v-if="reviewsByProduct[item.productId]">
            <div class="review-done">
              <StarRating :modelValue="reviewsByProduct[item.productId].rating" readonly :size="16" />
              <p class="review-done-comment">{{ reviewsByProduct[item.productId].comment || 'Ngon, sẽ ủng hộ tiếp.' }}</p>
              <span class="badge badge-success">Đã đánh giá</span>
            </div>
          </template>
          <template v-else>
            <div class="review-form-inline">
              <StarRating v-model="getReviewForm(item.productId).rating" :size="20" />
              <textarea v-model="getReviewForm(item.productId).comment" class="form-textarea" rows="2" maxlength="1000" placeholder="Chia sẻ cảm nhận..."></textarea>
              <button class="btn btn-sm btn-primary" :disabled="submitting" @click="submitReview(item)">
                {{ submitting ? 'Đang gửi...' : 'Gửi đánh giá' }}
              </button>
            </div>
          </template>
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
      <div v-if="order.status === 'PENDING'" style="margin-top:16px">
        <button class="btn btn-outline" style="border-color:var(--red-active);color:var(--red-active)" @click="cancelOrder">
          <i class="bi bi-x-lg"></i> Hủy đơn hàng
        </button>
      </div>
    </div>
  </div>
  <div v-else class="empty-state" style="padding:60px 0">
    <i class="bi bi-box"></i>
    <h3>Không tìm thấy đơn hàng</h3>
  </div>
</template>

<style scoped>
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.detail-header h3 { font-size: 18px; font-weight: 700; }
.detail-section { border-top: 1px solid var(--border); padding: 20px 0; }
.detail-section h4 { font-size: 15px; font-weight: 700; margin-bottom: 12px; }
.detail-section p { font-size: 14px; color: var(--text-mid); margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.detail-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid var(--border); }
.detail-item:last-child { border-bottom: none; }
.detail-item-img { width: 56px; height: 56px; border-radius: var(--radius-sm); object-fit: cover; }
.detail-item-info { flex: 1; }
.detail-item-name { font-size: 14px; font-weight: 600; }
.detail-item-price { font-size: 13px; color: var(--text-mid); }
.detail-item-qty { font-size: 14px; color: var(--text-mid); }
.detail-item-total { font-size: 14px; font-weight: 600; min-width: 80px; text-align: right; }
.detail-summary { border-top: 1px solid var(--border); padding: 20px 0; }
.detail-summary-row { display: flex; justify-content: space-between; font-size: 14px; padding: 6px 0; }
.detail-total { font-size: 18px; font-weight: 800; border-top: 1px solid var(--border); padding-top: 12px; margin-top: 8px; }
.review-box { background: var(--bg); padding: 16px; border-radius: var(--radius-sm); }
.review-stars i { color: #ddd; margin-right: 2px; }
.review-stars i.active { color: #f5a623; }
.review-item-block { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px; margin-bottom: 12px; }
.review-item-header { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.review-item-img { width: 40px; height: 40px; border-radius: var(--radius-sm); object-fit: cover; }
.review-item-name { font-size: 14px; font-weight: 600; }
.review-form-inline { display: flex; flex-direction: column; gap: 8px; }
.review-done { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.review-done-comment { font-size: 13px; color: var(--text-mid); margin: 0; }
</style>
