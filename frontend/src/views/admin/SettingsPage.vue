<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue';
import { adminApi, orderApi } from '@/api';
import { useToast } from '@/stores/toast';
import { buildSettingsPayload, SCOPE_KEYS } from '@/utils/settingsValidation';
import { nextEnabledSectionIndex } from '@/utils/adminProductEditor';

const toast = useToast();
const tabs = [
  { id: 'store', label: 'Cửa hàng', icon: 'bi-shop' },
  { id: 'hours', label: 'Giờ hoạt động', icon: 'bi-clock' },
  { id: 'fees', label: 'Phí & thuế', icon: 'bi-cash-stack' },
  { id: 'delivery', label: 'Giao hàng', icon: 'bi-truck' },
  { id: 'inventory', label: 'Tồn kho', icon: 'bi-boxes' },
  { id: 'notice', label: 'Thông báo', icon: 'bi-megaphone' },
  { id: 'payment', label: 'Thanh toán', icon: 'bi-credit-card' },
  { id: 'ghn', label: 'Vận chuyển GHN', icon: 'bi-box' },
];
const EDITABLE_SCOPES = ['store', 'hours', 'fees', 'delivery', 'inventory', 'notice'];
const PAYMENT_METHODS = [
  { key: 'COD', label: 'Thanh toán khi nhận hàng' },
  { key: 'BANK_TRANSFER', label: 'Chuyển khoản ngân hàng (PayOS)' },
];
const activeTab = ref('store');
const tabRefs = ref([]);
const form = ref(createForm());
const baseline = ref(null);
const paymentMethods = ref([]);
const paymentError = ref('');
const loading = ref(true);
const loadState = ref('loading');
const loadMessage = ref('');
const saving = ref(false);
const tabErrors = ref({ store: {}, hours: {}, fees: {}, delivery: {}, inventory: {}, notice: {} });
let stopped = false;

function createForm() {
  return {
    store_name: 'FastGuy', store_phone: '', store_address: '', store_logo: '',
    business_open_time: '00:00', business_close_time: '00:00',
    service_fee: 0, tax_rate: 0, delivery_fee: 0, min_order_amount: 0,
    estimated_delivery_minutes: 30,
    low_stock_threshold: 5,
    morning_count_notice_enabled: '1', morning_count_notice_title: 'Cửa hàng đang chuẩn bị nguyên liệu', morning_count_notice_message: 'Chúng tôi đang kiểm kê đầu ngày.', morning_count_notice_image_url: '', morning_count_notice_link: '', morning_count_notice_cta_label: 'Xem thông báo',
    ghn_from_district_id: '', ghn_from_ward_code: '', default_service_type_id: '',
    default_weight: '', default_length: '', default_width: '', default_height: '',
  };
}

const dirty = computed(() => {
  if (!baseline.value) return {};
  const out = {};
  for (const scope of EDITABLE_SCOPES) {
    out[scope] = SCOPE_KEYS[scope].some((key) => String(baseline.value[key] ?? '') !== String(form.value[key] ?? ''));
  }
  return out;
});

function isEnabled(method) {
  return paymentMethods.value.includes(method);
}

function fieldError(scope, key) {
  return tabErrors.value[scope]?.[key];
}

function updateBaseline(scope) {
  if (!baseline.value) return;
  for (const key of SCOPE_KEYS[scope] || []) baseline.value[key] = form.value[key];
}

function applySettings(settings) {
  form.value = createForm();
  for (const [key, fallback] of Object.entries(createForm())) {
    if (settings[key] !== undefined && settings[key] !== null && settings[key] !== '') form.value[key] = settings[key];
  }
  form.value.service_fee = Number(form.value.service_fee || 0);
  form.value.tax_rate = Number(form.value.tax_rate || 0);
  form.value.delivery_fee = Number(form.value.delivery_fee || 0);
  form.value.min_order_amount = Number(form.value.min_order_amount || 0);
  form.value.estimated_delivery_minutes = Number(form.value.estimated_delivery_minutes || 30);
  form.value.low_stock_threshold = Number(form.value.low_stock_threshold || 5);
}

async function load() {
  loading.value = true;
  loadState.value = 'loading';
  loadMessage.value = '';
  tabErrors.value = { store: {}, hours: {}, fees: {}, delivery: {}, inventory: {} };
  try {
    const settings = await adminApi.getSettings();
    if (stopped) return;
    applySettings(settings);
    baseline.value = structuredClone(form.value);
    loadState.value = 'ready';
  } catch (error) {
    if (stopped) return;
    loadState.value = 'error';
    loadMessage.value = error.message || 'Không thể tải cài đặt';
  } finally {
    if (!stopped) loading.value = false;
  }
  loadPaymentCapabilities();
}

async function loadPaymentCapabilities() {
  paymentError.value = '';
  try {
    const caps = await orderApi.getPaymentCapabilities();
    if (stopped) return;
    paymentMethods.value = Array.isArray(caps?.methods) ? caps.methods : [];
  } catch (error) {
    if (stopped) return;
    paymentError.value = error.message || 'Không thể tải phương thức thanh toán';
  }
}

async function saveTab(scope) {
  if (saving.value) return;
  const { payload, errors } = buildSettingsPayload(scope, form.value);
  tabErrors.value[scope] = errors;
  if (Object.keys(errors).length) return;
  saving.value = true;
  try {
    await adminApi.updateSettings(payload);
    updateBaseline(scope);
    toast.success(`Đã lưu ${tabs.find((tab) => tab.id === scope)?.label ?? 'cài đặt'}`);
  } catch (error) {
    toast.error(error.message || 'Không thể lưu cài đặt');
  } finally {
    saving.value = false;
  }
}

function selectTab(tab) {
  activeTab.value = tab.id;
}

function handleTabKeydown(event, index) {
  if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
  event.preventDefault();
  const nextIndex = nextEnabledSectionIndex(tabs, index, event.key);
  activeTab.value = tabs[nextIndex].id;
  nextTick(() => tabRefs.value[nextIndex]?.focus());
}

onMounted(load);
onUnmounted(() => {
  stopped = true;
});
</script>

<template>
  <div class="settings-page">
    <div class="page-header operations-studio-page-header"><div><p class="eyebrow">Cấu hình</p><h1>Cài đặt cửa hàng</h1><p>Điều chỉnh từng nhóm độc lập và xem rõ tác động trước khi lưu.</p></div></div>

    <section v-if="loadState === 'loading'" class="state" role="status">Đang tải cài đặt...</section>
    <section v-else-if="loadState === 'error'" class="state state-error" role="alert">
      <h2>Không thể tải cài đặt</h2>
      <p>{{ loadMessage }}</p>
      <button class="btn btn-outline" type="button" @click="load">Thử lại</button>
    </section>
    <template v-else>
      <div class="section-tabs" role="tablist" aria-label="Cài đặt cửa hàng">
        <button v-for="(tab, index) in tabs" :id="`settings-tab-${tab.id}`" :key="tab.id" :ref="(element) => { tabRefs[index] = element; }" role="tab" type="button" :aria-selected="activeTab === tab.id" :aria-controls="`settings-panel-${tab.id}`" :tabindex="activeTab === tab.id ? 0 : -1" @click="selectTab(tab)" @keydown="handleTabKeydown($event, index)"><i class="bi" :class="tab.icon" aria-hidden="true"></i>{{ tab.label }}<span v-if="dirty[tab.id]" class="dirty" aria-hidden="true">•</span><span v-if="dirty[tab.id]" class="visually-hidden">, có thay đổi chưa lưu</span></button>
      </div>

      <section :id="`settings-panel-${activeTab}`" class="consequential-settings-workspace" role="tabpanel" :aria-labelledby="`settings-tab-${activeTab}`" tabindex="0">
        <form v-if="activeTab === 'store'" class="card card-flat settings-card" @submit.prevent="saveTab('store')" novalidate>
          <h3 class="panel-title"><i class="bi bi-shop"></i> Cửa hàng</h3>
          <div class="form-group">
            <label class="form-label" for="settings-store-name">Tên cửa hàng <span class="req">*</span></label>
            <input id="settings-store-name" v-model="form.store_name" class="form-input" placeholder="FastGuy">
            <p v-if="fieldError('store', 'name')" class="field-error" role="alert">{{ fieldError('store', 'name') }}</p>
          </div>
          <div class="settings-grid">
            <div class="form-group">
              <label class="form-label" for="settings-store-phone">Số điện thoại</label>
              <input id="settings-store-phone" v-model="form.store_phone" class="form-input" type="tel">
            </div>
            <div class="form-group">
              <label class="form-label" for="settings-store-logo">Logo URL</label>
              <input id="settings-store-logo" v-model="form.store_logo" class="form-input" placeholder="https://...">
              <p v-if="fieldError('store', 'logo')" class="field-error" role="alert">{{ fieldError('store', 'logo') }}</p>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label" for="settings-store-address">Địa chỉ</label>
            <input id="settings-store-address" v-model="form.store_address" class="form-input">
          </div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
        </form>

        <form v-else-if="activeTab === 'hours'" class="card card-flat settings-card" @submit.prevent="saveTab('hours')" novalidate>
          <h3 class="panel-title"><i class="bi bi-clock"></i> Giờ hoạt động</h3>
          <div class="settings-grid">
            <div class="form-group">
              <label class="form-label" for="settings-open">Giờ mở cửa</label>
              <input id="settings-open" v-model="form.business_open_time" class="form-input" type="time">
              <p v-if="fieldError('hours', 'open')" class="field-error" role="alert">{{ fieldError('hours', 'open') }}</p>
            </div>
            <div class="form-group">
              <label class="form-label" for="settings-close">Giờ đóng cửa</label>
              <input id="settings-close" v-model="form.business_close_time" class="form-input" type="time">
              <p v-if="fieldError('hours', 'close')" class="field-error" role="alert">{{ fieldError('hours', 'close') }}</p>
            </div>
          </div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
        </form>

        <form v-else-if="activeTab === 'fees'" class="card card-flat settings-card" @submit.prevent="saveTab('fees')" novalidate>
          <h3 class="panel-title"><i class="bi bi-cash-stack"></i> Phí & Thuế</h3>
          <div class="settings-grid">
            <div class="form-group">
              <label class="form-label" for="settings-tax-rate">Thuế (%)</label>
              <input id="settings-tax-rate" v-model.number="form.tax_rate" class="form-input" type="number" min="0" max="100" step="0.1">
              <p v-if="fieldError('fees', 'tax_rate')" class="field-error" role="alert">{{ fieldError('fees', 'tax_rate') }}</p>
            </div>
            <div class="form-group">
              <label class="form-label" for="settings-delivery-fee">Phí ship mặc định (VNĐ)</label>
              <input id="settings-delivery-fee" v-model.number="form.delivery_fee" class="form-input" type="number" min="0">
              <p v-if="fieldError('fees', 'delivery_fee')" class="field-error" role="alert">{{ fieldError('fees', 'delivery_fee') }}</p>
            </div>
            <div class="form-group">
              <label class="form-label" for="settings-min-order">Đơn tối thiểu (VNĐ)</label>
              <input id="settings-min-order" v-model.number="form.min_order_amount" class="form-input" type="number" min="0">
              <p v-if="fieldError('fees', 'min_order_amount')" class="field-error" role="alert">{{ fieldError('fees', 'min_order_amount') }}</p>
            </div>
          </div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
        </form>

        <form v-else-if="activeTab === 'delivery'" class="card card-flat settings-card" @submit.prevent="saveTab('delivery')" novalidate>
          <h3 class="panel-title"><i class="bi bi-truck"></i> Giao hàng</h3>
          <div class="form-group" style="max-width:280px">
            <label class="form-label" for="settings-delivery-minutes">Thời gian giao ước tính (phút)</label>
            <input id="settings-delivery-minutes" v-model.number="form.estimated_delivery_minutes" class="form-input" type="number" min="10" max="180">
            <p v-if="fieldError('delivery', 'delivery')" class="field-error" role="alert">{{ fieldError('delivery', 'delivery') }}</p>
          </div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
        </form>

        <form v-else-if="activeTab === 'inventory'" class="card card-flat settings-card" @submit.prevent="saveTab('inventory')" novalidate>
          <h3 class="panel-title"><i class="bi bi-boxes"></i> Tồn kho</h3>
          <div class="form-group" style="max-width:280px">
            <label class="form-label" for="settings-low-stock-threshold">Ngưỡng cảnh báo sắp hết (SKU)</label>
            <input id="settings-low-stock-threshold" v-model.number="form.low_stock_threshold" class="form-input" type="number" min="1" max="1000" step="1" :aria-invalid="Boolean(fieldError('inventory', 'low_stock_threshold'))" :aria-describedby="fieldError('inventory', 'low_stock_threshold') ? 'settings-low-stock-help settings-low-stock-error' : 'settings-low-stock-help'">
            <small id="settings-low-stock-help" class="readonly-note">SKU có tồn từ 1 đến ngưỡng này được tính là sắp hết.</small>
            <p v-if="fieldError('inventory', 'low_stock_threshold')" id="settings-low-stock-error" class="field-error" role="alert">{{ fieldError('inventory', 'low_stock_threshold') }}</p>
          </div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu cài đặt' }}</button></div>
        </form>

        <form v-else-if="activeTab === 'notice'" class="card card-flat settings-card" @submit.prevent="saveTab('notice')" novalidate>
          <h3 class="panel-title"><i class="bi bi-megaphone"></i> Popup kiểm kê đầu ngày</h3><p class="readonly-note">Popup chỉ thông báo, không khóa xem menu hoặc đặt hàng. Mỗi khách thấy tối đa một lần mỗi ngày.</p>
          <label class="checkbox-row"><input v-model="form.morning_count_notice_enabled" type="checkbox" true-value="1" false-value="0"> Bật popup khi chưa duyệt kiểm kê hôm nay</label>
          <div class="form-group"><label class="form-label" for="notice-title">Tiêu đề</label><input id="notice-title" v-model="form.morning_count_notice_title" class="form-input" maxlength="200"><p v-if="fieldError('notice','title')" class="field-error">{{ fieldError('notice','title') }}</p></div>
          <div class="form-group"><label class="form-label" for="notice-message">Nội dung</label><textarea id="notice-message" v-model="form.morning_count_notice_message" class="form-input" rows="4" maxlength="500"></textarea><p v-if="fieldError('notice','message')" class="field-error">{{ fieldError('notice','message') }}</p></div>
          <div class="settings-grid"><div class="form-group"><label class="form-label" for="notice-image">URL ảnh</label><input id="notice-image" v-model="form.morning_count_notice_image_url" class="form-input" placeholder="https://..."><p v-if="fieldError('notice','morning_count_notice_image_url')" class="field-error">{{ fieldError('notice','morning_count_notice_image_url') }}</p></div><div class="form-group"><label class="form-label" for="notice-link">URL liên kết</label><input id="notice-link" v-model="form.morning_count_notice_link" class="form-input" placeholder="/menu hoặc https://..."><p v-if="fieldError('notice','morning_count_notice_link')" class="field-error">{{ fieldError('notice','morning_count_notice_link') }}</p></div></div>
          <div class="form-group"><label class="form-label" for="notice-cta">Nhãn nút</label><input id="notice-cta" v-model="form.morning_count_notice_cta_label" class="form-input" maxlength="100"></div>
          <div class="panel-actions"><button class="btn btn-primary" type="submit" :disabled="saving">{{ saving ? 'Đang lưu...' : 'Lưu thông báo' }}</button></div>
        </form>

        <div v-else-if="activeTab === 'payment'" class="card card-flat settings-card">
          <h3 class="panel-title"><i class="bi bi-credit-card"></i> Thanh toán</h3>
          <p class="readonly-note">Phương thức thanh toán được quản lý bởi hệ thống thanh toán. Bảng này chỉ hiển thị trạng thái hiện tại.</p>
          <div v-if="paymentError" class="payment-retry" role="alert">
            <p class="field-error">{{ paymentError }}</p>
            <button class="btn btn-outline" type="button" @click="loadPaymentCapabilities">Thử lại</button>
          </div>
          <div v-else v-for="method in PAYMENT_METHODS" :key="method.key" class="method-row">
            <div><strong>{{ method.label }}</strong><small>{{ method.key }}</small></div>
            <span class="badge" :class="isEnabled(method.key) ? 'badge-on' : 'badge-off'">{{ isEnabled(method.key) ? 'Đang bật' : 'Đã tắt' }}</span>
          </div>
        </div>

        <div v-else class="card card-flat settings-card">
          <h3 class="panel-title"><i class="bi bi-box"></i> Vận chuyển GHN</h3>
          <p class="readonly-note">Cấu hình giao hàng GHN được quản lý ở hệ thống vận chuyển. Các giá trị dưới đây chỉ hiển thị.</p>
          <div class="ghn-grid">
            <div class="form-group"><label class="form-label">Mã quận gửi (ghn_from_district_id)</label><input class="form-input" :value="form.ghn_from_district_id" readonly></div>
            <div class="form-group"><label class="form-label">Mã phường/xã gửi (ghn_from_ward_code)</label><input class="form-input" :value="form.ghn_from_ward_code" readonly></div>
            <div class="form-group"><label class="form-label">Loại dịch vụ mặc định (default_service_type_id)</label><input class="form-input" :value="form.default_service_type_id" readonly></div>
            <div class="form-group"><label class="form-label">Cân nặng mặc định (default_weight)</label><input class="form-input" :value="form.default_weight" readonly></div>
            <div class="form-group"><label class="form-label">Chiều dài mặc định (default_length)</label><input class="form-input" :value="form.default_length" readonly></div>
            <div class="form-group"><label class="form-label">Chiều rộng mặc định (default_width)</label><input class="form-input" :value="form.default_width" readonly></div>
            <div class="form-group"><label class="form-label">Chiều cao mặc định (default_height)</label><input class="form-input" :value="form.default_height" readonly></div>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.settings-page{display:grid;gap:18px;color:var(--text-dark)}
.section-tabs{display:flex;gap:8px;overflow-x:auto;padding:6px;border-radius:15px;background:#eee8e3}
.section-tabs button{display:inline-flex;align-items:center;gap:7px;min-height:44px;padding:10px 15px;white-space:nowrap;border:0;border-radius:10px;background:transparent;font-weight:700}
.section-tabs button[aria-selected=true]{color:#fff;background:#34251e}
.section-tabs button:focus-visible{outline:2px solid var(--primary);outline-offset:2px}
.dirty{margin-left:5px;color:var(--route-amber)}
.visually-hidden{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0 0 0 0);white-space:nowrap;border:0}
section[role=tabpanel]:focus-visible{outline:2px solid var(--primary);outline-offset:4px;border-radius:4px}
.settings-card{max-width:640px}
.settings-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.ghn-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}
.panel-title{display:flex;align-items:center;gap:8px;font-size:15px;margin:0 0 14px}
.req{color:#dc2626}
.field-error{margin:6px 0 0;color:#dc2626;font-size:13px}
.payment-retry{display:grid;gap:10px}
.panel-actions{margin-top:20px}
.readonly-note{margin:0 0 16px;color:var(--text-mid,#6b7280);font-size:13px}
.method-row{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:12px 14px;border:1px solid var(--border,#e5e7eb);border-radius:12px;margin-bottom:10px}
.method-row strong{display:block}
.method-row small{color:var(--text-mid,#6b7280)}
.badge{padding:4px 10px;border-radius:999px;font-size:12px;font-weight:700;white-space:nowrap}
.badge-on{color:#166534;background:#dcfce7}
.badge-off{color:#7f1d1d;background:#fee2e2}
.state{display:grid;gap:12px;place-items:center;min-height:240px;padding:30px;border:1px solid rgba(23,23,23,.08);border-radius:20px;background:#fff;text-align:center}
.state-error h2{margin:0}
@media(max-width:700px){.settings-grid,.ghn-grid{grid-template-columns:1fr}.section-tabs{margin-inline:-4px}}
</style>
