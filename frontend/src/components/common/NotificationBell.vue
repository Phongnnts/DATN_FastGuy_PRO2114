<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';
import { formatDate } from '@/utils/format';

const router = useRouter();
const auth = useAuthStore();
const notificationStore = useNotificationStore();
const wrapper = ref(null);

const show = computed(() => auth.isLoggedIn);

onMounted(() => {
  document.addEventListener('click', handleOutside);
});

onUnmounted(() => {
  document.removeEventListener('click', handleOutside);
});

function handleOutside(e) {
  if (wrapper.value && !wrapper.value.contains(e.target)) {
    notificationStore.close();
  }
}

async function go(item) {
  if (item && !item.isRead) {
    await notificationStore.markRead(item.notificationId);
  }
  if (item?.targetUrl) {
    router.push(item.targetUrl);
  }
  notificationStore.close();
}
</script>

<template>
  <div v-if="show" class="notif-wrapper" ref="wrapper">
    <button class="top-bar-btn" type="button" aria-label="Thông báo" @click="notificationStore.toggle()">
      <i class="bi bi-bell"></i>
      <span v-if="notificationStore.unreadCount > 0" class="notif-badge">{{ notificationStore.unreadCount > 99 ? '99+' : notificationStore.unreadCount }}</span>
    </button>
    <div v-if="notificationStore.open" class="notif-dropdown card-flat">
      <div class="notif-header">
        <h4>Thông báo</h4>
        <button class="btn btn-sm btn-ghost" @click="notificationStore.markAllRead()">Đánh dấu đã đọc</button>
      </div>
      <div class="notif-list" v-if="notificationStore.items.length">
        <button
          v-for="item in notificationStore.items"
          :key="item.notificationId"
          class="notif-item"
          :class="{ unread: !item.isRead }"
          @click="go(item)"
        >
          <div class="notif-item-title">{{ item.title }}</div>
          <div class="notif-item-msg">{{ item.message }}</div>
          <div class="notif-item-time">{{ item.createdAt ? formatDate(item.createdAt) : '' }}</div>
        </button>
      </div>
      <div v-else class="notif-empty">Chưa có thông báo</div>
    </div>
  </div>
</template>

<style scoped>
.notif-wrapper {
  position: relative;
}
.notif-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  border-radius: 999px;
  background: var(--red-active);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}
.notif-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 8px);
  width: 340px;
  max-height: 420px;
  display: flex;
  flex-direction: column;
  z-index: 100;
}
.notif-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 8px;
}
.notif-header h4 {
  font-size: 15px;
  font-weight: 700;
  margin: 0;
}
.notif-list {
  display: flex;
  flex-direction: column;
  max-height: 360px;
  overflow-y: auto;
}
.notif-item {
  display: block;
  width: 100%;
  text-align: left;
  padding: 10px 8px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
}
.notif-item:hover,
.notif-item:focus {
  background: #f3f4f6;
}
.notif-item.unread {
  background: #fff9f3;
}
.notif-item-title {
  font-size: 13px;
  font-weight: 700;
  margin-bottom: 2px;
}
.notif-item-msg {
  font-size: 12px;
  color: var(--text-mid);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.notif-item-time {
  font-size: 11px;
  color: var(--text-light);
  margin-top: 4px;
}
.notif-empty {
  text-align: center;
  color: var(--text-mid);
  padding: 24px 0;
  font-size: 13px;
}
@media (max-width: 480px) {
  .notif-dropdown {
    width: 280px;
    right: -10px;
  }
}
</style>
