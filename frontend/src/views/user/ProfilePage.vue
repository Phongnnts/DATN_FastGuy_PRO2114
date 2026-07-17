<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { userApi, loyaltyApi, shippingApi } from '@/api';
import { formatDate } from '@/utils/format';
import { useToast } from '@/utils/toast';

const auth = useAuthStore();
const toast = useToast();
const form = ref({ fullName: '', email: '', phone: '' });
const editMode = ref(false);
const success = ref('');
const loyalty = ref(null);
const loyaltyThresholds = { Bronze: 0, Silver: 500, Gold: 2000 };
const nextTier = computed(() => loyalty.value?.tier === 'Bronze' ? 'Silver' : loyalty.value?.tier === 'Silver' ? 'Gold' : null);
const loyaltyProgress = computed(() => {
  if (!loyalty.value || !nextTier.value) return 100;
  const start = loyaltyThresholds[loyalty.value.tier] || 0;
  const end = loyaltyThresholds[nextTier.value];
  return Math.min(100, Math.max(0, ((loyalty.value.points - start) / (end - start)) * 100));
});

const addresses = ref([]);
const showAddressForm = ref(false);
const editingAddress = ref(null);
const addressForm = ref({
  recipientName: '',
  phone: '',
  street: '',
  wardName: '',
  districtName: '',
  provinceName: '',
  ghnProvinceId: null,
  ghnDistrictId: null,
  ghnWardCode: null,
  isDefault: false,
});

const provinces = ref([]);
const districts = ref([]);
const wards = ref([]);
const selectedProvince = ref(null);
const selectedDistrict = ref(null);
const selectedWard = ref(null);
const loadingProvinces = ref(false);
const loadingDistricts = ref(false);
const loadingWards = ref(false);
let pendingWardCode = null;

onMounted(async () => {
  if (auth.user) {
    form.value = {
      fullName: auth.user.fullName,
      email: auth.user.email,
      phone: auth.user.phone || '',
    };
  }
  await Promise.all([loadAddresses(), loadLoyalty(), loadProvinces()]);
});

async function loadProvinces() {
  loadingProvinces.value = true;
  try {
    const data = await shippingApi.getProvinces();
    provinces.value = (data || []).map(p => ({
      id: p.ProvinceID || p.province_id || p.provinceId,
      name: p.ProvinceName || p.province_name || p.provinceName,
    }));
  } catch {
    provinces.value = [];
  } finally {
    loadingProvinces.value = false;
  }
}

watch(selectedProvince, async (id) => {
  districts.value = [];
  wards.value = [];
  selectedDistrict.value = null;
  selectedWard.value = null;
  addressForm.value.districtName = '';
  addressForm.value.wardName = '';
  addressForm.value.ghnDistrictId = null;
  addressForm.value.ghnWardCode = null;
  if (!id) return;
  loadingDistricts.value = true;
  try {
    const data = await shippingApi.getDistricts(id);
    districts.value = (data || []).map(d => ({
      id: d.DistrictID || d.district_id || d.districtId,
      name: d.DistrictName || d.district_name || d.districtName,
    }));
  } catch {
    districts.value = [];
  } finally {
    loadingDistricts.value = false;
  }
});

watch(selectedDistrict, async (id) => {
  wards.value = [];
  selectedWard.value = null;
  addressForm.value.wardName = '';
  addressForm.value.ghnWardCode = null;
  if (!id) return;
  const dist = districts.value.find(d => d.id === id);
  addressForm.value.districtName = dist?.name || '';
  addressForm.value.ghnDistrictId = id;
  loadingWards.value = true;
  try {
    const data = await shippingApi.getWards(id);
    wards.value = (data || []).map(w => ({
      code: w.WardCode || w.ward_code || w.wardCode,
      name: w.WardName || w.ward_name || w.wardName,
    }));
    if (pendingWardCode && wards.value.some(w => w.code === pendingWardCode)) {
      selectedWard.value = pendingWardCode;
    }
    pendingWardCode = null;
  } catch {
    wards.value = [];
    pendingWardCode = null;
  } finally {
    loadingWards.value = false;
  }
});

watch(selectedWard, (code) => {
  if (!code) {
    addressForm.value.wardName = '';
    addressForm.value.ghnWardCode = null;
    return;
  }
  const ward = wards.value.find(w => w.code === code);
  addressForm.value.wardName = ward?.name || '';
  addressForm.value.ghnWardCode = code;
});

watch(selectedProvince, (id) => {
  const prov = provinces.value.find(p => p.id === id);
  addressForm.value.provinceName = prov?.name || '';
  addressForm.value.ghnProvinceId = id;
});

async function loadAddresses() {
  try {
    addresses.value = await userApi.getAddresses();
  } catch {
    addresses.value = [];
  }
}

async function loadLoyalty() {
  try {
    loyalty.value = await loyaltyApi.getMe();
  } catch {
    loyalty.value = null;
  }
}

function openAddAddress() {
  editingAddress.value = null;
  addressForm.value = {
    recipientName: '',
    phone: '',
    street: '',
    wardName: '',
    districtName: '',
    provinceName: '',
    ghnProvinceId: null,
    ghnDistrictId: null,
    ghnWardCode: null,
    isDefault: addresses.value.length === 0,
  };
  selectedProvince.value = null;
  selectedDistrict.value = null;
  selectedWard.value = null;
  pendingWardCode = null;
  showAddressForm.value = true;
}

function openEditAddress(addr) {
  editingAddress.value = addr;
  addressForm.value = {
    recipientName: addr.recipientName || '',
    phone: addr.phone || '',
    street: addr.street || '',
    wardName: addr.wardName || '',
    districtName: addr.districtName || '',
    provinceName: addr.provinceName || '',
    ghnProvinceId: addr.ghnProvinceId || null,
    ghnDistrictId: addr.ghnDistrictId || null,
    ghnWardCode: addr.ghnWardCode || null,
    isDefault: addr.isDefault || false,
  };
  showAddressForm.value = true;
  if (addr.ghnDistrictId) {
    const prov = provinces.value.find(p => p.name?.includes(addr.provinceName));
    if (prov) {
      selectedProvince.value = prov.id;
      pendingWardCode = addr.ghnWardCode || null;
      selectedDistrict.value = addr.ghnDistrictId;
    }
  }
}

async function saveAddress() {
  const PHONE_PATTERN = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  const f = addressForm.value;
  if (!f.recipientName?.trim() || f.recipientName.trim().length < 2) {
    toast.error('Ten nguoi nhan phai tu 2 ky tu');
    return;
  }
  if (!f.phone?.trim() || !PHONE_PATTERN.test(f.phone.trim())) {
    toast.error('So dien thoai khong hop le (VD: 0912345678)');
    return;
  }
  if (!f.street?.trim() || f.street.trim().length < 5) {
    toast.error('Dia chi phai tu 5 ky tu');
    return;
  }
  if (!f.wardName?.trim() || !f.districtName?.trim() || !f.provinceName?.trim()) {
    toast.error('Vui long chon day du phuong/xa, quan/huyen, tinh/thanh pho');
    return;
  }
  try {
    const data = {
      recipientName: f.recipientName.trim(),
      phone: f.phone.trim(),
      street: f.street.trim(),
      wardName: f.wardName.trim(),
      districtName: f.districtName.trim(),
      provinceName: f.provinceName.trim(),
      ghnProvinceId: f.ghnProvinceId,
      ghnDistrictId: f.ghnDistrictId,
      ghnWardCode: f.ghnWardCode,
      isDefault: f.isDefault,
    };
    if (editingAddress.value) {
      await userApi.updateAddress(editingAddress.value.addressId, data);
      toast.success('Cap nhat dia chi thanh cong!');
    } else {
      await userApi.createAddress(data);
      toast.success('Them dia chi thanh cong!');
    }
    showAddressForm.value = false;
    await loadAddresses();
  } catch (e) {
    toast.error(e.message || 'Loi khi luu dia chi');
  }
}

async function deleteAddress(addr) {
  if (!confirm(`Xoa dia chi "${addr.street}, ${addr.wardName}"?`)) return;
  try {
    await userApi.deleteAddress(addr.addressId);
    toast.success('Da xoa dia chi');
    await loadAddresses();
  } catch (e) {
    toast.error(e.message || 'Loi khi xoa dia chi');
  }
}

async function setDefault(addr) {
  try {
    await userApi.setDefaultAddress(addr.addressId);
    toast.success('Da dat dia chi mac dinh');
    await loadAddresses();
  } catch (e) {
    toast.error(e.message || 'Loi khi dat mac dinh');
  }
}

async function saveProfile() {
  const PHONE_PATTERN = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  const name = form.value.fullName?.trim();
  const phone = form.value.phone?.trim();
  const email = form.value.email?.trim();
  if (!name || name.length < 2 || name.length > 100) {
    toast.error('Ho ten phai tu 2 den 100 ky tu');
    return;
  }
  if (phone && !PHONE_PATTERN.test(phone)) {
    toast.error('So dien thoai khong hop le');
    return;
  }
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    toast.error('Email khong hop le');
    return;
  }
  try {
    await auth.updateProfile(form.value);
    toast.success('Cap nhat thanh cong!');
    editMode.value = false;
  } catch (e) {
    toast.error(e.message || 'Cap nhat that bai');
  }
}
</script>

<template>
  <div class="profile-page">
    <div class="card">
      <div class="card-header">
        <h3>Thong tin ca nhan</h3>
        <button
          v-if="!editMode"
          class="btn btn-sm btn-outline"
          @click="editMode = true"
        >
          <i class="bi bi-pencil"></i> Chinh sua
        </button>
      </div>
      <div class="profile-avatar-section">
        <img
          :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'"
          class="profile-avatar"
        />
        <div>
          <div class="profile-name">{{ auth.user?.fullName }}</div>
          <div class="profile-role">Thanh vien</div>
        </div>
      </div>
      <form @submit.prevent="saveProfile">
        <div class="form-group">
          <label class="form-label">Ho ten</label>
          <input v-model="form.fullName" class="form-input" :disabled="!editMode" />
        </div>
        <div class="form-group">
          <label class="form-label">Email</label>
          <input
            v-model="form.email"
            type="email"
            class="form-input"
            :disabled="!editMode"
          />
        </div>
        <div class="form-group">
          <label class="form-label">So dien thoai</label>
          <input
            v-model="form.phone"
            type="tel"
            class="form-input"
            :disabled="!editMode"
          />
        </div>
        <div v-if="editMode" class="form-actions">
          <button type="submit" class="btn btn-primary">
            <i class="bi bi-check-lg"></i> Luu thay doi
          </button>
          <button
            type="button"
            class="btn btn-outline"
            @click="editMode = false"
          >
            Huy
          </button>
        </div>
      </form>
    </div>

    <div v-if="loyalty" class="card mt-3 loyalty-card">
      <div class="card-header"><h3><i class="bi bi-award"></i> Thanh vien {{ loyalty.tier }}</h3><strong>{{ loyalty.points }} diem</strong></div>
      <div class="tier-list"><span v-for="tier in ['Bronze', 'Silver', 'Gold']" :key="tier" :class="{ active: loyalty.tier === tier }">{{ tier }}<small>{{ loyaltyThresholds[tier] }} diem</small></span></div>
      <div class="loyalty-progress"><div :style="{ width: loyaltyProgress + '%' }"></div></div>
      <p v-if="nextTier" class="loyalty-next">Con {{ loyaltyThresholds[nextTier] - loyalty.points }} diem de dat {{ nextTier }}</p>
      <p v-else class="loyalty-next">Ban da dat hang Gold</p>
      <div v-if="loyalty.history?.length" class="loyalty-history">
        <strong>Giao dich gan day</strong>
        <div v-for="item in loyalty.history.slice(0, 5)" :key="item.transactionId" class="loyalty-transaction">
          <span>{{ item.type === 'EARN' ? 'Don ' + item.orderCode : 'Hoan diem don ' + item.orderCode }}<small>{{ formatDate(item.createdAt) }}</small></span>
          <b :class="{ negative: item.points < 0 }">{{ item.points > 0 ? '+' : '' }}{{ item.points }} diem</b>
        </div>
      </div>
    </div>

    <div class="card mt-3">
      <div class="card-header">
        <h3>Dia chi giao hang</h3>
        <button class="btn btn-sm btn-primary" @click="openAddAddress">
          <i class="bi bi-plus-lg"></i> Them dia chi
        </button>
      </div>
      <div v-if="addresses.length === 0" class="empty-state" style="padding: 24px 0">
        <p style="color: var(--text-mid)">Chua co dia chi nao</p>
      </div>
      <div v-else class="address-list">
        <div
          v-for="addr in addresses"
          :key="addr.addressId"
          class="address-card"
        >
          <div class="address-info">
            <div class="address-name">
              <strong>{{ addr.recipientName }}</strong>
              <span class="address-phone">{{ addr.phone }}</span>
              <span v-if="addr.isDefault" class="badge badge-primary">Mac dinh</span>
            </div>
            <div class="address-detail">
              {{ addr.street }}, {{ addr.wardName }}, {{ addr.districtName }}, {{ addr.provinceName }}
            </div>
          </div>
          <div class="address-actions">
            <button
              v-if="!addr.isDefault"
              class="btn btn-sm btn-outline"
              @click="setDefault(addr)"
            >
              Dat mac dinh
            </button>
            <button class="btn btn-sm btn-outline" @click="openEditAddress(addr)">
              <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-sm btn-outline" style="color: var(--red-active)" @click="deleteAddress(addr)">
              <i class="bi bi-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="showAddressForm" class="modal-overlay" @click.self="showAddressForm = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ editingAddress ? 'Sua dia chi' : 'Them dia chi moi' }}</h3>
          <button class="btn btn-sm btn-outline" @click="showAddressForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="saveAddress">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Ten nguoi nhan</label>
              <input v-model="addressForm.recipientName" class="form-input" maxlength="100" required />
            </div>
            <div class="form-group">
              <label class="form-label">So dien thoai</label>
              <input v-model="addressForm.phone" type="tel" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">Tinh / Thanh pho</label>
              <select v-model="selectedProvince" class="form-select" required>
                <option :value="null">Chon tinh/thanh pho</option>
                <option v-for="p in provinces" :key="p.id" :value="p.id">{{ p.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Quan / Huyen</label>
              <select v-model="selectedDistrict" class="form-select" :disabled="!selectedProvince || loadingDistricts" required>
                <option :value="null">{{ loadingDistricts ? 'Dang tai...' : 'Chon quan/huyen' }}</option>
                <option v-for="d in districts" :key="d.id" :value="d.id">{{ d.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">Phuong / Xa</label>
              <select v-model="selectedWard" class="form-select" :disabled="!selectedDistrict || loadingWards" required>
                <option :value="null">{{ loadingWards ? 'Dang tai...' : 'Chon phuong/xa' }}</option>
                <option v-for="w in wards" :key="w.code" :value="w.code">{{ w.name }}</option>
              </select>
            </div>
            <div class="form-group">
              <label class="form-label">So nha, ten duong</label>
              <input v-model="addressForm.street" class="form-input" placeholder="VD: 123 Nguyen Hue" maxlength="255" required />
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input type="checkbox" v-model="addressForm.isDefault" />
                <span>Dat lam dia chi mac dinh</span>
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline" @click="showAddressForm = false">Huy</button>
            <button type="submit" class="btn btn-primary">
              {{ editingAddress ? 'Cap nhat' : 'Them moi' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { padding: 32px 0; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.card-header h3 { font-size: 18px; font-weight: 700; }
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}
.profile-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
}
.profile-name { font-size: 18px; font-weight: 700; }
.profile-role { font-size: 13px; color: var(--text-mid); }
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border-light);
}
.loyalty-card { overflow: hidden; }
.loyalty-card .card-header h3 { display: flex; align-items: center; gap: 8px; }
.loyalty-card .card-header h3 i { color: #b7791f; }
.tier-list { display: flex; justify-content: space-between; font-size: 13px; font-weight: 600; color: var(--text-light); }
.tier-list span { display: flex; flex-direction: column; gap: 2px; }
.tier-list span:nth-child(2) { text-align: center; }
.tier-list span:last-child { text-align: right; }
.tier-list .active { color: var(--primary-dark); }
.tier-list small, .loyalty-next, .loyalty-transaction small { font-size: 11px; font-weight: 400; color: var(--text-mid); }
.loyalty-progress { height: 8px; overflow: hidden; border-radius: var(--radius-full); background: var(--border-light); margin: 10px 0 6px; }
.loyalty-progress div { height: 100%; border-radius: inherit; background: linear-gradient(90deg, #cd7f32, #d4af37); }
.loyalty-next { margin: 0; }
.loyalty-history { border-top: 1px solid var(--border-light); margin-top: 16px; padding-top: 14px; font-size: 14px; }
.loyalty-transaction { display: flex; justify-content: space-between; gap: 12px; padding: 10px 0; border-bottom: 1px solid var(--border-light); }
.loyalty-transaction span { display: flex; flex-direction: column; }
.loyalty-transaction b { color: var(--success); white-space: nowrap; }
.loyalty-transaction b.negative { color: var(--red-active); }
.alert-success {
  background: #dcfce7;
  color: #166534;
  padding: 12px 16px;
  border-radius: var(--radius);
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.address-list { display: flex; flex-direction: column; gap: 10px; }
.address-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  gap: 12px;
  background: #fff;
  transition: border-color var(--transition-fast);
}
.address-card:hover { border-color: var(--primary-100); }
.address-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.address-phone { font-size: 13px; color: var(--text-mid); }
.address-detail { font-size: 14px; color: var(--text-mid); }
.address-actions { display: flex; gap: 6px; flex-shrink: 0; }
.badge-primary {
  background: var(--primary-light);
  color: var(--primary-dark);
  padding: 2px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}
.form-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}
.form-checkbox input[type="checkbox"] { width: 18px; height: 18px; accent-color: var(--primary); }
@media (max-width: 768px) {
  .profile-avatar-section { flex-direction: column; align-items: center; text-align: center; }
  .address-card { flex-direction: column; }
  .address-actions { align-self: flex-end; margin-top: 8px; }
}
</style>
