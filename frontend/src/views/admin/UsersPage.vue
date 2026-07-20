<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAdminStore } from '@/stores/admin';
import { adminApi } from '@/api';
import { useToast } from '@/stores/toast';
import { formatPrice, formatDate } from '@/utils/format';

const toast = useToast();
const adminStore = useAdminStore();
const searchTerm = ref('');
const activeRole = ref('');
const currentPage = ref(1);
const pageSize = 15;
const showForm = ref(false);
const editingId = ref(null);
const form = ref({ fullName: '', email: '', phone: '', password: '', roleName: 'USER' });
const success = ref('');
const loadError = ref('');
const showOrdersModal = ref(false);
const userOrders = ref([]);
const userOrdersLoading = ref(false);
const selectedUser = ref(null);

const roleFilters = [
  { key: '', label: 'Tất cả' },
  { key: 'USER', label: 'Khách hàng' },
  { key: 'STAFF', label: 'Staff' },
  { key: 'SHIPPER', label: 'Shipper' },
  { key: 'ADMIN', label: 'Admin' },
];

async function load() {
  loadError.value = '';
  try { await adminStore.fetchUsers(); }
  catch (e) { loadError.value = e.message || 'Không thể tải người dùng'; }
}

onMounted(load);

const filtered = computed(() => {
  let list = adminStore.allUsers;
  if (activeRole.value) list = list.filter((u) => u.roleName === activeRole.value);
  if (searchTerm.value) {
    const q = searchTerm.value.toLowerCase();
    list = list.filter((u) => (u.fullName || '').toLowerCase().includes(q) || (u.email || '').toLowerCase().includes(q) || (u.phone || '').toLowerCase().includes(q));
  }
  return list;
});

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => {
  const start = (currentPage.value - 1) * pageSize;
  return filtered.value.slice(start, start + pageSize);
});

function setRole(r) { activeRole.value = r; currentPage.value = 1; }

function openAdd() {
  editingId.value = null;
  form.value = { fullName: '', email: '', phone: '', password: '', roleName: 'USER' };
  showForm.value = true;
}

function openEdit(u) {
  editingId.value = u.userId;
  form.value = { fullName: u.fullName, email: u.email, phone: u.phone || '', password: '', roleName: u.roleName || 'USER' };
  showForm.value = true;
}

async function save() {
  try {
    if (editingId.value) {
      const data = { fullName: form.value.fullName, email: form.value.email, phone: form.value.phone, roleName: form.value.roleName };
      if (form.value.password) data.password = form.value.password;
      await adminStore.updateUser(editingId.value, data);
      success.value = 'Cập nhật thành công!';
    } else {
      await adminStore.createUser({ ...form.value });
      success.value = 'Thêm người dùng thành công!';
    }
    showForm.value = false;
    await adminStore.fetchUsers();
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    toast.error(e.message || 'Lỗi khi lưu');
  }
}

async function deleteUser(u) {
  if (!confirm(`Xóa người dùng "${u.fullName}"?`)) return;
  try {
    await adminStore.deleteUser(u.userId);
    success.value = 'Đã xóa!';
    await adminStore.fetchUsers();
    setTimeout(() => (success.value = ''), 3000);
  } catch (e) {
    toast.error(e.message || 'Lỗi khi xóa');
  }
}

async function toggleStatus(u) {
  const newStatus = u.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
  if (!confirm(`${newStatus === 'ACTIVE' ? 'Kích hoạt' : 'Vô hiệu hóa'} tài khoản "${u.fullName}"?`)) return;
  try {
    await adminApi.updateUserStatus(u.userId, { status: newStatus });
    u.status = newStatus;
    toast.success(`Đã ${newStatus === 'ACTIVE' ? 'kích hoạt' : 'vô hiệu hóa'} tài khoản`);
  } catch (e) { toast.error(e.message); }
}

async function viewOrders(u) {
  selectedUser.value = u;
  showOrdersModal.value = true;
  userOrdersLoading.value = true;
  try {
    const data = await adminApi.getUserOrders(u.userId);
    userOrders.value = Array.isArray(data) ? data : [];
  } catch { userOrders.value = []; }
  finally { userOrdersLoading.value = false; }
}

function roleBadgeClass(role) {
  if (role === 'ADMIN') return 'badge bg-danger';
  if (role === 'STAFF') return 'badge bg-primary';
  if (role === 'SHIPPER') return 'badge bg-info';
  return 'badge bg-secondary';
}
</script>

<template>
  <div>
    <div class="page-header">
      <h1>Quản lý người dùng</h1>
      <button class="btn btn-sm btn-primary" @click="openAdd"><i class="bi bi-plus-lg"></i> Thêm</button>
    </div>
    <div v-if="success" style="padding:10px 14px;margin-bottom:12px;border-radius:var(--radius);background:#dcfce7;color:#166534;font-size:14px">{{ success }}</div>
    <div v-if="loadError" style="padding:10px 14px;margin-bottom:12px;border-radius:var(--radius);background:#fef2f2;color:var(--red-active);font-size:14px;display:flex;align-items:center;gap:10px">
      <span>{{ loadError }}</span><button class="btn btn-sm btn-outline" @click="load">Thử lại</button>
    </div>
    <div v-else>
      <div style="display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin-bottom:16px">
        <div class="search-box" style="max-width:320px">
          <i class="bi bi-search"></i>
          <input v-model="searchTerm" class="form-input" placeholder="Tìm tên, email, SĐT..." />
        </div>
      </div>
      <div class="tabs" style="margin-bottom:16px">
        <button v-for="f in roleFilters" :key="f.key" class="tab" :class="{ active: activeRole === f.key }" @click="setRole(f.key)">{{ f.label }}</button>
      </div>
      <div class="card card-flat">
        <div class="table-wrapper">
          <table class="table">
            <thead><tr><th>ID</th><th>Họ tên</th><th>Email</th><th>SĐT</th><th>Vai trò</th><th>Trạng thái</th><th>Điểm TL</th><th></th></tr></thead>
            <tbody>
              <tr v-for="u in paged" :key="u.userId">
                <td>{{ u.userId }}</td>
                <td><strong>{{ u.fullName }}</strong></td>
                <td>{{ u.email }}</td>
                <td>{{ u.phone || '—' }}</td>
                <td><span :class="roleBadgeClass(u.roleName)" v-text="u.roleName"></span></td>
                <td><span class="badge" :class="u.status === 'ACTIVE' ? 'bg-success' : 'bg-warning'" @click="toggleStatus(u)" style="cursor:pointer" :title="`Click để ${u.status === 'ACTIVE' ? 'vô hiệu hóa' : 'kích hoạt'}`">{{ u.status || 'ACTIVE' }}</span></td>
                <td>{{ u.loyaltyPoints || 0 }}</td>
                <td style="white-space:nowrap">
                  <button class="btn btn-sm btn-ghost" title="Đơn hàng" @click="viewOrders(u)"><i class="bi bi-receipt"></i></button>
                  <button class="btn btn-sm btn-outline" style="margin:0 2px" @click="openEdit(u)"><i class="bi bi-pencil"></i></button>
                  <button class="btn btn-sm btn-ghost" style="color:var(--red-active)" @click="deleteUser(u)"><i class="bi bi-trash3"></i></button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-if="filtered.length === 0" style="padding:20px;text-align:center;color:var(--text-mid)">Không có dữ liệu</p>
      </div>
      <div v-if="totalPages > 1" style="display:flex;justify-content:center;gap:8px;margin-top:16px">
        <button class="btn btn-sm btn-outline" :disabled="currentPage <= 1" @click="currentPage--"><i class="bi bi-chevron-left"></i></button>
        <span style="padding:6px 12px;font-size:13px">{{ currentPage }} / {{ totalPages }}</span>
        <button class="btn btn-sm btn-outline" :disabled="currentPage >= totalPages" @click="currentPage++"><i class="bi bi-chevron-right"></i></button>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal">
        <div class="modal-header"><h3>{{ editingId ? 'Sửa người dùng' : 'Thêm người dùng' }}</h3><button class="btn btn-sm btn-outline" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div class="form-group"><label class="form-label">Họ tên</label><input v-model="form.fullName" class="form-input" required /></div>
          <div class="form-group"><label class="form-label">Email</label><input v-model="form.email" type="email" class="form-input" required /></div>
          <div class="form-group"><label class="form-label">Số điện thoại</label><input v-model="form.phone" class="form-input" /></div>
          <div class="form-group"><label class="form-label">{{ editingId ? 'Mật khẩu mới (để trống nếu không đổi)' : 'Mật khẩu' }}</label><input v-model="form.password" type="password" class="form-input" :required="!editingId" /></div>
          <div class="form-group"><label class="form-label">Vai trò</label><select v-model="form.roleName" class="form-select"><option value="USER">USER</option><option value="STAFF">STAFF</option><option value="SHIPPER">SHIPPER</option><option value="ADMIN">ADMIN</option></select></div>
        </div>
        <div class="modal-footer"><button class="btn btn-outline" @click="showForm = false">Hủy</button><button class="btn btn-primary" @click="save">{{ editingId ? 'Cập nhật' : 'Thêm' }}</button></div>
      </div>
    </div>

    <!-- User Orders Modal -->
    <div v-if="showOrdersModal" class="modal-overlay" @click.self="showOrdersModal = false">
      <div class="modal" style="max-width:700px">
        <div class="modal-header"><h3>Đơn hàng của {{ selectedUser?.fullName }}</h3><button class="btn btn-sm btn-outline" @click="showOrdersModal = false"><i class="bi bi-x-lg"></i></button></div>
        <div class="modal-body">
          <div v-if="userOrdersLoading" style="text-align:center;padding:20px">Đang tải...</div>
          <div v-else-if="userOrders.length === 0" style="text-align:center;padding:20px;color:var(--text-mid)">Chưa có đơn hàng</div>
          <table v-else class="table">
            <thead><tr><th>Mã đơn</th><th>Trạng thái</th><th>Tổng</th><th>Thanh toán</th><th>Ngày</th></tr></thead>
            <tbody>
              <tr v-for="o in userOrders" :key="o.orderId">
                <td><strong>{{ o.orderCode }}</strong></td>
                <td>{{ o.status }}</td>
                <td>{{ formatPrice(o.finalAmount) }}</td>
                <td>{{ o.paymentMethod === 'BANK_TRANSFER' ? 'CK' : 'COD' }} · {{ o.paymentStatus }}</td>
                <td>{{ formatDate(o.createdAt) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>
