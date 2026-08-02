<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <section class="card history-info" aria-labelledby="history-title">
    <i class="bi bi-clock-history" aria-hidden="true"></i>
    <h1 id="history-title">Lịch sử mua hàng đã được gộp</h1>
    <p>Xem đơn đã giao và toàn bộ trạng thái tại trang đơn hàng.</p>
    <button class="btn btn-primary" @click="router.replace('/account/orders')">Xem đơn hàng</button>
  </section>
</template>

<style scoped>
.history-info { margin: 32px 0; padding: 56px 24px; display: flex; flex-direction: column; align-items: center; gap: 12px; text-align: center; }
.history-info i { font-size: 40px; color: var(--primary); }
.history-info h1 { font-size: 20px; }
.history-info p { color: var(--text-mid); }
</style>
