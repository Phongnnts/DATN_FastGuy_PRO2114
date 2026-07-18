<script setup>
import { onMounted, ref } from 'vue';
import bannerApi from '@/api/banner';
import { useToastStore } from '@/stores/toast';

const toast = useToastStore();

const banners = ref([]);
const loading = ref(true);
const error = ref('');
const editing = ref(null);
const showModal = ref(false);
const saving = ref(false);
const form = ref(defaultForm());

function defaultForm() { return { title: '', subtitle: '', imageUrl: '', link: '', sortOrder: 0, isActive: true }; }
async function load() { loading.value = true; error.value = ''; try { banners.value = await bannerApi.getAll() || []; } catch (e) { error.value = e.message || 'Không thể tải banner'; } finally { loading.value = false; } }
function create() { editing.value = null; form.value = defaultForm(); showModal.value = true; }
function edit(banner) { editing.value = banner; form.value = { title: banner.title || '', subtitle: banner.subtitle || '', imageUrl: banner.imageUrl || '', link: banner.link || '', sortOrder: Number(banner.sortOrder || 0), isActive: banner.isActive !== false }; showModal.value = true; }
async function save() { if (!form.value.title.trim() || !form.value.imageUrl.trim()) return toast.error('Nhập tiêu đề và URL ảnh'); saving.value = true; try { const data = { ...form.value, title: form.value.title.trim(), imageUrl: form.value.imageUrl.trim(), sortOrder: Number(form.value.sortOrder || 0) }; if (editing.value) await bannerApi.update(editing.value.bannerId, data); else await bannerApi.create(data); showModal.value = false; await load(); } catch (e) { toast.error(e.message || 'Không thể lưu banner'); } finally { saving.value = false; } }
async function toggle(banner) { const previous = banner.isActive; banner.isActive = !previous; try { await bannerApi.update(banner.bannerId, { isActive: banner.isActive }); } catch (e) { banner.isActive = previous; toast.error(e.message || 'Không thể cập nhật banner'); } }
async function remove(banner) { if (!confirm(`Xóa banner "${banner.title}"?`)) return; try { await bannerApi.delete(banner.bannerId); await load(); } catch (e) { toast.error(e.message || 'Không thể xóa banner'); } }
onMounted(load);
</script>

<template>
  <div>
    <div class="page-header"><div><h1>Quản lý banner</h1><p>Banner hiển thị trên trang chủ.</p></div><button class="btn btn-primary" @click="create"><i class="bi bi-plus-lg"></i> Thêm banner</button></div>
    <div v-if="loading" class="admin-state"><span class="spinner"></span> Đang tải banner...</div>
    <div v-else-if="error" class="admin-state admin-error"><span>{{ error }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
    <div v-else-if="!banners.length" class="empty-state"><i class="bi bi-images"></i><h3>Chưa có banner</h3><p>Thêm banner để quảng bá ưu đãi trên trang chủ.</p></div>
    <div v-else class="banner-grid"><article v-for="banner in banners" :key="banner.bannerId" class="banner-card"><img :src="banner.imageUrl" :alt="banner.title"><div class="banner-body"><strong>{{ banner.title }}</strong><p>{{ banner.subtitle || 'Không có mô tả' }}</p><div class="banner-actions"><label class="toggle-switch" :aria-label="`Kích hoạt ${banner.title}`"><input type="checkbox" :checked="banner.isActive" @change="toggle(banner)"><span class="toggle-slider"></span></label><button class="btn btn-sm btn-ghost" :aria-label="`Sửa ${banner.title}`" @click="edit(banner)"><i class="bi bi-pencil"></i></button><button class="btn btn-sm btn-ghost text-danger" :aria-label="`Xóa ${banner.title}`" @click="remove(banner)"><i class="bi bi-trash3"></i></button></div></div></article></div>
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false"><div class="modal" role="dialog" aria-modal="true" aria-labelledby="banner-modal-title"><div class="modal-header"><h2 id="banner-modal-title" class="modal-title">{{ editing ? 'Sửa banner' : 'Thêm banner' }}</h2><button class="modal-close" aria-label="Đóng" @click="showModal = false"><i class="bi bi-x-lg"></i></button></div><form class="modal-body" @submit.prevent="save"><div class="form-group"><label class="form-label" for="banner-title">Tiêu đề *</label><input id="banner-title" v-model="form.title" class="form-input" maxlength="200"></div><div class="form-group"><label class="form-label" for="banner-subtitle">Phụ đề</label><input id="banner-subtitle" v-model="form.subtitle" class="form-input" maxlength="500"></div><div class="form-group"><label class="form-label" for="banner-image">URL ảnh *</label><input id="banner-image" v-model="form.imageUrl" type="url" class="form-input"></div><div class="form-group"><label class="form-label" for="banner-link">Liên kết</label><input id="banner-link" v-model="form.link" class="form-input" maxlength="500"></div><div class="form-group"><label class="form-label" for="banner-order">Thứ tự</label><input id="banner-order" v-model.number="form.sortOrder" type="number" class="form-input" min="0"></div><label class="checkbox-label"><input v-model="form.isActive" type="checkbox"> Kích hoạt banner</label><div class="modal-footer"><button type="button" class="btn btn-ghost" @click="showModal = false">Hủy</button><button type="submit" class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu' }}</button></div></form></div></div>
  </div>
</template>

<style scoped>
.page-header p { margin:4px 0 0; color:var(--text-mid); font-size:14px; }
.admin-state { display:flex; justify-content:center; align-items:center; gap:10px; min-height:180px; color:var(--text-mid); }.admin-error { flex-direction:column; color:var(--red-active); }
.banner-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:16px; }.banner-card { overflow:hidden; border:1px solid var(--border); border-radius:var(--radius); background:#fff; }.banner-card img { display:block; width:100%; height:140px; object-fit:cover; background:var(--surface); }.banner-body { padding:14px; }.banner-body p { margin:6px 0 12px; color:var(--text-mid); font-size:13px; min-height:20px; }.banner-actions { display:flex; align-items:center; gap:4px; }.banner-actions .toggle-switch { margin-right:auto; }
.toggle-switch { position:relative; display:inline-block; width:42px; height:24px; }.toggle-switch input { opacity:0; width:0; height:0; }.toggle-slider { position:absolute; inset:0; cursor:pointer; border-radius:99px; background:var(--border); }.toggle-slider::before { content:''; position:absolute; width:18px; height:18px; left:3px; bottom:3px; border-radius:50%; background:#fff; transition:transform var(--transition-fast); }.toggle-switch input:checked + .toggle-slider { background:var(--role-accent,var(--primary)); }.toggle-switch input:checked + .toggle-slider::before { transform:translateX(18px); }.checkbox-label { display:flex; gap:8px; align-items:center; color:var(--text-mid); }
</style>
