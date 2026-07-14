<script setup>
import { computed, onMounted, ref } from 'vue';
import { loyaltyApi } from '@/api';
import { formatDate } from '@/utils/format';

const loyalty = ref({ points: 0, tier: 'Bronze', history: [] });
const error = ref('');
const tiers = [{ name: 'Bronze', at: 0 }, { name: 'Silver', at: 500 }, { name: 'Gold', at: 2000 }];
const nextTier = computed(() => tiers.find((tier) => tier.at > loyalty.value.points));
const progress = computed(() => nextTier.value ? Math.min(100, loyalty.value.points / nextTier.value.at * 100) : 100);

onMounted(async () => {
  try { loyalty.value = await loyaltyApi.getMe(); } catch (e) { error.value = e.message; }
});
</script>

<template>
  <div class="loyalty-page">
    <div class="page-header"><div><h1>Thành viên FastGuy</h1><p>Tích điểm từ các đơn hàng đã giao thành công.</p></div></div>
    <p v-if="error" class="form-error">{{ error }}</p>
    <section class="card loyalty-card"><div><span class="badge badge-primary">{{ loyalty.tier }}</span><div class="points">{{ loyalty.points.toLocaleString('vi-VN') }} <small>điểm</small></div><p v-if="nextTier">Còn {{ (nextTier.at - loyalty.points).toLocaleString('vi-VN') }} điểm để đạt {{ nextTier.name }}</p><p v-else>Bạn đã đạt hạng thành viên cao nhất.</p></div><i class="bi bi-stars"></i><div class="progress"><span :style="{ width: `${progress}%` }"></span></div></section>
    <section class="tiers"><div v-for="tier in tiers" :key="tier.name" class="card tier" :class="{ active: loyalty.tier === tier.name }"><i :class="tier.name === 'Gold' ? 'bi bi-trophy-fill' : 'bi bi-award'" /><strong>{{ tier.name }}</strong><span>{{ tier.at.toLocaleString('vi-VN') }}+ điểm</span></div></section>
    <section class="card history"><h2>Lịch sử điểm</h2><div v-if="loyalty.history?.length" class="history-list"><div v-for="item in loyalty.history" :key="item.transactionId" class="history-item"><div><strong>{{ item.type === 'EARN' ? 'Tích điểm đơn hàng' : 'Điều chỉnh điểm' }}</strong><span>{{ item.orderCode || 'Đơn hàng' }} · {{ formatDate(item.createdAt) }}</span></div><b :class="item.points >= 0 ? 'earn' : 'reverse'">{{ item.points >= 0 ? '+' : '' }}{{ item.points }} điểm</b></div></div><div v-else class="empty-state">Chưa có giao dịch điểm.</div></section>
  </div>
</template>

<style scoped>
.loyalty-page { padding:32px 0; }.page-header h1 { margin:0 0 6px; font-size:26px; }.page-header p,.loyalty-card p { margin:0; color:var(--text-mid); }.loyalty-card { display:grid; grid-template-columns:1fr auto; gap:16px; overflow:hidden; margin:24px 0 16px; background:linear-gradient(135deg,#fff7ed,#fff); }.loyalty-card>i { font-size:58px; color:var(--primary); }.points { margin:8px 0; font-size:36px; font-weight:800; }.points small { font-size:16px; font-weight:600; }.progress { grid-column:1/-1; height:8px; background:var(--border-light); border-radius:99px; overflow:hidden; }.progress span { display:block; height:100%; background:var(--primary); border-radius:inherit; }.tiers { display:grid; grid-template-columns:repeat(3,1fr); gap:12px; margin-bottom:16px; }.tier { display:grid; gap:6px; text-align:center; padding:18px; color:var(--text-mid); }.tier i { font-size:24px; }.tier.active { border-color:var(--primary); color:var(--primary); }.tier strong { color:var(--text-dark); }.tier span { font-size:13px; }.history h2 { margin:0 0 18px; font-size:18px; }.history-list { display:grid; }.history-item { display:flex; justify-content:space-between; align-items:center; gap:16px; padding:14px 0; border-top:1px solid var(--border-light); }.history-item div { display:grid; gap:4px; }.history-item span { color:var(--text-mid); font-size:13px; }.earn { color:var(--green); }.reverse { color:var(--red-active); }.empty-state { color:var(--text-mid); padding:20px 0; }@media(max-width:600px){.tiers{grid-template-columns:1fr}.points{font-size:30px}}
</style>
