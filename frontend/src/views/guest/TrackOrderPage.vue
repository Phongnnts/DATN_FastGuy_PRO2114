<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useOrderStore } from '@/stores/order';
import { formatDate } from '@/utils/format';
import OrderTimeline from '@/components/common/OrderTimeline.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const route = useRoute();
const router = useRouter();
const orderStore = useOrderStore();
const orderCode = ref('');
const phoneSuffix = ref('');
const trackingResult = ref(null);
const error = ref('');
const loading = ref(false);
const justCreated = ref(route.query.created === '1');

async function track() {
  trackingResult.value = null;
  error.value = '';
  justCreated.value = false;
  if (route.query.created) router.replace({ query: { ...route.query, created: undefined } });
  const code = orderCode.value.trim();
  if (!code) {
    error.value = 'Vui lòng nhập mã đơn hàng.';
    return;
  }
  if (!/^\d{4}$/.test(phoneSuffix.value)) {
    error.value = 'Vui lòng nhập đúng 4 chữ số cuối số điện thoại.';
    return;
  }
  loading.value = true;
  try {
    const result = await orderStore.trackOrder(code, phoneSuffix.value);
    if (!result) error.value = 'Không tìm thấy đơn hàng với thông tin đã nhập.';
    else trackingResult.value = result;
  } catch (e) {
    error.value = e.message || 'Không thể tra cứu đơn hàng. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}

function normalizePhoneInput() {
  phoneSuffix.value = phoneSuffix.value.replace(/\D/g, '').slice(0, 4);
}

onMounted(() => {
  if (route.query.code) orderCode.value = String(route.query.code);
});
</script>

<template>
  <main class="track-page">
    <div class="container track-container">
      <header class="page-header">
        <p class="eyebrow">Theo dõi đơn hàng</p>
        <h1>Tra cứu trạng thái giao hàng</h1>
        <p>Nhập mã đơn và 4 chữ số cuối số điện thoại dùng khi đặt hàng.</p>
      </header>

      <form class="search-card" aria-describedby="security-note" novalidate @submit.prevent="track">
        <div class="field">
          <label for="order-code">Mã đơn hàng</label>
          <input
            id="order-code"
            v-model="orderCode"
            class="form-input"
            type="text"
            autocomplete="off"
            placeholder="Ví dụ: FG-20240201-001"
            :disabled="loading"
          />
          <small>Mã được gửi sau khi đặt hàng thành công.</small>
        </div>
        <div class="field">
          <label for="phone-suffix">4 chữ số cuối số điện thoại</label>
          <input
            id="phone-suffix"
            v-model="phoneSuffix"
            class="form-input"
            type="text"
            inputmode="numeric"
            autocomplete="off"
            maxlength="4"
            placeholder="Ví dụ: 1234"
            :disabled="loading"
            @input="normalizePhoneInput"
          />
          <small id="security-note">Thông tin này giúp xác minh đơn hàng và không được lưu trên thiết bị.</small>
        </div>
        <button class="submit-button" type="submit" :disabled="loading">
          <span v-if="loading" class="spinner" aria-hidden="true"></span>
          {{ loading ? 'Đang tra cứu...' : 'Tra cứu đơn hàng' }}
        </button>
      </form>

      <div v-if="error" class="message error" role="alert">{{ error }}</div>

      <section v-if="trackingResult" class="result" aria-live="polite">
        <div v-if="justCreated" class="message success" role="status">
          Đặt đơn thành công. Hãy lưu mã đơn để tra cứu khi cần.
        </div>
        <div class="result-header">
          <div>
            <span class="section-label">Đơn hàng</span>
            <h2>{{ trackingResult.orderCode }}</h2>
            <p v-if="trackingResult.createdAt">Đặt lúc {{ formatDate(trackingResult.createdAt) }}</p>
          </div>
          <OrderStatusBadge :status="trackingResult.status" />
        </div>

        <div v-if="trackingResult.items.length" class="result-section">
          <h3>Sản phẩm</h3>
          <ul class="item-list">
            <li v-for="(item, index) in trackingResult.items" :key="item.productId || index">
              <img v-if="item.image" :src="item.image" :alt="item.productName" />
              <div class="item-info">
                <strong>{{ item.productName }}</strong>
                <span>Số lượng: {{ item.quantity }}</span>
              </div>
            </li>
          </ul>
        </div>

        <div v-if="trackingResult.statusHistory.length" class="result-section timeline-section">
          <h3>Tiến trình đơn hàng</h3>
          <OrderTimeline :history="trackingResult.statusHistory" />
        </div>
      </section>
    </div>
  </main>
</template>

<style scoped>
.track-page { min-height: 100vh; padding: 48px 0 72px; background: var(--bg); }
.track-container { max-width: 760px; }
.page-header { margin-bottom: 28px; text-align: center; }
.eyebrow, .section-label { display: block; margin: 0 0 8px; color: var(--primary-dark); font-size: 12px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
.page-header h1 { margin: 0 0 10px; font-size: clamp(28px, 5vw, 38px); color: var(--text-dark); }
.page-header > p:last-child { margin: 0; color: var(--text-mid); }
.search-card, .result { border: 1px solid var(--border); border-radius: var(--radius-lg); background: #fff; box-shadow: var(--shadow-sm); }
.search-card { display: grid; grid-template-columns: 1fr 1fr; gap: 18px; padding: 26px; }
.field { display: grid; align-content: start; gap: 7px; }
.field label { font-size: 14px; font-weight: 700; color: var(--text-dark); }
.field small { color: var(--text-light); font-size: 12px; line-height: 1.45; }
.form-input { width: 100%; min-height: 46px; }
.submit-button { display: inline-flex; align-items: center; justify-content: center; gap: 8px; min-height: 46px; padding: 0 20px; border: 0; border-radius: 8px; background: var(--primary); color: #fff; font: inherit; font-size: 14px; font-weight: 700; text-decoration: none; cursor: pointer; }
.search-card > .submit-button { grid-column: 1 / -1; }
.submit-button:hover { background: var(--primary-dark); }
.submit-button:disabled { opacity: .65; cursor: wait; }
.submit-button:focus-visible, .form-input:focus-visible { outline: 3px solid rgba(212, 118, 74, .28); outline-offset: 2px; }
.spinner { width: 16px; height: 16px; border: 2px solid rgba(255,255,255,.45); border-top-color: #fff; border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.message { margin-top: 18px; padding: 13px 16px; border-radius: var(--radius-sm); font-size: 14px; font-weight: 600; }
.error { border: 1px solid #fecaca; background: #fef2f2; color: #b91c1c; }
.success { margin: 0 0 22px; border: 1px solid #a7f3d0; background: #ecfdf5; color: #047857; }
.result { margin-top: 24px; padding: 28px; }
.result-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding-bottom: 24px; border-bottom: 1px solid var(--border); }
.result-header h2 { margin: 0 0 6px; font-size: 22px; }
.result-header p { margin: 0; color: var(--text-mid); font-size: 13px; }
.result-section { padding-top: 24px; }
.result-section h3 { margin: 0 0 16px; font-size: 16px; }
.item-list { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
.item-list li { display: flex; align-items: center; gap: 13px; min-height: 62px; padding: 10px 0; border-bottom: 1px solid var(--border-light); }
.item-list img { width: 48px; height: 48px; border-radius: 8px; object-fit: cover; }
.item-info { display: grid; gap: 4px; }
.item-info strong { font-size: 14px; }
.item-info span { color: var(--text-mid); font-size: 13px; }
.payment-card { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-top: 24px; padding: 18px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: var(--bg); }
.payment-card p { margin: 4px 0 0; color: var(--text-mid); font-size: 13px; }
.timeline-section { margin-top: 24px; border-top: 1px solid var(--border); }
@media (max-width: 640px) {
  .track-page { padding: 34px 0 52px; }
  .search-card { grid-template-columns: 1fr; padding: 20px; }
  .result { padding: 20px; }
  .result-header, .payment-card { align-items: stretch; flex-direction: column; }
  .payment-card .submit-button { width: 100%; }
}
</style>
