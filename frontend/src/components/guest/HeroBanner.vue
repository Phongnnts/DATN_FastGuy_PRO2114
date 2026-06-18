<script setup>
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <section class="hero">
    <div class="container">
      <div class="hero-content">
        <h1 class="hero-title">
          Đồ ăn ngon,<br />giao tận <span class="text-primary">nơi bạn</span>
        </h1>
        <p class="hero-sub">
          Từ bếp nóng đến tay bạn trong 30 phút. Đặt ngay hôm nay!
        </p>
        <div class="hero-actions">
          <button class="btn btn-lg btn-primary" @click="router.push('/menu')">
            <i class="bi bi-search"></i> Khám phá thực đơn
          </button>
          <button class="btn btn-lg" @click="router.push('/track-order')">
            <i class="bi bi-truck"></i> Tra cứu đơn
          </button>
        </div>
        <div class="hero-stats">
          <span class="hero-stat">30 phút giao hàng</span>
          <span class="hero-stat-dot">·</span>
          <span class="hero-stat">200+ món ngon</span>
          <span class="hero-stat-dot">·</span>
          <span class="hero-stat">10k+ khách hàng</span>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  padding: 64px 0 80px;
}
.hero-content {
  max-width: 640px;
}
.hero-title {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.15;
  margin-bottom: 12px;
  letter-spacing: -0.5px;
}
.hero-sub {
  font-size: 16px;
  color: var(--text-mid);
  line-height: 1.6;
  margin-bottom: 28px;
}
.hero-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 32px;
}
.hero-stats {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-mid);
}
.hero-stat-dot {
  color: var(--text-light);
}
@media (max-width: 768px) {
  .hero {
    padding: 40px 0 48px;
  }
  .hero-title {
    font-size: 30px;
  }
  .hero-sub {
    font-size: 15px;
  }
  .hero-actions {
    flex-direction: column;
  }
  .hero-stats {
    flex-wrap: wrap;
  }
}
</style>
