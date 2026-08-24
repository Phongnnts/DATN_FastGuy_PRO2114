<template>
  <router-view />
  <AppToast />
  <AppAlert />
</template>

<script setup>
import AppToast from '@/components/common/AppToast.vue';
import AppAlert from '@/components/common/AppAlert.vue';
</script>
