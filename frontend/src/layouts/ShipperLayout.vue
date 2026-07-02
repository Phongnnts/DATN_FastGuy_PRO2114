<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter, useRoute } from 'vue-router';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const navItems = [
  { path: '/shipper', name: 'Dashboard', icon: 'bi-house-door' },
  { path: '/shipper/orders', name: 'Đang giao', icon: 'bi-bicycle' },
];

function activeClass(path) {
  return route.path === path ? 'active' : '';
}

function logout() {
  auth.logout();
  router.push('/login');
}
</script>

<template>
  <div class="shipper-layout">
    <header class="shipper-header">
      <div class="shipper-brand">
        <i class="bi bi-truck"></i>
        <span>FastGuy Shipper</span>
      </div>
      <button class="btn btn-sm btn-ghost" @click="logout">
        <i class="bi bi-box-arrow-right"></i>
      </button>
    </header>
    <main class="shipper-main">
      <router-view />
    </main>
    <nav class="shipper-nav">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="shipper-nav-item"
        :class="activeClass(item.path)"
      >
        <i :class="item.icon"></i>
        <span>{{ item.name }}</span>
      </router-link>
    </nav>
  </div>
</template>

<style scoped>
.shipper-layout {
  display: flex;
  flex-direction: column;
  min-height: 100dvh;
  max-width: 480px;
  margin: 0 auto;
  background: #f8f9fa;
}
.shipper-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}
.shipper-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
}
.shipper-brand i {
  font-size: 20px;
  color: var(--primary);
}
.shipper-main {
  flex: 1;
  padding: 12px 16px;
  padding-bottom: 80px;
  overflow-y: auto;
}
.shipper-nav {
  display: flex;
  background: #fff;
  border-top: 1px solid #eee;
  position: fixed;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 480px;
  z-index: 10;
}
.shipper-nav-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0;
  font-size: 11px;
  color: #999;
  text-decoration: none;
  gap: 2px;
}
.shipper-nav-item i {
  font-size: 20px;
}
.shipper-nav-item.active {
  color: var(--primary);
  font-weight: 600;
}
</style>
