<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useStaffStore } from '@/stores/staff';
import { formatPrice, formatDate } from '@/utils/format';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';

const route = useRoute();
const router = useRouter();
const staffStore = useStaffStore();
const activeTab = ref('PENDING');
const searchTerm = ref('');
let refreshTimer;
let refreshing = false;
const tabs = [{ key:'WAITING_STOCK_CONFIRM', label:'Chờ xác nhận tồn' }, { key:'PENDING', label:'Chờ xử lý' }, { key:'CONFIRMED', label:'Đã xác nhận' }, { key:'PREPARING', label:'Đang chế biến' }, { key:'READY', label:'Sẵn sàng giao' }];
function normalizedTab(value) { return tabs.some(tab => tab.key === value) ? value : 'PENDING'; }
async function refresh(tab = activeTab.value) { if (refreshing) return; refreshing = true; try { if (tab === 'PENDING' || tab === 'WAITING_STOCK_CONFIRM') await staffStore.fetchOrders(); else if (tab === 'CONFIRMED') await staffStore.fetchConfirmedOrders(); else if (tab === 'PREPARING') await staffStore.fetchPreparingOrders(); else await staffStore.fetchReadyOrders(); } finally { refreshing = false; } }
async function switchTab(tab) { activeTab.value = normalizedTab(tab); await refresh(); }
const filteredOrders = computed(() => staffStore.allOrders.filter(order => order.status === activeTab.value && (!searchTerm.value || order.orderCode.toLowerCase().includes(searchTerm.value.toLowerCase()))).sort((a,b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0)));
function isOverdue(order) { return ['PENDING','CONFIRMED','PREPARING'].includes(order.status) && Date.now() - new Date(order.createdAt).getTime() > 25 * 60 * 1000; }
function age(order) { return `${Math.max(0, Math.floor((Date.now() - new Date(order.createdAt).getTime()) / 60000))} phút`; }
function goDetail(id) { router.push(`/staff/orders/${id}`); }
watch(() => route.query.tab, value => { const tab = normalizedTab(value); if (tab !== activeTab.value) switchTab(tab); });
onMounted(async () => { activeTab.value = normalizedTab(route.query.tab); await refresh(); refreshTimer = setInterval(() => refresh(), 30000); });
onUnmounted(() => clearInterval(refreshTimer));
</script>

<template>
  <div><div class="page-header"><div><h1>Quản lý đơn hàng</h1><p>Ưu tiên đơn theo thời gian tạo.</p></div><label class="search-box"><span class="sr-only">Tìm mã đơn</span><i class="bi bi-search"></i><input v-model="searchTerm" class="form-input" placeholder="Tìm mã đơn"></label></div>
    <div class="card card-flat"><div class="tabs"><button v-for="tab in tabs" :key="tab.key" class="tab" :class="{ active:activeTab === tab.key }" @click="switchTab(tab.key)">{{ tab.label }}</button></div><div v-if="staffStore.error" class="order-error"><span>{{ staffStore.error }}</span><button class="btn btn-sm btn-outline" @click="refresh">Thử lại</button></div><div v-else-if="staffStore.loading" class="staff-state"><span class="spinner"></span> Đang tải đơn hàng...</div><div v-else-if="filteredOrders.length" class="table-wrapper"><table class="table"><thead><tr><th>Mã đơn</th><th>Khách hàng</th><th>Sản phẩm</th><th>Tổng tiền</th><th>Ngày đặt</th><th>Trạng thái</th><th></th></tr></thead><tbody><tr v-for="order in filteredOrders" :key="order.id" :class="{ overdue:isOverdue(order) }"><td data-label="Mã đơn"><router-link :to="`/staff/orders/${order.id}`" class="order-link">{{ order.orderCode }}</router-link></td><td data-label="Khách hàng">{{ order.customerName || `Khách #${order.userId}` }}</td><td data-label="Sản phẩm">{{ order.items?.length || 0 }} món</td><td data-label="Tổng tiền">{{ formatPrice(order.total) }}</td><td data-label="Ngày đặt">{{ formatDate(order.createdAt) }} <span v-if="isOverdue(order)" class="overdue-label"><i class="bi bi-exclamation-triangle-fill"></i> {{ age(order) }}</span></td><td data-label="Trạng thái"><OrderStatusBadge :status="order.status" /></td><td><button class="btn btn-sm btn-ghost" :aria-label="`Mở ${order.orderCode}`" @click="goDetail(order.id)"><i class="bi bi-chevron-right"></i></button></td></tr></tbody></table></div><div v-else class="empty-state"><i class="bi bi-receipt"></i><h3>Không có đơn trong hàng đợi</h3><p>Đơn mới sẽ xuất hiện tại đây.</p></div></div>
  </div>
</template>

<style scoped>
.page-header p { margin:4px 0 0; color:var(--text-mid); font-size:14px; }.search-box { max-width:320px; }.order-link { color:var(--text-dark); font-weight:750; }.order-link:hover { color:var(--role-accent,var(--primary)); }.overdue { background:#fff7ed; }.overdue td:first-child { border-left:3px solid #f97316; }.overdue-label { display:block; margin-top:3px; color:#c2410c; font-size:11px; font-weight:700; }.order-error { display:flex; align-items:center; justify-content:space-between; gap:12px; margin:12px; padding:10px 12px; border-radius:var(--radius-sm); color:#b91c1c; background:#fef2f2; font-size:13px; }.staff-state { display:flex; justify-content:center; align-items:center; gap:10px; min-height:180px; color:var(--text-mid); }@media(max-width:768px){.page-header{align-items:flex-start;flex-direction:column;}.search-box{width:100%;max-width:none;}.table thead{display:none;}.table tbody tr{display:block;margin-bottom:8px;padding:12px;border:1px solid var(--border-light);border-radius:var(--radius-sm);background:#fff;}.table tbody td{display:flex;justify-content:space-between;gap:12px;padding:6px 0;border:0;font-size:13px;}.table tbody td::before{content:attr(data-label);color:var(--text-mid);font-weight:650;}.table tbody td:last-child::before{content:'';}}
</style>
