<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/order'

const route = useRoute()
const router = useRouter()
const orderStore = useOrderStore()

const order = ref(null)

onMounted(async () => {
  order.value = await orderStore.fetchById(route.params.id)
})
const rating = ref(5)
const content = ref('')
const submitted = ref(false)

async function submit() {
  if (!content.value) return
  await orderStore.reviewOrder(order.value.id, { rating: rating.value, content: content.value })
  submitted.value = true
  setTimeout(() => router.push(`/account/orders/${order.value.id}`), 1500)
}
</script>

<template>
  <div class="review-page" v-if="order && !submitted">
    <div class="card">
      <h3 style="font-size:18px;font-weight:700;margin-bottom:8px">Đánh giá đơn hàng</h3>
      <p style="color:var(--text-mid);font-size:14px;margin-bottom:24px">{{ order.orderCode }}</p>
      <div class="review-items">
        <div v-for="item in order.items" :key="item.productId" class="review-item">
          <img :src="item.image" :alt="item.productName" />
          <span>{{ item.productName }} x{{ item.quantity }}</span>
        </div>
      </div>
      <div class="review-rating-section">
        <label class="form-label">Chất lượng sản phẩm</label>
        <div class="star-input">
          <i v-for="s in 5" :key="s" class="bi bi-star-fill" :class="{ active: s <= rating }" @click="rating = s"></i>
        </div>
      </div>
      <div class="form-group">
        <label class="form-label">Nhận xét</label>
        <textarea v-model="content" class="form-textarea" rows="4" placeholder="Chia sẻ trải nghiệm của bạn..." required></textarea>
      </div>
      <button class="btn btn-primary" @click="submit"><i class="bi bi-send"></i> Gửi đánh giá</button>
    </div>
  </div>
  <div v-else-if="submitted" class="empty-state" style="padding:60px 0">
    <i class="bi bi-check-circle-fill" style="font-size:48px;color:#4CAF50"></i>
    <h3>Cảm ơn bạn đã đánh giá!</h3>
  </div>
</template>

<style scoped>
.review-items { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid var(--border); }
.review-item { display: flex; align-items: center; gap: 8px; background: var(--bg); padding: 8px 12px; border-radius: var(--radius-sm); font-size: 13px; }
.review-item img { width: 36px; height: 36px; border-radius: 8px; object-fit: cover; }
.review-rating-section { margin-bottom: 20px; }
.star-input { display: flex; gap: 4px; margin-top: 8px; }
.star-input i { font-size: 32px; color: #ddd; cursor: pointer; transition: color 0.2s; }
.star-input i.active { color: #f5a623; }
.star-input i:hover { color: #f5a623; }
</style>
