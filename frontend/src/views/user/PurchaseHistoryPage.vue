<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import { formatPrice, formatDate } from '@/utils/format';
import { reviewApi } from '@/api';
import StarRating from '@/components/common/StarRating.vue';

const router = useRouter();
const orderStore = useOrderStore();
const reviewsMap = ref({});
const reviewForm = ref({ rating: 5, comment: '' });
const submitting = ref(false);
const editingOrderId = ref(null);
const loading = ref(true);

const deliveredOrders = computed(() =>
  orderStore.userOrders.filter(o => o.status === 'DELIVERED')
);

onMounted(async () => {
  await orderStore.fetchOrders();
  await loadReviews();
  loading.value = false;
});

async function loadReviews() {
  const promises = orderStore.userOrders
    .filter(o => o.status === 'DELIVERED')
    .map(async (order) => {
      try {
        const data = await reviewApi.getByOrder(order.id);
        return { orderId: order.id, review: data };
      } catch {
        return { orderId: order.id, review: null };
      }
    });
  const results = await Promise.all(promises);
  const map = {};
  for (const r of results) {
    if (r.review) map[r.orderId] = r.review;
  }
  reviewsMap.value = map;
}

function startReview(orderId) {
  editingOrderId.value = orderId;
  reviewForm.value = { rating: 5, comment: '' };
}

async function submitReview(orderId) {
  submitting.value = true;
  try {
    await reviewApi.create({
      orderId,
      rating: Number(reviewForm.value.rating),
      comment: reviewForm.value.comment || '',
    });
    const data = await reviewApi.getByOrder(orderId);
    reviewsMap.value = { ...reviewsMap.value, [orderId]: data };
    editingOrderId.value = null;
    reviewForm.value = { rating: 5, comment: '' };
  } catch (e) {
    alert(e.message || 'Không thể gửi đánh giá');
  } finally {
    submitting.value = false;
  }
}

function cancelReview() {
  editingOrderId.value = null;
}

function goDetail(id) {
  router.push(`/account/orders/${id}`);
}
</script>

<template>
  <div class="history-page">
    <div class="card">
      <h3 class="page-title">Lịch sử mua hàng</h3>
      <p class="page-desc">Các đơn đã giao thành công.</p>

      <div v-if="loading" class="empty-state" style="padding:60px 0">
        <i class="bi bi-arrow-repeat spin"></i>
        <h3>Đang tải...</h3>
      </div>

      <div v-else-if="deliveredOrders.length === 0" class="empty-state" style="padding:60px 0">
        <i class="bi bi-clock-history"></i>
        <h3>Chưa có đơn nào đã giao</h3>
        <router-link to="/menu" class="btn btn-primary">Đặt hàng ngay</router-link>
      </div>

      <div v-else class="order-list">
        <div v-for="order in deliveredOrders" :key="order.id" class="order-block">
          <div class="order-head" @click="goDetail(order.id)">
            <div>
              <span class="order-code">{{ order.orderCode }}</span>
              <span class="order-date">{{ formatDate(order.createdAt) }}</span>
            </div>
            <span class="order-total">{{ formatPrice(order.total) }}</span>
          </div>

          <div class="items-preview">
            <div v-for="item in order.items" :key="item.productId" class="item-chip">
              <img :src="item.image" :alt="item.productName" class="item-img" />
              <div>
                <div class="item-name">{{ item.productName }}</div>
                <div v-if="item.variantName" class="item-variant">{{ item.variantName }}</div>
              </div>
            </div>
          </div>

          <div v-if="reviewsMap[order.id]" class="review-done">
            <div class="review-done-inner">
              <StarRating :modelValue="reviewsMap[order.id].rating" readonly :size="16" />
              <span class="done-comment">{{ reviewsMap[order.id].comment || 'Ngon, sẽ ủng hộ tiếp.' }}</span>
              <span class="badge badge-success">Đã đánh giá</span>
            </div>
          </div>
          <div v-else-if="editingOrderId === order.id" class="review-edit">
            <StarRating v-model="reviewForm.rating" :size="22" />
            <textarea v-model="reviewForm.comment" class="form-textarea" rows="2" maxlength="1000" placeholder="Chia sẻ cảm nhận về đơn hàng..."></textarea>
            <div class="review-edit-actions">
              <button class="btn btn-sm btn-ghost" @click="cancelReview">Hủy</button>
              <button class="btn btn-sm btn-primary" :disabled="submitting" @click="submitReview(order.id)">
                {{ submitting ? 'Đang gửi...' : 'Gửi đánh giá' }}
              </button>
            </div>
          </div>
          <div v-else>
            <button class="btn btn-sm btn-outline" @click="startReview(order.id)">
              <i class="bi bi-star"></i> Đánh giá
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title { font-size: 18px; font-weight: 700; margin-bottom: 4px; }
.page-desc { font-size: 14px; color: var(--text-mid); margin-bottom: 24px; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.order-list { display: flex; flex-direction: column; gap: 16px; }
.order-block { border: 2px solid var(--border); border-radius: var(--radius); padding: 16px; }
.order-head { display: flex; justify-content: space-between; align-items: center; cursor: pointer; margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid var(--border); }
.order-head:hover .order-code { color: var(--primary); }
.order-code { font-size: 14px; font-weight: 700; display: block; }
.order-date { font-size: 12px; color: var(--text-light); }
.order-total { font-size: 16px; font-weight: 800; }
.items-preview { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.item-chip { display: flex; align-items: center; gap: 8px; background: var(--bg); padding: 6px 10px; border-radius: var(--radius-sm); }
.item-img { width: 32px; height: 32px; border-radius: 4px; object-fit: cover; }
.item-name { font-size: 13px; font-weight: 600; }
.item-variant { font-size: 11px; color: var(--text-mid); }
.review-done-inner { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; padding-top: 8px; border-top: 1px solid var(--border); }
.done-comment { font-size: 13px; color: var(--text-mid); }
.review-edit { border-top: 1px solid var(--border); padding-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.review-edit-actions { display: flex; justify-content: flex-end; gap: 8px; }
</style>
