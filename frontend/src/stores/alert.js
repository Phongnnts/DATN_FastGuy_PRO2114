import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useAlertStore = defineStore('alert', () => {
  const message = ref('');
  function show(value) { message.value = value; }
  function close() { message.value = ''; }
  return { message, show, close };
});
