<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { shipperApi } from '@/api';
import { useShipperStore } from '@/stores/shipper';
import { formatPrice } from '@/utils/format';
import { toLocalDateKey } from '@/api/shift';

const store = useShipperStore();
const loading = ref(false);
const error = ref('');
const codOrders = ref([]);
const today = ref(toLocalDateKey(new Date()));
let generation = 0;
let stopped = false;

const dashboard = computed(() => store.dashboard);
const todayCodCollected = computed(() => dashboard.value?.todayCodCollected || 0);
const pendingCodCollected = computed(() => dashboard.value?.pendingCodCollected || 0);

const mapOrder = o => ({ id: o.orderId ?? o.id, orderCode: o.orderCode || '', status: o.status || '', customerName: o.customerName || '', paymentMethod: o.paymentMethod || '', deliveredAt: o.deliveredAt || null, codCollectedAmount: o.codCollectedAmount == null ? null : Number(o.codCollectedAmount) });

function daysAgo(days) {
  const d = new Date();
  d.setDate(d.getDate() - days);
  return toLocalDateKey(d);
}

async function load() {
  const requestGeneration = ++generation;
  loading.value = true;
  error.value = '';
  today.value = toLocalDateKey(new Date());
  try {
    const [, history] = await Promise.all([
      store.fetchDashboard(true),
      shipperApi.getHistory({ page: 1, size: 100, fromDate: daysAgo(7), toDate: today.value }),
    ]);
    if (stopped || requestGeneration !== generation) return;
    const items = (Array.isArray(history) ? history : history?.items || []).map(mapOrder);
    codOrders.value = items.filter(order => order.paymentMethod === 'COD' && order.status === 'DELIVERED' && String(order.deliveredAt || '').startsWith(today.value));
  } catch (e) {
    if (!stopped && requestGeneration === generation) error.value = e?.response?.data?.message || e?.message || 'Không thể tải dữ liệu';
  } finally {
    if (!stopped && requestGeneration === generation) loading.value = false;
  }
}

onMounted(load);
onUnmounted(() => { stopped = true; generation += 1; store.invalidateListRequests(); });
</script>

<template>
  <div>
    <div class="page-header"><h1><i class="bi bi-cash-coin"></i> Đối soát COD</h1></div>
    <div v-if="loading" class="state">Đang tải...</div>
    <div v-else-if="error" class="state error" role="alert"><p>{{ error }}</p><button class="btn btn-outline btn-sm" @click="load">Thử lại</button></div>
    <template v-else>
      <div class="cash-stats">
        <div class="cash-card today"><small>Tổng thu hôm nay</small><strong>{{ formatPrice(todayCodCollected) }}</strong></div>
        <div class="cash-card pending"><small>COD đang giữ</small><strong>{{ formatPrice(pendingCodCollected) }}</strong><em class="hint">Chưa có luồng nộp tiền — tạm tính bằng tổng thu hôm nay</em></div>
      </div>
      <section class="cash-list">
        <h2>Đơn COD đã giao hôm nay</h2>
        <div v-if="!codOrders.length" class="state">Chưa có đơn COD nào được giao hôm nay.</div>
        <ul v-else>
          <li v-for="order in codOrders" :key="order.id" class="cod-order">
            <div class="cod-info"><strong>{{ order.orderCode }}</strong><small>{{ order.customerName }}</small></div>
            <strong>{{ formatPrice(order.codCollectedAmount) }}</strong>
          </li>
        </ul>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page-header { margin-bottom: 14px; }.page-header h1 { font-size: 18px; }
.cash-stats { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-bottom: 12px; }
.cash-card { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 14px; text-align: center; }
.cash-card small, .cash-card strong { display: block; }.cash-card small { color: var(--text-mid); font-size: 11px; }.cash-card strong { font-size: 20px; margin-top: 4px; }.cash-card.today strong { color: var(--primary); }.cash-card.pending strong { color: #92400e; }.hint { display: block; font-style: normal; font-size: 10px; color: var(--text-light); margin-top: 4px; }
.cash-list { background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); padding: 14px; }
.cash-list h2 { color: var(--text-mid); font-size: 12px; text-transform: uppercase; margin-bottom: 10px; }
.cash-list ul { list-style: none; margin: 0; padding: 0; }
.cod-order { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 0; border-top: 1px solid var(--border-light); }
.cod-order:first-child { border-top: 0; }
.cod-info { display: flex; flex-direction: column; gap: 2px; }
.cod-info small { color: var(--text-mid); font-size: 12px; }
.state { text-align: center; padding: 24px; color: var(--text-mid); }.error { color: var(--red-active); }
</style>
