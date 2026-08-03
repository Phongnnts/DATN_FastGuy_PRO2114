<script setup>
import { onMounted, ref } from 'vue';
import couponApi from '@/api/coupon';
import { useToast } from '@/stores/toast';

const toast = useToast();
const coupons = ref([]);
const loading = ref(true);
const error = ref('');
const copiedId = ref(null);

async function loadCoupons() {
  loading.value = true;
  error.value = '';
  try {
    const data = await couponApi.getClaimed();
    coupons.value = Array.isArray(data) ? data : [];
  } catch {
    coupons.value = [];
    error.value = 'Không thể tải ví mã giảm giá. Vui lòng thử lại.';
  } finally {
    loading.value = false;
  }
}

async function copyCode(coupon) {
  try {
    await navigator.clipboard.writeText(coupon.code);
    copiedId.value = coupon.claimedId;
    setTimeout(() => {
      if (copiedId.value === coupon.claimedId) copiedId.value = null;
    }, 2000);
  } catch {
    toast.error('Không thể sao chép mã.');
  }
}

function valueLabel(coupon) {
  if (coupon.type === 'PERCENT') return `${Number(coupon.value || 0)}%`;
  if (coupon.type === 'FIXED') return `${Number(coupon.value || 0).toLocaleString('vi-VN')}₫`;
  if (coupon.type === 'FREE_SHIPPING') return 'Freeship';
  return 'Ưu đãi';
}

function conditionLabel(coupon) {
  const conditions = [];
  if (Number(coupon.minOrder) > 0) conditions.push(`Đơn từ ${Number(coupon.minOrder).toLocaleString('vi-VN')}₫`);
  if (Number(coupon.maxDiscount) > 0) conditions.push(`Tối đa ${Number(coupon.maxDiscount).toLocaleString('vi-VN')}₫`);
  return conditions.join(' · ') || 'Không yêu cầu giá trị đơn tối thiểu';
}

function claimedLabel(value) {
  if (!value) return '';
  return new Date(value).toLocaleDateString('vi-VN');
}

onMounted(loadCoupons);
</script>

<template>
  <div class="wallet-page">
    <header class="wallet-header">
      <span>Ưu đãi của bạn</span>
      <h1>Ví mã giảm giá</h1>
      <p>Lưu mã, sao chép nhanh và dùng khi đặt món.</p>
    </header>

    <section aria-live="polite">
      <div v-if="loading" class="state-panel" role="status">
        <span class="spinner" aria-hidden="true"></span>
        <strong>Đang tải ví mã</strong>
        <p>Vui lòng chờ trong giây lát.</p>
      </div>

      <div v-else-if="error" class="state-panel" role="alert">
        <i class="bi bi-exclamation-circle" aria-hidden="true"></i>
        <strong>Chưa thể tải ví mã</strong>
        <p>{{ error }}</p>
        <button type="button" class="secondary-button" @click="loadCoupons">Thử lại</button>
      </div>

      <div v-else-if="coupons.length === 0" class="state-panel">
        <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
        <strong>Ví mã đang trống</strong>
        <p>Nhận ưu đãi mới để dùng cho đơn hàng tiếp theo.</p>
        <router-link class="secondary-button" to="/promotions">Khám phá ưu đãi</router-link>
      </div>

      <div v-else class="coupon-group">
        <div class="group-heading">
          <div><span>Khả dụng</span><h2>Mã có thể dùng</h2></div>
          <strong>{{ coupons.length }}</strong>
        </div>

        <div class="coupon-grid">
          <article v-for="coupon in coupons" :key="coupon.claimedId" class="coupon-card">
            <div class="value-block">
              <i class="bi bi-ticket-perforated" aria-hidden="true"></i>
              <strong>{{ valueLabel(coupon) }}</strong>
              <span>Sẵn sàng sử dụng</span>
            </div>
            <div class="coupon-body">
              <button type="button" class="coupon-code" :aria-label="`Sao chép mã ${coupon.code}`" @click="copyCode(coupon)">
                <span>{{ coupon.code }}</span>
                <small><i :class="copiedId === coupon.claimedId ? 'bi bi-check2' : 'bi bi-copy'" aria-hidden="true"></i>{{ copiedId === coupon.claimedId ? 'Đã chép' : 'Sao chép' }}</small>
              </button>
              <h3>{{ coupon.description || 'Ưu đãi cho đơn hàng đủ điều kiện' }}</h3>
              <p class="condition">{{ conditionLabel(coupon) }}</p>
              <p v-if="coupon.claimedAt" class="claimed-at">Đã nhận {{ claimedLabel(coupon.claimedAt) }}</p>
              <div class="actions">
                <router-link class="primary-button" to="/menu">Dùng ngay</router-link>
                <router-link class="text-link" to="/checkout">Đến thanh toán</router-link>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.wallet-page { max-width: 980px; margin: 0 auto; color: var(--text-dark); }
.wallet-header { margin-bottom: 28px; }
.wallet-header > span, .group-heading span { color: var(--primary-dark); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
h1 { margin: 5px 0 7px; font-size: clamp(28px, 5vw, 38px); }
.wallet-header p, .state-panel p { margin: 0; color: var(--text-mid); }
.state-panel { display: grid; min-height: 300px; place-items: center; align-content: center; gap: 10px; padding: 32px; border: 1px solid var(--border); border-radius: 18px; background: #fff; text-align: center; box-shadow: 0 8px 28px rgba(24,39,75,.05); }
.state-panel > i { font-size: 34px; color: var(--primary); }
.state-panel strong { font-size: 18px; }
.spinner { width: 30px; height: 30px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin .7s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.group-heading { display: flex; align-items: end; justify-content: space-between; margin-bottom: 14px; }
.group-heading h2 { margin: 3px 0 0; font-size: 21px; }
.group-heading > strong { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 50%; background: var(--primary-light); color: var(--primary-dark); }
.coupon-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.coupon-card { display: grid; grid-template-columns: 132px minmax(0, 1fr); overflow: hidden; border: 1px solid var(--border); border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgba(24,39,75,.06); }
.value-block { display: grid; align-content: center; justify-items: center; gap: 7px; min-height: 230px; padding: 18px; background: linear-gradient(145deg, var(--primary-light), #fff6f1); color: var(--primary-dark); text-align: center; }
.value-block i { font-size: 25px; }
.value-block strong { font-size: 22px; }
.value-block span { font-size: 11px; font-weight: 700; }
.coupon-body { min-width: 0; padding: 20px; }
.coupon-code { display: flex; width: 100%; align-items: center; justify-content: space-between; gap: 8px; padding: 9px 10px; border: 1px dashed var(--primary); border-radius: 8px; background: var(--primary-light); color: var(--primary-dark); cursor: pointer; }
.coupon-code > span { overflow: hidden; font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-weight: 800; letter-spacing: .04em; text-overflow: ellipsis; }
.coupon-code small { display: inline-flex; flex: none; align-items: center; gap: 4px; font-weight: 700; }
h3 { margin: 15px 0 8px; font-size: 16px; line-height: 1.4; }
.condition, .claimed-at { margin: 0; color: var(--text-mid); font-size: 12px; line-height: 1.5; }
.claimed-at { margin-top: 4px; color: var(--text-light); }
.actions { display: flex; align-items: center; gap: 14px; margin-top: 18px; }
.primary-button, .secondary-button { display: inline-flex; min-height: 40px; align-items: center; justify-content: center; padding: 0 16px; border-radius: 8px; font: inherit; font-size: 13px; font-weight: 700; text-decoration: none; cursor: pointer; }
.primary-button { border: 0; background: var(--primary); color: #fff; }
.secondary-button { margin-top: 5px; border: 1px solid var(--border); background: #fff; color: var(--text-dark); }
.text-link { color: var(--primary-dark); font-size: 12px; font-weight: 700; text-decoration: none; }
.primary-button:hover { background: var(--primary-dark); }
.primary-button:focus-visible, .secondary-button:focus-visible, .coupon-code:focus-visible, .text-link:focus-visible { outline: 3px solid rgba(212,118,74,.3); outline-offset: 2px; }
@media (max-width: 760px) { .coupon-grid { grid-template-columns: 1fr; } }
@media (max-width: 440px) { .coupon-card { grid-template-columns: 1fr; } .value-block { min-height: auto; padding: 18px; } .coupon-body { padding: 18px; } .actions { justify-content: space-between; } }
</style>
