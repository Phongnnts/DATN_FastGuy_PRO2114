<script setup>
import { useAuthStore } from '@/stores/auth';
import { useCartStore } from '@/stores/cart';
import { useRouter } from 'vue-router';
import { ref } from 'vue';

const auth = useAuthStore();
const cart = useCartStore();
const router = useRouter();
const mobileMenuOpen = ref(false);

function goCart() {
  router.push('/cart');
}

function logout() {
  auth.logout();
  router.push('/');
}

const sidebarLinks = [
  {
    label: 'Thông tin cá nhân',
    path: '/account/profile',
    icon: 'bi-person',
  },
  {
    label: 'Đơn hàng',
    path: '/account/orders',
    icon: 'bi-box',
  },
  {
    label: 'Yêu thích',
    path: '/account/favorites',
    icon: 'bi-heart',
  },
  {
    label: 'Đổi mật khẩu',
    path: '/account/change-password',
    icon: 'bi-lock',
  },
];
</script>
