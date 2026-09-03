<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';
import { buildItemPayload } from '@/utils/inventoryItem';

const router = useRouter();
const toast = useToast();
const items = ref([]);
const loading = ref(true);
const error = ref('');
const search = ref('');
const status = ref('ACTIVE');
const editing = ref(null);
const saving = ref(false);
const form = ref(blank());

function blank() { return { inventoryCode: '', name: '', itemType: 'INGREDIENT', baseUnit: 'G', minimumQuantity: '0', countFrequency: 'DAILY', active: true }; }
const ingredients = computed(() => items.value.filter(item => item.itemType === 'INGREDIENT').filter(item => status.value === 'ALL' || (status.value === 'ACTIVE') === (item.active !== false)).filter(item => `${item.name} ${item.inventoryCode}`.toLocaleLowerCase('vi').includes(search.value.trim().toLocaleLowerCase('vi'))));
const unit = value => ({ G: 'g', ML: 'ml', PIECE: 'cái' }[value] || value);
const money = value => Number(value || 0).toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 });
async function load() { loading.value = true; error.value = ''; try { const data = await adminApi.getInventoryItems(); items.value = Array.isArray(data) ? data : []; } catch (e) { error.value = e.message || 'Không thể tải nguyên liệu'; } finally { loading.value = false; } }
function create() { editing.value = { mode: 'create' }; form.value = blank(); }
function edit(item) { editing.value = item; form.value = { inventoryCode: item.inventoryCode, name: item.name, itemType: 'INGREDIENT', baseUnit: item.baseUnit, minimumQuantity: String(item.minimumQuantity), countFrequency: 'DAILY', active: item.active !== false }; }
async function save() { if (!form.value.name.trim() || !/^[A-Z0-9-]{1,30}$/.test(form.value.inventoryCode.trim().toUpperCase())) { error.value = 'Nhập tên và mã nguyên liệu hợp lệ'; return; } saving.value = true; error.value = ''; try { const payload = buildItemPayload(form.value); if (editing.value.mode === 'create') await adminApi.createInventoryItem(payload); else await adminApi.updateInventoryItem(editing.value.inventoryItemId, payload); toast.success(editing.value.mode === 'create' ? 'Đã thêm nguyên liệu' : 'Đã lưu nguyên liệu'); editing.value = null; await load(); } catch (e) { error.value = e.message || 'Không thể lưu nguyên liệu'; } finally { saving.value = false; } }
onMounted(load);
</script>

<template>
  <main class="ingredients-page">
    <header class="page-header"><div><p class="eyebrow">Kho · Dữ liệu nền</p><h1>Nguyên liệu</h1><p>Tạo nguyên liệu trước, sau đó nhập hàng để tăng tồn và hình thành giá vốn.</p></div><button class="btn btn-primary" @click="create">Thêm nguyên liệu</button></header>
    <section class="summary" aria-label="Tổng quan nguyên liệu"><article><span>Đang sử dụng</span><strong>{{ items.filter(i => i.itemType === 'INGREDIENT' && i.active !== false).length }}</strong></article><article><span>Chưa có giá vốn</span><strong>{{ items.filter(i => i.itemType === 'INGREDIENT' && Number(i.averageUnitCost) <= 0).length }}</strong></article><article><span>Cần nhập thêm</span><strong>{{ items.filter(i => i.itemType === 'INGREDIENT' && Number(i.availableQuantity) <= Number(i.minimumQuantity)).length }}</strong></article></section>
    <section class="workspace"><div class="toolbar"><label><span class="sr-only">Tìm nguyên liệu</span><input v-model="search" class="form-input" type="search" placeholder="Tìm tên hoặc mã..." /></label><label><span class="sr-only">Trạng thái</span><select v-model="status" class="form-select"><option value="ACTIVE">Đang sử dụng</option><option value="INACTIVE">Ngừng sử dụng</option><option value="ALL">Tất cả</option></select></label></div>
      <p v-if="error" class="error" role="alert">{{ error }}</p><div v-if="loading" class="state" role="status">Đang tải nguyên liệu...</div><div v-else-if="!ingredients.length" class="empty"><h2>Chưa có nguyên liệu</h2><p>Thêm nguyên liệu để sử dụng trong phiếu nhập, công thức và kiểm kê.</p><button class="btn btn-primary" @click="create">Thêm nguyên liệu đầu tiên</button></div>
      <div v-else class="table-wrap"><table><thead><tr><th>Nguyên liệu</th><th>Đơn vị chuẩn</th><th>Ngưỡng cảnh báo</th><th>Giá vốn hiện tại</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody><tr v-for="item in ingredients" :key="item.inventoryItemId"><td><strong>{{ item.name }}</strong><small>{{ item.inventoryCode }}</small></td><td>{{ unit(item.baseUnit) }}</td><td>{{ item.minimumQuantity }} {{ unit(item.baseUnit) }}</td><td>{{ Number(item.averageUnitCost) > 0 ? `${money(item.averageUnitCost)}/${unit(item.baseUnit)}` : 'Chưa có dữ liệu' }}</td><td><span class="badge">{{ item.active !== false ? 'Đang sử dụng' : 'Ngừng sử dụng' }}</span></td><td><div class="actions"><button class="btn btn-sm btn-outline" @click="router.push({ name: 'AdminGoodsReceipts' })">Nhập hàng</button><button class="btn btn-sm btn-ghost" @click="edit(item)">Sửa</button></div></td></tr></tbody></table></div>
    </section>
    <div v-if="editing" class="backdrop" @mousedown.self="editing=null"><form class="dialog" role="dialog" aria-modal="true" aria-labelledby="ingredient-title" @submit.prevent="save"><header><div><small>NGUYÊN LIỆU</small><h2 id="ingredient-title">{{ editing.mode === 'create' ? 'Thêm nguyên liệu' : `Sửa ${editing.name}` }}</h2></div><button type="button" aria-label="Đóng" @click="editing=null">×</button></header><label>Mã nguyên liệu<input v-model="form.inventoryCode" class="form-input" maxlength="30" required /></label><label>Tên nguyên liệu<input v-model="form.name" class="form-input" maxlength="150" required /></label><label>Đơn vị chuẩn<select v-model="form.baseUnit" class="form-select" :disabled="editing.mode !== 'create'"><option value="G">g (gram)</option><option value="ML">ml (mililit)</option><option value="PIECE">cái</option></select></label><label>Ngưỡng cảnh báo<input v-model="form.minimumQuantity" class="form-input" type="number" min="0" step="0.0001" required /></label><label class="check"><input v-model="form.active" type="checkbox" /> Đang sử dụng</label><footer><button type="button" class="btn btn-outline" @click="editing=null">Hủy</button><button class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu nguyên liệu' }}</button></footer></form></div>
  </main>
</template>

<style scoped>
.ingredients-page{display:grid;gap:20px}.page-header,.toolbar,.actions,.dialog header,.dialog footer{display:flex;align-items:center;justify-content:space-between;gap:12px}.page-header h1{margin:2px 0;font-size:30px}.page-header p{margin:0;color:var(--admin-muted)}.eyebrow{font-size:11px;font-weight:800;letter-spacing:.1em;color:var(--admin-brand)!important;text-transform:uppercase}.summary{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.summary article,.workspace{padding:18px;border:1px solid var(--admin-border);border-radius:var(--admin-panel-radius);background:var(--admin-surface)}.summary article{display:grid;gap:6px}.summary span,small{color:var(--admin-muted)}.summary strong{font-size:26px}.workspace{display:grid;gap:14px}.toolbar{justify-content:flex-start}.toolbar label:first-child{flex:1;max-width:480px}.table-wrap{overflow-x:auto}table{width:100%;min-width:800px;border-collapse:collapse}th,td{padding:14px 10px;border-bottom:1px solid var(--admin-border);text-align:left}td:first-child{display:grid;gap:4px}.state,.empty{padding:48px;text-align:center}.error{padding:12px;background:var(--admin-danger-soft);color:var(--admin-danger)}.backdrop{position:fixed;z-index:120;inset:0;display:grid;place-items:center;padding:20px;background:#1118}.dialog{display:grid;gap:14px;width:min(520px,100%);padding:22px;border-radius:18px;background:#fff;box-shadow:var(--admin-shadow-elevated)}.dialog label{display:grid;gap:6px}.dialog .check{display:flex}.dialog header h2{margin:3px 0}.dialog header button{width:44px;height:44px}.dialog footer{justify-content:flex-end}@media(max-width:700px){.page-header{align-items:flex-start;flex-direction:column}.summary{grid-template-columns:1fr}.toolbar{align-items:stretch;flex-direction:column}.toolbar label:first-child{max-width:none}}
</style>
