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
  { label: 'Tổng quan', path: '/staff', icon: 'bi-speedometer2' },
  { label: 'Đơn hàng', path: '/staff/orders', icon: 'bi-receipt' },
  {
    label: 'Lịch sử đơn',
    path: '/staff/orders/history',
    icon: 'bi-clock-history',
  },
  { label: 'Nguyên liệu', path: '/staff/ingredients', icon: 'bi-basket' },
  {
    label: 'Tồn kho thấp',
    path: '/staff/ingredients/low-stock',
    icon: 'bi-exclamation-triangle',
  },
  { label: 'Ca làm việc', path: '/staff/shifts', icon: 'bi-calendar-check' },
];
</script>

<template>
  <div class="sidebar-layout">
    <aside class="sidebar" :class="{ open: sidebarOpen }">
      <div class="sidebar-brand">
        <span class="brand-text">
          Fast<span class="brand-highlight">Guy</span>
        </span>
        <div class="sidebar-subtitle">
          Staff
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="link in sidebarLinks"
          :key="link.path"
          :to="link.path"
          class="sidebar-link"
          @click="sidebarOpen = false"
        >
          <i :class="['bi', link.icon]"></i>
          <span>{{ link.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <img
            :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'"
            class="user-avatar"
            alt="Avatar"
          />
          <div>
            <div class="user-name">
              {{ auth.user?.fullName || 'Nhân viên' }}
            </div>
            <div class="user-role">
              Nhân viên
            </div>
          </div>
        </div>
      </div>
    </aside>

    <div class="main-content">
      <header class="topbar">
        <div class="topbar-left">
          <button
            class="btn btn-ghost menu-btn"
            @click="sidebarOpen = !sidebarOpen"
          >
            <i class="bi bi-list"></i>
          </button>

          <h2>Staff Panel</h2>
        </div>

        <div class="topbar-right">
          <router-link to="/" class="btn btn-sm">
            <i class="bi bi-house"></i>
            Website
          </router-link>

          <button class="btn btn-sm btn-ghost" @click="logout">
            <i class="bi bi-box-arrow-right"></i>
            Đăng xuất
          </button>
        </div>
      </header>

      <main class="page-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.sidebar-layout {
  display: flex;
  min-height: 100vh;
  background: var(--bg);
}

.sidebar {
  width: 250px;
  background: #fff;
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-brand {
  padding: 20px;
  border-bottom: 1px solid var(--border);
}

.brand-text {
  font-size: 20px;
  font-weight: 800;
}

.brand-highlight {
  color: var(--primary);
}

.sidebar-subtitle {
  font-size: 11px;
  color: var(--text-mid);
  margin-top: 2px;
}

.sidebar-nav {
  padding: 12px;
  flex: 1;
}

.sidebar-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: var(--radius-sm);
  color: var(--text-mid);
  transition: all 0.15s;
  margin-bottom: 4px;
}

.sidebar-link:hover {
  background: var(--bg);
  color: var(--text-dark);
}

.sidebar-link.router-link-active {
  background: var(--primary-light);
  color: var(--primary);
  font-weight: 600;
}

.sidebar-link i {
  width: 18px;
  text-align: center;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid var(--border);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-avatar {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
}

.user-role {
  font-size: 12px;
  color: var(--text-mid);
}

.main-content {
  flex: 1;
  min-width: 0;
}

.topbar {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.topbar-left h2 {
  font-size: 20px;
  margin: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.menu-btn {
  display: none;
}

.page-content {
  padding: 24px;
}

@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: -250px;
    top: 0;
    bottom: 0;
    z-index: 999;
    transition: left 0.25s ease;
  }

  .sidebar.open {
    left: 0;
  }

  .menu-btn {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .topbar {
    padding: 0 16px;
  }

  .page-content {
    padding: 16px;
  }

  .topbar-left h2 {
    font-size: 18px;
  }
}
</style>
