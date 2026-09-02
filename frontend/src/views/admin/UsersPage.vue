<script setup>
import axios from 'axios';
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { adminApi } from '@/api';
import { useAdminStore } from '@/stores/admin';
import { useAuthStore } from '@/stores/auth';
import { useToast } from '@/stores/toast';
import { formatDate, formatPrice } from '@/utils/format';
import { CLOUDINARY } from '@/utils/constants';

const adminStore = useAdminStore();
const authStore = useAuthStore();
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
const uploadingAvatar = ref(false);
const avatarError = ref('');
const actionId = ref(null);
const form = ref(emptyForm());
const showOrdersModal = ref(false);
const userOrders = ref([]);
const userOrdersLoading = ref(false);
const selectedUser = ref(null);
const showDetail = ref(false);
const detailUser = ref(null);
const detailPanel = ref(null);
let previousFocus = null;

const ordersModalTitle = computed(() => ({
  STAFF: 'Đơn đã xử lý',
  SHIPPER: 'Đơn đã giao',
}[selectedUser.value?.roleName] || 'Lịch sử mua hàng'));

const completedDateLabel = computed(() => selectedUser.value?.roleName === 'USER' ? 'Ngày tạo' : 'Hoàn thành');

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
  return { fullName: '', email: '', phone: '', password: '', roleName: 'USER', avatarUrl: '' };
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
const percent = value => stats.value.total ? Math.round(value / stats.value.total * 1000) / 10 : 0;
const activePercent = computed(() => percent(stats.value.active));
const workforcePercent = computed(() => percent(stats.value.staff));
const inactivePercent = computed(() => percent(stats.value.inactive));

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

function isSelf(user) {
  return user.userId === authStore.user?.id;
}

function setRole(role) {
  activeRole.value = role;
  currentPage.value = 1;
}

function focusableDetailElements() {
  return [...detailPanel.value.querySelectorAll('button:not([disabled]),a[href]')];
}

function restoreFocus() {
  const target = previousFocus?.isConnected ? previousFocus : null;
  previousFocus = null;
  target?.focus();
}

async function openDetail(user, event) {
  previousFocus = event?.currentTarget || document.activeElement;
  detailUser.value = user;
  showDetail.value = true;
  await nextTick();
  focusableDetailElements()[0]?.focus();
  document.addEventListener('keydown', handleOverlayKeydown);
}

function closeDetail() {
  showDetail.value = false;
  document.removeEventListener('keydown', handleOverlayKeydown);
  restoreFocus();
}

function handleOverlayKeydown(event) {
  if (!showDetail.value) return;
  if (event.key === 'Escape') {
    event.preventDefault();
    closeDetail();
    return;
  }
  if (event.key !== 'Tab') return;
  const elements = focusableDetailElements();
  const first = elements[0];
  const last = elements.at(-1);
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
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
    avatarUrl: user.avatarUrl || '',
  };
  showForm.value = true;
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0];
  if (!file || uploadingAvatar.value || saving.value) return;
  uploadingAvatar.value = true;
  avatarError.value = '';
  try {
    const data = new FormData();
    data.append('file', file);
    data.append('upload_preset', CLOUDINARY.uploadPreset);
    const response = await axios.post(CLOUDINARY.uploadUrl, data);
    if (typeof response.data?.secure_url !== 'string' || !response.data.secure_url) throw new Error('Cloudinary không trả về URL ảnh hợp lệ');
    form.value.avatarUrl = response.data.secure_url;
  } catch (error) {
    avatarError.value = error.message || 'Không thể tải ảnh lên';
  } finally {
    uploadingAvatar.value = false;
    event.target.value = '';
  }
}

function removeAvatar() {
  form.value.avatarUrl = '';
  avatarError.value = '';
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
    const payload = { fullName, email, phone, roleName: form.value.roleName, avatarUrl: form.value.avatarUrl || null };
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
    await adminStore.updateUserStatus(user.userId, { status: nextStatus });
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

onBeforeUnmount(() => document.removeEventListener('keydown', handleOverlayKeydown));
</script>

<template>
  <div class="users-page">
    <div class="page-header operations-studio-page-header">
      <div><h1>Quản lý người dùng</h1><p>Quản lý tài khoản, vai trò và quyền truy cập hệ thống.</p></div>
      <button class="btn btn-primary add-button" @click="openAdd"><i class="bi bi-person-plus"></i> Thêm người dùng</button>
    </div>

    <div class="users-summary-grid stats-grid">
      <article class="user-stat stat-total"><div class="stat-heading"><span>Tổng tài khoản</span><span class="stat-icon"><i class="bi bi-people-fill" aria-hidden="true"></i></span></div><strong>{{ stats.total }}</strong><div class="stat-context"><span>Toàn hệ thống</span><b>100%</b></div><span class="stat-meter"><i :style="{ width: stats.total ? '100%' : '0%' }"></i></span></article>
      <article class="user-stat stat-active"><div class="stat-heading"><span>Đang hoạt động</span><span class="stat-icon"><i class="bi bi-person-check-fill" aria-hidden="true"></i></span></div><strong>{{ stats.active }}</strong><div class="stat-context"><span>Tài khoản khả dụng</span><b>{{ activePercent }}%</b></div><span class="stat-meter"><i :style="{ width: `${activePercent}%` }"></i></span></article>
      <article class="user-stat stat-staff"><div class="stat-heading"><span>Nhân sự vận hành</span><span class="stat-icon"><i class="bi bi-person-workspace" aria-hidden="true"></i></span></div><strong>{{ stats.staff }}</strong><div class="stat-context"><span>Nhân viên và shipper</span><b>{{ workforcePercent }}%</b></div><span class="stat-meter"><i :style="{ width: `${workforcePercent}%` }"></i></span></article>
      <article class="user-stat stat-inactive"><div class="stat-heading"><span>Đã vô hiệu hóa</span><span class="stat-icon"><i class="bi bi-person-dash-fill" aria-hidden="true"></i></span></div><strong>{{ stats.inactive }}</strong><div class="stat-context"><span>Không thể đăng nhập</span><b>{{ inactivePercent }}%</b></div><span class="stat-meter"><i :style="{ width: `${inactivePercent}%` }"></i></span></article>
    </div>

    <div class="users-workspace users-panel">
      <div class="users-toolbar toolbar">
        <label class="user-search"><i class="bi bi-search"></i><input v-model="searchTerm" placeholder="Tìm tên, email, SĐT hoặc ID"><button v-if="searchTerm" type="button" aria-label="Xóa tìm kiếm" @click="searchTerm = ''"><i class="bi bi-x-circle-fill"></i></button></label>
        <span class="result-count">{{ filtered.length }} kết quả</span>
      </div>

      <div class="role-tabs" role="tablist" aria-label="Lọc người dùng theo vai trò">
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
              <td><div class="identity"><img v-if="user.avatarUrl" :src="user.avatarUrl" :alt="`Ảnh đại diện của ${user.fullName}`" class="avatar avatar-image" /><div v-else class="avatar" :class="roleMeta[user.roleName]?.className">{{ initials(user.fullName) }}</div><div><strong>{{ user.fullName }}</strong><span>#{{ user.userId }}</span></div></div></td>
              <td><div class="contact"><span><i class="bi bi-envelope"></i>{{ user.email }}</span><span><i class="bi bi-telephone"></i>{{ user.phone || 'Chưa cập nhật' }}</span></div></td>
              <td><span class="role-pill" :class="roleMeta[user.roleName]?.className"><i class="bi" :class="roleMeta[user.roleName]?.icon"></i>{{ roleMeta[user.roleName]?.label || user.roleName }}</span></td>
              <td><button class="status-pill" :class="user.status === 'INACTIVE' ? 'inactive' : 'active'" :disabled="actionId === user.userId || (isSelf(user) && user.status !== 'INACTIVE')" @click="toggleStatus(user)"><span></span>{{ user.status === 'INACTIVE' ? 'Vô hiệu hóa' : 'Hoạt động' }}</button></td>
              <td><span class="points"><i class="bi bi-star-fill"></i>{{ Number(user.loyaltyPoints || 0).toLocaleString() }}</span></td>
              <td><div class="row-actions"><button class="icon-button details" aria-label="Xem chi tiết tài khoản" @click="openDetail(user, $event)"><i class="bi bi-three-dots"></i></button><button class="icon-button orders" title="Xem đơn hàng" @click="viewOrders(user)"><i class="bi bi-receipt"></i></button><button class="icon-button edit" title="Chỉnh sửa" @click="openEdit(user)"><i class="bi bi-pencil-square"></i></button><button class="icon-button disable" :title="user.status === 'INACTIVE' ? 'Kích hoạt' : 'Vô hiệu hóa'" :disabled="actionId === user.userId || (isSelf(user) && user.status !== 'INACTIVE')" @click="toggleStatus(user)"><i class="bi" :class="user.status === 'INACTIVE' ? 'bi-person-check' : 'bi-person-slash'"></i></button><button class="icon-button delete" title="Xóa" :disabled="actionId === user.userId || isSelf(user)" @click="removeUser(user)"><i class="bi bi-trash3"></i></button></div></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="!loading && paged.length" class="users-mobile-list" aria-label="Danh sách người dùng trên thiết bị nhỏ">
        <article v-for="user in paged" :key="`mobile-${user.userId}`" class="user-mobile-card" :class="{ muted: user.status === 'INACTIVE' }">
          <div class="mobile-user-head"><div class="identity"><img v-if="user.avatarUrl" :src="user.avatarUrl" alt="" class="avatar avatar-image" /><div v-else class="avatar" :class="roleMeta[user.roleName]?.className">{{ initials(user.fullName) }}</div><div><strong>{{ user.fullName }}</strong><span>#{{ user.userId }} · {{ user.email }}</span></div></div><button class="icon-button details" type="button" :aria-label="`Xem chi tiết ${user.fullName}`" @click="openDetail(user, $event)"><i class="bi bi-chevron-right" aria-hidden="true"></i></button></div>
          <div class="mobile-user-meta"><span class="role-pill" :class="roleMeta[user.roleName]?.className"><i class="bi" :class="roleMeta[user.roleName]?.icon" aria-hidden="true"></i>{{ roleMeta[user.roleName]?.label || user.roleName }}</span><span class="status-pill" :class="user.status === 'INACTIVE' ? 'inactive' : 'active'"><span></span>{{ user.status === 'INACTIVE' ? 'Vô hiệu hóa' : 'Hoạt động' }}</span><span class="points"><i class="bi bi-star-fill" aria-hidden="true"></i>{{ Number(user.loyaltyPoints || 0).toLocaleString() }}</span></div>
          <div class="mobile-user-actions"><button type="button" @click="viewOrders(user)"><i class="bi bi-receipt" aria-hidden="true"></i>Đơn hàng</button><button type="button" @click="openEdit(user)"><i class="bi bi-pencil-square" aria-hidden="true"></i>Chỉnh sửa</button></div>
        </article>
      </div>

      <div v-if="!loading && filtered.length" class="pagination"><span>Trang {{ currentPage }} / {{ totalPages }}</span><div><button :disabled="currentPage === 1" @click="currentPage--"><i class="bi bi-chevron-left"></i></button><button :disabled="currentPage === totalPages" @click="currentPage++"><i class="bi bi-chevron-right"></i></button></div></div>
    </div>

    <div v-if="showDetail" class="detail-overlay" @click.self="closeDetail">
      <aside ref="detailPanel" class="user-detail-panel" role="dialog" aria-modal="true" aria-labelledby="user-detail-title">
        <header><div><span>Chi tiết tài khoản</span><h2 id="user-detail-title">{{ detailUser?.fullName }}</h2><p>{{ roleMeta[detailUser?.roleName]?.label || detailUser?.roleName }} · #{{ detailUser?.userId }}</p></div><button class="icon-button" type="button" aria-label="Đóng chi tiết tài khoản" @click="closeDetail"><i class="bi bi-x-lg"></i></button></header>
        <div class="detail-identity"><img v-if="detailUser?.avatarUrl" :src="detailUser.avatarUrl" alt="" class="detail-avatar" /><span v-else class="detail-avatar">{{ initials(detailUser?.fullName) }}</span><span class="status-pill" :class="detailUser?.status === 'INACTIVE' ? 'inactive' : 'active'"><span></span>{{ detailUser?.status === 'INACTIVE' ? 'Vô hiệu hóa' : 'Hoạt động' }}</span></div>
        <dl><div><dt>Email</dt><dd>{{ detailUser?.email }}</dd></div><div><dt>Số điện thoại</dt><dd>{{ detailUser?.phone || 'Chưa cập nhật' }}</dd></div><div><dt>Điểm tích lũy</dt><dd>{{ Number(detailUser?.loyaltyPoints || 0).toLocaleString('vi-VN') }}</dd></div></dl>
        <footer><button class="btn btn-outline" type="button" @click="viewOrders(detailUser)">Xem đơn hàng</button><button class="btn btn-primary" type="button" @click="closeDetail(); openEdit(detailUser)">Chỉnh sửa</button></footer>
      </aside>
    </div>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal user-modal" role="dialog" aria-modal="true" aria-labelledby="user-modal-title">
        <div class="modal-accent"></div>
        <div class="modal-header"><div><span class="modal-icon"><i class="bi" :class="editingId ? 'bi-person-gear' : 'bi-person-plus'"></i></span><div><h2 id="user-modal-title" class="modal-title">{{ editingId ? 'Chỉnh sửa người dùng' : 'Thêm người dùng' }}</h2><p>{{ editingId ? 'Cập nhật thông tin và phân quyền.' : 'Tạo tài khoản mới trong hệ thống.' }}</p></div></div><button class="modal-close" aria-label="Đóng" @click="showForm = false"><i class="bi bi-x-lg"></i></button></div>
        <form class="modal-body" @submit.prevent="save">
          <div class="avatar-field"><img v-if="form.avatarUrl" :src="form.avatarUrl" alt="Xem trước ảnh đại diện" class="avatar-preview" /><span v-else class="avatar-preview avatar-empty">{{ initials(form.fullName) }}</span><div><label class="btn btn-sm btn-outline avatar-upload" :class="{ disabled: uploadingAvatar || saving }"><input type="file" accept="image/*" :disabled="uploadingAvatar || saving" @change="uploadAvatar">{{ uploadingAvatar ? 'Đang tải ảnh...' : 'Chọn ảnh' }}</label><button v-if="form.avatarUrl" type="button" class="btn btn-sm btn-ghost remove-avatar" :disabled="uploadingAvatar || saving" @click="removeAvatar">Xóa ảnh</button></div><p v-if="avatarError" class="avatar-error" role="alert">{{ avatarError }}</p></div>
          <div class="form-grid"><div class="form-group full"><label class="form-label" for="user-name">Họ và tên *</label><input id="user-name" v-model="form.fullName" class="form-input" maxlength="100" autocomplete="name" required></div><div class="form-group"><label class="form-label" for="user-email">Email *</label><input id="user-email" v-model="form.email" class="form-input" type="email" maxlength="150" autocomplete="email" required></div><div class="form-group"><label class="form-label" for="user-phone">Số điện thoại</label><input id="user-phone" v-model="form.phone" class="form-input" maxlength="12" autocomplete="tel" placeholder="0912345678"></div><div class="form-group"><label class="form-label" for="user-role">Vai trò *</label><select id="user-role" v-model="form.roleName" class="form-select" :disabled="editingId === authStore.user?.id"><option value="USER">Khách hàng</option><option value="STAFF">Nhân viên</option><option value="SHIPPER">Shipper</option><option value="ADMIN">Quản trị viên</option></select></div><div class="form-group"><label class="form-label" for="user-password">{{ editingId ? 'Mật khẩu mới' : 'Mật khẩu *' }}</label><input id="user-password" v-model="form.password" class="form-input" type="password" minlength="6" maxlength="72" autocomplete="new-password" :required="!editingId" :placeholder="editingId ? 'Để trống nếu không đổi' : 'Tối thiểu 6 ký tự'"></div></div>
          <div class="modal-footer"><button type="button" class="btn btn-ghost" @click="showForm = false">Hủy</button><button type="submit" class="btn btn-primary" :disabled="saving"><span v-if="saving" class="spinner"></span>{{ saving ? 'Đang lưu...' : editingId ? 'Lưu thay đổi' : 'Tạo tài khoản' }}</button></div>
        </form>
      </div>
    </div>

    <div v-if="showOrdersModal" class="modal-overlay" @click.self="showOrdersModal = false">
       <div class="modal orders-modal" role="dialog" aria-modal="true" aria-labelledby="orders-modal-title"><div class="modal-header"><div><span class="modal-icon orders-icon"><i class="bi bi-receipt-cutoff"></i></span><div><h2 id="orders-modal-title" class="modal-title">{{ ordersModalTitle }}</h2><p>{{ selectedUser?.fullName }} · {{ selectedUser?.email }}</p></div></div><button class="modal-close" aria-label="Đóng" @click="showOrdersModal = false"><i class="bi bi-x-lg"></i></button></div><div class="modal-body orders-body"><div v-if="userOrdersLoading" class="state-message"><span class="spinner"></span>Đang tải đơn hàng...</div><div v-else-if="!userOrders.length" class="empty-state compact"><i class="bi bi-bag-x"></i><h3>Chưa có đơn hàng</h3></div><div v-else class="table-wrapper"><table class="table"><thead><tr><th>Mã đơn</th><th>Trạng thái</th><th>Tổng tiền</th><th>Thanh toán</th><th>{{ completedDateLabel }}</th></tr></thead><tbody><tr v-for="order in userOrders" :key="order.orderId"><td><strong>{{ order.orderCode }}</strong></td><td><span class="order-status">{{ order.status }}</span></td><td><strong>{{ formatPrice(order.finalAmount) }}</strong></td><td>{{ order.paymentMethod === 'BANK_TRANSFER' ? 'PayOS' : 'COD' }} · {{ order.paymentStatus }}</td><td>{{ formatDate(order.completedAt || order.createdAt) }}</td></tr></tbody></table></div></div></div>
    </div>
  </div>
</template>

<style scoped>
.page-header p { margin: 5px 0 0; color: var(--text-mid); font-size: 14px; }
.add-button { padding: 10px 17px; box-shadow: 0 8px 20px rgba(212, 118, 74, .25); }
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 18px; }
.user-stat { position:relative; min-height: 132px; padding: 17px; overflow:hidden; border: 1px solid var(--admin-hairline); border-radius: 14px; background: var(--admin-surface); box-shadow: var(--admin-card-shadow); }
.stat-heading,.stat-context{display:flex;align-items:center;justify-content:space-between;gap:10px}.stat-heading>span:first-child{color:var(--admin-muted);font-size:12px}.user-stat .stat-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 9px; font-size: 15px; }
.user-stat strong { display: block; margin-top:12px; color: var(--admin-foreground); font-size: 27px; line-height: 1; font-variant-numeric:tabular-nums; }
.stat-context{margin-top:9px;color:var(--admin-muted);font-size:10px}.stat-context b{color:var(--admin-foreground);font-variant-numeric:tabular-nums}.stat-meter{display:block;height:3px;margin-top:10px;overflow:hidden;border-radius:3px;background:var(--admin-surface-subtle)}.stat-meter i{display:block;height:100%;border-radius:inherit;background:var(--admin-brand)}
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
.identity .avatar-image { object-fit: cover; }
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
.icon-button { display: grid; width: 40px; height: 40px; place-items: center; border: 1px solid transparent; border-radius: 9px; background: transparent; cursor: pointer; transition: transform var(--transition-fast), background-color var(--transition-fast), border-color var(--transition-fast); }
.icon-button.details { color: var(--admin-muted); }
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
.avatar-field { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; margin-bottom: 16px; padding: 14px; border: 1px solid var(--border); border-radius: 12px; background: var(--bg); }
.avatar-preview { width: 64px; height: 64px; flex: 0 0 64px; border-radius: 50%; object-fit: cover; }
.avatar-empty { display: grid; place-items: center; color: #fff; background: var(--primary); font-weight: 800; }
.avatar-field > div { display: flex; gap: 8px; flex-wrap: wrap; }
.avatar-upload { cursor: pointer; }
.avatar-upload input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
.avatar-upload.disabled { opacity: .55; cursor: wait; }
.remove-avatar, .avatar-error { color: var(--red-active); }
.avatar-error { width: 100%; margin: 0; font-size: 12px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 14px; }
.form-grid .full { grid-column: 1 / -1; }
.modal-footer { margin: 8px -24px -24px; padding: 16px 24px; background: #fafafa; }
.modal-footer .btn-primary { min-width: 135px; }
.orders-body { padding-top: 6px; }
.detail-overlay{position:fixed;inset:0;z-index:var(--z-modal);display:flex;justify-content:flex-end;padding:20px;background:rgba(20,20,35,.12);backdrop-filter:blur(4px)}.user-detail-panel{display:flex;width:min(380px,100%);height:100%;flex-direction:column;padding:20px;border:1px solid var(--admin-hairline);border-radius:16px;background:var(--admin-surface);box-shadow:0 24px 70px rgba(20,20,35,.18)}.user-detail-panel header{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.user-detail-panel header>div>span{color:var(--admin-muted);font-size:11px}.user-detail-panel h2{margin:4px 0 0;font-size:21px}.user-detail-panel header p{margin:2px 0 0;color:var(--admin-muted);font-size:12px}.detail-identity{display:flex;align-items:center;justify-content:space-between;gap:12px;margin:24px 0}.detail-avatar{display:grid;width:64px;height:64px;place-items:center;border-radius:17px;background:var(--admin-brand-soft);color:var(--admin-brand-dark);object-fit:cover;font-weight:800}.user-detail-panel dl{margin:0;border-top:1px solid var(--admin-hairline)}.user-detail-panel dl>div{padding:13px 0;border-bottom:1px solid var(--admin-hairline)}.user-detail-panel dt{color:var(--admin-muted);font-size:11px}.user-detail-panel dd{margin:3px 0 0;color:var(--admin-foreground);font-weight:650}.user-detail-panel footer{display:grid;grid-template-columns:1fr 1fr;gap:8px;margin-top:auto}
.order-status { padding: 5px 8px; border-radius: 8px; color: #1d4ed8; background: #dbeafe; font-size: 11px; font-weight: 700; }
@media (max-width: 1050px) { .stats-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 640px) { .page-header { align-items: flex-start; } .add-button { width: 100%; justify-content: center; } .stats-grid { grid-template-columns: 1fr 1fr; gap: 9px; } .user-stat { min-height: 84px; padding: 12px; } .user-stat .stat-icon { width: 38px; height: 38px; flex-basis: 38px; } .user-stat strong { font-size: 20px; } .user-stat span { font-size: 10px; } .toolbar { padding: 14px; } .result-count { display: none; } .role-tabs { padding: 0 14px 14px; } .form-grid { grid-template-columns: 1fr; } .form-grid .full { grid-column: auto; } .modal-footer { margin: 8px -16px -16px; padding: 14px 16px; } }
</style>

<style scoped>
.users-page{display:grid;gap:18px}.page-header{position:relative;padding:14px 4px 8px}.page-header h1{font-size:clamp(30px,3vw,40px);line-height:1.08;letter-spacing:-.05em}.add-button{min-height:44px;border-radius:var(--admin-control-radius);background:var(--admin-action);box-shadow:0 9px 22px rgba(196,63,22,.2)}.users-summary-grid{gap:14px;margin:0}.users-summary-grid .user-stat{min-height:140px;padding:20px;border-radius:var(--admin-panel-radius);box-shadow:var(--admin-card-shadow)}.users-summary-grid .stat-meter{height:4px}.users-workspace{border-color:var(--admin-hairline);border-radius:var(--admin-panel-radius);background:var(--admin-surface);box-shadow:var(--admin-card-shadow)}.users-toolbar{padding:20px 20px 14px}.user-search{height:46px;border-color:var(--admin-hairline);border-radius:var(--admin-control-radius);background:var(--admin-surface-subtle)}.role-tabs{padding:0 20px 17px;border-color:var(--admin-hairline)}.role-tab{min-height:40px}.table-wrapper{border-top:1px solid var(--admin-hairline)}.users-table th{padding:13px 18px;background:var(--admin-surface-subtle);color:var(--admin-muted)}.users-table td{padding:15px 18px;border-color:var(--admin-hairline)}.users-table tbody tr:hover{background:#fff8f4}.users-mobile-list{display:none}.pagination{border-color:var(--admin-hairline)}.pagination button{width:40px;height:40px;border-color:var(--admin-hairline);background:var(--admin-surface)}@media(max-width:760px){.table-wrapper{display:none}.users-mobile-list{display:grid;gap:10px;padding:12px;border-top:1px solid var(--admin-hairline)}.user-mobile-card{display:grid;gap:13px;padding:15px;border:1px solid var(--admin-hairline);border-radius:14px;background:var(--admin-surface)}.user-mobile-card.muted{opacity:.68}.mobile-user-head{display:flex;align-items:center;justify-content:space-between;gap:12px}.mobile-user-head .identity{min-width:0}.mobile-user-head .identity>div:last-child{min-width:0}.mobile-user-head .identity strong,.mobile-user-head .identity span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.mobile-user-meta{display:flex;align-items:center;gap:7px;flex-wrap:wrap}.mobile-user-actions{display:grid;grid-template-columns:1fr 1fr;gap:8px}.mobile-user-actions button{display:inline-flex;align-items:center;justify-content:center;gap:7px;min-height:42px;border:1px solid var(--admin-hairline);border-radius:10px;color:var(--admin-foreground);background:var(--admin-surface-subtle);font-weight:650}.users-toolbar{align-items:stretch;flex-direction:column}.result-count{margin:0}.user-search{width:100%}}@media(max-width:430px){.users-summary-grid{grid-template-columns:1fr}.users-summary-grid .user-stat{min-height:112px}.role-tabs{padding-inline:14px}.users-mobile-list{padding:10px}}
</style>
