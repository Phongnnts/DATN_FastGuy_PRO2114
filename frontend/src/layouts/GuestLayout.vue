<script setup>
import { onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useFavoriteStore } from '@/stores/favorite';
import PublicHeader from '@/components/common/PublicHeader.vue';

const auth = useAuthStore();
const favoriteStore = useFavoriteStore();
const year = new Date().getFullYear();

onMounted(() => {
  if (auth.isLoggedIn) favoriteStore.fetchFavorites();
});
</script>

<template>
  <div class="guest-layout fg-shell fg-shell-guest">
    <a class="skip-link" href="#main-content">Bỏ qua đến nội dung</a>
    <PublicHeader />
    <main id="main-content" class="main" tabindex="-1"><router-view /></main>
    <footer class="footer">
      <div class="container">
        <div class="footer-grid">
          <div class="footer-about"><div class="brand">Fast<span>Guy</span></div><p>Đặt đồ ăn nhanh trực tuyến. Thực đơn đa dạng, thanh toán tiện lợi.</p><div class="footer-social"><a href="#" aria-label="Facebook"><i class="bi bi-facebook"></i></a><a href="#" aria-label="Instagram"><i class="bi bi-instagram"></i></a><a href="#" aria-label="TikTok"><i class="bi bi-tiktok"></i></a></div></div>
          <div class="footer-col"><h2>Về FastGuy</h2><router-link to="/home">Trang chủ</router-link><router-link to="/menu">Thực đơn</router-link><router-link to="/promotions">Khuyến mãi</router-link></div>
          <div class="footer-col"><h2>Liên kết</h2><router-link to="/track-order">Tra cứu đơn</router-link><router-link to="/help">Trung tâm trợ giúp</router-link><router-link to="/terms">Điều khoản sử dụng</router-link><router-link to="/privacy">Chính sách bảo mật</router-link></div>
          <div class="footer-col"><h2>Hỗ trợ</h2><a href="tel:19001234">1900 1234</a><a href="mailto:support@fastguy.vn">support@fastguy.vn</a><router-link v-if="auth.isUser" to="/account/support">Gửi yêu cầu hỗ trợ</router-link></div>
        </div>
        <div class="footer-bottom">&copy; {{ year }} FastGuy. Tất cả quyền được bảo lưu.</div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.skip-link { position: fixed; top: 8px; left: 8px; z-index: var(--z-toast); padding: 10px 14px; background: var(--color-text); color: var(--text-inverse); border-radius: var(--radius-sm); transform: translateY(-150%); }
.skip-link:focus { transform: translateY(0); }
.brand { font-size: 22px; font-weight: 800; letter-spacing: -.5px; }
.brand span { color: var(--color-accent); }
.main { min-height: calc(100vh - 260px); }
.footer { margin-top: var(--space-8); background: var(--color-surface); border-top: 1px solid var(--color-border); }
.footer-grid { display: grid; grid-template-columns: 1.5fr 1fr 1fr 1fr; gap: 36px; padding: 48px 0 32px; }
.footer-about p { color: var(--color-text-muted); font-size: 14px; line-height: 1.6; }
.footer p, .footer-col a { color: var(--color-text-muted); font-size: 14px; }
.footer p { margin-top: 12px; }
.footer-col h2 { margin-bottom: 14px; color: var(--color-text); font-size: 13px; text-transform: uppercase; }
.footer-col a { display: block; min-height: 32px; }
.footer-col a:hover { color: var(--color-accent); }
.footer-bottom { padding: 20px 0; border-top: 1px solid var(--border-light); text-align: center; color: var(--text-light); font-size: 13px; }
@media (max-width: 768px) {
  .footer-grid { grid-template-columns: 1fr; gap: 28px; padding: 32px 0 20px; }
}
</style>

<style scoped>
.footer{margin-top:0;color:#fff;background:#1b1714;border-top:1px solid rgba(255,255,255,.08)}.footer-grid{grid-template-columns:1.6fr 1fr 1fr 1fr;gap:54px;padding:68px 0 46px}.footer .brand{color:#fff;font-size:27px}.footer-about p{max-width:330px;color:rgba(255,255,255,.48);line-height:1.8}.footer-col h2{margin-bottom:18px;color:rgba(255,255,255,.9);font-size:10px;font-weight:800;letter-spacing:.13em}.footer-col a{color:rgba(255,255,255,.48);font-size:13px;transition:color var(--transition-fast),transform var(--transition-fast)}.footer-col a:hover{color:var(--route-amber);transform:translateX(3px)}.footer-social{display:flex;gap:8px;margin-top:22px}.footer-social a{display:grid;width:38px;height:38px;place-items:center;border:1px solid rgba(255,255,255,.12);border-radius:50%;color:rgba(255,255,255,.6)}.footer-social a:hover{color:#1b1714;background:var(--route-amber);border-color:var(--route-amber)}.footer-bottom{padding:22px 0;border-color:rgba(255,255,255,.08);color:rgba(255,255,255,.3);text-align:left;font-size:11px}@media(max-width:768px){.footer-grid{grid-template-columns:1fr 1fr;gap:34px;padding:48px 0 30px}.footer-about{grid-column:1/-1}}@media(max-width:480px){.footer-grid{grid-template-columns:1fr}.footer-about{grid-column:auto}}
</style>
