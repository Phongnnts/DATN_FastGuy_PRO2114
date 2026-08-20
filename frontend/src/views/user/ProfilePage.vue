<script setup>
import axios from 'axios';
import { ref, onMounted, onUnmounted } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { useToast } from '@/stores/toast';
import LoyaltyWallet from '@/components/common/LoyaltyWallet.vue';
import { createProfileLoadController } from '@/utils/profileHydration';
import { CLOUDINARY } from '@/utils/constants';

const auth = useAuthStore();
const toast = useToast();
const form = ref({ fullName: '', email: '', phone: '', avatarUrl: '' });
const profileSnapshot = ref(null);
const editMode = ref(false);
const savingProfile = ref(false);
const uploadingAvatar = ref(false);
const avatarError = ref('');
const profileLoading = ref(true);
const profileError = ref('');

const profileLoader = createProfileLoadController({
  hydrate: () => auth.hydrateProfile(),
  apply: syncProfile,
  fail: (error) => { profileError.value = error.message || 'Không thể tải hồ sơ. Vui lòng thử lại.'; },
  setLoading: (value) => { profileLoading.value = value; },
  reset: () => { profileError.value = ''; },
});

onMounted(loadProfile);
onUnmounted(profileLoader.stop);

function loadProfile() {
  return profileLoader.load();
}

function syncProfile() {
  form.value = { fullName: auth.user?.fullName || '', email: auth.user?.email || '', phone: auth.user?.phone || '', avatarUrl: auth.user?.avatarUrl || '' };
}

function startProfileEdit() {
  profileSnapshot.value = { ...form.value };
  editMode.value = true;
}

function cancelProfileEdit() {
  if (profileSnapshot.value) form.value = { ...profileSnapshot.value };
  editMode.value = false;
}

async function uploadAvatar(event) {
  const file = event.target.files?.[0];
  if (!file || uploadingAvatar.value || savingProfile.value) return;
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

async function saveProfile() {
  const phonePattern = /^(0|\+84)(3|5|7|8|9)[0-9]{8}$/;
  const name = form.value.fullName?.trim();
  const phone = form.value.phone?.trim();
  const email = form.value.email?.trim();
  if (!name || name.length < 2 || name.length > 100) return toast.error('Họ tên phải có từ 2 đến 100 ký tự.');
  if (!phone || !phonePattern.test(phone)) return toast.error('Số điện thoại không hợp lệ.');
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return toast.error('Email không hợp lệ.');
  savingProfile.value = true;
  try {
    await auth.updateProfile({ fullName: name, email, phone, avatarUrl: form.value.avatarUrl || null });
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
  <div class="profile-page">
    <header class="page-heading">
      <div><span class="eyebrow">Tài khoản</span><h1>Hồ sơ của tôi</h1><p>Quản lý thông tin cá nhân và quyền lợi thành viên.</p></div>
    </header>
    <div v-if="profileLoading" class="profile-state" role="status">
      <span class="spinner" aria-hidden="true"></span> Đang tải hồ sơ...
    </div>
    <div v-else-if="profileError" class="profile-state profile-error" role="alert">
      <span>{{ profileError }}</span>
      <button type="button" class="btn btn-primary" @click="loadProfile">Thử lại</button>
    </div>
    <section v-else class="profile-grid" :class="{ single: !auth.isUser }" aria-label="Thông tin tài khoản">
      <article class="panel identity-panel">
        <div class="section-heading"><div><span class="section-kicker">Hồ sơ</span><h2>Thông tin cá nhân</h2></div><button v-if="!editMode" type="button" class="btn btn-outline" @click="startProfileEdit"><i class="bi bi-pencil" aria-hidden="true"></i> Chỉnh sửa</button></div>
        <div class="profile-summary">
          <img v-if="auth.user?.avatarUrl" :src="auth.user.avatarUrl" :alt="`Ảnh đại diện của ${auth.user?.fullName || 'thành viên'}`" class="profile-avatar" />
          <span v-else class="profile-avatar avatar-placeholder" aria-hidden="true">{{ (auth.user?.fullName || 'F').trim().charAt(0).toUpperCase() }}</span>
          <div><strong>{{ auth.user?.fullName }}</strong><span>Thành viên FastGuy</span></div>
        </div>
        <form v-if="editMode" class="profile-form" @submit.prevent="saveProfile">
          <div class="avatar-editor">
            <img v-if="form.avatarUrl" :src="form.avatarUrl" alt="Xem trước ảnh đại diện" class="profile-avatar" />
            <span v-else class="profile-avatar avatar-placeholder" aria-hidden="true">{{ (form.fullName || 'F').trim().charAt(0).toUpperCase() }}</span>
            <div><label class="btn btn-outline avatar-upload" :class="{ disabled: uploadingAvatar || savingProfile }"><input type="file" accept="image/*" :disabled="uploadingAvatar || savingProfile" @change="uploadAvatar" />{{ uploadingAvatar ? 'Đang tải ảnh...' : 'Chọn ảnh' }}</label><button v-if="form.avatarUrl" type="button" class="btn btn-ghost remove-avatar" :disabled="uploadingAvatar || savingProfile" @click="removeAvatar">Xóa ảnh</button></div>
            <p v-if="avatarError" class="avatar-error" role="alert">{{ avatarError }}</p>
          </div>
          <div class="field"><label for="profile-name">Họ và tên</label><input id="profile-name" v-model="form.fullName" class="form-input" autocomplete="name" maxlength="100" required /></div>
          <div class="field"><label for="profile-email">Email</label><input id="profile-email" v-model="form.email" type="email" class="form-input" autocomplete="email" required /></div>
          <div class="field"><label for="profile-phone">Số điện thoại</label><input id="profile-phone" v-model="form.phone" type="tel" class="form-input" autocomplete="tel" required /></div>
          <div class="form-actions"><button type="button" class="btn btn-outline" :disabled="savingProfile" @click="cancelProfileEdit">Hủy</button><button type="submit" class="btn btn-primary" :disabled="savingProfile"><span v-if="savingProfile" class="spinner" aria-hidden="true"></span>{{ savingProfile ? 'Đang lưu...' : 'Lưu thay đổi' }}</button></div>
        </form>
        <dl v-else class="detail-list">
          <div><dt>Họ và tên</dt><dd>{{ form.fullName || 'Chưa cập nhật' }}</dd></div>
          <div><dt>Email</dt><dd>{{ form.email || 'Chưa cập nhật' }}</dd></div>
          <div><dt>Số điện thoại</dt><dd>{{ form.phone || 'Chưa cập nhật' }}</dd></div>
        </dl>
      </article>
      <article v-if="auth.isUser" class="panel loyalty-panel">
        <LoyaltyWallet compact />
        <router-link class="btn btn-outline wallet-link" to="/account/rewards">Xem ví điểm</router-link>
      </article>
    </section>
  </div>
</template>

<style scoped>
.profile-page { max-width: 1120px; margin: 0 auto; padding: 36px 20px 56px; color: var(--text-dark); }
.page-heading { margin-bottom: 24px; }
.page-heading h1 { margin: 4px 0 6px; font-size: clamp(26px, 4vw, 36px); line-height: 1.2; }
.page-heading p { margin: 0; color: var(--text-mid); }
.eyebrow, .section-kicker { color: var(--primary-dark); font-size: 11px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.profile-state { display: flex; align-items: center; justify-content: center; gap: var(--space-3); min-height: 180px; padding: var(--space-6); border: 1px solid var(--border-light); border-radius: var(--radius-lg); background: var(--bg-card); }
.profile-error { flex-direction: column; color: var(--red-active); text-align: center; }
.profile-state .btn { min-height: var(--control-height); }
.profile-grid { display: grid; grid-template-columns: minmax(0, 1.08fr) minmax(0, .92fr); gap: 20px; }
.profile-grid.single { grid-template-columns: minmax(0, 760px); }
.panel { padding: 24px; border: 1px solid var(--border-light); border-radius: 16px; background: #fff; box-shadow: 0 8px 28px rgba(24, 39, 75, .06); }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 22px; }
.section-heading h2 { margin: 3px 0 0; font-size: 20px; }
.profile-summary { display: flex; align-items: center; gap: 15px; padding: 16px; margin-bottom: 20px; border-radius: 12px; background: linear-gradient(135deg, var(--primary-light), #fff); }
.profile-avatar { width: 64px; height: 64px; flex: 0 0 64px; border: 3px solid #fff; border-radius: 50%; object-fit: cover; box-shadow: 0 4px 14px rgba(0, 0, 0, .1); }
.avatar-placeholder { display: grid; place-items: center; background: var(--primary); color: #fff; font-size: 24px; font-weight: 800; }
.profile-summary div { display: flex; flex-direction: column; gap: 3px; }
.profile-summary strong { font-size: 18px; }
.profile-summary span { color: var(--text-mid); font-size: 13px; }
.detail-list { margin: 0; }
.detail-list div { display: grid; grid-template-columns: 140px 1fr; gap: 14px; padding: 15px 0; border-bottom: 1px solid var(--border-light); }
.detail-list div:last-child { border-bottom: 0; }
.detail-list dt { color: var(--text-mid); font-size: 13px; }
.detail-list dd { margin: 0; font-weight: 600; overflow-wrap: anywhere; }
.profile-form { display: flex; flex-direction: column; gap: 16px; }
.avatar-editor { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; padding: 14px; border: 1px solid var(--border-light); border-radius: 12px; background: var(--color-surface-muted); }
.avatar-editor > div { display: flex; gap: 8px; flex-wrap: wrap; }
.avatar-upload { cursor: pointer; }
.avatar-upload input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0, 0, 0, 0); }
.avatar-upload.disabled { opacity: .55; cursor: wait; }
.remove-avatar { color: var(--red-active); }
.avatar-error { width: 100%; margin: 0; color: var(--red-active); font-size: 12px; }
.field { min-width: 0; }
.field label { display: block; margin-bottom: 7px; font-size: 13px; font-weight: 700; }
.form-input { width: 100%; min-height: 44px; box-sizing: border-box; }
.form-actions { display: flex; justify-content: flex-end; gap: 10px; padding-top: 4px; }
.spinner { display: inline-block; width: 15px; height: 15px; margin-right: 7px; border: 2px solid currentColor; border-right-color: transparent; border-radius: 50%; animation: spin .65s linear infinite; vertical-align: -2px; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .spinner { animation: none; } }
@media (max-width: 820px) { .profile-grid { grid-template-columns: 1fr; } }
@media (max-width: 560px) { .profile-page { padding: 24px 12px 40px; } .panel { padding: 18px; border-radius: 13px; } .section-heading { align-items: flex-start; } .detail-list div { grid-template-columns: 1fr; gap: 4px; } .profile-summary { align-items: flex-start; } }
</style>
