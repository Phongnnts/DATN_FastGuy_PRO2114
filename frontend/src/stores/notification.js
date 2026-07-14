import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { notificationApi } from '@/api';

let pollingTimer = null;

export const useNotificationStore = defineStore('notification', () => {
  const items = ref([]);
  const unreadCount = ref(0);
  const open = ref(false);

  function reset() {
    items.value = [];
    unreadCount.value = 0;
    open.value = false;
    stopPolling();
  }

  function startPolling() {
    stopPolling();
    fetchOnce();
    pollingTimer = setInterval(fetchOnce, 30000);
  }

  function stopPolling() {
    if (pollingTimer) {
      clearInterval(pollingTimer);
      pollingTimer = null;
    }
  }

  async function fetchOnce() {
    try {
      const auth = useAuthStore();
      if (!auth.isLoggedIn) { reset(); return; }
      const data = await notificationApi.get();
      items.value = data?.items || [];
      unreadCount.value = Number(data?.unreadCount) || 0;
    } catch {}
  }

  async function markRead(id) {
    await notificationApi.markRead(id);
    const item = items.value.find((n) => n.notificationId === id);
    if (item && !item.isRead) {
      item.isRead = true;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
  }

  async function markAllRead() {
    await notificationApi.markAllRead();
    items.value.forEach((n) => { n.isRead = true; });
    unreadCount.value = 0;
  }

  function toggle() { open.value = !open.value; }
  function close() { open.value = false; }

  return {
    items,
    unreadCount,
    open,
    reset,
    startPolling,
    stopPolling,
    fetchOnce,
    markRead,
    markAllRead,
    toggle,
    close,
  };
});
