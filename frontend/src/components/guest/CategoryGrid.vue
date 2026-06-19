<script setup>
import { useRouter } from 'vue-router';

const props = defineProps({
  categories: { type: Array, required: true },
});

const router = useRouter();

function goCategory(slug) {
  router.push({ path: '/menu', query: { category: slug } });
}
</script>

<template>
  <section class="section">
    <div class="container">
      <div class="section-header">
        <h2 class="section-title">Danh mục</h2>
        <router-link to="/menu" class="section-all"
          >Xem tất cả <i class="bi bi-arrow-right"></i
        ></router-link>
      </div>
      <div class="cat-grid">
        <div
          v-for="cat in categories"
          :key="cat.id"
          class="cat-card"
          @click="goCategory(cat.slug)"
        >
          <div class="cat-name">{{ cat.name }}</div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.section {
  padding: 24px 0;
}
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.section-title {
  font-size: 18px;
  font-weight: 700;
}
.section-all {
  font-size: 14px;
  color: var(--primary);
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}
.cat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 8px;
}
.cat-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.15s;
}
.cat-card:hover {
  border-color: var(--primary);
}
.cat-name {
  font-size: 13px;
  font-weight: 500;
}
</style>
