import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8');
const staffLayout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');
const notificationBell = readFileSync(new URL('../src/components/common/NotificationBell.vue', import.meta.url), 'utf8');
const notificationsPage = readFileSync(new URL('../src/views/user/NotificationsPage.vue', import.meta.url), 'utf8');
const notificationStore = readFileSync(new URL('../src/stores/notification.js', import.meta.url), 'utf8');

test('staff inbox reuses notifications page under staff authorization with breadcrumbs', () => {
  assert.match(router, /path: '\/staff',[\s\S]*meta: \{ requiresAuth: true, role: ROLES\.STAFF \}/);
  assert.match(router, /path: 'notifications',[\s\S]{0,160}name: 'StaffNotifications',[\s\S]{0,160}views\/user\/NotificationsPage\.vue/);
  assert.match(router, /name: 'StaffNotifications'[\s\S]{0,260}breadcrumb:[\s\S]*Thông báo/);
  assert.match(router, /StaffNotifications: 'Thông báo'/);
});

test('staff layout exposes inbox without requiring checked-in shift', () => {
  assert.match(staffLayout, /label: 'Thông báo', path: '\/staff\/notifications', icon: 'bi-bell'/);
  assert.match(staffLayout, /\$route\.name === 'StaffNotifications'/);
  assert.doesNotMatch(router, /name: 'StaffNotifications',[\s\S]{0,220}requiresCheckedInShift/);
});

test('notification bell links staff to staff inbox', () => {
  assert.match(notificationBell, /auth\.role === 'STAFF' \? '\/staff\/notifications'/);
  assert.match(notificationBell, /aria-label="Thông báo"/);
  assert.match(notificationBell, /aria-live="polite"/);
});

test('notification page preserves loading error retry and read actions accessibly', () => {
  assert.match(notificationStore, /const loading = ref\(false\)/);
  assert.match(notificationStore, /const error = ref\(''\)/);
  assert.match(notificationsPage, /v-if="store\.loading"[\s\S]*role="status"[\s\S]*Đang tải thông báo/);
  assert.match(notificationsPage, /v-else-if="store\.error"[\s\S]*role="alert"[\s\S]*Thử lại/);
  assert.match(notificationsPage, /store\.fetchOnce\(\)/);
  assert.match(notificationsPage, /store\.markAllRead\(\)/);
  assert.match(notificationsPage, /store\.markRead\(item\.notificationId\)/);
  assert.match(notificationsPage, /aria-label="Danh sách thông báo"/);
  assert.match(notificationsPage, /var\(--role-accent/);
});
