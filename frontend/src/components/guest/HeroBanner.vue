<script setup>
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <section class="hero">
    <div class="container">
      <div class="hero-layout">
        <div class="hero-content">
          <h1 class="hero-title">Nhanh Ngon</h1>
          <p class="hero-subtitle">Chuẩn Việt Chuẩn Tây</p>
          <p class="hero-desc">
            Hương vị Việt truyền thống + phương tây sẵn sàng và nhanh chóng phục
            vụ tiện lợi cho nhịp sống hiện đại
          </p>
          <div class="hero-actions">
            <button
              class="btn btn-lg btn-primary"
              @click="router.push('/menu')"
            >
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
        <div class="hero-image">
          <img
            src="https://i.pinimg.com/736x/5d/15/d8/5d15d86b4ebb28312d470569ed8711e2.jpg"
            alt=""
            style="border: none; background: none"
          />
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.hero {
  padding: 64px 0 80px;
}
.hero-layout {
  display: flex;
  align-items: center;
  gap: 40px;
}
.hero-content {
  flex: 1;
}
.hero-title {
  font-size: 52px;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 4px;
  letter-spacing: -0.5px;
  color: var(--primary);
}
.hero-subtitle {
  font-size: 20px;
  font-weight: 600;
  color: var(--text);
  margin-bottom: 16px;
}
.hero-desc {
  font-size: 15px;
  color: var(--text-mid);
  line-height: 1.7;
  margin-bottom: 28px;
  max-width: 500px;
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
.hero-image {
  flex-shrink: 0;
  width: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-image img {
  max-width: 100%;
  height: auto;
  border: none;
  background: none;
}
@media (max-width: 768px) {
  .hero {
    padding: 40px 0 48px;
  }
  .hero-layout {
    flex-direction: column;
  }
  .hero-title {
    font-size: 36px;
  }
  .hero-subtitle {
    font-size: 17px;
  }
  .hero-desc {
    font-size: 14px;
  }
  .hero-actions {
    flex-direction: column;
  }
  .hero-stats {
    flex-wrap: wrap;
  }
  .hero-image {
    width: 100%;
    max-width: 300px;
  }
}
</style>
