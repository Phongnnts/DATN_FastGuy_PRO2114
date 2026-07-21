<script setup>
defineProps({ categories: { type: Array, required: true } });
</script>

<template>
  <section class="section">
    <div class="container">
      <div class="section-head"><h2>Danh mục</h2><router-link to="/menu" class="see-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link></div>
      <div v-if="categories.length" class="cat-grid"><router-link v-for="category in categories" :key="category.id" :to="{ path: '/menu', query: { category: category.id } }" class="cat-card"><img :src="category.image" alt="" loading="lazy" decoding="async"><span class="cat-name">{{ category.name }}</span><i class="bi bi-arrow-right cat-arrow"></i></router-link></div>
      <p v-else class="section-empty">Danh mục đang được cập nhật.</p>
    </div>
  </section>
</template>

<style scoped>
.section { padding:48px 0; }.section-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:20px; }.section-head h2 { font-size:22px; font-weight:700; letter-spacing:-.025em; }.see-all { display:flex; align-items:center; gap:5px; color:var(--primary); font-size:14px; font-weight:700; }.cat-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(130px,1fr)); gap:10px; }.cat-card { display:grid; grid-template-columns:56px 1fr auto; align-items:center; gap:12px; min-height:82px; padding:12px; border:1px solid color-mix(in srgb,var(--primary) 14%,transparent); border-radius:20px; color:var(--text-dark); background:linear-gradient(135deg,#fff,#fff5ed); box-shadow:var(--shadow-xs); transition:border-color var(--transition-fast),background var(--transition-fast),transform var(--transition-fast); }.cat-card img { width:48px; height:48px; border-radius:14px; object-fit:cover; }.cat-card:hover { transform:translateY(-2px); }.cat-card:hover,.cat-card:focus-visible { border-color:var(--primary); background:var(--primary-50); outline:none; }.cat-name { font-size:13px; font-weight:650; }.cat-arrow { color:var(--text-light); font-size:12px; }.cat-card:hover .cat-arrow,.cat-card:focus-visible .cat-arrow { color:var(--primary); transform:translateX(3px); }.section-empty { margin:0; color:var(--text-mid); }
</style>
