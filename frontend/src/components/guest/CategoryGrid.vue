<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

const props = defineProps({ categories: { type: Array, required: true } });
const scroller = ref(null);
const canScrollLeft = ref(false);
const canScrollRight = ref(false);

function updateScrollState() {
  const element = scroller.value;
  if (!element) return;
  canScrollLeft.value = element.scrollLeft > 4;
  canScrollRight.value = element.scrollLeft + element.clientWidth < element.scrollWidth - 4;
}

function scrollCategories(direction) {
  scroller.value?.scrollBy({ left: direction * Math.min(scroller.value.clientWidth * .72, 620), behavior: 'smooth' });
}

function handleResize() { updateScrollState(); }

onMounted(() => {
  nextTick(updateScrollState);
  window.addEventListener('resize', handleResize);
});
onBeforeUnmount(() => window.removeEventListener('resize', handleResize));
watch(() => props.categories.length, () => nextTick(updateScrollState));
</script>

<template>
  <section class="category-section">
    <div class="container">
      <div class="section-head">
        <div>
          <p>Khám phá thực đơn</p>
          <h2>Danh mục món ăn</h2>
        </div>
        <router-link to="/menu" class="see-all">Xem tất cả <i class="bi bi-arrow-right"></i></router-link>
      </div>
      <div v-if="categories.length" class="category-rail" :class="{ 'fade-left': canScrollLeft, 'fade-right': canScrollRight }">
        <button v-show="canScrollLeft" class="scroll-btn prev" type="button" aria-label="Xem danh mục trước" @click="scrollCategories(-1)"><i class="bi bi-chevron-left"></i></button>
        <div ref="scroller" class="category-grid" @scroll.passive="updateScrollState">
          <router-link
            v-for="category in categories"
            :key="category.id"
            :to="{ path: '/menu', query: { category: category.id } }"
            class="category-card"
          >
            <span class="category-name">{{ category.name }}</span>
          </router-link>
        </div>
        <button v-show="canScrollRight" class="scroll-btn next" type="button" aria-label="Xem danh mục tiếp theo" @click="scrollCategories(1)"><i class="bi bi-chevron-right"></i></button>
      </div>
      <p v-else class="section-empty">Danh mục đang được cập nhật.</p>
    </div>
  </section>
</template>

<style scoped>
.category-section {
  padding: 72px 0;
  overflow: hidden;
  background:
    radial-gradient(circle at 90% 0, rgba(232, 115, 74, .08), transparent 28%),
    #fff;
}

.section-head {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 28px;
}

.section-head p {
  margin-bottom: 7px;
  color: var(--primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .14em;
  text-transform: uppercase;
}

.section-head h2 {
  font-size: clamp(27px, 4vw, 40px);
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -.04em;
}

.see-all {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 7px;
  min-height: 44px;
  color: var(--primary);
  font-size: 14px;
  font-weight: 700;
  white-space: nowrap;
}

.category-rail {
  position: relative;
  padding: 8px;
  border: 1px solid rgba(23, 23, 23, .07);
  border-radius: 25px;
  background: rgba(250, 247, 244, .82);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .8), 0 14px 36px rgba(49, 32, 23, .05);
}

.category-rail::before,
.category-rail::after {
  position: absolute;
  z-index: 1;
  top: 8px;
  bottom: 8px;
  width: 72px;
  pointer-events: none;
  opacity: 0;
  content: "";
  transition: opacity var(--transition-fast);
}

.category-rail::before { left: 8px; background: linear-gradient(90deg, #faf7f4 12%, transparent); }
.category-rail::after { right: 8px; background: linear-gradient(270deg, #faf7f4 12%, transparent); }
.category-rail.fade-left::before,
.category-rail.fade-right::after { opacity: 1; }

.category-grid {
  display: flex;
  gap: 6px;
  padding: 0;
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  scrollbar-width: none;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
}

.category-grid::-webkit-scrollbar { display: none; }

.category-card {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  min-height: 54px;
  padding: 12px 22px;
  border: 1px solid transparent;
  border-radius: 18px;
  color: var(--text-dark);
  background: transparent;
  scroll-snap-align: start;
  transition: color var(--transition-fast), border-color var(--transition-fast), background var(--transition-fast), box-shadow var(--transition-fast), transform var(--transition-fast);
}

.category-name {
  font-size: 14px;
  font-weight: 700;
  line-height: 1.3;
  letter-spacing: -.01em;
  white-space: nowrap;
}

.category-card:hover,
.category-card:focus-visible {
  color: var(--primary-dark);
  background: #fff;
  border-color: rgba(232, 115, 74, .18);
  box-shadow: 0 8px 24px rgba(72, 39, 24, .09);
  outline: none;
  transform: translateY(-2px);
}

.category-card.router-link-exact-active {
  color: #fff;
  background: var(--charcoal);
  border-color: var(--charcoal);
}

.scroll-btn {
  position: absolute;
  z-index: 2;
  top: 50%;
  display: grid;
  width: 38px;
  height: 38px;
  min-height: 38px;
  place-items: center;
  border: 1px solid rgba(23, 23, 23, .09);
  border-radius: 50%;
  color: var(--text-dark);
  background: #fff;
  box-shadow: 0 8px 20px rgba(23, 23, 23, .12);
  transform: translateY(-50%);
  transition: color var(--transition-fast), background var(--transition-fast), transform var(--transition-fast);
}

.scroll-btn:hover { color: #fff; background: var(--charcoal); transform: translateY(-50%) scale(1.05); }
.scroll-btn.prev { left: 14px; }
.scroll-btn.next { right: 14px; }

.section-empty {
  margin: 0;
  color: var(--text-mid);
}

@media (max-width: 700px) {
  .category-section { padding: 48px 0; }
  .category-rail { margin-right: -16px; padding: 6px 16px 6px 6px; border-right: 0; border-radius: 20px 0 0 20px; }
  .category-rail::before,
  .category-rail::after { display: none; }
  .category-grid { gap: 4px; }
  .category-card { min-height: 46px; padding: 10px 16px; border-radius: 14px; }
  .category-name { font-size: 13px; }
  .scroll-btn { display: none; }
}

@media (max-width: 430px) {
  .section-head { align-items: center; margin-bottom: 22px; }
  .section-head h2 { font-size: 25px; }
  .see-all { font-size: 13px; }
}

@media (prefers-reduced-motion: reduce) {
  .category-card { transition: none; }
}
</style>
