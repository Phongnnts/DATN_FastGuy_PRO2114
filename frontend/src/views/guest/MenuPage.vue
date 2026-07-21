<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useProductStore } from '@/stores/product';
import ProductCard from '@/components/common/ProductCard.vue';

const store = useProductStore();
const route = useRoute();
const router = useRouter();
const q = ref('');
const category = ref(null);
const price = ref('ALL');
const min = ref('');
const max = ref('');
const availability = ref('ALL');
const productType = ref('ALL');
const discounted = ref(false);
const bestSeller = ref(false);
const sort = ref('name');
const page = ref(1);
const view = ref('grid');
const drawer = ref(false);
const mobile = ref(false);
const drawerPanel = ref(null);
const closeButton = ref(null);
const drawerTrigger = ref(null);
const pageSize = 12;
let searchTimer;
let restoring = false;
const prices = [{ key: 'ALL', label: 'Mọi mức giá' }, { key: 'UNDER_30', label: 'Dưới 30.000₫' }, { key: '30_60', label: '30.000₫ – 60.000₫' }, { key: 'OVER_60', label: 'Trên 60.000₫' }, { key: 'CUSTOM', label: 'Tùy chỉnh' }];
const availabilityOptions = [{ key: 'ALL', label: 'Tất cả' }, { key: 'AVAILABLE', label: 'Đang bán' }, { key: 'OUT_OF_STOCK', label: 'Hết hàng' }, { key: 'OUTSIDE_HOURS', label: 'Ngoài giờ bán' }];
const categories = computed(() => [{ id: null, name: 'Tất cả', count: store.allCategories.reduce((sum, item) => sum + item.productCount, 0) || store.allProducts.length }, ...store.allCategories.map((item) => ({ ...item, count: item.productCount || store.allProducts.filter((product) => product.categoryId === item.id).length }))]);
const totalPages = computed(() => store.catalogMeta.totalPages);
const pages = computed(() => [...new Set([1, totalPages.value, page.value - 1, page.value, page.value + 1].filter((value) => value > 0 && value <= totalPages.value).sort((a, b) => a - b))].reduce((result, value, index, array) => { if (index && value - array[index - 1] > 1) result.push('…'); result.push(value); return result; }, []));
const chips = computed(() => [q.value && { key: 'q', label: `“${q.value}”` }, category.value && { key: 'category', label: store.allCategories.find((item) => item.id === category.value)?.name }, price.value !== 'ALL' && { key: 'price', label: prices.find((item) => item.key === price.value)?.label }, availability.value !== 'ALL' && { key: 'availability', label: availabilityOptions.find((item) => item.key === availability.value)?.label }, productType.value !== 'ALL' && { key: 'productType', label: { SIMPLE: 'Món đơn', VARIANT: 'Nhiều lựa chọn', COMBO: 'Combo', CUSTOMIZABLE: 'Có tùy chọn' }[productType.value] }, discounted.value && { key: 'discounted', label: 'Đang giảm giá' }, bestSeller.value && { key: 'bestSeller', label: 'Bán chạy' }].filter(Boolean));
function priceParams() {
  if (price.value === 'UNDER_30') return { maxPrice: 29999 };
  if (price.value === '30_60') return { minPrice: 30000, maxPrice: 60000 };
  if (price.value === 'OVER_60') return { minPrice: 60001 };
  if (price.value === 'CUSTOM') return { minPrice: min.value || undefined, maxPrice: max.value || undefined };
  return {};
}
function fetchCatalog() { store.fetchCatalog({ q: q.value.trim() || undefined, categoryId: category.value || undefined, ...priceParams(), availability: availability.value, productType: productType.value === 'ALL' ? undefined : productType.value, discounted: discounted.value || undefined, sold: bestSeller.value ? 1 : undefined, sort: sort.value, page: page.value - 1, size: pageSize }); }
function hydrate() {
  restoring = true;
  q.value = String(route.query.q || ''); category.value = route.query.category ? Number(route.query.category) : null;
  price.value = prices.some((item) => item.key === route.query.price) ? route.query.price : 'ALL'; min.value = String(route.query.min || ''); max.value = String(route.query.max || '');
  availability.value = availabilityOptions.some((item) => item.key === route.query.availability) ? route.query.availability : 'ALL';
  productType.value = ['SIMPLE', 'VARIANT', 'COMBO', 'CUSTOMIZABLE'].includes(route.query.productType) ? route.query.productType : 'ALL'; discounted.value = route.query.discounted === 'true'; bestSeller.value = route.query.bestSeller === 'true';
  sort.value = ['name', 'name-desc', 'newest', 'price-asc', 'price-desc', 'best-selling', 'discount-desc'].includes(route.query.sort) ? route.query.sort : 'name'; page.value = Math.max(1, Number(route.query.page) || 1); view.value = route.query.view === 'list' ? 'list' : 'grid';
  nextTick(() => { restoring = false; fetchCatalog(); });
}
function queryValue() { const query = {}; if (q.value.trim()) query.q = q.value.trim(); if (category.value) query.category = category.value; if (price.value !== 'ALL') query.price = price.value; if (price.value === 'CUSTOM' && min.value) query.min = min.value; if (price.value === 'CUSTOM' && max.value) query.max = max.value; if (availability.value !== 'ALL') query.availability = availability.value; if (productType.value !== 'ALL') query.productType = productType.value; if (discounted.value) query.discounted = 'true'; if (bestSeller.value) query.bestSeller = 'true'; if (sort.value !== 'name') query.sort = sort.value; if (page.value > 1) query.page = page.value; if (view.value === 'list') query.view = 'list'; return query; }
function navigate(replace = false) { if (!restoring) router[replace ? 'replace' : 'push']({ query: queryValue() }); }
function reset() { q.value = ''; category.value = null; price.value = 'ALL'; min.value = ''; max.value = ''; availability.value = 'ALL'; productType.value = 'ALL'; discounted.value = false; bestSeller.value = false; sort.value = 'name'; page.value = 1; }
function remove(key) { if (key === 'q') q.value = ''; if (key === 'category') category.value = null; if (key === 'price') { price.value = 'ALL'; min.value = ''; max.value = ''; } if (key === 'availability') availability.value = 'ALL'; if (key === 'productType') productType.value = 'ALL'; if (key === 'discounted') discounted.value = false; if (key === 'bestSeller') bestSeller.value = false; }
function openDrawer() { drawer.value = true; document.body.style.overflow = 'hidden'; nextTick(() => closeButton.value?.focus()); }
function closeDrawer() { drawer.value = false; document.body.style.overflow = ''; nextTick(() => drawerTrigger.value?.focus()); }
function onKeydown(event) {
  if (!drawer.value) return;
  if (event.key === 'Escape') return closeDrawer();
  if (event.key !== 'Tab') return;
  const focusable = [...drawerPanel.value.querySelectorAll('button:not([disabled]),input:not([disabled]),select:not([disabled]),a[href]')];
  if (!focusable.length) return;
  const first = focusable[0]; const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); } else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
}
watch(() => route.query, hydrate);
watch([category, price, min, max, availability, productType, discounted, bestSeller, sort], () => { if (restoring) return; page.value = 1; navigate(); });
watch(page, () => { if (!restoring) navigate(); });
watch(view, () => { if (!restoring) navigate(); });
watch(q, () => { if (restoring) return; page.value = 1; clearTimeout(searchTimer); searchTimer = setTimeout(() => navigate(true), 300); });
onMounted(() => { mobile.value = window.matchMedia('(max-width: 900px)').matches; hydrate(); store.init(); window.addEventListener('keydown', onKeydown); });
onBeforeUnmount(() => { clearTimeout(searchTimer); document.body.style.overflow = ''; window.removeEventListener('keydown', onKeydown); });
</script>

<template>
  <div class="menu-page"><div class="container">
    <nav class="breadcrumb" aria-label="Điều hướng"><router-link to="/">Trang chủ</router-link><i class="bi bi-chevron-right"></i><span>Thực đơn</span></nav>
    <header><div><p>FASTGUY MENU</p><h1>Chọn món theo cách của bạn</h1><span>Khám phá món ngon, tìm nhanh, lọc chính xác.</span></div><label class="search"><span class="sr-only">Tìm món</span><i class="bi bi-search"></i><input v-model="q" type="search" placeholder="Tìm món..."></label></header>
    <div class="category-chips"><button v-for="item in categories" :key="item.id ?? 'all'" :class="{ active: category === item.id }" @click="category = item.id">{{ item.name }}</button></div>
    <div class="mobile-tools"><button ref="drawerTrigger" aria-controls="menu-filters" :aria-expanded="drawer" @click="openDrawer"><i class="bi bi-sliders"></i> Bộ lọc <span v-if="chips.length">{{ chips.length }}</span></button><select v-model="sort" aria-label="Sắp xếp"><option value="name">Tên A–Z</option><option value="best-selling">Bán chạy nhất</option><option value="discount-desc">Giảm giá nhiều nhất</option><option value="name-desc">Tên Z–A</option><option value="newest">Mới nhất</option><option value="price-asc">Giá tăng</option><option value="price-desc">Giá giảm</option></select></div>
    <div v-if="drawer" class="backdrop" @click="closeDrawer"></div>
    <div class="layout">
      <aside id="menu-filters" ref="drawerPanel" class="menu-sidebar" :class="{ open: drawer }" :role="drawer ? 'dialog' : undefined" :aria-modal="drawer ? 'true' : undefined" :inert="mobile && !drawer ? '' : undefined"><div class="drawer-head"><strong>Bộ lọc</strong><button ref="closeButton" aria-label="Đóng bộ lọc" @click="closeDrawer"><i class="bi bi-x-lg"></i></button></div><div class="filter-head"><h2>Danh mục</h2><button v-if="chips.length" @click="reset">Đặt lại</button></div><div class="categories"><button v-for="item in categories" :key="item.id ?? 'all'" :class="{ active: category === item.id }" @click="category = item.id"><span>{{ item.name }}</span><small>{{ item.count }}</small></button></div><fieldset><legend>Loại món</legend><label><input v-model="productType" type="radio" name="productType" value="ALL"> Tất cả</label><label><input v-model="productType" type="radio" name="productType" value="SIMPLE"> Món đơn</label><label><input v-model="productType" type="radio" name="productType" value="VARIANT"> Nhiều lựa chọn</label><label><input v-model="productType" type="radio" name="productType" value="CUSTOMIZABLE"> Có tùy chọn</label><label><input v-model="productType" type="radio" name="productType" value="COMBO"> Combo</label></fieldset><fieldset><legend>Ưu tiên</legend><label><input v-model="discounted" type="checkbox"> Đang giảm giá</label><label><input v-model="bestSeller" type="checkbox"> Bán chạy</label></fieldset><fieldset><legend>Khoảng giá</legend><label v-for="item in prices" :key="item.key"><input v-model="price" type="radio" name="price" :value="item.key"> {{ item.label }}</label><div v-if="price === 'CUSTOM'" class="price-range"><input v-model="min" type="number" min="0" placeholder="Từ" aria-label="Giá tối thiểu"><input v-model="max" type="number" min="0" placeholder="Đến" aria-label="Giá tối đa"></div></fieldset><fieldset><legend>Tình trạng</legend><label v-for="item in availabilityOptions" :key="item.key"><input v-model="availability" type="radio" name="availability" :value="item.key"> {{ item.label }}</label></fieldset><button class="apply" @click="closeDrawer">Xem {{ store.catalogMeta.totalItems }} món</button></aside>
      <main class="content"><div class="result"><strong>{{ store.catalogMeta.totalItems }} món</strong><div class="view-toggle" aria-label="Kiểu hiển thị"><button :class="{ active: view === 'grid' }" aria-label="Dạng lưới" @click="view = 'grid'"><i class="bi bi-grid"></i></button><button :class="{ active: view === 'list' }" aria-label="Dạng danh sách" @click="view = 'list'"><i class="bi bi-list"></i></button></div><select v-model="sort" aria-label="Sắp xếp"><option value="name">Tên A–Z</option><option value="best-selling">Bán chạy nhất</option><option value="discount-desc">Giảm giá nhiều nhất</option><option value="name-desc">Tên Z–A</option><option value="newest">Mới nhất</option><option value="price-asc">Giá tăng dần</option><option value="price-desc">Giá giảm dần</option></select></div><div v-if="chips.length" class="chips"><button v-for="chip in chips" :key="chip.key" @click="remove(chip.key)">{{ chip.label }} <i class="bi bi-x"></i></button><button class="clear" @click="reset">Xóa tất cả</button></div>
        <div v-if="store.catalogLoading" class="grid"><div v-for="n in 12" :key="n" class="skeleton"></div></div><section v-else-if="store.catalogError" class="state" role="alert"><h2>Không thể tải thực đơn</h2><p>{{ store.catalogError }}</p><button @click="fetchCatalog">Thử lại</button></section><section v-else-if="!store.catalog.length" class="state"><i class="bi bi-search"></i><h2>Không tìm thấy món</h2><button @click="reset">Xóa bộ lọc</button></section><template v-else><div class="grid" :class="{ list: view === 'list' }"><ProductCard v-for="product in store.catalog" :key="product.productId" :product="product" :list-mode="view === 'list'" /></div><nav v-if="totalPages > 1" class="pagination" aria-label="Phân trang"><button :disabled="page === 1" @click="page--">‹</button><template v-for="item in pages" :key="item"><span v-if="item === '…'">…</span><button v-else :class="{ active: page === item }" :aria-current="page === item ? 'page' : undefined" @click="page = item">{{ item }}</button></template><button :disabled="page === totalPages" @click="page++">›</button></nav></template>
      </main>
    </div>
  </div></div>
</template>

<style scoped>
.menu-page{min-height:100vh;padding:28px 0 64px;background:#fffaf6}.breadcrumb{display:flex;gap:8px;color:var(--text-mid);font-size:12px}header{display:flex;align-items:end;justify-content:space-between;gap:32px;padding:34px 0}header p{color:var(--primary);font-size:11px;font-weight:900;letter-spacing:.15em}h1{font-size:clamp(34px,5vw,52px);line-height:1.05;letter-spacing:-.05em}header span{color:var(--text-mid)}.search{position:relative;width:min(390px,100%)}.search i{position:absolute;left:16px;top:16px}.search input,.result select,.mobile-tools select{width:100%;height:48px;padding:0 16px;border:1px solid var(--border);border-radius:14px;background:#fff}.search input{padding-left:44px}.layout{display:grid;grid-template-columns:240px minmax(0,1fr);gap:28px}.menu-sidebar{position:sticky;top:84px;height:max-content;padding:20px;border:1px solid var(--border);border-radius:20px;background:#fff}.drawer-head,.mobile-tools,.category-chips{display:none}.filter-head,.result{display:flex;align-items:center;justify-content:space-between}.filter-head h2,legend{font-size:12px;text-transform:uppercase}.filter-head button,.clear{color:var(--primary)}.categories{display:grid;margin-top:10px}.categories button{display:flex;justify-content:space-between;padding:10px;border-radius:10px;color:var(--text-mid)}.categories button.active{color:var(--primary);background:var(--primary-light);font-weight:800}fieldset{margin-top:20px;padding-top:18px;border:0;border-top:1px solid var(--border)}fieldset label{display:block;padding:7px 0;color:var(--text-mid);font-size:13px}.price-range{display:grid;grid-template-columns:1fr 1fr;gap:6px}.price-range input{min-width:0;padding:8px;border:1px solid var(--border);border-radius:8px}.result{margin-bottom:16px}.result>select{width:180px}.view-toggle{display:flex;margin-left:auto;margin-right:10px}.view-toggle button{width:44px;height:44px;border-radius:10px}.view-toggle button.active{color:#fff;background:var(--primary)}.chips{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:16px}.chips button{padding:7px 10px;border-radius:99px;background:var(--primary-light)}.grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}.grid.list{grid-template-columns:1fr}.skeleton{height:280px;border-radius:20px;background:linear-gradient(90deg,#eee 25%,#fafafa 50%,#eee 75%);background-size:200%;animation:pulse 1.4s infinite}.state{text-align:center;padding:80px 20px}.pagination{display:flex;justify-content:center;align-items:center;gap:6px;margin-top:30px}.pagination button{min-width:44px;height:44px;border-radius:10px}.pagination button.active{color:#fff;background:var(--primary)}@keyframes pulse{to{background-position:-200%}}
@media(max-width:900px){header{align-items:stretch;flex-direction:column}.layout{grid-template-columns:1fr}.result>select{display:none}.mobile-tools,.category-chips{display:flex;gap:8px;margin-bottom:18px}.category-chips{overflow:auto}.category-chips button{flex:none;min-height:44px;padding:8px 14px;border:1px solid var(--border);border-radius:99px;background:#fff}.category-chips button.active{color:#fff;background:var(--primary)}.mobile-tools>*{flex:1}.mobile-tools button{height:48px;border:1px solid var(--border);border-radius:14px;background:#fff}.backdrop{position:fixed;z-index:40;inset:0;background:#0007}.menu-sidebar{position:fixed;z-index:41;inset:0 0 0 auto;width:min(88vw,370px);overflow:auto;border-radius:20px 0 0 20px;transform:translateX(105%);transition:transform .25s}.menu-sidebar.open{transform:none}.drawer-head{display:flex;justify-content:space-between;margin-bottom:20px}.drawer-head button{width:44px;height:44px}.apply{display:block;width:100%}.grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(min-width:901px){.menu-sidebar[inert]{pointer-events:auto}.apply{display:none}}@media(max-width:560px){.grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.grid.list{grid-template-columns:1fr}.state{padding:50px 10px}}@media(prefers-reduced-motion:reduce){.skeleton{animation:none}.menu-sidebar{transition:none}}
</style>
