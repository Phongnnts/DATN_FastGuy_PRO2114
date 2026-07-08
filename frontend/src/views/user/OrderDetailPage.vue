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
const orderReview = ref(null)
const reviewForm = ref({ rating: 5, comment: '' })
const submitting = ref(false)
const showReviewForm = ref(false)

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
      await loadReview(data.orderId)
    }
  } catch {}
})

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

      <div v-if="isDelivered" class="detail-section">
        <h4>Đánh giá đơn hàng</h4>
        <div v-if="orderReview" class="review-done">
          <StarRating :modelValue="orderReview.rating" readonly :size="18" />
          <p class="review-done-comment">{{ orderReview.comment || 'Ngon, sẽ ủng hộ tiếp.' }}</p>
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
.review-done { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.review-done-comment { font-size: 14px; color: var(--text-mid); margin: 0; }
.review-form-block { display: flex; flex-direction: column; gap: 10px; }
.review-form-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
