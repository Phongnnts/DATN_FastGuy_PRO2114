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
const reviewForms = ref({});
const submitting = ref({});
const loading = ref(true);

const deliveredOrders = computed(() =>
  orderStore.userOrders.filter(o => o.status === 'DELIVERED')
);

onMounted(async () => {
  await orderStore.fetchOrders();
  await loadReviewsForDelivered();
  loading.value = false;
});

async function loadReviewsForDelivered() {
  const promises = orderStore.userOrders
    .filter(o => o.status === 'DELIVERED')
    .map(async (order) => {
      try {
        const list = await reviewApi.getByOrder(order.id);
        return { orderId: order.id, reviews: list };
      } catch {
        return { orderId: order.id, reviews: [] };
      }
    });
  const results = await Promise.all(promises);
  const map = {};
  for (const r of results) {
    for (const rev of r.reviews) {
      map[`${r.orderId}-${rev.productId}`] = rev;
    }
  }
  reviewsMap.value = map;
}

function getReviewKey(orderId, productId) {
  return `${orderId}-${productId}`;
}

function getForm(orderId, productId) {
  const key = `${orderId}-${productId}`;
  if (!reviewForms.value[key]) {
    reviewForms.value = { ...reviewForms.value, [key]: { rating: 5, comment: '' } };
  }
  return reviewForms.value[key];
}

async function submitReview(orderId, productId) {
  const key = getReviewKey(orderId, productId);
  const form = reviewForms.value[key];
  if (!form) return;
  submitting.value = { ...submitting.value, [key]: true };
  try {
    await reviewApi.create({
      orderId,
      productId,
      rating: Number(form.rating),
      comment: form.comment || '',
    });
    const list = await reviewApi.getByOrder(orderId);
    for (const rev of list) {
      reviewsMap.value = { ...reviewsMap.value, [getReviewKey(orderId, rev.productId)]: rev };
    }
    reviewForms.value = { ...reviewForms.value, [key]: { rating: 5, comment: '' } };
  } catch (e) {
    alert(e.message || 'Không thể gửi đánh giá');
  } finally {
    submitting.value = { ...submitting.value, [key]: false };
  }
}

function goDetail(id) {
  router.push(`/account/orders/${id}`);
}
</script>

<template>
  <div class="history-page">
    <div class="card">
      <h3 class="page-title">Lịch sử mua hàng</h3>
      <p class="page-desc">Các đơn đã giao thành công. Bạn có thể đánh giá từng món bên dưới.</p>

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

          <div class="item-reviews">
            <div v-for="item in order.items" :key="item.productId" class="item-review-row">
              <div class="item-info">
                <img :src="item.image" :alt="item.productName" class="item-img" />
                <div>
                  <div class="item-name">{{ item.productName }}</div>
                  <div v-if="item.variantName" class="item-variant">{{ item.variantName }}</div>
                </div>
              </div>

              <div v-if="reviewsMap[getReviewKey(order.id, item.productId)]" class="review-done">
                <StarRating :modelValue="reviewsMap[getReviewKey(order.id, item.productId)].rating" readonly :size="14" />
                <span class="done-comment">{{ reviewsMap[getReviewKey(order.id, item.productId)].comment || 'Ngon, sẽ ủng hộ tiếp.' }}</span>
                <span class="badge badge-success">Đã đánh giá</span>
              </div>

              <div v-else class="review-form-inline">
                <StarRating v-model="getForm(order.id, item.productId).rating" :size="18" />
                <textarea v-model="getForm(order.id, item.productId).comment" class="form-textarea" rows="2" maxlength="1000" placeholder="Chia sẻ cảm nhận..."></textarea>
                <button class="btn btn-sm btn-primary" :disabled="submitting[getReviewKey(order.id, item.productId)]" @click="submitReview(order.id, item.productId)">
                  {{ submitting[getReviewKey(order.id, item.productId)] ? 'Đang gửi...' : 'Gửi đánh giá' }}
                </button>
              </div>
            </div>
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
.item-reviews { display: flex; flex-direction: column; gap: 12px; }
.item-review-row { border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 12px; }
.item-info { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.item-img { width: 40px; height: 40px; border-radius: var(--radius-sm); object-fit: cover; }
.item-name { font-size: 14px; font-weight: 600; }
.item-variant { font-size: 12px; color: var(--text-mid); }
.review-form-inline { display: flex; flex-direction: column; gap: 8px; }
.review-done { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.done-comment { font-size: 13px; color: var(--text-mid); }
</style>
