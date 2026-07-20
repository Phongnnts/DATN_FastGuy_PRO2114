<script setup>
import { ref, onMounted } from 'vue';
import couponApi from '@/api/coupon';
import { useAuthStore } from '@/stores/auth';
import { useToast } from '@/stores/toast';

const toast = useToast();
const auth = useAuthStore();
const coupons = ref([]);
const loading = ref(true);
const claiming = ref({});
const claimSuccess = ref('');

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
  if (!auth.isLoggedIn) return toast.error('Vui lòng đăng nhập để nhận mã');
  claiming.value[couponId] = true;
  try {
    await couponApi.claim(couponId);
    const c = coupons.value.find(c => c.couponId === couponId);
    if (c) {
      c.isClaimed = true;
      claimSuccess.value = `Đã nhận mã ${c.code}. Bạn có thể dùng tại trang thanh toán.`;
    }
  } catch (e) {
    toast.error(e.message);
  } finally {
    claiming.value[couponId] = false;
  }
}

function typeDesc(c) {
  if (c.type === 'PERCENT') return `Giảm ${c.value}%`;
  if (c.type === 'FIXED') return `Giảm ${c.value?.toLocaleString()}đ`;
  return 'Miễn phí vận chuyển';
}

function typeIcon(c) {
  if (c.type === 'PERCENT') return 'bi-percent';
  if (c.type === 'FIXED') return 'bi-cash-stack';
  return 'bi-truck';
}

onMounted(load);
</script>

<template>
  <div class="promo-page">
    <section class="promo-hero">
      <div class="promo-hero-bg">
        <div class="promo-circle c1"></div>
        <div class="promo-circle c2"></div>
        <div class="promo-circle c3"></div>
      </div>
      <div class="container promo-hero-inner">
        <span class="promo-chip">🎉 Khuyến mãi</span>
        <h1 class="promo-title">Ưu đãi dành cho bạn</h1>
        <p class="promo-desc">Nhận mã giảm giá và khuyến mãi hấp dẫn</p>
      </div>
    </section>

    <section class="promo-body">
      <div class="container">
        <div v-if="claimSuccess" class="claim-success"><i class="bi bi-check-circle-fill"></i>{{ claimSuccess }}</div>
        <div v-if="loading" class="promo-loading">
          <div class="skeleton-grid">
            <div v-for="i in 3" :key="i" class="skeleton-card">
              <div class="skeleton-left skeleton-shine"></div>
              <div class="skeleton-right">
                <div class="skeleton-line w-60 skeleton-shine"></div>
                <div class="skeleton-line w-80 skeleton-shine"></div>
                <div class="skeleton-line w-40 skeleton-shine"></div>
              </div>
            </div>
          </div>
        </div>

        <div v-else-if="coupons.length === 0" class="promo-empty">
          <div class="empty-illustration">
            <span class="empty-emoji">🎁</span>
          </div>
          <h3>Hiện chưa có khuyến mãi</h3>
          <p>Quay lại sau để không bỏ lỡ ưu đãi nhé!</p>
        </div>

        <div v-else class="promo-grid">
          <div
            v-for="c in coupons"
            :key="c.couponId"
            class="promo-card"
            :class="{ claimed: c.isClaimed }"
          >
            <div class="promo-badge">
              <div class="promo-badge-icon">
                <i :class="'bi ' + typeIcon(c)"></i>
              </div>
              <span class="promo-badge-value">
                {{ c.type === 'PERCENT' ? c.value + '%' : c.value?.toLocaleString() + (c.type === 'FIXED' ? 'đ' : '') }}
              </span>
            </div>

            <div class="promo-divider"></div>

            <div class="promo-info">
              <h4 class="promo-name">{{ typeDesc(c) }}</h4>
              <div class="promo-terms">
                <span v-if="c.minOrder > 0">
                  <i class="bi bi-cart3"></i> Đơn từ {{ c.minOrder?.toLocaleString() }}đ
                </span>
                <span v-if="c.maxDiscount">
                  <i class="bi bi-tag"></i> Giảm tối đa {{ c.maxDiscount?.toLocaleString() }}đ
                </span>
                <span v-if="c.expiresAt">
                  <i class="bi bi-clock"></i> HSD: {{ new Date(c.expiresAt).toLocaleDateString('vi-VN') }}
                </span>
              </div>
              <button
                v-if="c.isClaimed"
                class="promo-action claimed"
                disabled
              >
                <i class="bi bi-check-lg"></i> Đã nhận
              </button>
              <button
                v-else
                class="promo-action"
                :disabled="claiming[c.couponId]"
                @click="handleClaim(c.couponId)"
              >
                <template v-if="claiming[c.couponId]">
                  <span class="promo-spinner"></span> Đang nhận...
                </template>
                <template v-else>
                  Nhận ngay <i class="bi bi-arrow-right"></i>
                </template>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
/* ── Page ── */
.claim-success { display: flex; align-items: center; gap: 8px; margin: 18px 0 0; padding: 12px 14px; border-radius: var(--radius-sm); background: #ecfdf5; color: #047857; font-size: 14px; font-weight: 700; }
.promo-page {
  background: var(--bg);
  min-height: 100vh;
}

/* ── Hero ── */
.promo-hero {
  background: linear-gradient(145deg, var(--primary-dark) 0%, var(--primary) 50%, #c9653a 100%);
  padding: 52px 0 48px;
  position: relative;
  overflow: hidden;
}
.promo-hero-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}
.promo-hero-bg::before {
  content: '';
  position: absolute;
  inset: 0;
  background: repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 28px,
    rgba(255,255,255,0.03) 28px,
    rgba(255,255,255,0.03) 56px
  );
}
.promo-circle {
  position: absolute;
  border-radius: 50%;
}
.promo-circle.c1 {
  width: 200px; height: 200px;
  background: rgba(255,255,255,0.06);
  top: -60px; right: -40px;
}
.promo-circle.c2 {
  width: 120px; height: 120px;
  background: rgba(255,255,255,0.08);
  bottom: -30px; left: 10%;
}
.promo-circle.c3 {
  width: 60px; height: 60px;
  background: rgba(255,255,255,0.1);
  top: 20%; left: 60%;
}
.promo-hero-inner {
  position: relative;
  z-index: 1;
  text-align: center;
  color: #fff;
}
.promo-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: rgba(255,255,255,0.15);
  backdrop-filter: blur(8px);
  padding: 6px 18px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 14px;
  border: 1px solid rgba(255,255,255,0.1);
}
.promo-title {
  font-size: 34px;
  font-weight: 900;
  margin: 0 0 10px;
  line-height: 1.15;
}
.promo-desc {
  font-size: 15px;
  opacity: 0.85;
  margin: 0;
}

/* ── Body ── */
.promo-body {
  padding: 36px 0 56px;
}

/* ── Loading ── */
.promo-loading {
  max-width: 800px;
  margin: 0 auto;
}
.skeleton-grid { display: flex; flex-direction: column; gap: 14px; }
.skeleton-card {
  display: flex;
  background: #fff;
  border-radius: var(--radius);
  border: 1px solid var(--border);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.skeleton-left {
  width: 110px;
  min-height: 120px;
  background: #eee;
}
.skeleton-right { flex: 1; padding: 16px; display: flex; flex-direction: column; gap: 10px; }
.skeleton-line { height: 14px; border-radius: 4px; background: #eee; }
.skeleton-line.w-60 { width: 60%; }
.skeleton-line.w-80 { width: 80%; }
.skeleton-line.w-40 { width: 40%; }
.skeleton-shine {
  background: linear-gradient(90deg, #eeeeee 25%, #f5f5f5 50%, #eeeeee 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}
@keyframes shimmer {
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
}

/* ── Empty ── */
.promo-empty {
  text-align: center;
  padding: 56px 0;
}
.empty-illustration {
  width: 80px; height: 80px;
  margin: 0 auto 16px;
  background: var(--primary-light);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-emoji { font-size: 36px; }
.promo-empty h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--text-dark);
}
.promo-empty p {
  font-size: 14px;
  color: var(--text-mid);
  margin: 0;
}

/* ── Card grid ── */
.promo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
  max-width: 820px;
  margin: 0 auto;
}

/* ── Card ── */
.promo-card {
  display: flex;
  background: #fff;
  border-radius: var(--radius-lg);
  border: 1px solid var(--border);
  overflow: hidden;
  transition: all 0.25s;
  box-shadow: var(--shadow-sm);
}
.promo-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
  border-color: var(--primary-dark);
}
.promo-card.claimed {
  opacity: 0.65;
}
.promo-card.claimed:hover {
  border-color: var(--border);
  transform: none;
  box-shadow: var(--shadow-sm);
}

/* ── Badge ── */
.promo-badge {
  width: 110px;
  min-width: 110px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  background: linear-gradient(145deg, var(--primary-dark) 0%, var(--primary) 100%);
  color: #fff;
  position: relative;
  padding: 14px 0;
}
.promo-badge-icon {
  width: 36px; height: 36px;
  background: rgba(255,255,255,0.18);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.promo-badge-value {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.1;
  letter-spacing: -0.3px;
}

/* ── Divider (ticket cut-out) ── */
.promo-divider {
  position: relative;
  width: 14px;
  min-width: 14px;
  background: #fff;
  overflow: hidden;
}
.promo-divider::before,
.promo-divider::after {
  content: '';
  position: absolute;
  left: 50%;
  width: 18px;
  height: 18px;
  background: var(--bg);
  border-radius: 50%;
  transform: translateX(-50%);
}
.promo-divider::before { top: -9px; }
.promo-divider::after { bottom: -9px; }

/* ── Info ── */
.promo-info {
  flex: 1;
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.promo-name {
  font-size: 16px;
  font-weight: 700;
  margin: 0;
  color: var(--text-dark);
}
.promo-terms {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.promo-terms span {
  font-size: 12px;
  color: var(--text-mid);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.promo-terms i { font-size: 12px; }

/* ── Action ── */
.promo-action {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  background: linear-gradient(145deg, var(--primary-dark) 0%, var(--primary) 100%);
  color: #fff;
  margin-top: 2px;
}
.promo-action:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(212,118,74,0.4);
}
.promo-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}
.promo-action.claimed {
  background: var(--bg);
  color: var(--text-mid);
  border: 1px solid var(--border);
  box-shadow: none;
}
.promo-action.claimed:hover {
  transform: none;
  box-shadow: none;
}
.promo-spinner {
  display: inline-block;
  width: 14px; height: 14px;
  border: 2px solid #fff;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ── Responsive ── */
@media (max-width: 640px) {
  .promo-hero { padding: 36px 0 32px; }
  .promo-title { font-size: 26px; }
  .promo-grid { grid-template-columns: 1fr; }
  .promo-card { flex-wrap: wrap; }
  .promo-badge {
    width: 100%;
    min-width: unset;
    flex-direction: row;
    gap: 10px;
    padding: 12px 16px;
  }
  .promo-divider { display: none; }
  .promo-info { padding: 14px 16px; }
  .promo-action { width: 100%; justify-content: center; }
}
</style>
