<script setup>
import { useAuthStore } from '@/stores/auth';
import { useRouter } from 'vue-router';
import { ref } from 'vue';

const auth = useAuthStore();
const router = useRouter();
const sidebarOpen = ref(false);

function logout() {
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  { label: 'Tổng quan', path: '/admin', icon: 'bi-speedometer2' },
  { label: 'Người dùng', path: '/admin/users', icon: 'bi-people' },
  { label: 'Sản phẩm', path: '/admin/products', icon: 'bi-box-seam' },
  { label: 'Danh mục', path: '/admin/categories', icon: 'bi-tags' },
  // { label: 'Khu vực', path: '/admin/delivery-zones', icon: 'bi-geo-alt' },
  { label: 'Đơn hàng', path: '/admin/orders', icon: 'bi-receipt' },
  { label: 'Ca làm việc', path: '/admin/shifts', icon: 'bi-calendar-check' },
  { label: 'Doanh thu', path: '/admin/reports/revenue', icon: 'bi-graph-up' },
  { label: 'Bán chạy', path: '/admin/reports/top-products', icon: 'bi-trophy' },
];
</script>

<template>
  <div class="sidebar-layout">
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="brand-text" style="font-size: 18px; font-weight: 800"
          >Fast<span style="color: var(--primary)">Guy</span></span
        >
        <div style="font-size: 11px; color: var(--text-mid); margin-top: 1px">
          Admin
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
            <div class="user-role">Quản trị viên</div>
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
          <h2>Admin Panel</h2>
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
