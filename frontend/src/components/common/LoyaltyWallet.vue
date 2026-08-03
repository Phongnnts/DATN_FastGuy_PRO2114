<script setup>
import { computed, onMounted, ref } from 'vue';
import { loyaltyApi } from '@/api';
import { formatDate } from '@/utils/format';

defineProps({ compact: Boolean });

const loyalty = ref(null);
const loading = ref(true);
const error = ref('');
const thresholds = { Bronze: 0, Silver: 500, Gold: 2000 };
const nextTier = computed(() => loyalty.value?.tier === 'Bronze' ? 'Silver' : loyalty.value?.tier === 'Silver' ? 'Gold' : null);
const progress = computed(() => {
  if (!loyalty.value || !nextTier.value) return 100;
  const start = thresholds[loyalty.value.tier] || 0;
  return Math.min(100, Math.max(0, ((loyalty.value.points - start) / (thresholds[nextTier.value] - start)) * 100));
});

async function load() {
  loading.value = true;
  error.value = '';
  try {
    loyalty.value = await loyaltyApi.getMe();
  } catch {
    loyalty.value = null;
    error.value = 'Không thể tải thông tin thành viên.';
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<template>
  <section class="wallet" aria-labelledby="wallet-title">
    <div class="wallet-heading"><div><span>Quyền lợi</span><h2 id="wallet-title">Ví điểm thưởng</h2></div><i class="bi bi-award" aria-hidden="true"></i></div>
    <div v-if="loading" class="state" role="status">Đang tải ví điểm...</div>
    <div v-else-if="error" class="state error" role="alert"><span>{{ error }}</span><button type="button" class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <template v-else-if="loyalty">
      <div class="summary"><div><span>Hạng hiện tại</span><strong>{{ loyalty.tier }}</strong></div><div class="points"><strong>{{ Number(loyalty.points || 0).toLocaleString('vi-VN') }}</strong><span>điểm tích lũy</span></div></div>
      <div class="tiers"><span v-for="tier in ['Bronze', 'Silver', 'Gold']" :key="tier" :class="{ active: loyalty.tier === tier }">{{ tier }}<small>{{ thresholds[tier].toLocaleString('vi-VN') }} điểm</small></span></div>
      <div class="progress" role="progressbar" aria-label="Tiến độ hạng thành viên" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="Math.round(progress)"><div :style="{ width: progress + '%' }"></div></div>
      <p class="next">{{ nextTier ? `Còn ${Math.max(0, thresholds[nextTier] - loyalty.points).toLocaleString('vi-VN')} điểm để đạt ${nextTier}` : 'Bạn đã đạt hạng Gold cao nhất.' }}</p>
      <div class="history"><h3>Giao dịch gần đây</h3><div v-for="item in loyalty.history?.slice(0, compact ? 5 : undefined) || []" :key="item.transactionId" class="transaction"><span>{{ item.type === 'EARN' ? `Đơn ${item.orderCode || ''}` : `Hoàn điểm đơn ${item.orderCode || ''}` }}<small>{{ formatDate(item.createdAt) }}</small></span><b :class="{ negative: item.points < 0 }">{{ item.points > 0 ? '+' : '' }}{{ item.points }} điểm</b></div><p v-if="!loyalty.history?.length" class="empty">Chưa có giao dịch điểm.</p></div>
    </template>
  </section>
</template>

<style scoped>
.wallet-heading { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:22px; }
.wallet-heading span { color:var(--primary-dark); font-size:11px; font-weight:800; letter-spacing:.12em; text-transform:uppercase; }
.wallet-heading h2 { margin:3px 0 0; font-size:20px; }
.wallet-heading i { display:grid; width:42px; height:42px; place-items:center; border-radius:50%; background:#fff4d6; color:#a66a00; font-size:22px; }
.summary { display:flex; align-items:flex-end; justify-content:space-between; gap:16px; padding:18px; border-radius:12px; background:#172033; color:#fff; }
.summary div { display:flex; flex-direction:column; gap:4px; }
.summary span { color:#cbd3df; font-size:12px; }
.summary strong { font-size:22px; }
.summary .points { text-align:right; }
.summary .points strong { color:#ffd166; font-size:28px; }
.tiers { display:flex; justify-content:space-between; margin-top:18px; color:var(--text-light); font-size:12px; font-weight:700; }
.tiers span { display:flex; flex-direction:column; gap:2px; }
.tiers span:nth-child(2) { text-align:center; }
.tiers span:last-child { text-align:right; }
.tiers .active { color:var(--primary-dark); }
.tiers small, .next, .transaction small, .empty { color:var(--text-mid); font-size:11px; font-weight:400; }
.progress { height:8px; margin:9px 0 6px; overflow:hidden; border-radius:999px; background:var(--border-light); }
.progress div { height:100%; border-radius:inherit; background:linear-gradient(90deg,#cd7f32,#d4af37); }
.next { margin:0; }
.history { margin-top:18px; padding-top:16px; border-top:1px solid var(--border-light); }
.history h3 { margin:0 0 5px; font-size:14px; }
.transaction { display:flex; justify-content:space-between; gap:12px; padding:10px 0; border-bottom:1px solid var(--border-light); font-size:13px; }
.transaction span { display:flex; flex-direction:column; gap:2px; }
.transaction b { color:var(--success); white-space:nowrap; }
.transaction b.negative, .error { color:var(--red-active); }
.state { display:flex; min-height:130px; align-items:center; justify-content:center; gap:10px; color:var(--text-mid); text-align:center; }
.error { flex-direction:column; }
@media (max-width:560px) { .summary { align-items:flex-start; flex-direction:column; } .summary .points { text-align:left; } }
</style>
