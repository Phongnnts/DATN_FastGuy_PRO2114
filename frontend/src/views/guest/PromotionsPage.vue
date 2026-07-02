<script setup>
import { ref, onMounted } from 'vue';
import couponApi from '@/api/coupon';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();
const coupons = ref([]);
const loading = ref(true);
const claiming = ref({});

async function load() {
  loading.value = true;
  try {
    coupons.value = await couponApi.getPublic() || [];
  } catch {
    coupons.value = [];
  } finally {
    loading.value = false;
  }
}

async function handleClaim(couponId) {
  if (!auth.isLoggedIn) return alert('Vui lòng đăng nhập để nhận mã');
  claiming.value[couponId] = true;
  try {
    await couponApi.claim(couponId);
    const c = coupons.value.find(c => c.couponId === couponId);
    if (c) c.isClaimed = true;
  } catch (e) {
    alert(e.response?.data?.message || e.message);
  } finally {
    claiming.value[couponId] = false;
  }
}

function typeLabel(t) {
  return { PERCENT: '%', FIXED: 'đ', FREE_SHIPPING: 'Free ship' }[t] || t;
}

function typeDesc(c) {
  if (c.type === 'PERCENT') return `Giảm ${c.value}%`;
  if (c.type === 'FIXED') return `Giảm ${c.value?.toLocaleString()}đ`;
  return 'Miễn phí vận chuyển';
}

onMounted(load);
</script>

<template>
  <div class="promotions-page">
    <div class="container">
      <div class="page-header text-center">
        <h1>Khuyến mãi</h1>
        <p>Nhận mã giảm giá và ưu đãi hấp dẫn</p>
      </div>

      <div v-if="loading" class="text-center py-5"><div class="spinner"></div></div>

      <div v-else-if="coupons.length === 0" class="empty-state">
        <i class="bi bi-gift"></i>
        <h4>Hiện chưa có khuyến mãi</h4>
      </div>

      <div v-else class="coupon-grid">
        <div v-for="c in coupons" :key="c.couponId" class="coupon-card">
          <div class="coupon-left">
            <div class="coupon-value">{{ c.type === 'PERCENT' ? c.value + '%' : c.value?.toLocaleString() }}</div>
            <div class="coupon-unit">{{ typeLabel(c.type) }}</div>
          </div>
          <div class="coupon-body">
            <div class="coupon-desc">{{ typeDesc(c) }}</div>
            <div class="coupon-meta">
              <span v-if="c.minOrder > 0">Đơn từ {{ c.minOrder?.toLocaleString() }}đ</span>
              <span v-if="c.maxDiscount">Giảm tối đa {{ c.maxDiscount?.toLocaleString() }}đ</span>
              <span v-if="c.expiresAt">HSD: {{ new Date(c.expiresAt).toLocaleDateString('vi') }}</span>
            </div>
          </div>
          <div class="coupon-action">
            <button
              v-if="c.isClaimed"
              class="btn btn-sm btn-outline" disabled
            >Đã nhận</button>
            <button
              v-else
              class="btn btn-sm btn-primary"
              :disabled="claiming[c.couponId]"
              @click="handleClaim(c.couponId)"
            >{{ claiming[c.couponId] ? '...' : 'Nhận ngay' }}</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.promotions-page { padding: 40px 0; }
.page-header { margin-bottom: 32px; }
.empty-state { text-align: center; padding: 60px 0; color: var(--text-mid); }
.empty-state i { font-size: 48px; display: block; margin-bottom: 16px; }
.coupon-grid { display: flex; flex-direction: column; gap: 12px; max-width: 600px; margin: 0 auto; }
.coupon-card { display: flex; align-items: center; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); overflow: hidden; }
.coupon-left { background: var(--primary); color: #fff; padding: 16px; text-align: center; min-width: 100px; display: flex; flex-direction: column; justify-content: center; }
.coupon-value { font-size: 24px; font-weight: 800; line-height: 1; }
.coupon-unit { font-size: 12px; opacity: .8; margin-top: 4px; }
.coupon-body { flex: 1; padding: 12px 16px; }
.coupon-desc { font-weight: 600; margin-bottom: 4px; }
.coupon-meta { display: flex; flex-wrap: wrap; gap: 8px; font-size: 12px; color: var(--text-mid); }
.coupon-action { padding: 12px 16px; }
</style>
