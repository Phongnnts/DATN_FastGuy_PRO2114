<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();

const crumbs = computed(() => {
  const trail = Array.isArray(route.meta.breadcrumb) ? route.meta.breadcrumb : [];
  return trail.map((item) => (typeof item === 'string' ? { label: item } : item));
});
</script>

<template>
  <nav v-if="crumbs.length" class="app-breadcrumbs" aria-label="Breadcrumb">
    <ol class="breadcrumbs-list">
      <li v-for="(crumb, index) in crumbs" :key="index" class="breadcrumb-item">
        <router-link
          v-if="crumb.to && index < crumbs.length - 1"
          :to="crumb.to"
          class="breadcrumb-link"
        >{{ crumb.label }}</router-link>
        <span
          v-else
          class="breadcrumb-current"
          aria-current="page"
        >{{ crumb.label }}</span>
      </li>
    </ol>
  </nav>
</template>

<style scoped>
.app-breadcrumbs { margin-bottom: var(--space-4); font-size: 13px; }
.breadcrumbs-list { display: flex; flex-wrap: wrap; align-items: center; list-style: none; margin: 0; padding: 0; }
.breadcrumb-item { display: inline-flex; align-items: center; }
.breadcrumb-item:not(:last-child)::after { content: '›'; margin: 0 7px; color: var(--color-text-muted); }
.breadcrumb-link { color: var(--color-text-muted); text-decoration: none; font-weight: 600; }
.breadcrumb-link:hover { color: var(--color-accent); text-decoration: underline; }
.breadcrumb-current { color: var(--color-text); font-weight: 700; }
</style>
