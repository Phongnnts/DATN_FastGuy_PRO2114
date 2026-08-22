<script setup>
import { computed, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useOrderStore } from '@/stores/order';
import LoyaltyWallet from '@/components/common/LoyaltyWallet.vue';
import OrderStatusBadge from '@/components/common/OrderStatusBadge.vue';
import { formatDate, formatPrice } from '@/utils/format';

const auth = useAuthStore();
const orders = useOrderStore();
const activeStatuses = ['PENDING', 'CONFIRMED', 'PREPARING', 'READY', 'ASSIGNED', 'PICKED_UP'];
const recentOrders = computed(() => orders.userOrders.slice(0, 3));
const activeOrders = computed(() => orders.userOrders.filter(order => activeStatuses.includes(order.status)));

onMounted(() => orders.fetchOrders());
</script>

<template>
  <div class="overview-page">
    <header class="hero">
      <div><span class="eyebrow">Tài khoản FastGuy</span><h1>Xin chào, {{ auth.user?.fullName || 'thành viên' }}</h1><p>Đơn hàng, ưu đãi và quyền lợi của bạn trong một nơi.</p></div>
      <router-link to="/menu" class="btn btn-primary"><i class="bi bi-bag-plus" aria-hidden="true"></i> Đặt món</router-link>
    </header>

    <section class="shortcut-grid" aria-label="Truy cập nhanh">
      <router-link to="/account/orders" class="shortcut"><i class="bi bi-receipt"></i><span><strong>{{ activeOrders.length }}</strong> đơn đang hoạt động</span></router-link>
      <router-link to="/account/addresses" class="shortcut"><i class="bi bi-geo-alt"></i><span><strong>Địa chỉ</strong> giao hàng</span></router-link>
      <router-link to="/account/coupons" class="shortcut"><i class="bi bi-ticket-perforated"></i><span><strong>Ví mã</strong> ưu đãi</span></router-link>
    </section>

    <div class="overview-grid">
      <section class="panel" aria-labelledby="recent-orders-title">
        <div class="section-heading"><div><span class="eyebrow">Gần đây</span><h2 id="recent-orders-title">Đơn hàng</h2></div><router-link to="/account/orders">Xem tất cả</router-link></div>
        <div v-if="orders.loading" class="state" role="status">Đang tải đơn hàng...</div>
        <div v-else-if="orders.error" class="state error" role="alert"><span>{{ orders.error }}</span><button class="btn btn-sm btn-outline" @click="orders.fetchOrders">Thử lại</button></div>
        <div v-else-if="!recentOrders.length" class="state"><span>Bạn chưa có đơn hàng.</span><router-link to="/menu" class="btn btn-sm btn-primary">Khám phá thực đơn</router-link></div>
        <div v-else class="order-list">
          <router-link v-for="order in recentOrders" :key="order.id" :to="`/account/orders/${order.id}`" class="order-row">
            <div><strong>{{ order.orderCode }}</strong><small>{{ formatDate(order.createdAt) }}</small></div>
            <OrderStatusBadge :status="order.status" />
            <b>{{ formatPrice(order.total) }}</b>
          </router-link>
        </div>
      </section>
      <section class="panel wallet"><LoyaltyWallet compact /><router-link to="/account/rewards" class="btn btn-outline">Xem ví điểm thưởng</router-link></section>
    </div>
  </div>
</template>

<style scoped>
.overview-page{display:flex;max-width:1120px;margin:auto;padding:12px 0 40px;flex-direction:column;gap:20px}.hero{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:30px;border-radius:20px;background:linear-gradient(135deg,#211a16,#4b2a1d);color:#fff}.hero h1{margin:5px 0;font-size:clamp(25px,4vw,38px)}.hero p{margin:0;color:rgba(255,255,255,.68)}.eyebrow{color:var(--primary);font-size:11px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.shortcut-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.shortcut{display:flex;align-items:center;gap:12px;padding:17px;border:1px solid var(--border-light);border-radius:14px;background:#fff;color:var(--text-dark)}.shortcut:hover{border-color:var(--primary);transform:translateY(-1px)}.shortcut i{display:grid;width:40px;height:40px;place-items:center;border-radius:11px;background:var(--primary-light);color:var(--primary-dark);font-size:19px}.shortcut span{display:flex;flex-direction:column;color:var(--text-mid);font-size:12px}.shortcut strong{color:var(--text-dark);font-size:14px}.overview-grid{display:grid;grid-template-columns:1.2fr .8fr;gap:20px}.panel{padding:22px;border:1px solid var(--border-light);border-radius:16px;background:#fff}.section-heading{display:flex;align-items:center;justify-content:space-between;margin-bottom:15px}.section-heading h2{margin:3px 0}.order-list{display:flex;flex-direction:column}.order-row{display:grid;grid-template-columns:1fr auto auto;align-items:center;gap:14px;padding:14px 0;border-bottom:1px solid var(--border-light);color:var(--text-dark)}.order-row:last-child{border-bottom:0}.order-row div{display:flex;flex-direction:column}.order-row small{color:var(--text-mid)}.state{display:flex;min-height:150px;align-items:center;justify-content:center;flex-direction:column;gap:10px;color:var(--text-mid)}.error{color:var(--red-active)}.wallet .btn{width:100%;margin-top:12px}@media(max-width:850px){.shortcut-grid{grid-template-columns:1fr 1fr}.overview-grid{grid-template-columns:1fr}}@media(max-width:520px){.overview-page{padding-inline:0}.hero{align-items:flex-start;flex-direction:column;padding:22px}.shortcut-grid{grid-template-columns:1fr}.order-row{grid-template-columns:1fr auto}.order-row>b{grid-column:1/-1}}
</style>
