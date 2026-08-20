<script setup>
import { onMounted } from 'vue';
import { useFavoriteStore } from '@/stores/favorite';
import PublicHeader from '@/components/common/PublicHeader.vue';
import AccountTabs from '@/components/common/AccountTabs.vue';
import AppBreadcrumbs from '@/components/common/AppBreadcrumbs.vue';

const favoriteStore = useFavoriteStore();
onMounted(() => favoriteStore.fetchFavorites());
</script>

<template>
  <div class="user-layout fg-shell fg-shell-user">
    <a class="skip-link" href="#account-content">Bỏ qua đến nội dung</a>
    <PublicHeader />
    <div class="container account-shell">
      <AccountTabs />
      <main id="account-content" class="content fg-page" tabindex="-1"><AppBreadcrumbs /><router-view /></main>
    </div>
  </div>
</template>

<style scoped>
.user-layout { min-height: 100vh; background: var(--color-bg); }
.skip-link { position: fixed; top: 8px; left: 8px; z-index: var(--z-toast); padding: 10px 14px; background: var(--color-text); color: white; border-radius: var(--radius-sm); transform: translateY(-150%); }
.skip-link:focus { transform: translateY(0); }
.account-shell { padding-top: var(--space-6); padding-bottom: var(--space-8); }
.content { min-height: calc(100vh - 180px); padding-top: var(--space-5); }
</style>
