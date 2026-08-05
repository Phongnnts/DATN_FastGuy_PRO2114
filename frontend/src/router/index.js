import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { ROLES } from '@/utils/constants';
import { shiftApi } from '@/api';
import { isValidProductId } from '@/utils/adminProductEditor';
import { resolveCanonical, isIndexable } from './seo';

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
        name: 'Login',
        component: () => import('@/views/guest/LoginPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/guest/HomePage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'FastGuy — đặt đồ ăn nhanh giao tận nơi chỉ trong 30 phút. Đa dạng món ngon, combo tiết kiệm và nhiều ưu đãi hấp dẫn mỗi ngày.',
          canonical: '/home',
        },
      },
      {
        path: 'menu',
        name: 'Menu',
        component: () => import('@/views/guest/MenuPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Thực đơn FastGuy — burger, pizza, gà rán, mì ý, đồ uống và combo tiết kiệm. Đặt online, giao nhanh tận nơi.',
          canonical: '/menu',
        },
      },
      {
        path: 'product/:id',
        name: 'ProductDetail',
        component: () => import('@/views/guest/ProductDetailPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Chi tiết món tại FastGuy — xem mô tả, giá và đặt món giao nhanh tận nơi.',
          canonical: '/product/:id',
        },
      },
      {
        path: 'cart',
        name: 'Cart',
        component: () => import('@/views/guest/CartPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'login',
        redirect: { name: 'Login' },
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
        path: 'order-success',
        name: 'OrderSuccess',
        component: () => import('@/views/user/OrderSuccessPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'promotions',
        name: 'Promotions',
        component: () => import('@/views/guest/PromotionsPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Khuyến mãi FastGuy — cập nhật ưu đãi, voucher và chương trình giảm giá mới nhất.',
          canonical: '/promotions',
        },
      },
      {
        path: 'checkout',
        name: 'Checkout',
        component: () => import('@/views/user/CheckoutPage.vue'),
        meta: { guest: true },
      },
      {
        path: 'help',
        name: 'Help',
        component: () => import('@/views/guest/HelpPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Trung tâm trợ giúp FastGuy — hướng dẫn đặt hàng, thanh toán và câu hỏi thường gặp.',
          canonical: '/help',
        },
      },
      {
        path: 'terms',
        name: 'Terms',
        component: () => import('@/views/guest/TermsPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Điều khoản sử dụng FastGuy — quy định và điều kiện khi sử dụng dịch vụ của chúng tôi.',
          canonical: '/terms',
        },
      },
      {
        path: 'privacy',
        name: 'Privacy',
        component: () => import('@/views/guest/PrivacyPage.vue'),
        meta: {
          guest: true,
          robots: 'index,follow',
          description: 'Chính sách bảo mật FastGuy — cách chúng tôi thu thập, sử dụng và bảo vệ thông tin của bạn.',
          canonical: '/privacy',
        },
      },
    ],
  },

  // ─── User ──────────────────────────────────
  {
    path: '/account',
    component: UserLayout,
    meta: { requiresAuth: true, role: ROLES.USER },
    children: [
      { path: '', redirect: { name: 'AccountOverview' } },
      {
        path: 'overview',
        name: 'AccountOverview',
        component: () => import('@/views/user/AccountOverviewPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Tổng quan' },
          ],
        },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/user/ProfilePage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Hồ sơ' },
          ],
        },
      },
      {
        path: 'orders',
        name: 'UserOrders',
        component: () => import('@/views/user/OrdersPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Đơn hàng' },
          ],
        },
      },
      {
        path: 'orders/:id',
        name: 'UserOrderDetail',
        component: () => import('@/views/user/OrderDetailPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Đơn hàng', to: '/account/orders' },
            { label: 'Chi tiết' },
          ],
        },
      },
      {
        path: 'favorites',
        name: 'UserFavorites',
        component: () => import('@/views/user/FavoritesPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Món yêu thích' },
          ],
        },
      },
      {
        path: 'addresses',
        name: 'UserAddresses',
        component: () => import('@/views/user/AddressesPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Sổ địa chỉ' },
          ],
        },
      },
      {
        path: 'coupons',
        name: 'UserCoupons',
        component: () => import('@/views/user/CouponWalletPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Ví mã ưu đãi' },
          ],
        },
      },
      {
        path: 'rewards',
        name: 'UserRewards',
        component: () => import('@/views/user/RewardsPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Ví điểm thưởng' },
          ],
        },
      },
      {
        path: 'notifications',
        name: 'UserNotifications',
        component: () => import('@/views/user/NotificationsPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Thông báo' },
          ],
        },
      },
      {
        path: 'history',
        redirect: { path: '/account/orders', query: { status: 'DELIVERED' } },
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/user/ChangePasswordPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Đổi mật khẩu' },
          ],
        },
      },
      {
        path: 'support',
        name: 'UserSupport',
        component: () => import('@/views/user/SupportPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tài khoản', to: '/account/overview' },
            { label: 'Hỗ trợ' },
          ],
        },
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
        path: 'kitchen',
        redirect: { name: 'StaffOrders' },
      },
      {
        path: 'dispatch',
        name: 'StaffDispatch',
        component: () => import('@/views/staff/DispatchPage.vue'),
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
        meta: { requiresCheckedInShift: true },
      },
      {
        path: 'orders',
        name: 'ShipperOrders',
        component: () => import('@/views/shipper/MyOrdersPage.vue'),
        meta: { requiresCheckedInShift: true },
      },
      {
        path: 'history',
        name: 'ShipperOrderHistory',
        component: () => import('@/views/shipper/MyOrdersPage.vue'),
      },
      {
        path: 'shifts',
        name: 'ShipperShifts',
        component: () => import('@/views/shipper/ShipperShiftsPage.vue'),
      },
      {
        path: 'cash',
        name: 'ShipperCash',
        component: () => import('@/views/shipper/CashPage.vue'),
      },
      {
        path: 'orders/history',
        redirect: { name: 'ShipperOrderHistory' },
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
        path: 'products/new',
        name: 'AdminProductCreate',
        component: () => import('@/views/admin/ProductEditorPage.vue'),
      },
      {
        path: 'products/:id/edit',
        name: 'AdminProductEdit',
        component: () => import('@/views/admin/ProductEditorPage.vue'),
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
        path: 'inventory/ledger',
        name: 'AdminInventoryLedger',
        component: () => import('@/views/admin/InventoryLedgerPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Tồn kho', to: '/admin/inventory' },
            { label: 'Sổ tồn kho' },
          ],
        },
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
        path: 'refunds',
        name: 'AdminRefunds',
        component: () => import('@/views/admin/RefundsPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Đơn hàng', to: '/admin/orders' },
            { label: 'Hoàn tiền' },
          ],
        },
      },
      {
        path: 'orders/:id',
        name: 'AdminOrderDetail',
        component: () => import('@/views/admin/OrderDetailPage.vue'),
        meta: {
          breadcrumb: [
            { label: 'Đơn hàng', to: '/admin/orders' },
            { label: 'Chi tiết' },
          ],
        },
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

  { path: '/reports', redirect: { name: 'AdminReports' } },
  { path: '/loyalty', redirect: { name: 'UserRewards' } },
  { path: '/history', redirect: { name: 'UserOrders', query: { status: 'DELIVERED' } } },

  // ─── 404 ───────────────────────────────────
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundPage.vue'),
  },
];

const pageTitles = {
  Home: 'Trang chủ', Login: 'Đăng nhập', Menu: 'Thực đơn', ProductDetail: 'Chi tiết món', Cart: 'Giỏ hàng',
  Register: 'Đăng ký', TrackOrder: 'Tra cứu đơn', ForgotPassword: 'Quên mật khẩu', ResetPassword: 'Đặt lại mật khẩu',
  PaymentReturn: 'Kết quả thanh toán', OrderSuccess: 'Đặt hàng thành công', Promotions: 'Khuyến mãi', Checkout: 'Thanh toán', Help: 'Trung tâm trợ giúp', Terms: 'Điều khoản sử dụng', Privacy: 'Chính sách bảo mật',
  AccountOverview: 'Tổng quan tài khoản', Profile: 'Thông tin cá nhân', UserAddresses: 'Sổ địa chỉ', UserCoupons: 'Ví mã ưu đãi',
  UserOrders: 'Đơn hàng', UserOrderDetail: 'Chi tiết đơn hàng', UserFavorites: 'Món yêu thích', ChangePassword: 'Đổi mật khẩu',
  UserRewards: 'Ví điểm thưởng', UserNotifications: 'Thông báo', UserSupport: 'Hỗ trợ', StaffDashboard: 'Tổng quan nhân viên', StaffOrders: 'Quản lý đơn hàng',
  StaffOrderHistory: 'Lịch sử đơn hàng', StaffOrderDetail: 'Chi tiết đơn hàng', StaffDispatch: 'Điều phối giao hàng', StaffSupport: 'Hỗ trợ', StaffShifts: 'Ca làm việc',
  ShipperDashboard: 'Tổng quan giao hàng', ShipperOrders: 'Đơn giao', ShipperOrderHistory: 'Lịch sử giao hàng',
  ShipperShifts: 'Ca làm việc', ShipperOrderDetail: 'Chi tiết đơn giao', ShipperCash: 'Đối soát COD', AdminDashboard: 'Tổng quan quản trị', AdminUsers: 'Người dùng',
  AdminProducts: 'Sản phẩm', AdminProductCreate: 'Thêm sản phẩm', AdminProductEdit: 'Chỉnh sửa sản phẩm', AdminInventory: 'Kho hàng', AdminInventoryLedger: 'Sổ tồn kho', AdminCategories: 'Danh mục', AdminOrders: 'Đơn hàng',
  AdminOrderDetail: 'Chi tiết đơn hàng', AdminReports: 'Báo cáo', AdminCoupons: 'Mã giảm giá', AdminBanners: 'Banner', AdminRefunds: 'Hoàn tiền',
  AdminSettings: 'Cài đặt', AdminShifts: 'Ca làm việc', NotFound: 'Không tìm thấy trang',
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
  const hasValidSession = auth.validateSession();
  const roleRoutes = {
    [ROLES.USER]: '/home',
    [ROLES.STAFF]: '/staff',
    [ROLES.ADMIN]: '/admin',
    [ROLES.SHIPPER]: '/shipper',
  };

  if (to.name === 'Login' && hasValidSession) {
    return next(roleRoutes[auth.role] || '/home');
  }

  if (
    to.matched.some((r) => r.meta.guest) &&
    hasValidSession &&
    auth.role !== ROLES.USER
  ) {
    const redirect = roleRoutes[auth.role];
    if (redirect) return next(redirect);
  }

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
  const requiredRole = [...to.matched].reverse().find((record) => record.meta.role)?.meta.role;

  if (requiresAuth && !hasValidSession) {
    return next({ name: 'Login', query: { redirect: to.fullPath } });
  }

  if (requiredRole && auth.role !== requiredRole) {
    return next(
      hasValidSession ? roleRoutes[auth.role] || '/' : { name: 'Login' },
    );
  }

  if (to.matched.some((record) => record.meta.requiresCheckedInShift)) {
    try {
      const current = await shiftApi.getCurrent();
      if (current?.state !== 'CHECKED_IN') {
        const shiftRoutes = { STAFF: '/staff/shifts', SHIPPER: '/shipper/shifts' };
        return next(shiftRoutes[auth.role] || '/staff/shifts');
      }
    } catch {
      const shiftRoutes = { STAFF: '/staff/shifts', SHIPPER: '/shipper/shifts' };
      return next(shiftRoutes[auth.role] || '/staff/shifts');
    }
  }

  if (to.name === 'AdminProducts' && 'edit' in to.query) {
    const editId = to.query.edit;
    if (isValidProductId(editId)) return next({ name: 'AdminProductEdit', params: { id: Number(editId) } });
    return next({ name: 'AdminProducts' });
  }

  next();
});

function upsertMeta(name, value) {
  const attr = name.startsWith('og:') ? 'property' : 'name';
  const selector = `meta[${attr}="${name}"]`;
  let el = document.head.querySelector(selector);
  if (!value) {
    if (el) el.remove();
    return;
  }
  if (!el) {
    el = document.createElement('meta');
    el.setAttribute(attr, name);
    document.head.appendChild(el);
  }
  el.setAttribute('content', value);
}

function upsertCanonical(href) {
  let el = document.head.querySelector('link[rel="canonical"]');
  if (!href) {
    if (el) el.remove();
    return;
  }
  if (!el) {
    el = document.createElement('link');
    el.setAttribute('rel', 'canonical');
    document.head.appendChild(el);
  }
  el.setAttribute('href', href);
}

router.afterEach((to) => {
  const title = [...to.matched].reverse().find((record) => record.meta.title)?.meta.title;
  document.title = title ? `${title} | FastGuy` : 'FastGuy';

  const meta = to.meta;
  const robots = meta.robots || 'noindex,nofollow';
  const indexable = isIndexable(robots);
  const canonical = meta.canonical
    ? new URL(resolveCanonical(meta.canonical, to), window.location.origin).href
    : '';

  upsertMeta('description', meta.description || '');
  upsertMeta('robots', robots);
  upsertMeta('og:title', indexable && title ? `${title} | FastGuy` : '');
  upsertMeta('og:description', indexable ? meta.description || '' : '');
  upsertMeta('og:url', indexable ? canonical : '');
  upsertCanonical(canonical);
});

export default router;
