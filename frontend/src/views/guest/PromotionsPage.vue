<script setup>
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import couponApi from '@/api/coupon';
import { useAuthStore } from '@/stores/auth';
import { useToast } from '@/stores/toast';

const route = useRoute();
const auth = useAuthStore();
const toast = useToast();
const coupons = ref([]);
const loading = ref(true);
const error = ref('');
const claiming = ref({});
const claimSuccess = ref({});
const copied = ref({});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    coupons.value = (await couponApi.getPublic()) || [];
  } catch {
    coupons.value = [];
    error.value = 'Không thể tải danh sách ưu đãi. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}

async function handleClaim(coupon) {
  claimSuccess.value[coupon.couponId] = '';
  claiming.value[coupon.couponId] = true;
  try {
    await couponApi.claim(coupon.couponId);
    coupon.isClaimed = true;
    claimSuccess.value[coupon.couponId] = `Đã lưu mã ${coupon.code} vào tài khoản.`;
  } catch (e) {
    toast.error(e.message || 'Không thể nhận mã. Vui lòng thử lại.');
  } finally {
    claiming.value[coupon.couponId] = false;
  }
}

async function copyCode(coupon) {
  try {
    await navigator.clipboard.writeText(coupon.code);
    copied.value[coupon.couponId] = true;
    setTimeout(() => { copied.value[coupon.couponId] = false; }, 2000);
  } catch {
    toast.error('Không thể sao chép mã.');
  }
}

function valueLabel(coupon) {
  if (coupon.type === 'PERCENT') return `${coupon.value}%`;
  if (coupon.type === 'FIXED') return `${Number(coupon.value || 0).toLocaleString('vi-VN')}₫`;
  return 'Freeship';
}

function conditionLabel(coupon) {
  const conditions = [];
  if (coupon.minOrder > 0) conditions.push(`Đơn tối thiểu ${Number(coupon.minOrder).toLocaleString('vi-VN')}₫`);
  if (coupon.maxDiscount) conditions.push(`Giảm tối đa ${Number(coupon.maxDiscount).toLocaleString('vi-VN')}₫`);
  return conditions.length ? conditions.join(' · ') : 'Không yêu cầu giá trị đơn tối thiểu';
}

function expiryLabel(value) {
  if (!value) return 'Không giới hạn';
  return new Date(value).toLocaleDateString('vi-VN');
}

onMounted(load);
</script>

<template>
  <main class="promotions-page">
    <header class="promo-header">
      <div class="container header-inner">
        <p class="eyebrow">Ưu đãi FastGuy</p>
        <h1>Mã giảm giá dành cho bạn</h1>
        <p>Chọn ưu đãi phù hợp, lưu vào tài khoản và áp dụng khi thanh toán.</p>
      </div>
    </header>

    <section class="container promo-content" aria-live="polite">

      <div v-if="loading" class="state-panel" role="status">
        <span class="spinner" aria-hidden="true"></span>
        <strong>Đang tải ưu đãi</strong>
        <span>Vui lòng chờ trong giây lát.</span>
      </div>

      <div v-else-if="error" class="state-panel" role="alert">
        <strong>Chưa thể tải ưu đãi</strong>
        <span>{{ error }}</span>
        <button type="button" class="secondary-button" @click="load">Thử lại</button>
      </div>

      <div v-else-if="coupons.length === 0" class="state-panel">
        <strong>Hiện chưa có ưu đãi</strong>
        <span>Các chương trình mới sẽ được cập nhật tại đây.</span>
        <router-link class="secondary-button" to="/menu">Xem thực đơn</router-link>
      </div>

      <div v-else class="promo-grid">
        <article v-for="coupon in coupons" :key="coupon.couponId" class="promo-card">
          <div class="discount-block">
            <span>Ưu đãi</span>
            <strong>{{ valueLabel(coupon) }}</strong>
          </div>
          <div class="promo-details">
            <div class="code-row">
               <button type="button" class="coupon-code" :aria-label="`Sao chép mã ${coupon.code}`" @click="copyCode(coupon)">{{ coupon.code }} <span>{{ copied[coupon.couponId] ? 'Đã chép' : 'Sao chép' }}</span></button>
              <span v-if="coupon.isClaimed" class="claimed-label">Đã nhận</span>
            </div>
            <h2>{{ coupon.description || 'Ưu đãi cho đơn hàng đủ điều kiện' }}</h2>
            <dl>
              <div><dt>Điều kiện</dt><dd>{{ conditionLabel(coupon) }}</dd></div>
              <div><dt>Hạn dùng</dt><dd>{{ expiryLabel(coupon.expiresAt) }}</dd></div>
            </dl>
             <p v-if="claimSuccess[coupon.couponId]" class="card-success" role="status">{{ claimSuccess[coupon.couponId] }}</p>
             <div class="actions">
               <router-link v-if="coupon.isClaimed" class="primary-button" to="/menu">Chọn món</router-link>
              <router-link
                v-else-if="!auth.isLoggedIn"
                class="primary-button"
                :to="{ path: '/login', query: { redirect: route.fullPath } }"
              >Đăng nhập để nhận</router-link>
              <button
                v-else
                type="button"
                class="primary-button"
                :disabled="claiming[coupon.couponId]"
                @click="handleClaim(coupon)"
              >{{ claiming[coupon.couponId] ? 'Đang nhận...' : 'Nhận mã' }}</button>
            </div>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.promotions-page { min-height: 100vh; background: var(--bg); }
.promo-header { padding: 52px 0; background: linear-gradient(135deg, var(--primary-dark), var(--primary)); color: #fff; }
.header-inner { max-width: 820px; }
.eyebrow { margin: 0 0 10px; font-size: 13px; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; opacity: .82; }
.promo-header h1 { margin: 0 0 12px; font-size: clamp(28px, 5vw, 42px); line-height: 1.15; }
.promo-header p:last-child { max-width: 600px; margin: 0; opacity: .9; }
.promo-content { max-width: 920px; padding-top: 32px; padding-bottom: 64px; }
.notice { margin-bottom: 18px; padding: 13px 16px; border-radius: var(--radius-sm); font-weight: 600; }
.success { border: 1px solid #a7f3d0; background: #ecfdf5; color: #047857; }
.state-panel { display: grid; justify-items: center; gap: 10px; padding: 64px 20px; border: 1px solid var(--border); border-radius: var(--radius-lg); background: #fff; text-align: center; color: var(--text-mid); }
.state-panel strong { font-size: 18px; color: var(--text-dark); }
.spinner { width: 28px; height: 28px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.promo-grid { display: grid; gap: 18px; }
.promo-card { display: grid; grid-template-columns: 170px 1fr; overflow: hidden; border: 1px solid var(--border); border-radius: var(--radius-lg); background: #fff; box-shadow: var(--shadow-sm); }
.discount-block { display: grid; align-content: center; justify-items: center; gap: 6px; min-height: 210px; padding: 24px; background: var(--primary-light); color: var(--primary-dark); }
.discount-block span { font-size: 12px; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
.discount-block strong { font-size: 28px; text-align: center; }
.promo-details { padding: 24px; }
.code-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.coupon-code { padding: 0; border: 0; background: transparent; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: 16px; font-weight: 800; letter-spacing: .06em; color: var(--primary-dark); cursor: pointer; }
.coupon-code span { margin-left: 5px; font-family: inherit; font-size: 10px; letter-spacing: 0; color: var(--text-mid); }
.card-success { margin: 0 0 12px; color: #047857; font-size: 13px; font-weight: 600; }
.claimed-label { padding: 4px 9px; border-radius: 999px; background: #ecfdf5; color: #047857; font-size: 12px; font-weight: 700; }
.promo-details h2 { margin: 12px 0 18px; font-size: 18px; line-height: 1.4; }
dl { display: grid; gap: 9px; margin: 0 0 20px; }
dl div { display: grid; grid-template-columns: 88px 1fr; gap: 12px; font-size: 13px; }
dt { color: var(--text-light); } dd { margin: 0; color: var(--text-mid); }
.actions { display: flex; }
.primary-button, .secondary-button { display: inline-flex; align-items: center; justify-content: center; min-height: 42px; padding: 0 18px; border-radius: 8px; font: inherit; font-size: 14px; font-weight: 700; text-decoration: none; cursor: pointer; }
.primary-button { border: 0; background: var(--primary); color: #fff; }
.primary-button:hover { background: var(--primary-dark); }
.primary-button:disabled { opacity: .6; cursor: wait; }
.secondary-button { margin-top: 8px; border: 1px solid var(--border); background: #fff; color: var(--text-dark); }
.primary-button:focus-visible, .secondary-button:focus-visible { outline: 3px solid rgba(212, 118, 74, .3); outline-offset: 2px; }
@media (max-width: 640px) {
  .promo-header { padding: 38px 0; }
  .promo-content { padding-top: 20px; }
  .promo-card { grid-template-columns: 1fr; }
  .discount-block { min-height: auto; padding: 20px; }
  .promo-details { padding: 20px; }
  dl div { grid-template-columns: 1fr; gap: 2px; }
  .primary-button { width: 100%; }
}
</style>
