<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  size: { type: Number, default: 18 },
})
const emit = defineEmits(['update:modelValue'])
const buttons = ref([])

function setRating(value) {
  if (!props.readonly) emit('update:modelValue', value)
}
async function handleKey(event) {
  if (props.readonly || !['ArrowRight', 'ArrowUp', 'ArrowLeft', 'ArrowDown', 'Home', 'End'].includes(event.key)) return
  event.preventDefault()
  const current = Number.isInteger(props.modelValue) && props.modelValue > 0 ? props.modelValue : 1
  const value = event.key === 'Home' ? 1 : event.key === 'End' ? 5 : event.key === 'ArrowRight' || event.key === 'ArrowUp' ? Math.min(5, current + 1) : Math.max(1, current - 1)
  setRating(value)
  await nextTick()
  buttons.value[value - 1]?.focus()
}
</script>

<template>
  <div class="star-rating" :class="{ readonly }" :role="readonly ? 'img' : 'radiogroup'" :aria-label="readonly ? `${modelValue} trên 5 sao` : 'Chọn số sao đánh giá'" @keydown="handleKey">
    <template v-if="readonly"><i v-for="value in 5" :key="value" class="bi" :class="value <= Math.floor(modelValue) ? 'bi-star-fill active' : value === Math.ceil(modelValue) && modelValue % 1 >= .5 ? 'bi-star-half active' : 'bi-star'" :style="{ fontSize: size + 'px' }" aria-hidden="true"></i></template>
    <button v-for="value in readonly ? [] : 5" v-else :key="value" ref="buttons" type="button" role="radio" :aria-label="`${value} sao`" :aria-checked="modelValue === value" :tabindex="modelValue === value || (!modelValue && value === 1) ? 0 : -1" @click="setRating(value)"><i class="bi" :class="value <= modelValue ? 'bi-star-fill active' : 'bi-star'" :style="{ fontSize: size + 'px' }" aria-hidden="true"></i></button>
  </div>
</template>

<style scoped>
.star-rating { display: inline-flex; }
.star-rating button { border: 0; padding: 2px; background: transparent; color: inherit; cursor: pointer; }
.star-rating button:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }
.star-rating i { transition: all var(--transition-fast); }
.star-rating:not(.readonly) button:hover i { transform: scale(1.2); }
</style>
