import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ROLES } from '@/utils/constants';
import { shiftApi } from '@/api';

import GuestLayout from '@/layouts/GuestLayout.vue';
import UserLayout from '@/layouts/UserLayout.vue';
import StaffLayout from '@/layouts/StaffLayout.vue';
import ShipperLayout from '@/layouts/ShipperLayout.vue';
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
      {
        path: 'reset-password',
        name: 'ResetPassword',
        component: () => import('@/views/guest/ResetPasswordPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'payment-return',
        name: 'PaymentReturn',
        component: () => import('@/views/user/PaymentReturnPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'promotions',
        name: 'Promotions',
        component: () => import('@/views/guest/PromotionsPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/user/CheckoutPage.vue'),
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
        path: 'favorites',
        name: 'UserFavorites',
        component: () => import('@/views/user/FavoritesPage.vue'),
      },
      {
        path: 'history',
        redirect: { path: '/account/orders', query: { status: 'DELIVERED' } },
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePasswordPage.vue'),
      },
      {
        path: 'support',
        name: 'UserSupport',
        component: () => import('@/views/user/SupportPage.vue'),
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
        meta: { requiresCheckedInShift: true },
      },
      {
        path: 'orders',
        name: 'StaffOrders',
        component: () => import('@/views/staff/OrdersPage.vue'),
        meta: { requiresCheckedInShift: true },
      },
      {
        path: 'orders/history',
        name: 'StaffOrderHistory',
        component: () => import('@/views/staff/OrderHistoryPage.vue'),
      },
      {
        path: 'orders/:id',
        name: 'StaffOrderDetail',
        component: () => import('@/views/staff/OrderDetailPage.vue'),
        meta: { requiresCheckedInShift: true },
      },
      {
        path: 'support',
        name: 'StaffSupport',
        component: () => import('@/views/staff/SupportPage.vue'),
      },
      {
        path: 'shifts',
        name: 'StaffShifts',
        component: () => import('@/views/staff/StaffShiftsPage.vue'),
      },
    ],
  },

  // ─── Shipper ───────────────────────────────
  {
    path: '/shipper',
    component: ShipperLayout,
    meta: { requiresAuth: true, role: ROLES.SHIPPER },
    children: [
      {
        path: '',
        name: 'ShipperDashboard',
        component: () => import('@/views/shipper/DashboardPage.vue'),
      },
      {
        path: 'orders',
        name: 'ShipperOrders',
        component: () => import('@/views/shipper/MyOrdersPage.vue'),
      },
      {
        path: 'orders/:id',
        name: 'ShipperOrderDetail',
        component: () => import('@/views/shipper/OrderDetailPage.vue'),
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
         path: 'inventory',
         name: 'AdminInventory',
         component: () => import('@/views/admin/InventoryPage.vue'),
       },
      {
        path: 'categories',
        name: 'AdminCategories',
        component: () => import('@/views/admin/CategoriesPage.vue'),
      },
      {
        path: 'orders',
        name: 'AdminOrders',
        component: () => import('@/views/admin/OrdersPage.vue'),
      },
      {
        path: 'orders/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/OrderDetailPage.vue'),
      },
      {
        path: 'reports',
        name: 'AdminReports',
        component: () => import('@/views/admin/ReportsPage.vue'),
      },
      {
        path: 'coupons',
        name: 'AdminCoupons',
        component: () => import('@/views/admin/CouponsPage.vue'),
      },
      {
        path: 'banners',
        name: 'AdminBanners',
        component: () => import('@/views/admin/BannersPage.vue'),
      },
      {
        path: 'settings',
        name: 'AdminSettings',
        component: () => import('@/views/admin/SettingsPage.vue'),
      },
      {
        path: 'shifts',
        name: 'AdminShifts',
        component: () => import('@/views/admin/ShiftsPage.vue'),
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

const pageTitles = {
  Home: 'Trang chủ', Menu: 'Thực đơn', ProductDetail: 'Chi tiết món', Cart: 'Giỏ hàng',
  Login: 'Đăng nhập', Register: 'Đăng ký', TrackOrder: 'Tra cứu đơn', Promotions: 'Khuyến mãi',
  Checkout: 'Thanh toán', Profile: 'Thông tin cá nhân', UserOrders: 'Đơn hàng',
  UserOrderDetail: 'Chi tiết đơn hàng', UserFavorites: 'Món yêu thích', ChangePassword: 'Đổi mật khẩu',
  UserSupport: 'Hỗ trợ', NotFound: 'Không tìm thấy trang',
};

function applyTitles(records) {
  records.forEach((record) => {
    if (record.name) record.meta = { ...record.meta, title: pageTitles[record.name] || record.name };
    if (record.children) applyTitles(record.children);
  });
}
applyTitles(routes);

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    return savedPosition || (to.hash ? { el: to.hash, behavior: 'smooth' } : { top: 0 });
  },
});

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore();

  if (
    to.matched.some((r) => r.meta.guest) &&
    auth.isLoggedIn &&
    auth.role !== ROLES.USER
  ) {
    const roleRoutes = {
      [ROLES.STAFF]: '/staff',
      [ROLES.ADMIN]: '/admin',
      [ROLES.SHIPPER]: '/shipper',
    };
    const redirect = roleRoutes[auth.role];
    if (redirect) return next(redirect);
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
  const requiredRole = to.matched.findLast((record) => record.meta.role)?.meta.role;

  if (requiresAuth && !auth.isLoggedIn) {
    return next({ name: 'Login', query: { redirect: to.fullPath } });
  }

  if (requiredRole && auth.role !== requiredRole) {
    const roleRoutes = {
      [ROLES.USER]: '/account/profile',
      [ROLES.STAFF]: '/staff',
      [ROLES.ADMIN]: '/admin',
      [ROLES.SHIPPER]: '/shipper',
    };
    return next(
      auth.isLoggedIn ? roleRoutes[auth.role] || '/' : { name: 'Login' },
    );
  }

  if (to.matched.some((record) => record.meta.requiresCheckedInShift)) {
    try {
      const current = await shiftApi.getCurrent();
      if (current?.state !== 'CHECKED_IN') return next('/staff/shifts');
    } catch {
      return next('/staff/shifts');
    }
  }

  next();
});

router.afterEach((to) => {
  const title = [...to.matched].reverse().find((record) => record.meta.title)?.meta.title;
  document.title = title ? `${title} | FastGuy` : 'FastGuy';
});

export default router;
