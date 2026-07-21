<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { userApi, loyaltyApi, shippingApi } from '@/api';
import { formatDate } from '@/utils/format';
import { useToast } from '@/stores/toast';

const auth = useAuthStore();
const toast = useToast();
const form = ref({ fullName: '', email: '', phone: '' });
const profileSnapshot = ref(null);
const editMode = ref(false);
const savingProfile = ref(false);
const loyalty = ref(null);
const loyaltyLoading = ref(true);
const loyaltyError = ref('');
const loyaltyThresholds = { Bronze: 0, Silver: 500, Gold: 2000 };
const nextTier = computed(() => loyalty.value?.tier === 'Bronze' ? 'Silver' : loyalty.value?.tier === 'Silver' ? 'Gold' : null);
const loyaltyProgress = computed(() => {
  if (!loyalty.value || !nextTier.value) return 100;
  const start = loyaltyThresholds[loyalty.value.tier] || 0;
  const end = loyaltyThresholds[nextTier.value];
  return Math.min(100, Math.max(0, ((loyalty.value.points - start) / (end - start)) * 100));
});

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
let pendingDistrictId = null;
let pendingWardCode = null;

onMounted(() => {
  syncProfile();
  Promise.all([loadAddresses(), loadLoyalty(), loadProvinces()]);
  document.addEventListener('keydown', handleModalKeydown);
});

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleModalKeydown);
  document.body.style.overflow = '';
});

watch(showAddressForm, open => {
  document.body.style.overflow = open ? 'hidden' : '';
});

function syncProfile() {
  form.value = { fullName: auth.user?.fullName || '', email: auth.user?.email || '', phone: auth.user?.phone || '' };
}

function startProfileEdit() {
  profileSnapshot.value = { ...form.value };
  editMode.value = true;
}

function cancelProfileEdit() {
  if (profileSnapshot.value) form.value = { ...profileSnapshot.value };
  editMode.value = false;
}

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
  districts.value = [];
  wards.value = [];
  selectedDistrict.value = null;
  selectedWard.value = null;
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
  wards.value = [];
  selectedWard.value = null;
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

async function loadLoyalty() {
  loyaltyLoading.value = true;
  loyaltyError.value = '';
  try {
    loyalty.value = await loyaltyApi.getMe();
  } catch {
    loyalty.value = null;
    loyaltyError.value = 'Không thể tải thông tin thành viên.';
  } finally {
    loyaltyLoading.value = false;
  }
}

function prepareModal(trigger) {
  modalTrigger = trigger;
  showAddressForm.value = true;
  nextTick(() => firstModalInput.value?.focus());
}

function openAddAddress(event) {
  editingAddress.value = null;
  addressForm.value = { ...emptyAddress(), isDefault: addresses.value.length === 0 };
  selectedProvince.value = null;
  selectedDistrict.value = null;
  selectedWard.value = null;
  pendingDistrictId = null;
  pendingWardCode = null;
  prepareModal(event.currentTarget);
}

function openEditAddress(addr, event) {
  editingAddress.value = addr;
  addressForm.value = { recipientName: addr.recipientName || '', phone: addr.phone || '', street: addr.street || '', wardName: addr.wardName || '', districtName: addr.districtName || '', provinceName: addr.provinceName || '', ghnProvinceId: addr.ghnProvinceId || null, ghnDistrictId: addr.ghnDistrictId || null, ghnWardCode: addr.ghnWardCode || null, isDefault: addr.isDefault || false };
  pendingDistrictId = addr.ghnDistrictId || null;
  pendingWardCode = addr.ghnWardCode || null;
  selectedProvince.value = addr.ghnProvinceId || null;
  prepareModal(event.currentTarget);
}

function closeAddressModal() {
  if (savingAddress.value) return;
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

async function saveAddress() {
  const phonePattern = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  const f = addressForm.value;
  if (!f.recipientName?.trim() || f.recipientName.trim().length < 2) return toast.error('Tên người nhận phải có ít nhất 2 ký tự.');
  if (!f.phone?.trim() || !phonePattern.test(f.phone.trim())) return toast.error('Số điện thoại không hợp lệ, ví dụ: 0912345678.');
  if (!f.street?.trim() || f.street.trim().length < 5) return toast.error('Địa chỉ phải có ít nhất 5 ký tự.');
  if (!f.wardName?.trim() || !f.districtName?.trim() || !f.provinceName?.trim()) return toast.error('Vui lòng chọn đầy đủ phường, xã; quận, huyện; tỉnh, thành phố.');
  savingAddress.value = true;
  try {
    const data = { recipientName: f.recipientName.trim(), phone: f.phone.trim(), street: f.street.trim(), wardName: f.wardName.trim(), districtName: f.districtName.trim(), provinceName: f.provinceName.trim(), ghnProvinceId: f.ghnProvinceId, ghnDistrictId: f.ghnDistrictId, ghnWardCode: f.ghnWardCode, isDefault: f.isDefault };
    if (editingAddress.value) await userApi.updateAddress(editingAddress.value.addressId, data);
    else await userApi.createAddress(data);
    toast.success(editingAddress.value ? 'Cập nhật địa chỉ thành công.' : 'Thêm địa chỉ thành công.');
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

async function saveProfile() {
  const phonePattern = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  const name = form.value.fullName?.trim();
  const phone = form.value.phone?.trim();
  const email = form.value.email?.trim();
  if (!name || name.length < 2 || name.length > 100) return toast.error('Họ tên phải có từ 2 đến 100 ký tự.');
  if (phone && !phonePattern.test(phone)) return toast.error('Số điện thoại không hợp lệ.');
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return toast.error('Email không hợp lệ.');
  savingProfile.value = true;
  try {
    await auth.updateProfile({ fullName: name, email, phone });
    syncProfile();
    profileSnapshot.value = null;
    editMode.value = false;
    toast.success('Cập nhật thông tin thành công.');
  } catch (error) {
    toast.error(error.message || 'Không thể cập nhật thông tin.');
  } finally {
    savingProfile.value = false;
  }
}
</script>

<template>
  <main class="profile-page">
    <header class="page-heading">
      <div><span class="eyebrow">Tài khoản</span><h1>Hồ sơ của tôi</h1><p>Quản lý thông tin cá nhân, quyền lợi thành viên và địa chỉ giao hàng.</p></div>
    </header>

    <section class="profile-grid" aria-label="Thông tin tài khoản">
      <article class="panel identity-panel">
        <div class="section-heading"><div><span class="section-kicker">Hồ sơ</span><h2>Thông tin cá nhân</h2></div><button v-if="!editMode" type="button" class="btn btn-outline" @click="startProfileEdit"><i class="bi bi-pencil" aria-hidden="true"></i> Chỉnh sửa</button></div>
        <div class="profile-summary">
          <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" :alt="`Ảnh đại diện của ${auth.user?.fullName || 'thành viên'}`" class="profile-avatar" />
          <span v-else class="profile-avatar avatar-placeholder" aria-hidden="true">{{ (auth.user?.fullName || 'F').trim().charAt(0).toUpperCase() }}</span>
          <div><strong>{{ auth.user?.fullName }}</strong><span>Thành viên FastGuy</span></div>
        </div>
        <form v-if="editMode" class="profile-form" @submit.prevent="saveProfile">
          <div class="field"><label for="profile-name">Họ và tên</label><input id="profile-name" v-model="form.fullName" class="form-input" autocomplete="name" maxlength="100" required /></div>
          <div class="field"><label for="profile-email">Email</label><input id="profile-email" v-model="form.email" type="email" class="form-input" autocomplete="email" required /></div>
          <div class="field"><label for="profile-phone">Số điện thoại</label><input id="profile-phone" v-model="form.phone" type="tel" class="form-input" autocomplete="tel" /></div>
          <div class="form-actions"><button type="button" class="btn btn-outline" :disabled="savingProfile" @click="cancelProfileEdit">Hủy</button><button type="submit" class="btn btn-primary" :disabled="savingProfile"><span v-if="savingProfile" class="spinner" aria-hidden="true"></span>{{ savingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div>
        </form>
        <dl v-else class="detail-list">
          <div><dt>Họ và tên</dt><dd>{{ form.fullName || 'Chưa cập nhật' }}</dd></div>
          <div><dt>Email</dt><dd>{{ form.email || 'Chưa cập nhật' }}</dd></div>
          <div><dt>Số điện thoại</dt><dd>{{ form.phone || 'Chưa cập nhật' }}</dd></div>
        </dl>
      </article>

      <article class="panel loyalty-panel">
        <div class="section-heading"><div><span class="section-kicker">Quyền lợi</span><h2>Thành viên</h2></div><i class="bi bi-award tier-icon" aria-hidden="true"></i></div>
        <div v-if="loyaltyLoading" class="state-box" role="status"><span class="spinner" aria-hidden="true"></span>Đang tải thông tin thành viên...</div>
        <div v-else-if="loyaltyError" class="state-box error-box" role="alert"><span>{{ loyaltyError }}</span><button type="button" class="btn btn-sm btn-outline" @click="loadLoyalty">Thử lại</button></div>
        <template v-else-if="loyalty">
          <div class="points-summary"><div><span>Hạng hiện tại</span><strong>{{ loyalty.tier }}</strong></div><div class="points"><strong>{{ loyalty.points.toLocaleString('vi-VN') }}</strong><span>điểm tích lũy</span></div></div>
          <div class="tier-list"><span v-for="tier in ['Bronze', 'Silver', 'Gold']" :key="tier" :class="{ active: loyalty.tier === tier }">{{ tier }}<small>{{ loyaltyThresholds[tier].toLocaleString('vi-VN') }} điểm</small></span></div>
          <div class="loyalty-progress" role="progressbar" aria-label="Tiến độ hạng thành viên" aria-valuemin="0" aria-valuemax="100" :aria-valuenow="Math.round(loyaltyProgress)"><div :style="{ width: loyaltyProgress + '%' }"></div></div>
          <p class="loyalty-next">{{ nextTier ? `Còn ${(loyaltyThresholds[nextTier] - loyalty.points).toLocaleString('vi-VN')} điểm để đạt ${nextTier}` : 'Bạn đã đạt hạng Gold cao nhất.' }}</p>
          <div class="loyalty-history"><h3>Giao dịch gần đây</h3><div v-for="item in loyalty.history?.slice(0, 5) || []" :key="item.transactionId" class="loyalty-transaction"><span>{{ item.type === 'EARN' ? `Đơn ${item.orderCode || ''}` : `Hoàn điểm đơn ${item.orderCode || ''}` }}<small>{{ formatDate(item.createdAt) }}</small></span><b :class="{ negative: item.points < 0 }">{{ item.points > 0 ? '+' : '' }}{{ item.points }} điểm</b></div><p v-if="!loyalty.history?.length" class="empty-copy">Chưa có giao dịch điểm.</p></div>
        </template>
      </article>
    </section>

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
        <form @submit.prevent="saveAddress">
          <div class="modal-body">
            <div class="form-grid"><div class="field"><label for="recipient-name">Tên người nhận</label><input id="recipient-name" ref="firstModalInput" v-model="addressForm.recipientName" class="form-input" autocomplete="name" maxlength="100" required /></div><div class="field"><label for="recipient-phone">Số điện thoại</label><input id="recipient-phone" v-model="addressForm.phone" type="tel" class="form-input" autocomplete="tel" required /></div></div>
            <div class="field"><label for="province">Tỉnh / Thành phố</label><div class="select-action"><select id="province" v-model="selectedProvince" class="form-select" :disabled="loadingProvinces" required><option :value="null">{{ loadingProvinces ? 'Đang tải...' : 'Chọn tỉnh, thành phố' }}</option><option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option></select><button v-if="provinceError" type="button" class="retry-link" @click="loadProvinces">Tải lại</button></div><small v-if="provinceError" class="field-error" role="alert">{{ provinceError }}</small></div>
            <div class="form-grid"><div class="field"><label for="district">Quận / Huyện</label><select id="district" v-model="selectedDistrict" class="form-select" :disabled="!selectedProvince || loadingDistricts" required><option :value="null">{{ loadingDistricts ? 'Đang tải...' : 'Chọn quận, huyện' }}</option><option v-for="d in districts" :key="d.id" :value="d.id">{{ d.name }}</option></select><small v-if="districtError" class="field-error" role="alert">{{ districtError }} Chọn lại tỉnh để thử lại.</small></div><div class="field"><label for="ward">Phường / Xã</label><select id="ward" v-model="selectedWard" class="form-select" :disabled="!selectedDistrict || loadingWards" required><option :value="null">{{ loadingWards ? 'Đang tải...' : 'Chọn phường, xã' }}</option><option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option></select><small v-if="wardError" class="field-error" role="alert">{{ wardError }} Chọn lại quận, huyện để thử lại.</small></div></div>
            <div class="field"><label for="street">Số nhà, tên đường</label><input id="street" v-model="addressForm.street" class="form-input" autocomplete="street-address" placeholder="Ví dụ: 123 Nguyễn Huệ" maxlength="255" required /></div>
            <label class="form-checkbox"><input v-model="addressForm.isDefault" type="checkbox" /><span><strong>Đặt làm địa chỉ mặc định</strong><small>Ưu tiên sử dụng cho các đơn hàng tiếp theo.</small></span></label>
          </div>
          <div class="modal-footer"><button type="button" class="btn btn-outline" :disabled="savingAddress" @click="closeAddressModal">Hủy</button><button type="submit" class="btn btn-primary" :disabled="savingAddress"><span v-if="savingAddress" class="spinner" aria-hidden="true"></span>{{ savingAddress ? 'Đang lưu...' : editingAddress ? 'Cập nhật địa chỉ' : 'Thêm địa chỉ' }}</button></div>
        </form>
      </div>
    </div>
  </main>
</template>

<style scoped>
.profile-page { max-width: 1120px; margin: 0 auto; padding: 36px 20px 56px; color: var(--text-dark); }
.page-heading { margin-bottom: 24px; }
.page-heading h1 { margin: 4px 0 6px; font-size: clamp(26px, 4vw, 36px); line-height: 1.2; }
.page-heading p { margin: 0; color: var(--text-mid); }
.eyebrow, .section-kicker { color: var(--primary-dark); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.profile-grid { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(0, .92fr); gap: 20px; }
.panel { padding: 24px; border: 1px solid var(--border-light); border-radius: 16px; background: #fff; box-shadow: 0 8px 28px rgba(24, 39, 75, .06); }
.address-panel { margin-top: 20px; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.section-heading h2 { margin: 3px 0 0; font-size: 20px; }
.profile-summary { display: flex; align-items: center; gap: 15px; padding: 16px; margin-bottom: 20px; border-radius: 12px; background: linear-gradient(135deg, var(--primary-light), #fff); }
.profile-avatar { width: 64px; height: 64px; flex: 0 0 64px; border: 3px solid #fff; border-radius: 50%; object-fit: cover; box-shadow: 0 4px 14px rgba(0, 0, 0, .1); }
.avatar-placeholder { display: grid; place-items: center; background: var(--primary); color: #fff; font-size: 24px; font-weight: 800; }
.profile-summary div { display: flex; flex-direction: column; gap: 3px; }
.profile-summary strong { font-size: 18px; }
.profile-summary span, .empty-copy { color: var(--text-mid); font-size: 13px; }
.detail-list { margin: 0; }
.detail-list div { display: grid; grid-template-columns: 140px 1fr; gap: 14px; padding: 15px 0; border-bottom: 1px solid var(--border-light); }
.detail-list div:last-child { border-bottom: 0; }
.detail-list dt { color: var(--text-mid); font-size: 13px; }
.detail-list dd { margin: 0; font-weight: 600; overflow-wrap: anywhere; }
.profile-form, .modal-body { display: flex; flex-direction: column; gap: 16px; }
.field { min-width: 0; }
.field label { display: block; margin-bottom: 7px; font-size: 13px; font-weight: 700; }
.form-input, .form-select { width: 100%; min-height: 44px; box-sizing: border-box; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px; }
.tier-icon { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 50%; background: #fff4d6; color: #a66a00; font-size: 22px; }
.points-summary { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; padding: 18px; border-radius: 12px; background: #172033; color: #fff; }
.points-summary div { display: flex; flex-direction: column; gap: 4px; }
.points-summary span { color: #cbd3df; font-size: 12px; }
.points-summary strong { font-size: 22px; }
.points-summary .points { text-align: right; }
.points-summary .points strong { color: #ffd166; font-size: 28px; }
.tier-list { display: flex; justify-content: space-between; margin-top: 18px; color: var(--text-light); font-size: 12px; font-weight: 700; }
.tier-list span { display: flex; flex-direction: column; gap: 2px; }
.tier-list span:nth-child(2) { text-align: center; }
.tier-list span:last-child { text-align: right; }
.tier-list .active { color: var(--primary-dark); }
.tier-list small, .loyalty-next, .loyalty-transaction small { color: var(--text-mid); font-size: 11px; font-weight: 400; }
.loyalty-progress { height: 8px; margin: 9px 0 6px; overflow: hidden; border-radius: 999px; background: var(--border-light); }
.loyalty-progress div { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #cd7f32, #d4af37); transition: width .3s ease; }
.loyalty-next { margin: 0; }
.loyalty-history { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--border-light); }
.loyalty-history h3 { margin: 0 0 5px; font-size: 14px; }
.loyalty-transaction { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border-light); font-size: 13px; }
.loyalty-transaction span { display: flex; flex-direction: column; gap: 2px; }
.loyalty-transaction b { color: var(--success); white-space: nowrap; }
.loyalty-transaction b.negative { color: var(--red-active); }
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
.modal-header h2 { margin: 3px 0 0; font-size: 20px; }
.modal form { display: flex; max-height: calc(100vh - 135px); flex-direction: column; }
.modal-body { overflow-y: auto; padding: 22px 24px; }
.modal-footer { display: flex; justify-content: flex-end; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--border-light); background: #fff; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
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
@media (max-width: 820px) { .profile-grid { grid-template-columns: 1fr; } .address-card { grid-template-columns: 38px minmax(0, 1fr); } .address-actions { grid-column: 2; flex-wrap: wrap; } }
@media (max-width: 560px) { .profile-page { padding: 24px 12px 40px; } .panel { padding: 18px; border-radius: 13px; } .section-heading { align-items: flex-start; } .detail-list div { grid-template-columns: 1fr; gap: 4px; } .profile-summary { align-items: flex-start; } .points-summary { align-items: flex-start; flex-direction: column; } .points-summary .points { text-align: left; } .form-grid { grid-template-columns: 1fr; } .address-card { grid-template-columns: 1fr; } .address-marker { display: none; } .address-actions { grid-column: 1; justify-content: flex-end; } .modal-overlay { align-items: end; padding: 0; } .modal { max-height: 94vh; border-radius: 16px 16px 0 0; } .modal form { max-height: calc(94vh - 76px); } .modal-header, .modal-body { padding: 18px; } .modal-footer { position: sticky; bottom: 0; padding: 12px 18px calc(12px + env(safe-area-inset-bottom)); } .modal-footer .btn { flex: 1; } }
</style>
