<script setup>
import { nextTick, onBeforeUnmount, ref, watch } from 'vue';
import { useAlertStore } from '@/stores/alert';
const alert=useAlertStore();const button=ref(null);let previous=null;
let overflow='';
function keydown(event){if(event.key==='Escape'){alert.close();return}if(event.key==='Tab'){event.preventDefault();button.value?.focus();}}
watch(()=>alert.message,async value=>{if(value){previous=document.activeElement;overflow=document.body.style.overflow;document.body.style.overflow='hidden';document.addEventListener('keydown',keydown);await nextTick();button.value?.focus();}else{document.removeEventListener('keydown',keydown);document.body.style.overflow=overflow;await nextTick();previous?.focus?.();previous=null;}});
onBeforeUnmount(()=>{document.removeEventListener('keydown',keydown);document.body.style.overflow=overflow;});
</script>
<template><Teleport to="body"><div v-if="alert.message" class="alert-overlay"><section role="alertdialog" aria-modal="true" aria-labelledby="app-alert-title" aria-describedby="app-alert-message"><h2 id="app-alert-title">Giới hạn số lượng</h2><p id="app-alert-message">{{ alert.message }}</p><button ref="button" class="btn btn-primary" type="button" @click="alert.close">Đã hiểu</button></section></div></Teleport></template>
<style scoped>.alert-overlay{position:fixed;z-index:3000;inset:0;display:grid;place-items:center;padding:20px;background:rgba(15,23,42,.55)}section{width:min(440px,100%);padding:24px;border-radius:18px;background:#fff;box-shadow:0 24px 70px rgba(15,23,42,.28);text-align:center}h2{margin:0 0 10px}p{margin:0 0 20px;color:var(--text-mid);line-height:1.55}button{min-width:120px;min-height:44px}</style>
