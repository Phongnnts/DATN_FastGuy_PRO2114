<script setup>
import { reactive, ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { userApi, shippingApi } from '@/api';
import { useToast } from '@/stores/toast';
import FormField from '@/components/common/FormField.vue';
import { createShippingValidationState, legacyHierarchyState, runValidatedShippingSubmit } from '@/utils/shippingFormValidation';

const toast = useToast();
const addresses = ref([]);
const addressesLoading = ref(true);
const addressesError = ref('');
const showAddressForm = ref(false);
const editingAddress = ref(null);
const savingAddress = ref(false);
const deletingAddressId = ref(null);
const defaultingAddressId = ref(null);
const modal = ref(null);
const firstModalInput = ref(null);
let modalTrigger = null;
const emptyAddress = () => ({ recipientName: '', phone: '', street: '', wardName: '', districtName: '', provinceName: '', ghnProvinceId: null, ghnDistrictId: null, ghnWardCode: null, isDefault: false });
const addressForm = ref(emptyAddress());
const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvince = ref(null);
const selectedDistrict = ref(null);
const selectedWard = ref(null);
const loadingProvinces = ref(false);
const loadingDistricts = ref(false);
const loadingWards = ref(false);
const provinceError = ref('');
const districtError = ref('');
const wardError = ref('');
const shippingValidation = createShippingValidationState({ includeProvince: true });
const shippingTouched = reactive(shippingValidation.touched);
const shippingErrors = reactive(shippingValidation.errors);
let pendingDistrictId = null;
let pendingWardCode = null;
let applyingEditHierarchy = false;

onMounted(() => {
  Promise.all([loadAddresses(), loadProvinces()]);
  document.addEventListener('keydown', handleModalKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleModalKeydown);
  document.body.style.overflow = '';
});

watch(showAddressForm, open => {
  document.body.style.overflow = open ? 'hidden' : '';
});

async function loadProvinces() {
  loadingProvinces.value = true;
  provinceError.value = '';
  try {
    const data = await shippingApi.getProvinces();
    provinces.value = (data || []).map(p => ({ id: p.ProvinceID || p.province_id || p.provinceId, name: p.ProvinceName || p.province_name || p.provinceName }));
  } catch {
    provinces.value = [];
    provinceError.value = 'Không thể tải danh sách tỉnh, thành phố.';
  } finally {
    loadingProvinces.value = false;
  }
}

watch(selectedProvince, async id => {
  if (applyingEditHierarchy) return;
  districts.value = [];
  wards.value = [];
  selectedDistrict.value = null;
  selectedWard.value = null;
  shippingValidation.resetDependents(['district', 'ward'], shippingValues());
  addressForm.value.districtName = '';
  addressForm.value.wardName = '';
  addressForm.value.ghnDistrictId = null;
  addressForm.value.ghnWardCode = null;
  districtError.value = '';
  wardError.value = '';
  if (!id) return;
  loadingDistricts.value = true;
  try {
    const data = await shippingApi.getDistricts(id);
    if (selectedProvince.value !== id) return;
    districts.value = (data || []).map(d => ({ id: d.DistrictID || d.district_id || d.districtId, name: d.DistrictName || d.district_name || d.districtName }));
    if (pendingDistrictId && districts.value.some(d => d.id === pendingDistrictId)) selectedDistrict.value = pendingDistrictId;
    pendingDistrictId = null;
  } catch {
    districts.value = [];
    districtError.value = 'Không thể tải danh sách quận, huyện.';
  } finally {
    loadingDistricts.value = false;
  }
});

watch(selectedDistrict, async id => {
  if (applyingEditHierarchy) return;
  wards.value = [];
  selectedWard.value = null;
  shippingValidation.resetDependents(['ward'], shippingValues());
  inputShippingField('district');
  addressForm.value.wardName = '';
  addressForm.value.ghnWardCode = null;
  wardError.value = '';
  if (!id) return;
  const district = districts.value.find(item => item.id === id);
  addressForm.value.districtName = district?.name || '';
  addressForm.value.ghnDistrictId = id;
  loadingWards.value = true;
  try {
    const data = await shippingApi.getWards(id);
    if (selectedDistrict.value !== id) return;
    wards.value = (data || []).map(w => ({ code: w.WardCode || w.ward_code || w.wardCode, name: w.WardName || w.ward_name || w.wardName }));
    if (pendingWardCode && wards.value.some(w => w.code === pendingWardCode)) selectedWard.value = pendingWardCode;
    pendingWardCode = null;
  } catch {
    wards.value = [];
    pendingWardCode = null;
    wardError.value = 'Không thể tải danh sách phường, xã.';
  } finally {
    loadingWards.value = false;
  }
});

watch(selectedWard, code => {
  const ward = wards.value.find(item => item.code === code);
  addressForm.value.wardName = ward?.name || '';
  addressForm.value.ghnWardCode = code || null;
});

watch(selectedProvince, id => {
  const province = provinces.value.find(item => item.id === id);
  addressForm.value.provinceName = province?.name || '';
  addressForm.value.ghnProvinceId = id || null;
});

async function loadAddresses() {
  addressesLoading.value = true;
  addressesError.value = '';
  try {
    addresses.value = await userApi.getAddresses();
  } catch {
    addresses.value = [];
    addressesError.value = 'Không thể tải danh sách địa chỉ.';
  } finally {
    addressesLoading.value = false;
  }
}

function prepareModal(trigger) {
  modalTrigger = trigger;
  showAddressForm.value = true;
  nextTick(() => firstModalInput.value?.focus());
}

function openAddAddress(event) {
  shippingValidation.reset();
  editingAddress.value = null;
  addressForm.value = { ...emptyAddress(), isDefault: addresses.value.length === 0 };
  selectedProvince.value = null;
  selectedDistrict.value = null;
  selectedWard.value = null;
  pendingDistrictId = null;
  pendingWardCode = null;
  prepareModal(event.currentTarget);
}

async function loadEditAddressHierarchy(addr) {
  const saved = legacyHierarchyState(addr);
  const provinceId = saved.province;
  const districtId = saved.district;
  const wardCode = saved.ward;
  applyingEditHierarchy = true;
  selectedProvince.value = provinceId;
  selectedDistrict.value = districtId;
  selectedWard.value = wardCode;
  districts.value = [];
  wards.value = [];
  if (!provinceId) { applyingEditHierarchy = false; return; }
  const districtData = await shippingApi.getDistricts(provinceId);
  districts.value = (districtData || []).map(d => ({ id: d.DistrictID || d.district_id || d.districtId, name: d.DistrictName || d.district_name || d.districtName }));
  if (!districts.value.some(d => d.id === districtId)) { applyingEditHierarchy = false; return; }
  selectedDistrict.value = districtId;
  const wardData = await shippingApi.getWards(districtId);
  wards.value = (wardData || []).map(w => ({ code: w.WardCode || w.ward_code || w.wardCode, name: w.WardName || w.ward_name || w.wardName }));
  if (wards.value.some(w => w.code === wardCode)) selectedWard.value = wardCode;
  await nextTick();
  applyingEditHierarchy = false;
  const province = provinces.value.find(item => item.id === provinceId);
  const district = districts.value.find(item => item.id === districtId);
  const ward = wards.value.find(item => item.code === selectedWard.value);
  addressForm.value.provinceName = province?.name || saved.provinceName;
  addressForm.value.districtName = district?.name || saved.districtName;
  addressForm.value.wardName = ward?.name || saved.wardName;
  addressForm.value.ghnProvinceId = provinceId;
  addressForm.value.ghnDistrictId = districtId;
  addressForm.value.ghnWardCode = selectedWard.value;
}

async function openEditAddress(addr, event) {
  shippingValidation.reset();
  editingAddress.value = addr;
  addressForm.value = { recipientName: addr.recipientName || '', phone: addr.phone || '', street: addr.street || '', wardName: addr.wardName || '', districtName: addr.districtName || '', provinceName: addr.provinceName || '', ghnProvinceId: addr.ghnProvinceId || null, ghnDistrictId: addr.ghnDistrictId || null, ghnWardCode: addr.ghnWardCode || null, isDefault: addr.isDefault || false };
  pendingDistrictId = null;
  pendingWardCode = null;
  try { await loadEditAddressHierarchy(addr); }
  catch {
    applyingEditHierarchy = false;
    const saved = legacyHierarchyState(addr);
    selectedProvince.value = saved.province;
    selectedDistrict.value = saved.district;
    selectedWard.value = saved.ward;
    addressForm.value.provinceName = saved.provinceName;
    addressForm.value.districtName = saved.districtName;
    addressForm.value.wardName = saved.wardName;
  }
  prepareModal(event.currentTarget);
}

function closeAddressModal() {
  if (savingAddress.value) return;
  shippingValidation.reset();
  showAddressForm.value = false;
  nextTick(() => modalTrigger?.focus());
}

function handleModalKeydown(event) {
  if (!showAddressForm.value) return;
  if (event.key === 'Escape') {
    event.preventDefault();
    closeAddressModal();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = [...modal.value.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled)')];
  if (!focusable.length) return;
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

function shippingValues() { return { recipientName: addressForm.value.recipientName, phone: addressForm.value.phone, province: selectedProvince.value || addressForm.value.provinceName, district: selectedDistrict.value || addressForm.value.districtName, ward: selectedWard.value || addressForm.value.wardName, street: addressForm.value.street }; }
function blurShippingField(field) { shippingValidation.touch(field, shippingValues()); }
function inputShippingField(field) { shippingValidation.update(field, shippingValues()); }
function validateShippingForm() { return shippingValidation.validateAll(shippingValues()); }

async function saveAddress() {
  return runValidatedShippingSubmit(shippingValidation, shippingValues(), persistAddress);
}

async function persistAddress() {
  const f = addressForm.value;
  savingAddress.value = true;
  try {
    const data = { recipientName: f.recipientName.trim(), phone: f.phone.trim(), street: f.street.trim(), wardName: f.wardName.trim(), districtName: f.districtName.trim(), provinceName: f.provinceName.trim(), ghnProvinceId: f.ghnProvinceId, ghnDistrictId: f.ghnDistrictId, ghnWardCode: f.ghnWardCode, isDefault: f.isDefault };
    if (editingAddress.value) await userApi.updateAddress(editingAddress.value.addressId, data);
    else await userApi.createAddress(data);
    toast.success(editingAddress.value ? 'Cập nhật địa chỉ thành công.' : 'Thêm địa chỉ thành công.');
    shippingValidation.reset();
    showAddressForm.value = false;
    await loadAddresses();
    nextTick(() => modalTrigger?.focus());
  } catch (error) {
    toast.error(error.message || 'Không thể lưu địa chỉ.');
  } finally {
    savingAddress.value = false;
  }
}

async function deleteAddress(addr) {
  if (!confirm(`Xóa địa chỉ "${addr.street}, ${addr.wardName}"?`)) return;
  deletingAddressId.value = addr.addressId;
  try {
    await userApi.deleteAddress(addr.addressId);
    toast.success('Đã xóa địa chỉ.');
    await loadAddresses();
  } catch (error) {
    toast.error(error.message || 'Không thể xóa địa chỉ.');
  } finally {
    deletingAddressId.value = null;
  }
}

async function setDefault(addr) {
  defaultingAddressId.value = addr.addressId;
  try {
    await userApi.setDefaultAddress(addr.addressId);
    toast.success('Đã đặt làm địa chỉ mặc định.');
    await loadAddresses();
  } catch (error) {
    toast.error(error.message || 'Không thể đặt địa chỉ mặc định.');
  } finally {
    defaultingAddressId.value = null;
  }
}
</script>

<template>
  <div class="addresses-page">
    <header class="page-heading"><div><span class="eyebrow">Tài khoản</span><h1>Địa chỉ giao hàng</h1><p>Quản lý địa chỉ sử dụng cho đơn hàng.</p></div></header>
    <section class="panel address-panel" aria-labelledby="address-title">
      <div class="section-heading"><div><span class="section-kicker">Giao hàng</span><h2 id="address-title">Địa chỉ</h2></div><button type="button" class="btn btn-primary" @click="openAddAddress"><i class="bi bi-plus-lg" aria-hidden="true"></i> Thêm địa chỉ</button></div>
      <div v-if="addressesLoading" class="state-box" role="status"><span class="spinner" aria-hidden="true"></span>Đang tải địa chỉ...</div>
      <div v-else-if="addressesError" class="state-box error-box" role="alert"><span>{{ addressesError }}</span><button type="button" class="btn btn-sm btn-outline" @click="loadAddresses">Thử lại</button></div>
      <div v-else-if="!addresses.length" class="empty-address"><i class="bi bi-geo-alt" aria-hidden="true"></i><strong>Chưa có địa chỉ giao hàng</strong><span>Thêm địa chỉ để đặt hàng nhanh hơn.</span></div>
      <div v-else class="address-list">
        <article v-for="addr in addresses" :key="addr.addressId" class="address-card" :class="{ default: addr.isDefault }">
          <div class="address-marker"><i class="bi bi-geo-alt-fill" aria-hidden="true"></i></div>
          <div class="address-info"><div class="address-name"><strong>{{ addr.recipientName }}</strong><span>{{ addr.phone }}</span><span v-if="addr.isDefault" class="badge">Mặc định</span></div><p>{{ addr.street }}, {{ addr.wardName }}, {{ addr.districtName }}, {{ addr.provinceName }}</p></div>
          <div class="address-actions"><button v-if="!addr.isDefault" type="button" class="btn btn-sm btn-outline" :disabled="defaultingAddressId === addr.addressId" @click="setDefault(addr)">{{ defaultingAddressId === addr.addressId ? 'Đang đặt...' : 'Đặt mặc định' }}</button><button type="button" class="icon-btn" :aria-label="`Sửa địa chỉ của ${addr.recipientName}`" @click="openEditAddress(addr, $event)"><i class="bi bi-pencil" aria-hidden="true"></i></button><button type="button" class="icon-btn danger" :aria-label="`Xóa địa chỉ của ${addr.recipientName}`" :disabled="deletingAddressId === addr.addressId" @click="deleteAddress(addr)"><span v-if="deletingAddressId === addr.addressId" class="spinner" aria-hidden="true"></span><i v-else class="bi bi-trash" aria-hidden="true"></i></button></div>
        </article>
      </div>
    </section>
    <div v-if="showAddressForm" class="modal-overlay" @mousedown.self="closeAddressModal">
      <div ref="modal" class="modal" role="dialog" aria-modal="true" aria-labelledby="address-modal-title">
        <div class="modal-header"><div><span class="section-kicker">Giao hàng</span><h2 id="address-modal-title">{{ editingAddress ? 'Chỉnh sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h2></div><button type="button" class="icon-btn" aria-label="Đóng hộp thoại" :disabled="savingAddress" @click="closeAddressModal"><i class="bi bi-x-lg" aria-hidden="true"></i></button></div>
        <form novalidate @submit.prevent="saveAddress">
          <div class="modal-body">
            <div class="form-grid"><FormField id="recipient-name" label="Tên người nhận" required :error="shippingErrors.recipientName"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" ref="firstModalInput" v-model="addressForm.recipientName" class="form-input" autocomplete="name" placeholder="Họ tên người nhận" maxlength="100" @blur="blurShippingField('recipientName')" @input="inputShippingField('recipientName')" /></template></FormField><FormField id="recipient-phone" label="Số điện thoại" required :error="shippingErrors.phone"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="addressForm.phone" type="tel" class="form-input" autocomplete="tel" placeholder="Số điện thoại nhận hàng" @blur="blurShippingField('phone')" @input="inputShippingField('phone')" /></template></FormField></div>
            <FormField id="province" label="Tỉnh / Thành phố" required :error="shippingErrors.province || provinceError"><template #default="{ controlAttrs }"><div class="select-action"><select v-bind="controlAttrs" v-model="selectedProvince" class="form-select" :disabled="loadingProvinces" @blur="blurShippingField('province')" @change="inputShippingField('province')"><option :value="null">{{ loadingProvinces ? 'Đang tải...' : 'Chọn tỉnh, thành phố' }}</option><option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option></select><button v-if="provinceError" type="button" class="retry-link" @click="loadProvinces">Tải lại</button></div></template></FormField>
            <div class="form-grid"><FormField id="district" label="Quận / Huyện" required :error="shippingErrors.district || districtError"><template #default="{ controlAttrs }"><select v-bind="controlAttrs" v-model="selectedDistrict" class="form-select" :disabled="!selectedProvince || loadingDistricts" @blur="blurShippingField('district')" @change="inputShippingField('district')"><option :value="null">{{ loadingDistricts ? 'Đang tải...' : 'Chọn quận/huyện' }}</option><option v-for="d in districts" :key="d.id" :value="d.id">{{ d.name }}</option></select></template></FormField><FormField id="ward" label="Phường / Xã" required :error="shippingErrors.ward || wardError"><template #default="{ controlAttrs }"><select v-bind="controlAttrs" v-model="selectedWard" class="form-select" :disabled="!selectedDistrict || loadingWards" @blur="blurShippingField('ward')" @change="inputShippingField('ward')"><option :value="null">{{ loadingWards ? 'Đang tải...' : 'Chọn phường/xã' }}</option><option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option></select></template></FormField></div>
            <FormField id="street" label="Số nhà, tên đường" required :error="shippingErrors.street"><template #default="{ controlAttrs }"><input v-bind="controlAttrs" v-model="addressForm.street" class="form-input" autocomplete="street-address" placeholder="VD: 123 Nguyễn Huệ" maxlength="255" @blur="blurShippingField('street')" @input="inputShippingField('street')" /></template></FormField>
            <label class="form-checkbox"><input v-model="addressForm.isDefault" type="checkbox" /><span><strong>Đặt làm địa chỉ mặc định</strong><small>Ưu tiên sử dụng cho các đơn hàng tiếp theo.</small></span></label>
          </div>
          <div class="modal-footer"><button type="button" class="btn btn-outline" :disabled="savingAddress" @click="closeAddressModal">Hủy</button><button type="submit" class="btn btn-primary" :disabled="savingAddress"><span v-if="savingAddress" class="spinner" aria-hidden="true"></span>{{ savingAddress ? 'Đang lưu...' : editingAddress ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ' }}</button></div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.addresses-page { max-width: 1120px; margin: 0 auto; padding: 36px 20px 56px; color: var(--text-dark); }
.page-heading { margin-bottom: 24px; }
.page-heading h1 { margin: 4px 0 6px; font-size: clamp(26px, 4vw, 36px); line-height: 1.2; }
.page-heading p { margin: 0; color: var(--text-mid); }
.eyebrow, .section-kicker { color: var(--primary-dark); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.panel { padding: 24px; border: 1px solid var(--border-light); border-radius: 16px; background: #fff; box-shadow: 0 8px 28px rgba(24, 39, 75, .06); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.section-heading h2, .modal-header h2 { margin: 3px 0 0; font-size: 20px; }
.state-box { display: flex; min-height: 130px; align-items: center; justify-content: center; gap: 10px; color: var(--text-mid); text-align: center; }
.error-box { flex-direction: column; color: var(--red-active); }
.empty-address { display: flex; min-height: 180px; align-items: center; justify-content: center; flex-direction: column; gap: 7px; color: var(--text-mid); text-align: center; }
.empty-address i { font-size: 30px; color: var(--primary); }
.empty-address strong { color: var(--text-dark); }
.address-list { display: flex; flex-direction: column; gap: 12px; }
.address-card { display: grid; grid-template-columns: 38px minmax(0, 1fr) auto; align-items: start; gap: 12px; padding: 17px; border: 1px solid var(--border-light); border-radius: 12px; transition: border-color .2s, box-shadow .2s; }
.address-card:hover, .address-card.default { border-color: var(--primary-100); box-shadow: 0 5px 18px rgba(24, 39, 75, .05); }
.address-marker { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 10px; background: var(--primary-light); color: var(--primary-dark); }
.address-name { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.address-name span { color: var(--text-mid); font-size: 13px; }
.address-info p { margin: 7px 0 0; color: var(--text-mid); font-size: 14px; line-height: 1.55; }
.badge { padding: 3px 9px; border-radius: 999px; background: var(--primary-light); color: var(--primary-dark) !important; font-size: 10px !important; font-weight: 800; text-transform: uppercase; }
.address-actions { display: flex; align-items: center; gap: 7px; }
.icon-btn { display: inline-grid; width: 38px; height: 38px; padding: 0; place-items: center; border: 1px solid var(--border-light); border-radius: 9px; background: #fff; color: var(--text-dark); cursor: pointer; }
.icon-btn:hover { border-color: var(--primary); color: var(--primary-dark); }
.icon-btn.danger { color: var(--red-active); }
.icon-btn:disabled, .btn:disabled { cursor: not-allowed; opacity: .6; }
.modal-overlay { position: fixed; z-index: 1000; inset: 0; display: grid; padding: 20px; place-items: center; background: rgba(13, 20, 33, .64); backdrop-filter: blur(3px); }
.modal { width: min(680px, 100%); max-height: calc(100vh - 40px); overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 25px 80px rgba(0, 0, 0, .25); }
.modal-header { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 20px 24px; border-bottom: 1px solid var(--border-light); }
.modal form { display: flex; max-height: calc(100vh - 135px); flex-direction: column; }
.modal-body { display: flex; overflow-y: auto; padding: 22px 24px; flex-direction: column; gap: 16px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); background: #fff; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.field { min-width: 0; }
.field label { display: block; margin-bottom: 7px; font-size: 13px; font-weight: 700; }
.form-input, .form-select { width: 100%; min-height: 44px; box-sizing: border-box; }
.form-checkbox { display: flex; align-items: flex-start; gap: 10px; padding: 13px; border: 1px solid var(--border-light); border-radius: 10px; cursor: pointer; }
.form-checkbox input { width: 18px; height: 18px; accent-color: var(--primary); }
.form-checkbox span { display: flex; flex-direction: column; gap: 2px; font-size: 13px; }
.form-checkbox small { color: var(--text-mid); }
.field-error { display: block; margin-top: 5px; color: var(--red-active); }
.select-action { display: flex; align-items: center; gap: 8px; }
.retry-link { border: 0; background: transparent; color: var(--primary-dark); font-weight: 700; cursor: pointer; white-space: nowrap; }
.spinner { display: inline-block; width: 15px; height: 15px; margin-right: 2px; border: 2px solid currentColor; border-right-color: transparent; border-radius: 50%; animation: spin .65s linear infinite; vertical-align: -2px; }
.btn .spinner { margin-right: 7px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 820px) { .address-card { grid-template-columns: 38px minmax(0, 1fr); } .address-actions { grid-column: 2; flex-wrap: wrap; } }
@media (max-width: 560px) { .addresses-page { padding: 24px 12px 40px; } .panel { padding: 18px; border-radius: 13px; } .section-heading { align-items: flex-start; } .form-grid { grid-template-columns: 1fr; } .address-card { grid-template-columns: 1fr; } .address-marker { display: none; } .address-actions { grid-column: 1; justify-content: flex-end; } .modal-overlay { align-items: end; padding: 0; } .modal { max-height: 94vh; border-radius: 16px 16px 0 0; } .modal form { max-height: calc(94vh - 76px); } .modal-header, .modal-body { padding: 18px; } .modal-footer { position: sticky; bottom: 0; padding: 12px 18px calc(12px + env(safe-area-inset-bottom)); } .modal-footer .btn { flex: 1; } }
</style>
