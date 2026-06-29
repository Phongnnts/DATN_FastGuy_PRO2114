<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { userApi, deliveryZoneApi } from '@/api';

const auth = useAuthStore();
const form = ref({ fullName: '', email: '', phone: '' });
const editMode = ref(false);
const success = ref('');

const addresses = ref([]);
const zones = ref([]);
const showAddressForm = ref(false);
const editingAddress = ref(null);
const addressForm = ref({
  recipientName: '',
  phone: '',
  street: '',
  wardName: '',
  districtName: '',
  provinceName: '',
  isDefault: false,
});

onMounted(async () => {
  if (auth.user) {
    form.value = {
      fullName: auth.user.fullName,
      email: auth.user.email,
      phone: auth.user.phone || '',
    };
  }
  await Promise.all([loadAddresses(), loadZones()]);
});

async function loadAddresses() {
  try {
    addresses.value = await userApi.getAddresses();
  } catch {
    addresses.value = [];
  }
}

async function loadZones() {
  try {
    zones.value = await deliveryZoneApi.getAll();
  } catch {
    zones.value = [];
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
    isDefault: addresses.value.length === 0,
  };
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
    isDefault: addr.isDefault || false,
  };
  showAddressForm.value = true;
}

async function saveAddress() {
  try {
    const data = {
      recipientName: addressForm.value.recipientName,
      phone: addressForm.value.phone,
      street: addressForm.value.street,
      wardName: addressForm.value.wardName,
      districtName: addressForm.value.districtName,
      provinceName: addressForm.value.provinceName,
      isDefault: addressForm.value.isDefault,
    };
    if (editingAddress.value) {
      await userApi.updateAddress(editingAddress.value.addressId, data);
    } else {
      await userApi.createAddress(data);
    }
    showAddressForm.value = false;
    success.value = editingAddress.value ? 'Cập nhật địa chỉ thành công!' : 'Thêm địa chỉ thành công!';
    setTimeout(() => (success.value = ''), 3000);
    await loadAddresses();
  } catch (e) {
    alert(e.message || 'Lỗi khi lưu địa chỉ');
  }
}

async function deleteAddress(addr) {
  if (!confirm(`Xóa địa chỉ "${addr.street}, ${addr.wardName}"?`)) return;
  try {
    await userApi.deleteAddress(addr.addressId);
    await loadAddresses();
  } catch (e) {
    alert(e.message || 'Lỗi khi xóa địa chỉ');
  }
}

async function setDefault(addr) {
  try {
    await userApi.setDefaultAddress(addr.addressId);
    await loadAddresses();
  } catch (e) {
    alert(e.message || 'Lỗi khi đặt mặc định');
  }
}

async function saveProfile() {
  await auth.updateProfile(form.value);
  success.value = 'Cập nhật thành công!';
  editMode.value = false;
  setTimeout(() => (success.value = ''), 3000);
}
</script>

<template>
  <div class="profile-page">
    <div class="card">
      <div class="card-header">
        <h3>Thông tin cá nhân</h3>
        <button
          v-if="!editMode"
          class="btn btn-sm btn-outline"
          @click="editMode = true"
        >
          <i class="bi bi-pencil"></i> Chỉnh sửa
        </button>
      </div>
      <div v-if="success" class="alert alert-success">
        <i class="bi bi-check-circle-fill"></i> {{ success }}
      </div>
      <div class="profile-avatar-section">
        <img
          :src="auth.user?.avatarUrl || 'https://i.pravatar.cc/150?u=default'"
          class="profile-avatar"
        />
        <div>
          <div class="profile-name">{{ auth.user?.fullName }}</div>
          <div class="profile-role">Thành viên</div>
        </div>
      </div>
      <form @submit.prevent="saveProfile">
        <div class="form-group">
          <label class="form-label">Họ tên</label>
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
          <label class="form-label">Số điện thoại</label>
          <input
            v-model="form.phone"
            type="tel"
            class="form-input"
            :disabled="!editMode"
          />
        </div>
        <div v-if="editMode" class="form-actions">
          <button type="submit" class="btn btn-primary">
            <i class="bi bi-check-lg"></i> Lưu thay đổi
          </button>
          <button
            type="button"
            class="btn btn-outline"
            @click="editMode = false"
          >
            Hủy
          </button>
        </div>
      </form>
    </div>

    <div class="card mt-3">
      <div class="card-header">
        <h3>Địa chỉ giao hàng</h3>
        <button class="btn btn-sm btn-primary" @click="openAddAddress">
          <i class="bi bi-plus-lg"></i> Thêm địa chỉ
        </button>
      </div>
      <div v-if="addresses.length === 0" class="empty-state" style="padding: 24px 0">
        <p style="color: var(--text-mid)">Chưa có địa chỉ nào</p>
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
              <span v-if="addr.isDefault" class="badge badge-primary">Mặc định</span>
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
              Đặt mặc định
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
          <h3>{{ editingAddress ? 'Sửa địa chỉ' : 'Thêm địa chỉ mới' }}</h3>
          <button class="btn btn-sm btn-outline" @click="showAddressForm = false">
            <i class="bi bi-x-lg"></i>
          </button>
        </div>
        <form @submit.prevent="saveAddress">
          <div class="modal-body">
            <div class="form-group">
              <label class="form-label">Tên người nhận</label>
              <input v-model="addressForm.recipientName" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">Số điện thoại</label>
              <input v-model="addressForm.phone" type="tel" class="form-input" required />
            </div>
            <div class="form-group">
              <label class="form-label">Tỉnh / Thành phố</label>
              <input v-model="addressForm.provinceName" class="form-input" placeholder="VD: TP. Hồ Chí Minh" required />
            </div>
            <div class="form-group">
              <label class="form-label">Quận / Huyện</label>
              <input v-model="addressForm.districtName" class="form-input" placeholder="VD: Quận 1" required />
            </div>
            <div class="form-group">
              <label class="form-label">Phường / Xã</label>
              <input v-model="addressForm.wardName" class="form-input" placeholder="VD: Phường Bến Nghé" required />
            </div>
            <div class="form-group">
              <label class="form-label">Số nhà, tên đường</label>
              <input v-model="addressForm.street" class="form-input" placeholder="VD: 123 Nguyễn Huệ" required />
            </div>
            <div class="form-group">
              <label class="form-checkbox">
                <input type="checkbox" v-model="addressForm.isDefault" />
                <span>Đặt làm địa chỉ mặc định</span>
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <button type="button" class="btn btn-outline" @click="showAddressForm = false">Hủy</button>
            <button type="submit" class="btn btn-primary">
              {{ editingAddress ? 'Cập nhật' : 'Thêm mới' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}
.card-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.profile-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
}
.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 25px;
  object-fit: cover;
}
.profile-name {
  font-size: 18px;
  font-weight: 700;
}
.profile-role {
  font-size: 13px;
  color: var(--text-mid);
}
.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--border);
}
.alert-success {
  background: #d4edda;
  color: #155724;
  padding: 12px 16px;
  border-radius: var(--radius-sm);
  margin-bottom: 16px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.address-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.address-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  gap: 12px;
}
.address-name {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.address-phone {
  font-size: 13px;
  color: var(--text-mid);
}
.address-detail {
  font-size: 14px;
  color: var(--text-mid);
}
.address-zone {
  font-size: 13px;
  color: var(--text-light);
  margin-top: 2px;
}
.address-actions {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}
.badge-primary {
  background: var(--primary-light, #e3f2fd);
  color: var(--primary, #2563eb);
  padding: 2px 8px;
  border-radius: 99px;
  font-size: 11px;
  font-weight: 600;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal {
  background: #fff;
  border-radius: var(--radius);
  width: 480px;
  max-width: 90vw;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px 0;
}
.modal-header h3 {
  font-size: 18px;
  font-weight: 700;
}
.modal-body {
  padding: 20px 24px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
.form-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  cursor: pointer;
}
.form-checkbox input[type="checkbox"] {
  width: 18px;
  height: 18px;
}
</style>
