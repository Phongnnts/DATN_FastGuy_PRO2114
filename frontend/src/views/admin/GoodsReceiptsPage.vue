<script setup>
import { computed, onMounted, ref } from 'vue';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';
import { buildGoodsReceiptPayload, goodsReceiptPreview, validateGoodsReceipt } from '@/utils/inventoryOperations';
import { formatQuantity } from '@/utils/inventoryItem';

const toast = useToast();
const receipts = ref([]);
const inventoryItems = ref([]);
const loading = ref(true);
const loadError = ref('');
const saving = ref(false);
const formError = ref('');
const editingId = ref(null);
const form = ref(blankForm());
const itemById = computed(() => new Map(inventoryItems.value.map((item) => [item.inventoryItemId, item])));

function localDateTime() {
  const date = new Date();
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset());
  return date.toISOString().slice(0, 16);
}
function blankLine() { return { inventoryItemId: '', purchaseQuantity: '', purchaseUnit: '', conversionFactor: '1', purchaseUnitPrice: '' }; }
function blankForm() { return { supplierName: '', invoiceNumber: '', receivedAt: localDateTime(), items: [blankLine()] }; }
function money(value) { return Number(value).toLocaleString('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 }); }
function nameFor(id) { return itemById.value.get(Number(id))?.name || `#${id}`; }

async function load() {
  loading.value = true; loadError.value = '';
  try {
    const [list, items] = await Promise.all([adminApi.getGoodsReceipts(), adminApi.getInventoryItems()]);
    receipts.value = Array.isArray(list) ? list : [];
    inventoryItems.value = Array.isArray(items) ? items : [];
  } catch (error) { loadError.value = error.message || 'Không thể tải phiếu nhập'; }
  finally { loading.value = false; }
}
function addLine() { form.value.items.push(blankLine()); }
function removeLine(index) { if (form.value.items.length > 1) form.value.items.splice(index, 1); }
function resetForm() { editingId.value = null; form.value = blankForm(); formError.value = ''; }
async function editReceipt(receipt) {
  formError.value = '';
  try {
    const detail = await adminApi.getGoodsReceipt(receipt.goodsReceiptId);
    editingId.value = detail.goodsReceiptId;
    form.value = { supplierName: detail.supplierName, invoiceNumber: detail.invoiceNumber || '', receivedAt: String(detail.receivedAt).slice(0, 16), items: detail.items.map((line) => ({ ...line, inventoryItemId: String(line.inventoryItemId), purchaseQuantity: String(line.purchaseQuantity), conversionFactor: String(line.conversionFactor), purchaseUnitPrice: String(line.purchaseUnitPrice) })) };
    document.querySelector('#receipt-form')?.scrollIntoView({ behavior: 'smooth' });
  } catch (error) { formError.value = error.message || 'Không thể mở phiếu nhập'; }
}
async function save() {
  const errors = validateGoodsReceipt(form.value);
  if (Object.keys(errors).length) { formError.value = Object.values(errors)[0]; return; }
  saving.value = true; formError.value = '';
  try {
    const payload = buildGoodsReceiptPayload(form.value);
    if (editingId.value) await adminApi.updateGoodsReceipt(editingId.value, payload); else await adminApi.createGoodsReceipt(payload);
    toast.success(editingId.value ? 'Đã cập nhật phiếu nhập' : 'Đã tạo phiếu nhập nháp'); resetForm(); await load();
  } catch (error) { formError.value = error.message || 'Không thể lưu phiếu nhập'; }
  finally { saving.value = false; }
}
async function approve(receipt) {
  if (!window.confirm(`Duyệt phiếu nhập #${receipt.goodsReceiptId}? Tồn kho và giá vốn sẽ được cập nhật.`)) return;
  try { await adminApi.approveGoodsReceipt(receipt.goodsReceiptId); toast.success('Đã duyệt phiếu nhập'); resetForm(); await load(); }
  catch (error) { loadError.value = error.message || 'Không thể duyệt phiếu nhập'; }
}
async function remove(receipt) {
  if (!window.confirm(`Xóa phiếu nhập nháp #${receipt.goodsReceiptId}?`)) return;
  try { await adminApi.deleteGoodsReceipt(receipt.goodsReceiptId); if (editingId.value === receipt.goodsReceiptId) resetForm(); await load(); }
  catch (error) { loadError.value = error.message || 'Không thể xóa phiếu nhập'; }
}
onMounted(load);
</script>

<template>
  <main class="ops-page">
    <header class="page-heading"><div><p class="eyebrow">Tồn kho</p><h1>Phiếu nhập hàng</h1><p>Tạo nháp, kiểm tra quy đổi và duyệt nhập kho.</p></div><button class="btn btn-outline" :disabled="loading" @click="load">Làm mới</button></header>
    <section id="receipt-form" class="panel">
      <div class="section-heading"><h2>{{ editingId ? `Sửa phiếu #${editingId}` : 'Tạo phiếu nhập' }}</h2><button v-if="editingId" class="btn btn-sm btn-outline" type="button" @click="resetForm">Tạo phiếu mới</button></div>
      <form @submit.prevent="save">
        <div class="form-grid"><label>Nhà cung cấp<input v-model="form.supplierName" class="form-input" maxlength="150" required /></label><label>Số hóa đơn<input v-model="form.invoiceNumber" class="form-input" maxlength="100" /></label><label>Thời gian nhận<input v-model="form.receivedAt" class="form-input" type="datetime-local" required /></label></div>
        <fieldset><legend>Danh sách hàng nhập</legend><div v-for="(line,index) in form.items" :key="index" class="line-card">
          <label>Mặt hàng<select v-model="line.inventoryItemId" class="form-select" required><option value="">Chọn mặt hàng</option><option v-for="item in inventoryItems" :key="item.inventoryItemId" :value="String(item.inventoryItemId)">{{ item.inventoryCode }} · {{ item.name }} ({{ item.baseUnit }})</option></select></label>
          <label>SL mua<input v-model="line.purchaseQuantity" class="form-input" type="number" min="0.0001" step="0.0001" required /></label><label>Đơn vị mua<input v-model="line.purchaseUnit" class="form-input" maxlength="30" placeholder="thùng, kg..." required /></label><label>Quy đổi<input v-model="line.conversionFactor" class="form-input" type="number" min="0.0001" step="0.0001" required /></label><label>Đơn giá mua<input v-model="line.purchaseUnitPrice" class="form-input" type="number" min="0.0001" step="0.0001" required /></label>
          <div class="preview" aria-live="polite"><span>Nhập kho <strong>{{ formatQuantity(goodsReceiptPreview(line).baseQuantity) }}</strong></span><span>Thành tiền <strong>{{ goodsReceiptPreview(line).lineTotal == null ? '—' : money(goodsReceiptPreview(line).lineTotal) }}</strong></span><span>Giá/đơn vị cơ sở <strong>{{ goodsReceiptPreview(line).baseUnitCost == null ? '—' : money(goodsReceiptPreview(line).baseUnitCost) }}</strong></span></div>
          <button class="btn btn-sm btn-outline" type="button" :disabled="form.items.length === 1" :aria-label="`Bỏ dòng ${index + 1}`" @click="removeLine(index)">Bỏ</button>
        </div><button class="btn btn-sm btn-outline" type="button" @click="addLine">+ Thêm dòng</button></fieldset>
        <p v-if="formError" class="error-box" role="alert">{{ formError }}</p><div class="actions"><button class="btn btn-primary" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu nháp' }}</button></div>
      </form>
    </section>
    <section class="panel"><h2>Phiếu gần đây</h2><div v-if="loading" class="state" role="status">Đang tải...</div><div v-else-if="loadError" class="state error" role="alert">{{ loadError }} <button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div><div v-else-if="!receipts.length" class="state">Chưa có phiếu nhập.</div><div v-else class="table-wrapper"><table class="table"><thead><tr><th>Mã</th><th>Nhà cung cấp</th><th>Hóa đơn</th><th>Ngày nhận</th><th>Dòng</th><th>Trạng thái</th><th>Thao tác</th></tr></thead><tbody><tr v-for="receipt in receipts" :key="receipt.goodsReceiptId"><td data-label="Mã">#{{ receipt.goodsReceiptId }}</td><td data-label="Nhà cung cấp"><strong>{{ receipt.supplierName }}</strong></td><td data-label="Hóa đơn">{{ receipt.invoiceNumber || '—' }}</td><td data-label="Ngày nhận">{{ receipt.receivedAt }}</td><td data-label="Dòng">{{ receipt.items.length }}</td><td data-label="Trạng thái"><span class="badge" :class="receipt.status === 'DRAFT' ? 'badge-warning' : 'badge-success'">{{ receipt.status === 'DRAFT' ? 'Nháp' : 'Đã duyệt' }}</span></td><td data-label="Thao tác"><div v-if="receipt.status === 'DRAFT'" class="row-actions"><button class="btn btn-sm btn-outline" @click="editReceipt(receipt)">Sửa</button><button class="btn btn-sm btn-primary" @click="approve(receipt)">Duyệt</button><button class="btn btn-sm btn-outline" @click="remove(receipt)">Xóa</button></div><span v-else class="muted">Đã khóa</span></td></tr></tbody></table></div></section>
  </main>
</template>

<style scoped>
.ops-page{display:grid;gap:20px}.page-heading,.section-heading,.actions,.row-actions{display:flex;align-items:center;justify-content:space-between;gap:10px}.page-heading h1{margin:2px 0;font-size:28px}.page-heading p{margin:0;color:var(--text-mid)}.eyebrow{color:var(--role-admin)!important;font-size:11px!important;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.panel{background:#fff;border:1px solid var(--border-light);border-radius:14px;padding:20px;display:grid;gap:14px}.panel h2{margin:0;font-size:18px}.form-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:16px}label{display:grid;gap:6px;color:var(--text-mid);font-size:12px;font-weight:700}fieldset{border:1px solid var(--border-light);border-radius:10px;padding:14px;display:grid;gap:12px}legend{font-size:13px;font-weight:800;padding:0 6px}.line-card{display:grid;grid-template-columns:2fr repeat(4,1fr) auto;gap:10px;align-items:end;padding:12px;background:var(--surface);border-radius:10px}.preview{grid-column:1/-2;display:flex;gap:18px;flex-wrap:wrap;font-size:12px;color:var(--text-mid)}.actions{justify-content:flex-end;margin-top:14px}.error-box{color:#b91c1c;background:#fef2f2;padding:10px;border-radius:8px}.state{padding:34px;text-align:center;color:var(--text-mid)}.error{color:#b91c1c}.table-wrapper{overflow-x:auto}.table{min-width:900px}.muted{color:var(--text-mid)}@media(max-width:900px){.form-grid{grid-template-columns:1fr}.line-card{grid-template-columns:repeat(2,minmax(0,1fr))}.line-card label:first-child,.preview{grid-column:1/-1}.table-wrapper{overflow:visible}.table thead{display:none}.table,.table tbody,.table tr,.table td{display:block;width:100%}.table tr{padding:14px;border-bottom:1px solid var(--border-light)}.table td{display:grid;grid-template-columns:100px 1fr;gap:10px;padding:6px 0;border:0}.table td:before{content:attr(data-label);color:var(--text-mid);font-size:12px}}@media(max-width:600px){.page-heading{align-items:flex-start;flex-direction:column}.line-card{grid-template-columns:1fr}.line-card>*{grid-column:1!important}.row-actions{justify-content:flex-start;flex-wrap:wrap}}
</style>
