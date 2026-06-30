<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { ref, onMounted } from 'vue';
import { staffApi } from '@/api';

const auth = useAuthStore();
const router = useRouter();
const sidebarOpen = ref(false);
const sidebarHover = ref(false);
let hoverTimer = null;
const pendingCount = ref(0);

onMounted(async () => {
  try {
    const orders = await staffApi.getOrders();
    pendingCount.value = Array.isArray(orders) ? orders.length : 0;
  } catch {}
});

function showSidebar() {
  clearTimeout(hoverTimer);
  sidebarHover.value = true;
}
function hideSidebar() {
  hoverTimer = setTimeout(() => { sidebarHover.value = false; }, 300);
}
function isVisible() { return sidebarOpen.value || sidebarHover.value; }

function logout() {
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Tổng quan', path: '/staff', icon: 'bi-speedometer2' },
  { label: 'Đơn hàng', path: '/staff/orders', icon: 'bi-receipt' },
  {
    label: 'Lịch sử đơn',
    path: '/staff/orders/history',
    icon: 'bi-clock-history',
  },
  { label: 'Ca làm việc', path: '/staff/shifts', icon: 'bi-calendar-check' },
];
</script>

<template>
  <div class="sidebar-layout" @mouseleave="hideSidebar">
    <div class="sidebar-trigger" @mouseenter="showSidebar"></div>
    <aside class="sidebar" :class="{ open: isVisible() }" @mouseenter="showSidebar" @mouseleave="hideSidebar">
      <div class="sidebar-brand">
        <span class="brand-text" style="font-size: 18px; font-weight: 800"
          >Fast<span style="color: var(--primary)">Guy</span></span
        >
        <div style="font-size: 11px; color: var(--text-mid); margin-top: 1px">
          Staff
        </div>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="link in sidebarLinks"
          :key="link.path"
          :to="link.path"
          @click="sidebarOpen = false"
        >
          <i :class="link.icon"></i>
          <span>{{ link.label }}</span>
          <span v-if="link.path === '/staff/orders' && pendingCount > 0" class="badge badge-warning" style="margin-left:auto">{{ pendingCount }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <div class="user-info">
          <img
            :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'"
            class="user-avatar"
          />
          <div>
            <div class="user-name">{{ auth.user?.fullName }}</div>
            <div class="user-role">Nhân viên</div>
          </div>
        </div>
      </div>
    </aside>
    <div class="main-content">
      <div class="topbar">
        <div class="topbar-left">
          <button
            class="btn btn-ghost"
            @click="sidebarOpen = !sidebarOpen"
            style="font-size: 18px; padding: 4px"
          >
            <i class="bi bi-list"></i>
          </button>
          <h2>Staff Panel</h2>
        </div>
        <div class="topbar-right">
          <router-link to="/" class="btn btn-sm"
            ><i class="bi bi-house"></i> Website</router-link
          >
          <button class="btn btn-sm btn-ghost" @click="logout">
            <i class="bi bi-box-arrow-right"></i> Đăng xuất
          </button>
        </div>
      </div>
      <div class="page-content">
        <router-view />
      </div>
    </div>
  </div>
</template>
