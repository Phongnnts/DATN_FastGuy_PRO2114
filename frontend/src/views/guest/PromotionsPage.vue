<script setup>
import { computed, onMounted, ref } from 'vue';
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
const activeFilter = ref('ALL');
const filters = [
  { key: 'ALL', label: 'Tất cả' },
  { key: 'DISCOUNT', label: 'Giảm giá' },
  { key: 'FREE_SHIPPING', label: 'Freeship' },
  { key: 'EXPIRING', label: 'Sắp hết hạn' },
];

const filteredCoupons = computed(() => coupons.value.filter(coupon => {
  if (activeFilter.value === 'DISCOUNT') return coupon.type === 'PERCENT' || coupon.type === 'FIXED';
  if (activeFilter.value === 'FREE_SHIPPING') return coupon.type === 'FREE_SHIPPING';
  if (activeFilter.value === 'EXPIRING') return isExpiring(coupon);
  return true;
}));

async function load() {
  loading.value = true;
  error.value = '';
  try { coupons.value = (await couponApi.getPublic()) || []; }
  catch { coupons.value = []; error.value = 'Không thể tải danh sách ưu đãi. Vui lòng thử lại.'; }
  finally { loading.value = false; }
}

async function handleClaim(coupon) {
  claimSuccess.value[coupon.couponId] = '';
  claiming.value[coupon.couponId] = true;
  try {
    await couponApi.claim(coupon.couponId);
    coupon.isClaimed = true;
    claimSuccess.value[coupon.couponId] = `Đã lưu mã ${coupon.code} vào tài khoản.`;
  } catch (e) { toast.error(e.message || 'Không thể nhận mã. Vui lòng thử lại.'); }
  finally { claiming.value[coupon.couponId] = false; }
}

async function copyCode(coupon) {
  try {
    await navigator.clipboard.writeText(coupon.code);
    copied.value[coupon.couponId] = true;
    setTimeout(() => { copied.value[coupon.couponId] = false; }, 2000);
  } catch { toast.error('Không thể sao chép mã.'); }
}

function valueLabel(coupon) {
  if (coupon.type === 'PERCENT') return `${Number(coupon.value || 0)}%`;
  if (coupon.type === 'FIXED') return `${Number(coupon.value || 0).toLocaleString('vi-VN')}₫`;
  return 'Freeship';
}
function valueKicker(coupon) { return coupon.type === 'FREE_SHIPPING' ? 'Miễn phí' : 'Giảm'; }
function conditionLabel(coupon) {
  const conditions = [];
  if (Number(coupon.minOrder) > 0) conditions.push(`Đơn tối thiểu ${Number(coupon.minOrder).toLocaleString('vi-VN')}₫`);
  if (Number(coupon.maxDiscount) > 0) conditions.push(`Tối đa ${Number(coupon.maxDiscount).toLocaleString('vi-VN')}₫`);
  return conditions.length ? conditions.join(' · ') : 'Không yêu cầu giá trị đơn tối thiểu';
}
function expiryLabel(value) {
  if (!value) return 'Không giới hạn';
  return new Date(value).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
}
function isExpiring(coupon) {
  if (!coupon.expiresAt) return false;
  const remaining = new Date(coupon.expiresAt).getTime() - Date.now();
  return remaining > 0 && remaining <= 7 * 24 * 60 * 60 * 1000;
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
      <nav v-if="!loading && !error && coupons.length" class="promo-filters" aria-label="Lọc ưu đãi">
        <button v-for="filter in filters" :key="filter.key" type="button" :class="{ active: activeFilter === filter.key }" :aria-pressed="activeFilter === filter.key" @click="activeFilter = filter.key">{{ filter.label }}</button>
      </nav>

      <div v-if="loading" class="state-panel" role="status"><span class="spinner" aria-hidden="true"></span><strong>Đang tải ưu đãi</strong><span>Vui lòng chờ trong giây lát.</span></div>
      <div v-else-if="error" class="state-panel" role="alert"><strong>Chưa thể tải ưu đãi</strong><span>{{ error }}</span><button type="button" class="secondary-button" @click="load">Thử lại</button></div>
      <div v-else-if="coupons.length === 0" class="state-panel"><strong>Hiện chưa có ưu đãi</strong><span>Các chương trình mới sẽ được cập nhật tại đây.</span><router-link class="secondary-button" to="/menu">Xem thực đơn</router-link></div>
      <div v-else-if="filteredCoupons.length === 0" class="state-panel compact"><strong>Chưa có ưu đãi phù hợp</strong><span>Chọn bộ lọc khác để xem thêm mã.</span></div>

      <div v-else class="promo-grid">
        <article v-for="coupon in filteredCoupons" :key="coupon.couponId" class="promo-card" :class="{ claimed: coupon.isClaimed }">
          <div class="discount-block">
            <span>{{ valueKicker(coupon) }}</span>
            <strong :class="`value-${coupon.type.toLowerCase()}`">{{ valueLabel(coupon) }}</strong>
          </div>
          <span class="ticket-notch notch-top" aria-hidden="true"></span><span class="ticket-notch notch-bottom" aria-hidden="true"></span>
          <div class="promo-details">
            <div class="code-row">
              <button type="button" class="coupon-copy" :aria-label="`Sao chép mã ${coupon.code}`" @click="copyCode(coupon)"><strong>{{ coupon.code }}</strong><span><i :class="copied[coupon.couponId] ? 'bi bi-check2' : 'bi bi-copy'" aria-hidden="true"></i>{{ copied[coupon.couponId] ? 'Đã sao chép' : 'Sao chép' }}</span></button>
              <span v-if="coupon.isClaimed" class="status-badge saved">Đã lưu</span><span v-else-if="isExpiring(coupon)" class="status-badge expiring">Sắp hết hạn</span>
            </div>
            <h2>{{ coupon.description || 'Ưu đãi cho đơn hàng đủ điều kiện' }}</h2>
            <p class="coupon-meta">{{ conditionLabel(coupon) }}</p>
            <div class="coupon-bottom">
              <span class="expiry">HSD {{ expiryLabel(coupon.expiresAt) }}</span>
              <router-link v-if="coupon.isClaimed" class="primary-button saved-action" to="/menu">Chọn món <i class="bi bi-arrow-right" aria-hidden="true"></i></router-link>
              <router-link v-else-if="!auth.isLoggedIn" class="primary-button" :to="{ name: 'Login', query: { redirect: route.fullPath } }">Nhận mã <i class="bi bi-arrow-right" aria-hidden="true"></i></router-link>
              <button v-else type="button" class="primary-button" :disabled="claiming[coupon.couponId]" @click="handleClaim(coupon)">{{ claiming[coupon.couponId] ? 'Đang nhận...' : 'Nhận mã' }} <i v-if="!claiming[coupon.couponId]" class="bi bi-arrow-right" aria-hidden="true"></i></button>
            </div>
            <p v-if="claimSuccess[coupon.couponId]" class="card-success" role="status">{{ claimSuccess[coupon.couponId] }}</p>
          </div>
        </article>
      </div>
    </section>
  </main>
</template>

<style scoped>
.promotions-page{min-height:100vh;background:#faf8f6}.promo-header{padding:42px 0;color:#fff;background:linear-gradient(135deg,#b64520,#e8734a)}.header-inner{max-width:1180px}.eyebrow{margin:0 0 8px;font-size:11px;font-weight:900;letter-spacing:.14em;text-transform:uppercase;opacity:.86}.promo-header h1{margin:0 0 10px;font-size:clamp(30px,4vw,44px);line-height:1.08;letter-spacing:-.035em}.promo-header p:last-child{max-width:620px;margin:0;color:rgba(255,255,255,.86)}.promo-content{max-width:1180px;padding-top:24px;padding-bottom:72px}.promo-filters{display:flex;gap:8px;overflow-x:auto;margin-bottom:20px;padding-bottom:2px;scrollbar-width:none}.promo-filters button{flex:0 0 auto;min-height:42px;padding:8px 16px;border:1px solid #eee7e2;border-radius:999px;color:#74645b;background:#fff;font-size:12px;font-weight:750}.promo-filters button.active{border-color:#e8734a;color:#fff;background:#e8734a}.state-panel{display:grid;min-height:260px;justify-items:center;align-content:center;gap:10px;padding:40px 20px;border:1px solid #eee7e2;border-radius:16px;color:#74645b;background:#fff;text-align:center}.state-panel.compact{min-height:180px}.state-panel strong{color:#211b18;font-size:18px}.spinner{width:28px;height:28px;border:3px solid #eee7e2;border-top-color:#e8734a;border-radius:50%;animation:spin .7s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}.promo-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:18px}.promo-card{position:relative;display:grid;grid-template-columns:minmax(112px,24%) minmax(0,1fr);min-height:190px;overflow:hidden;border:1px solid #eee7e2;border-radius:16px;background:#fff;box-shadow:0 8px 24px rgba(45,31,24,.045)}.promo-card.claimed{border-color:#cfe8dc}.discount-block{display:grid;min-width:0;align-content:center;justify-items:center;gap:5px;padding:14px 8px;color:#a43e1d;background:#fff3ed}.discount-block span{font-size:10px;font-weight:900;letter-spacing:.13em;text-transform:uppercase}.discount-block strong{display:block;max-width:100%;font-size:clamp(25px,3vw,38px);font-weight:950;line-height:.95;text-align:center;letter-spacing:-.04em;overflow-wrap:anywhere}.discount-block .value-fixed{font-size:clamp(19px,1.8vw,24px);letter-spacing:-.025em;white-space:nowrap}.discount-block .value-free_shipping{font-size:clamp(18px,1.65vw,22px);letter-spacing:-.025em;white-space:nowrap}.promo-details{display:flex;min-width:0;flex-direction:column;padding:18px 18px 15px;border-left:1px dashed #e4d6ce}.ticket-notch{position:absolute;left:24%;z-index:2;width:18px;height:18px;border:1px solid #eee7e2;border-radius:50%;background:#faf8f6;transform:translateX(-50%)}.notch-top{top:-10px}.notch-bottom{bottom:-10px}.code-row{display:flex;align-items:flex-start;justify-content:space-between;gap:8px}.coupon-copy{display:inline-flex;min-width:0;align-items:center;gap:9px;padding:6px 9px;border:1px dashed #e5cfc4;border-radius:9px;color:#8b3217;background:#fff9f5;cursor:pointer}.coupon-copy>strong{overflow:hidden;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px;letter-spacing:.04em;text-overflow:ellipsis;white-space:nowrap}.coupon-copy>span{display:flex;align-items:center;gap:4px;color:#8b7569;font-size:9px;font-weight:700;white-space:nowrap}.status-badge{flex:0 0 auto;padding:5px 8px;border-radius:999px;font-size:9px;font-weight:850}.status-badge.saved{color:#087454;background:#e8f7f1}.status-badge.expiring{color:#a55d0b;background:#fff3d6}.promo-details h2{margin:13px 0 7px;font-size:17px;font-weight:850;line-height:1.25;letter-spacing:-.02em}.coupon-meta{margin:0;color:#75665e;font-size:11px;line-height:1.5}.coupon-bottom{display:flex;align-items:center;justify-content:space-between;gap:10px;margin-top:auto;padding-top:13px}.expiry{color:#9a887e;font-size:10px;font-weight:700}.primary-button,.secondary-button{display:inline-flex;min-height:38px;align-items:center;justify-content:center;gap:6px;padding:0 13px;border-radius:9px;font:inherit;font-size:11px;font-weight:850;text-decoration:none;cursor:pointer}.primary-button{border:0;color:#fff;background:#e8734a}.primary-button:hover{background:#b64520}.primary-button.saved-action{color:#1d6d53;background:#e8f7f1}.primary-button:disabled{cursor:wait;opacity:.6}.secondary-button{margin-top:6px;border:1px solid #ddd1ca;color:#211b18;background:#fff}.card-success{margin:9px 0 0;color:#087454;font-size:10px;font-weight:700}.primary-button:focus-visible,.secondary-button:focus-visible,.coupon-copy:focus-visible,.promo-filters button:focus-visible{outline:3px solid rgba(232,115,74,.28);outline-offset:2px}@media(max-width:820px){.promo-grid{grid-template-columns:1fr}}@media(max-width:520px){.promo-header{padding:34px 0}.promo-content{padding-top:18px}.promo-card{grid-template-columns:104px minmax(0,1fr);min-height:184px}.discount-block{padding:13px}.discount-block strong{font-size:25px}.promo-details{padding:14px 13px 12px}.ticket-notch{left:104px}.code-row{align-items:flex-start;flex-direction:column}.status-badge{position:absolute;top:12px;right:12px}.coupon-copy{max-width:calc(100% - 55px)}.promo-details h2{margin-top:10px;font-size:15px}.coupon-bottom{align-items:flex-end}.expiry{max-width:90px}.primary-button{min-height:40px}}@media(prefers-reduced-motion:reduce){.spinner{animation:none}}
</style>
