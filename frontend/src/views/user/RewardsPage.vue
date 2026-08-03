<script setup>
import LoyaltyWallet from '@/components/common/LoyaltyWallet.vue';
</script>

<template>
  <main class="rewards-page">
    <header><span>Thành viên FastGuy</span><h1>Ví điểm thưởng</h1><p>Theo dõi hạng thành viên, số điểm và lịch sử tích điểm.</p></header>
    <article class="panel"><LoyaltyWallet /></article>
  </main>
</template>

<style scoped>
.rewards-page { max-width:900px; margin:0 auto; color:var(--text-dark); }
header { margin-bottom:24px; }
header span { color:var(--primary-dark); font-size:11px; font-weight:800; letter-spacing:.12em; text-transform:uppercase; }
h1 { margin:4px 0 6px; font-size:clamp(26px,4vw,36px); }
p { margin:0; color:var(--text-mid); }
.panel { padding:24px; border:1px solid var(--border-light); border-radius:16px; background:#fff; box-shadow:0 8px 28px rgba(24,39,75,.06); }
@media (max-width:560px) { .panel { padding:18px; } }
</style>
