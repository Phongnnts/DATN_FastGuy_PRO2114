<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useToast } from '@/stores/toast';
import { formatDate, formatPrice } from '@/utils/format';

const adminStore = useAdminStore();
const toast = useToast();
const searchTerm = ref('');
const activeRole = ref('');
const currentPage = ref(1);
const pageSize = 12;
const loading = ref(true);
const loadError = ref('');
const showForm = ref(false);
const editingId = ref(null);
const saving = ref(false);
const actionId = ref(null);
const form = ref(emptyForm());
const showOrdersModal = ref(false);
const userOrders = ref([]);
const userOrdersLoading = ref(false);
const selectedUser = ref(null);

const roleFilters = [
  { key: '', label: 'Tất cả', icon: 'bi-people' },
  { key: 'USER', label: 'Khách hàng', icon: 'bi-person' },
  { key: 'STAFF', label: 'Nhân viên', icon: 'bi-person-badge' },
  { key: 'SHIPPER', label: 'Shipper', icon: 'bi-bicycle' },
  { key: 'ADMIN', label: 'Quản trị', icon: 'bi-shield-check' },
];

const roleMeta = {
  USER: { label: 'Khách hàng', icon: 'bi-person', className: 'role-user' },
  STAFF: { label: 'Nhân viên', icon: 'bi-person-badge', className: 'role-staff' },
  SHIPPER: { label: 'Shipper', icon: 'bi-bicycle', className: 'role-shipper' },
  ADMIN: { label: 'Quản trị', icon: 'bi-shield-check', className: 'role-admin' },
};

function emptyForm() {
  return { fullName: '', email: '', phone: '', password: '', roleName: 'USER' };
}

async function load() {
  loading.value = true;
  loadError.value = '';
  try {
    await adminStore.fetchUsers();
  } catch (error) {
    loadError.value = error.message || 'Không thể tải người dùng';
  } finally {
    loading.value = false;
  }
}

onMounted(load);

const stats = computed(() => ({
  total: adminStore.allUsers.length,
  active: adminStore.allUsers.filter((user) => (user.status || 'ACTIVE') === 'ACTIVE').length,
  staff: adminStore.allUsers.filter((user) => ['STAFF', 'SHIPPER'].includes(user.roleName)).length,
  inactive: adminStore.allUsers.filter((user) => user.status === 'INACTIVE').length,
}));

const filtered = computed(() => {
  const query = searchTerm.value.trim().toLowerCase();
  return adminStore.allUsers.filter((user) => {
    const matchesRole = !activeRole.value || user.roleName === activeRole.value;
    const matchesQuery = !query || [user.fullName, user.email, user.phone, String(user.userId)]
      .some((value) => String(value || '').toLowerCase().includes(query));
    return matchesRole && matchesQuery;
  });
});

const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / pageSize)));
const paged = computed(() => filtered.value.slice((currentPage.value - 1) * pageSize, currentPage.value * pageSize));

watch(searchTerm, () => { currentPage.value = 1; });
watch(totalPages, (pages) => { if (currentPage.value > pages) currentPage.value = pages; });

function roleCount(role) {
  return role ? adminStore.allUsers.filter((user) => user.roleName === role).length : adminStore.allUsers.length;
}

function setRole(role) {
  activeRole.value = role;
  currentPage.value = 1;
}

function openAdd() {
  editingId.value = null;
  form.value = emptyForm();
  showForm.value = true;
}

function openEdit(user) {
  editingId.value = user.userId;
  form.value = {
    fullName: user.fullName || '',
    email: user.email || '',
    phone: user.phone || '',
    password: '',
    roleName: user.roleName || 'USER',
  };
  showForm.value = true;
}

async function save() {
  const fullName = form.value.fullName.trim();
  const email = form.value.email.trim().toLowerCase();
  const phone = form.value.phone.trim();
  if (fullName.length < 2) return toast.error('Họ tên phải có ít nhất 2 ký tự');
  if (!/^\S+@\S+\.\S+$/.test(email)) return toast.error('Email không hợp lệ');
  if (phone && !/^(\+84|0)\d{9}$/.test(phone)) return toast.error('Số điện thoại không hợp lệ');
  if (!editingId.value && form.value.password.length < 6) return toast.error('Mật khẩu phải có ít nhất 6 ký tự');
  if (form.value.password && form.value.password.length < 6) return toast.error('Mật khẩu phải có ít nhất 6 ký tự');

  saving.value = true;
  try {
    const payload = { fullName, email, phone, roleName: form.value.roleName };
    if (form.value.password) payload.password = form.value.password;
    if (editingId.value) await adminStore.updateUser(editingId.value, payload);
    else await adminStore.createUser(payload);
    showForm.value = false;
    toast.success(editingId.value ? 'Đã cập nhật người dùng' : 'Đã thêm người dùng');
  } catch (error) {
    toast.error(error.message || 'Không thể lưu người dùng');
  } finally {
    saving.value = false;
  }
}

async function removeUser(user) {
  if (!confirm(`Xóa vĩnh viễn tài khoản "${user.fullName}"? Hành động này không thể hoàn tác.`)) return;
  actionId.value = user.userId;
  try {
    await adminStore.deleteUser(user.userId);
    toast.success('Đã xóa người dùng');
  } catch (error) {
    toast.error(error.message || 'Không thể xóa người dùng có dữ liệu liên quan');
  } finally {
    actionId.value = null;
  }
}

async function toggleStatus(user) {
  const nextStatus = user.status === 'INACTIVE' ? 'ACTIVE' : 'INACTIVE';
  const action = nextStatus === 'ACTIVE' ? 'kích hoạt' : 'vô hiệu hóa';
  if (!confirm(`${action[0].toUpperCase()}${action.slice(1)} tài khoản "${user.fullName}"?`)) return;
  actionId.value = user.userId;
  try {
    await adminApi.updateUserStatus(user.userId, { status: nextStatus });
    user.status = nextStatus;
    toast.success(`Đã ${action} tài khoản`);
  } catch (error) {
    toast.error(error.message || `Không thể ${action} tài khoản`);
  } finally {
    actionId.value = null;
  }
}

async function viewOrders(user) {
  selectedUser.value = user;
  showOrdersModal.value = true;
  userOrdersLoading.value = true;
  try {
    const data = await adminApi.getUserOrders(user.userId);
    userOrders.value = Array.isArray(data) ? data : [];
  } catch (error) {
    userOrders.value = [];
    toast.error(error.message || 'Không thể tải đơn hàng');
  } finally {
    userOrdersLoading.value = false;
  }
}

function initials(name) {
  return String(name || '?').split(/\s+/).filter(Boolean).slice(-2).map((part) => part[0]).join('').toUpperCase();
}
</script>

<template>
  <div class="users-page">
    <div class="page-header">
      <div><h1>Quản lý người dùng</h1><p>Quản lý tài khoản, vai trò và quyền truy cập hệ thống.</p></div>
      <button class="btn btn-primary add-button" @click="openAdd"><i class="bi bi-person-plus"></i> Thêm người dùng</button>
    </div>

    <div class="stats-grid">
      <article class="user-stat stat-total"><div class="stat-icon"><i class="bi bi-people-fill"></i></div><div><strong>{{ stats.total }}</strong><span>Tổng tài khoản</span></div></article>
      <article class="user-stat stat-active"><div class="stat-icon"><i class="bi bi-person-check-fill"></i></div><div><strong>{{ stats.active }}</strong><span>Đang hoạt động</span></div></article>
      <article class="user-stat stat-staff"><div class="stat-icon"><i class="bi bi-person-workspace"></i></div><div><strong>{{ stats.staff }}</strong><span>Nhân sự vận hành</span></div></article>
      <article class="user-stat stat-inactive"><div class="stat-icon"><i class="bi bi-person-dash-fill"></i></div><div><strong>{{ stats.inactive }}</strong><span>Đã vô hiệu hóa</span></div></article>
    </div>

    <div class="users-panel">
      <div class="toolbar">
        <label class="user-search"><i class="bi bi-search"></i><input v-model="searchTerm" placeholder="Tìm tên, email, SĐT hoặc ID"><button v-if="searchTerm" type="button" aria-label="Xóa tìm kiếm" @click="searchTerm = ''"><i class="bi bi-x-circle-fill"></i></button></label>
        <span class="result-count">{{ filtered.length }} kết quả</span>
      </div>

      <div class="role-tabs" role="tablist" aria-label="Lọc theo vai trò">
        <button v-for="filter in roleFilters" :key="filter.key" class="role-tab" :class="{ active: activeRole === filter.key }" @click="setRole(filter.key)"><i class="bi" :class="filter.icon"></i><span>{{ filter.label }}</span><b>{{ roleCount(filter.key) }}</b></button>
      </div>

      <div v-if="loadError" class="state-message error-state"><i class="bi bi-exclamation-triangle"></i><div><strong>Không thể tải dữ liệu</strong><span>{{ loadError }}</span></div><button class="btn btn-sm btn-outline" @click="load">Thử lại</button></div>
      <div v-else-if="loading" class="state-message"><span class="spinner"></span><span>Đang tải người dùng...</span></div>
      <div v-else-if="!paged.length" class="empty-state"><i class="bi bi-person-x"></i><h3>Không tìm thấy người dùng</h3><p>Thử thay đổi từ khóa hoặc bộ lọc vai trò.</p></div>

      <div v-else class="table-wrapper">
        <table class="table users-table">
          <thead><tr><th>Người dùng</th><th>Liên hệ</th><th>Vai trò</th><th>Trạng thái</th><th>Điểm</th><th><span class="sr-only">Thao tác</span></th></tr></thead>
          <tbody>
            <tr v-for="user in paged" :key="user.userId" :class="{ muted: user.status === 'INACTIVE' }">
              <td><div class="identity"><div class="avatar" :class="roleMeta[user.roleName]?.className">{{ initials(user.fullName) }}</div><div><strong>{{ user.fullName }}</strong><span>#{{ user.userId }}</span></div></div></td>
              <td><div class="contact"><span><i class="bi bi-envelope"></i>{{ user.email }}</span><span><i class="bi bi-telephone"></i>{{ user.phone || 'Chưa cập nhật' }}</span></div></td>
              <td><span class="role-pill" :class="roleMeta[user.roleName]?.className"><i class="bi" :class="roleMeta[user.roleName]?.icon"></i>{{ roleMeta[user.roleName]?.label || user.roleName }}</span></td>
              <td><button class="status-pill" :class="user.status === 'INACTIVE' ? 'inactive' : 'active'" :disabled="actionId === user.userId" @click="toggleStatus(user)"><span></span>{{ user.status === 'INACTIVE' ? 'Vô hiệu hóa' : 'Hoạt động' }}</button></td>
              <td><span class="points"><i class="bi bi-star-fill"></i>{{ Number(user.loyaltyPoints || 0).toLocaleString() }}</span></td>
              <td><div class="row-actions"><button class="icon-button orders" title="Xem đơn hàng" @click="viewOrders(user)"><i class="bi bi-receipt"></i></button><button class="icon-button edit" title="Chỉnh sửa" @click="openEdit(user)"><i class="bi bi-pencil-square"></i></button><button class="icon-button disable" :title="user.status === 'INACTIVE' ? 'Kích hoạt' : 'Vô hiệu hóa'" :disabled="actionId === user.userId" @click="toggleStatus(user)"><i class="bi" :class="user.status === 'INACTIVE' ? 'bi-person-check' : 'bi-person-slash'"></i></button><button class="icon-button delete" title="Xóa" :disabled="actionId === user.userId" @click="removeUser(user)"><i class="bi bi-trash3"></i></button></div></td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="!loading && filtered.length" class="pagination"><span>Trang {{ currentPage }} / {{ totalPages }}</span><div><button :disabled="currentPage === 1" @click="currentPage--"><i class="bi bi-chevron-left"></i></button><button :disabled="currentPage === totalPages" @click="currentPage++"><i class="bi bi-chevron-right"></i></button></div></div>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal user-modal" role="dialog" aria-modal="true" aria-labelledby="user-modal-title">
        <div class="modal-accent"></div>
        <div class="modal-header"><div><span class="modal-icon"><i class="bi" :class="editingId ? 'bi-person-gear' : 'bi-person-plus'"></i></span><div><h2 id="user-modal-title" class="modal-title">{{ editingId ? 'Chỉnh sửa người dùng' : 'Thêm người dùng' }}</h2><p>{{ editingId ? 'Cập nhật thông tin và phân quyền.' : 'Tạo tài khoản mới trong hệ thống.' }}</p></div></div><button class="modal-close" aria-label="Đóng" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <form class="modal-body" @submit.prevent="save">
          <div class="form-grid"><div class="form-group full"><label class="form-label" for="user-name">Họ và tên *</label><input id="user-name" v-model="form.fullName" class="form-input" maxlength="100" autocomplete="name" required></div><div class="form-group"><label class="form-label" for="user-email">Email *</label><input id="user-email" v-model="form.email" class="form-input" type="email" maxlength="150" autocomplete="email" required></div><div class="form-group"><label class="form-label" for="user-phone">Số điện thoại</label><input id="user-phone" v-model="form.phone" class="form-input" maxlength="12" autocomplete="tel" placeholder="0912345678"></div><div class="form-group"><label class="form-label" for="user-role">Vai trò *</label><select id="user-role" v-model="form.roleName" class="form-select"><option value="USER">Khách hàng</option><option value="STAFF">Nhân viên</option><option value="SHIPPER">Shipper</option><option value="ADMIN">Quản trị viên</option></select></div><div class="form-group"><label class="form-label" for="user-password">{{ editingId ? 'Mật khẩu mới' : 'Mật khẩu *' }}</label><input id="user-password" v-model="form.password" class="form-input" type="password" minlength="6" maxlength="72" autocomplete="new-password" :required="!editingId" :placeholder="editingId ? 'Để trống nếu không đổi' : 'Tối thiểu 6 ký tự'"></div></div>
          <div class="modal-footer"><button type="button" class="btn btn-ghost" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary" :disabled="saving"><span v-if="saving" class="spinner"></span>{{ saving ? 'Đang lưu...' : editingId ? 'Lưu thay đổi' : 'Tạo tài khoản' }}</button></div>
        </form>
      </div>
    </div>

    <div v-if="showOrdersModal" class="modal-overlay" @click.self="showOrdersModal = false">
      <div class="modal orders-modal" role="dialog" aria-modal="true" aria-labelledby="orders-modal-title"><div class="modal-header"><div><span class="modal-icon orders-icon"><i class="bi bi-receipt-cutoff"></i></span><div><h2 id="orders-modal-title" class="modal-title">Lịch sử đơn hàng</h2><p>{{ selectedUser?.fullName }} · {{ selectedUser?.email }}</p></div></div><button class="modal-close" aria-label="Đóng" @click="showOrdersModal = false"><i class="bi bi-x-lg"></i></button></div><div class="modal-body orders-body"><div v-if="userOrdersLoading" class="state-message"><span class="spinner"></span>Đang tải đơn hàng...</div><div v-else-if="!userOrders.length" class="empty-state compact"><i class="bi bi-bag-x"></i><h3>Chưa có đơn hàng</h3></div><div v-else class="table-wrapper"><table class="table"><thead><tr><th>Mã đơn</th><th>Trạng thái</th><th>Tổng tiền</th><th>Thanh toán</th><th>Ngày tạo</th></tr></thead><tbody><tr v-for="order in userOrders" :key="order.orderId"><td><strong>{{ order.orderCode }}</strong></td><td><span class="order-status">{{ order.status }}</span></td><td><strong>{{ formatPrice(order.finalAmount) }}</strong></td><td>{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }} · {{ order.paymentStatus }}</td><td>{{ formatDate(order.createdAt) }}</td></tr></tbody></table></div></div></div>
    </div>
  </div>
</template>

<style scoped>
.page-header p { margin: 5px 0 0; color: var(--text-mid); font-size: 14px; }
.add-button { padding: 10px 17px; box-shadow: 0 8px 20px rgba(212, 118, 74, .25); }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
.user-stat { display: flex; align-items: center; gap: 14px; min-height: 100px; padding: 18px; border: 1px solid var(--border); border-radius: 16px; background: var(--white); box-shadow: 0 4px 16px rgba(31, 41, 55, .04); }
.user-stat .stat-icon { display: grid; width: 48px; height: 48px; flex: 0 0 48px; place-items: center; border-radius: 14px; font-size: 21px; }
.user-stat strong { display: block; color: var(--text-dark); font-size: 25px; line-height: 1; }
.user-stat span { display: block; margin-top: 7px; color: var(--text-mid); font-size: 12px; }
.stat-total .stat-icon { color: #7c3aed; background: #ede9fe; }
.stat-active .stat-icon { color: #059669; background: #d1fae5; }
.stat-staff .stat-icon { color: #2563eb; background: #dbeafe; }
.stat-inactive .stat-icon { color: #dc2626; background: #fee2e2; }
.users-panel { overflow: hidden; border: 1px solid var(--border); border-radius: 16px; background: var(--white); box-shadow: 0 8px 30px rgba(31, 41, 55, .05); }
.toolbar { display: flex; align-items: center; gap: 14px; padding: 18px 20px 14px; }
.user-search { display: flex; align-items: center; width: min(440px, 100%); height: 44px; padding: 0 14px; border: 1px solid var(--border); border-radius: 12px; color: var(--text-mid); background: var(--bg); transition: .2s; }
.user-search:focus-within { border-color: var(--primary); background: var(--white); box-shadow: 0 0 0 3px rgba(212, 118, 74, .12); }
.user-search input { width: 100%; padding: 0 10px; border: 0; outline: 0; color: var(--text-dark); background: transparent; font: inherit; }
.user-search button { padding: 4px; border: 0; color: var(--text-mid); background: transparent; cursor: pointer; }
.result-count { margin-left: auto; color: var(--text-mid); font-size: 13px; }
.role-tabs { display: flex; gap: 7px; overflow-x: auto; padding: 0 20px 16px; border-bottom: 1px solid var(--border); }
.role-tab { display: inline-flex; align-items: center; gap: 7px; padding: 8px 11px; border: 1px solid transparent; border-radius: 10px; color: var(--text-mid); background: transparent; font: inherit; font-size: 13px; white-space: nowrap; cursor: pointer; }
.role-tab b { min-width: 22px; padding: 2px 6px; border-radius: 10px; color: #6b7280; background: #f3f4f6; font-size: 11px; }
.role-tab:hover { background: var(--bg); }
.role-tab.active { border-color: rgba(212, 118, 74, .22); color: var(--primary-dark); background: var(--primary-light); }
.role-tab.active b { color: var(--white); background: var(--primary); }
.users-table { min-width: 940px; }
.users-table th { padding: 12px 18px; color: #6b7280; background: #fafafa; font-size: 11px; letter-spacing: .05em; text-transform: uppercase; }
.users-table td { padding: 14px 18px; }
.users-table tbody tr { transition: .15s; }
.users-table tbody tr:hover { background: #fffaf7; }
.users-table tbody tr.muted { opacity: .64; background: #fafafa; }
.identity { display: flex; align-items: center; gap: 11px; }
.identity .avatar { display: grid; width: 40px; height: 40px; flex: 0 0 40px; place-items: center; border-radius: 12px; font-size: 12px; font-weight: 800; }
.identity strong, .identity span { display: block; }
.identity strong { color: var(--text-dark); font-size: 14px; }
.identity span { margin-top: 3px; color: var(--text-light); font-size: 11px; }
.contact { display: grid; gap: 5px; }
.contact span { display: flex; align-items: center; gap: 7px; color: var(--text-mid); font-size: 12px; }
.contact i { width: 13px; color: var(--text-light); }
.role-pill, .status-pill, .points { display: inline-flex; align-items: center; gap: 6px; white-space: nowrap; }
.role-pill { padding: 6px 9px; border-radius: 9px; font-size: 11px; font-weight: 700; }
.role-user { color: #7c3aed; background: #ede9fe; }
.role-staff { color: #2563eb; background: #dbeafe; }
.role-shipper { color: #0891b2; background: #cffafe; }
.role-admin { color: #dc2626; background: #fee2e2; }
.status-pill { padding: 6px 9px; border: 0; border-radius: 20px; font: inherit; font-size: 11px; font-weight: 700; cursor: pointer; }
.status-pill span { width: 7px; height: 7px; border-radius: 50%; }
.status-pill.active { color: #047857; background: #d1fae5; }
.status-pill.active span { background: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, .15); }
.status-pill.inactive { color: #b91c1c; background: #fee2e2; }
.status-pill.inactive span { background: #ef4444; }
.points { color: #92400e; font-size: 12px; font-weight: 700; }
.points i { color: #f59e0b; }
.row-actions { display: flex; justify-content: flex-end; gap: 5px; }
.icon-button { display: grid; width: 32px; height: 32px; place-items: center; border: 1px solid transparent; border-radius: 9px; background: transparent; cursor: pointer; transition: .15s; }
.icon-button.orders { color: #7c3aed; }
.icon-button.edit { color: #2563eb; }
.icon-button.disable { color: #d97706; }
.icon-button.delete { color: #dc2626; }
.icon-button:hover { border-color: currentColor; background: var(--white); transform: translateY(-1px); }
.icon-button:disabled, .status-pill:disabled { opacity: .45; cursor: wait; }
.state-message { display: flex; align-items: center; justify-content: center; gap: 10px; min-height: 230px; color: var(--text-mid); }
.state-message.error-state { justify-content: flex-start; min-height: auto; margin: 18px; padding: 14px; border: 1px solid #fecaca; border-radius: 12px; color: #b91c1c; background: #fef2f2; }
.error-state > i { font-size: 22px; }
.error-state div { display: grid; gap: 2px; }
.error-state span { font-size: 12px; }
.error-state .btn { margin-left: auto; }
.empty-state { padding: 55px 20px; }
.empty-state.compact { padding: 36px 20px; }
.pagination { display: flex; align-items: center; justify-content: space-between; padding: 13px 20px; border-top: 1px solid var(--border); color: var(--text-mid); font-size: 12px; }
.pagination div { display: flex; gap: 6px; }
.pagination button { display: grid; width: 34px; height: 34px; place-items: center; border: 1px solid var(--border); border-radius: 9px; color: var(--text-dark); background: var(--white); cursor: pointer; }
.pagination button:disabled { opacity: .4; cursor: not-allowed; }
.user-modal { max-width: 650px; overflow: hidden; }
.orders-modal { max-width: 850px; }
.modal-accent { height: 5px; background: linear-gradient(90deg, var(--primary), #f59e0b, #ec4899); }
.modal-header > div { display: flex; align-items: center; gap: 12px; }
.modal-header p { margin: 3px 0 0; color: var(--text-mid); font-size: 12px; }
.modal-icon { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 12px; color: var(--primary-dark); background: var(--primary-light); font-size: 19px; }
.modal-icon.orders-icon { color: #2563eb; background: #dbeafe; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.form-grid .full { grid-column: 1 / -1; }
.modal-footer { margin: 8px -24px -24px; padding: 16px 24px; background: #fafafa; }
.modal-footer .btn-primary { min-width: 135px; }
.orders-body { padding-top: 6px; }
.order-status { padding: 5px 8px; border-radius: 8px; color: #1d4ed8; background: #dbeafe; font-size: 11px; font-weight: 700; }
@media (max-width: 1050px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .page-header { align-items: flex-start; } .add-button { width: 100%; justify-content: center; } .stats-grid { grid-template-columns: 1fr 1fr; gap: 9px; } .user-stat { min-height: 84px; padding: 12px; } .user-stat .stat-icon { width: 38px; height: 38px; flex-basis: 38px; } .user-stat strong { font-size: 20px; } .user-stat span { font-size: 10px; } .toolbar { padding: 14px; } .result-count { display: none; } .role-tabs { padding: 0 14px 14px; } .form-grid { grid-template-columns: 1fr; } .form-grid .full { grid-column: auto; } .modal-footer { margin: 8px -16px -16px; padding: 14px 16px; } }
</style>
