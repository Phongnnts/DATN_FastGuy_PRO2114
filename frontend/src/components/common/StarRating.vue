<script setup>
import { computed } from 'vue';

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false },
  size: { type: Number, default: 18 },
});

const emit = defineEmits(['update:modelValue']);

const stars = computed(() => {
  const full = Math.floor(props.modelValue);
  const half = props.modelValue - full >= 0.5;
  const empty = 5 - full - (half ? 1 : 0);
  return { full, half, empty };
});

function setRating(val) {
  if (!props.readonly) emit('update:modelValue', val);
}
</script>

<template>
  <div class="star-rating" :class="{ readonly }" style="cursor:pointer">
    <template v-for="i in stars.full" :key="'f' + i">
      <i
        class="bi bi-star-fill active"
        :style="{ fontSize: size + 'px' }"
        @click="setRating(i)"
      ></i>
    </template>
    <i
      v-if="stars.half"
      class="bi bi-star-half active"
      :style="{ fontSize: size + 'px' }"
      @click="setRating(stars.full + 1)"
    ></i>
    <template v-for="i in stars.empty" :key="'e' + i">
      <i
        class="bi bi-star"
        :class="{ active: stars.full + (stars.half ? 1 : 0) + i <= modelValue }"
        :style="{ fontSize: size + 'px' }"
        @click="setRating(stars.full + (stars.half ? 1 : 0) + i)"
      ></i>
    </template>
  </div>
</template>

<style scoped>
.star-rating i {
  transition: all var(--transition-fast);
}
.star-rating:not(.readonly) i:hover {
  transform: scale(1.2);
}
</style>
