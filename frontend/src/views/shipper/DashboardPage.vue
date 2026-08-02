<script setup>
import { ref, computed, onMounted } from 'vue';
import { useShipperStore } from '@/stores/shipper';
import ShiftStatus from '@/components/common/ShiftStatus.vue';

const shipperStore = useShipperStore();
const loading = ref(true);

const dashboard = computed(() => shipperStore.dashboard);
const assignedCount = computed(() => dashboard.value?.activeCount || 0);

onMounted(async () => {
  await shipperStore.fetchDashboard();
  loading.value = false;
});

</script>

<template>
  <div>
    <div class="shipper-hero map-grid">
      <div class="hero-route-line"></div>
      <div class="hero-stat">
        <span class="hero-kicker">Delivery cockpit</span>
        <span class="hero-number">{{ assignedCount }}</span>
        <span class="hero-label">Đơn được giao</span>
      </div>
    </div>

    <ShiftStatus role="SHIPPER" />

    <section class="delivery-readiness">
      <div>
        <span>Phân công giao hàng</span>
        <strong>{{ assignedCount ? `${assignedCount} đơn đang phụ trách` : 'Chưa có đơn được phân công' }}</strong>
      </div>
      <i :class="assignedCount ? 'bi bi-box-seam-fill' : 'bi bi-hourglass-split'"></i>
    </section>

    <div v-if="dashboard" class="shipper-stats">
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.todayDelivered || 0 }}</span>
        <span class="mini-label">Đã giao hôm nay</span>
      </div>
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.activeCount || 0 }}</span>
        <span class="mini-label">Đang giao</span>
      </div>
      <div class="mini-stat">
        <span class="mini-num">{{ dashboard.totalDelivered || 0 }}</span>
        <span class="mini-label">Tổng đã giao</span>
      </div>
    </div>

    <div v-if="loading" class="shipper-empty">Đang tải...</div>
  </div>
</template>

<style scoped>
.shipper-hero {
  position: relative;
  overflow: hidden;
  background:
    radial-gradient(circle at 12% 15%, rgba(255,183,3,0.32), transparent 24%),
    linear-gradient(135deg, #1f130d, var(--primary-dark));
  color: #fff;
  border-radius: var(--radius-lg);
  padding: 30px 24px;
  text-align: center;
  margin-bottom: 12px;
  box-shadow: 0 18px 40px rgba(232,115,74,0.26);
}
.hero-route-line {
  position: absolute;
  left: 22px;
  right: 22px;
  top: 24px;
  height: 84px;
  border: 3px dashed rgba(255,255,255,0.35);
  border-bottom: 0;
  border-left: 0;
  border-radius: 0 80px 0 0;
  transform: rotate(-6deg);
}
.hero-route-line::before,
.hero-route-line::after {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 0 0 5px rgba(255,255,255,0.14);
}
.hero-route-line::before { left: -6px; bottom: -6px; }
.hero-route-line::after { right: -6px; top: -6px; }
.hero-kicker {
  display: block;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  opacity: 0.72;
}
  .delivery-readiness { align-items: center; background: #fff; border: 1px solid var(--border-light); border-radius: var(--radius); display: flex; justify-content: space-between; margin-bottom: 12px; padding: 13px 14px; }
  .delivery-readiness span { color: var(--text-mid); display: block; font-size: 11px; font-weight: 700; letter-spacing: .05em; text-transform: uppercase; }
  .delivery-readiness strong { display: block; font-size: 14px; margin-top: 3px; }
  .delivery-readiness i { color: var(--primary); font-size: 24px; }
  .shipper-stats {
    display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-bottom: 14px;
}
.mini-stat {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: 12px;
  text-align: center;
}
.mini-num { font-size: 22px; font-weight: 800; display: block; color: var(--primary); }
.mini-label { font-size: 11px; color: var(--text-mid); margin-top: 2px; }
.hero-number { font-size: 44px; font-weight: 800; display: block; }
.hero-label { font-size: 14px; opacity: 0.9; }
.shipper-empty { text-align: center; padding: 40px 0; color: var(--text-mid); }
.order-cards { display: flex; flex-direction: column; gap: 10px; }
.order-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  padding: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.order-card:hover { transform: translateY(-1px); box-shadow: var(--shadow-sm); }
.card-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-code { font-size: 14px; }
.order-status.pending {
  background: #fef3c7;
  color: #92400e;
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}
.card-body p {
  font-size: 13px;
  color: var(--text-mid);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.card-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--border-light);
}
.order-total { font-size: 17px; font-weight: 800; }
.order-time { font-size: 12px; color: var(--text-light); }
</style>
