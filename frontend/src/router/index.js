import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ROLES } from '@/utils/constants';

import GuestLayout from '@/layouts/GuestLayout.vue';
import UserLayout from '@/layouts/UserLayout.vue';
import StaffLayout from '@/layouts/StaffLayout.vue';

import AdminLayout from '@/layouts/AdminLayout.vue';

const routes = [
  // ─── Guest ─────────────────────────────────
  {
    path: '/',
    component: GuestLayout,
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/guest/HomePage.vue'),
        meta: { guest: true },
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/guest/MenuPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/guest/ProductDetailPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/guest/CartPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'login',
        name: 'Login',
        component: () => import('@/views/guest/LoginPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'register',
        name: 'Register',
        component: () => import('@/views/guest/RegisterPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'track-order',
        name: 'TrackOrder',
        component: () => import('@/views/guest/TrackOrderPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'forgot-password',
        name: 'ForgotPassword',
        component: () => import('@/views/guest/ForgotPasswordPage.vue'),
        meta: { guest: true },
      },
    ],
  },

  // ─── User ──────────────────────────────────
  {
    path: '/account',
    component: UserLayout,
    meta: { requiresAuth: true, role: ROLES.USER },
    children: [
      { path: '', redirect: { name: 'Profile' } },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfilePage.vue'),
      },
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('@/views/user/OrdersPage.vue'),
      },
      {
        path: 'orders/:id',
        name: 'UserOrderDetail',
        component: () => import('@/views/user/OrderDetailPage.vue'),
      },
      {
        path: 'orders/:id/review',
        name: 'OrderReview',
        component: () => import('@/views/user/OrderReviewPage.vue'),
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePasswordPage.vue'),
      },
      {
        path: 'favorites',
        name: 'Favorites',
        component: () => import('@/views/user/FavoritesPage.vue'),
      },
    ],
  },
  {
    path: '/checkout',
    component: UserLayout,
    meta: { requiresAuth: true, role: ROLES.USER },
    children: [
      {
        path: '',
        name: 'Checkout',
        component: () => import('@/views/user/CheckoutPage.vue'),
      },
    ],
  },

  // ─── Staff ─────────────────────────────────
  {
    path: '/staff',
    component: StaffLayout,
    meta: { requiresAuth: true, role: ROLES.STAFF },
    children: [
      {
        path: '',
        name: 'StaffDashboard',
        component: () => import('@/views/staff/DashboardPage.vue'),
      },
      {
        path: 'orders',
        name: 'StaffOrders',
        component: () => import('@/views/staff/OrdersPage.vue'),
      },
      {
        path: 'orders/:id',
        name: 'StaffOrderDetail',
        component: () => import('@/views/staff/OrderDetailPage.vue'),
      },
      {
        path: 'orders/history',
        name: 'StaffOrderHistory',
        component: () => import('@/views/staff/OrderHistoryPage.vue'),
      },
      {
        path: 'shifts',
        name: 'StaffShifts',
        component: () => import('@/views/staff/ShiftsPage.vue'),
      },
    ],
  },

  // ─── Admin ─────────────────────────────────
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAuth: true, role: ROLES.ADMIN },
    children: [
      {
        path: '',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/DashboardPage.vue'),
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/UsersPage.vue'),
      },
      {
        path: 'products',
        name: 'AdminProducts',
        component: () => import('@/views/admin/ProductsPage.vue'),
      },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/CategoriesPage.vue'),
      },
      {
        path: 'delivery-zones',
        name: 'AdminDeliveryZones',
        component: () => import('@/views/admin/DeliveryZonesPage.vue'),
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/OrdersPage.vue'),
      },
      {
        path: 'shifts',
        name: 'AdminShifts',
        component: () => import('@/views/admin/ShiftsPage.vue'),
      },
      {
        path: 'reports/revenue',
        name: 'AdminRevenueReport',
        component: () => import('@/views/admin/RevenueReportPage.vue'),
      },
      {
        path: 'reports/top-products',
        name: 'AdminTopProducts',
        component: () => import('@/views/admin/TopProductsReportPage.vue'),
      },
    ],
  },

  // ─── 404 ───────────────────────────────────
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundPage.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

router.beforeEach((to, from, next) => {
  const auth = useAuthStore();

  if (
    to.matched.some((r) => r.meta.guest) &&
    auth.isLoggedIn &&
    auth.role !== ROLES.USER
  ) {
    const roleRoutes = {
      [ROLES.STAFF]: '/staff',
      [ROLES.ADMIN]: '/admin',
    };
    const redirect = roleRoutes[auth.role];
    if (redirect) return next(redirect);
  }

  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } });
  }

  if (to.meta.role && auth.role !== to.meta.role) {
    const roleRoutes = {
      [ROLES.USER]: '/account/profile',
      [ROLES.STAFF]: '/staff',
      [ROLES.ADMIN]: '/admin',
    };
    return next(
      auth.isLoggedIn ? roleRoutes[auth.role] || '/' : { name: 'Login' },
    );
  }

  next();
});

export default router;
