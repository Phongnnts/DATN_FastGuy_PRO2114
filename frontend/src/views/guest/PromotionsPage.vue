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
  <div class="promo-page">
    <section class="promo-hero">
      <div class="container">
        <div class="promo-hero-content">
          <span class="promo-hero-badge">Ưu đãi hot</span>
          <h1>Khuyến mãi</h1>
          <p>Nhận mã giảm giá và ưu đãi hấp dẫn mỗi ngày</p>
        </div>
      </div>
    </section>

    <section class="promo-body">
      <div class="container">
        <div v-if="loading" class="promo-loading">
          <div class="spinner"></div>
        </div>

        <div v-else-if="coupons.length === 0" class="promo-empty">
          <div class="promo-empty-icon"><i class="bi bi-gift"></i></div>
          <h3>Hiện chưa có khuyến mãi</h3>
          <p>Quay lại sau để không bỏ lỡ ưu đãi nhé!</p>
        </div>

        <div v-else class="promo-grid">
          <div v-for="c in coupons" :key="c.couponId" class="promo-card" :class="{ claimed: c.isClaimed }">
            <div class="promo-card-badge">
              <span class="promo-card-value">{{ c.type === 'PERCENT' ? c.value + '%' : c.value?.toLocaleString() + (c.type === 'FIXED' ? 'đ' : '') }}</span>
              <span class="promo-card-label">{{ typeLabel(c.type) }}</span>
            </div>
            <div class="promo-card-body">
              <h4 class="promo-card-title">{{ typeDesc(c) }}</h4>
              <div class="promo-card-meta">
                <span v-if="c.minOrder > 0"><i class="bi bi-cart3"></i> Đơn từ {{ c.minOrder?.toLocaleString() }}đ</span>
                <span v-if="c.maxDiscount"><i class="bi bi-tag"></i> Tối đa {{ c.maxDiscount?.toLocaleString() }}đ</span>
                <span v-if="c.expiresAt"><i class="bi bi-clock"></i> {{ new Date(c.expiresAt).toLocaleDateString('vi') }}</span>
              </div>
            </div>
            <div class="promo-card-action">
              <button
                v-if="c.isClaimed"
                class="promo-btn claimed"
                disabled
              >
                <i class="bi bi-check-lg"></i> Đã nhận
              </button>
              <button
                v-else
                class="promo-btn"
                :disabled="claiming[c.couponId]"
                @click="handleClaim(c.couponId)"
              >
                <span v-if="claiming[c.couponId]" class="spinner-sm"></span>
                <span v-else><i class="bi bi-ticket-perforated"></i> Nhận ngay</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.promo-page {
  background: var(--bg);
  min-height: 100vh;
}
.promo-hero {
  background: linear-gradient(135deg, var(--primary) 0%, #e8632a 50%, #d4764a 100%);
  padding: 48px 0 40px;
  position: relative;
  overflow: hidden;
}
.promo-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    45deg,
    transparent,
    transparent 20px,
    rgba(255,255,255,0.03) 20px,
    rgba(255,255,255,0.03) 40px
  );
}
.promo-hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
}
.promo-hero-badge {
  display: inline-block;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(4px);
  padding: 4px 14px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 12px;
}
.promo-hero-content h1 {
  font-size: 32px;
  font-weight: 900;
  margin: 0 0 8px;
}
.promo-hero-content p {
  font-size: 15px;
  opacity: 0.85;
  margin: 0;
}
.promo-body {
  padding: 32px 0 48px;
}
.promo-loading {
  text-align: center;
  padding: 60px 0;
}
.promo-empty {
  text-align: center;
  padding: 60px 0;
  color: var(--text-mid);
}
.promo-empty-icon i {
  font-size: 56px;
  color: var(--primary-light);
  margin-bottom: 16px;
}
.promo-empty h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--text-dark);
}
.promo-empty p {
  font-size: 14px;
  margin: 0;
}
.promo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
  max-width: 900px;
  margin: 0 auto;
}
.promo-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  padding: 0;
  transition: all 0.25s;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  gap: 0;
}
.promo-card:hover {
  box-shadow: var(--shadow);
  transform: translateY(-3px);
}
.promo-card.claimed {
  opacity: 0.7;
}
.promo-card-badge {
  background: linear-gradient(135deg, var(--primary) 0%, #e8632a 100%);
  color: #fff;
  padding: 16px 14px;
  text-align: center;
  min-width: 90px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
  align-self: stretch;
}
.promo-card-badge::after {
  content: '';
  position: absolute;
  right: -8px;
  top: 0;
  bottom: 0;
  width: 16px;
  background: radial-gradient(circle at 0 50%, transparent 5px, #fff 5.5px);
  background-size: 16px 16px;
  background-repeat: repeat-y;
}
.promo-card-value {
  font-size: 22px;
  font-weight: 800;
  line-height: 1.1;
}
.promo-card-label {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 2px;
}
.promo-card-body {
  flex: 1;
  padding: 14px 18px;
  min-width: 0;
}
.promo-card-title {
  font-size: 15px;
  font-weight: 700;
  margin: 0 0 8px;
  color: var(--text-dark);
}
.promo-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.promo-card-meta span {
  font-size: 12px;
  color: var(--text-mid);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.promo-card-meta i {
  font-size: 12px;
}
.promo-card-action {
  padding: 14px 16px 14px 8px;
  flex-shrink: 0;
}
.promo-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  background: linear-gradient(135deg, var(--primary) 0%, #e8632a 100%);
  color: #fff;
  white-space: nowrap;
}
.promo-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(212, 118, 74, 0.35);
}
.promo-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.promo-btn.claimed {
  background: var(--bg);
  color: var(--text-mid);
  border: 1px solid var(--border);
}
.spinner-sm {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 600px) {
  .promo-hero { padding: 36px 0 28px; }
  .promo-hero-content h1 { font-size: 26px; }
  .promo-grid { grid-template-columns: 1fr; }
  .promo-card { flex-wrap: wrap; }
  .promo-card-badge { min-width: 80px; padding: 12px 10px; }
  .promo-card-badge::after { display: none; }
  .promo-card-value { font-size: 18px; }
  .promo-card-body { padding: 12px 14px; }
  .promo-card-action { padding: 0 14px 14px; width: 100%; }
  .promo-btn { width: 100%; justify-content: center; }
}
</style>
