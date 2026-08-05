<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useNotificationStore } from '@/stores/notification';
import { formatDate } from '@/utils/format';

const router = useRouter();
const store = useNotificationStore();

onMounted(store.fetchOnce);

async function open(item) {
  if (!item.isRead) await store.markRead(item.notificationId);
  if (item.targetUrl) router.push(item.targetUrl);
}
</script>

<template>
  <main class="notifications-page">
    <header><div><span>Hộp thư</span><h1>Thông báo</h1><p>{{ store.unreadCount }} thông báo chưa đọc</p></div><button type="button" class="btn btn-outline" :disabled="!store.unreadCount" @click="store.markAllRead()">Đánh dấu tất cả đã đọc</button></header>
    <div v-if="store.loading" class="empty" role="status" aria-live="polite">Đang tải thông báo...</div>
    <div v-else-if="store.error" class="empty error" role="alert">
      <p>{{ store.error }}</p>
      <button type="button" class="btn btn-outline" @click="store.fetchOnce()">Thử lại</button>
    </div>
    <section v-else-if="store.items.length" class="list" aria-label="Danh sách thông báo">
      <article v-for="item in store.items" :key="item.notificationId" class="item" :class="{ unread: !item.isRead }">
        <button type="button" @click="open(item)"><span class="title">{{ item.title }}</span><span class="message">{{ item.message }}</span><time>{{ item.createdAt ? formatDate(item.createdAt) : '' }}</time></button>
        <button v-if="!item.isRead" type="button" class="read-btn" :aria-label="`Đánh dấu ${item.title} đã đọc`" @click="store.markRead(item.notificationId)">Đánh dấu đã đọc</button>
      </article>
    </section>
    <div v-else class="empty">Chưa có thông báo</div>
  </main>
</template>

<style scoped>
.notifications-page { max-width:900px; margin:0 auto; color:var(--text-dark); }
header { display:flex; align-items:flex-end; justify-content:space-between; gap:16px; margin-bottom:24px; }
header span { color:var(--role-accent, var(--primary-dark)); font-size:11px; font-weight:800; letter-spacing:.12em; text-transform:uppercase; }
h1 { margin:4px 0 6px; font-size:clamp(26px,4vw,36px); }
p { margin:0; color:var(--text-mid); }
.list { display:flex; flex-direction:column; gap:10px; }
.item { display:flex; align-items:center; gap:12px; padding:8px; border:1px solid var(--border-light); border-radius:12px; background:#fff; }
.item.unread { border-color:var(--primary-100); background:#fff9f3; }
.item > button:first-child { display:flex; min-width:0; flex:1; flex-direction:column; align-items:flex-start; padding:10px; text-align:left; }
.title { color:var(--text-dark); font-size:14px; font-weight:700; }
.message { margin-top:4px; color:var(--text-mid); font-size:13px; }
time { margin-top:6px; color:var(--text-light); font-size:11px; }
.read-btn { flex:none; padding:8px; color:var(--primary-dark); font-size:12px; font-weight:700; }
.empty { padding:60px 20px; border:1px solid var(--border-light); border-radius:12px; background:#fff; color:var(--text-mid); text-align:center; }
@media (max-width:560px) { header { align-items:flex-start; flex-direction:column; } .item { align-items:flex-start; flex-direction:column; } }
</style>
