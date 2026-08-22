import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const globalCss = read('../src/assets/styles/global.css');
const variables = read('../src/assets/styles/variables.css');
const layouts = [
  read('../src/layouts/AdminLayout.vue'),
  read('../src/layouts/StaffLayout.vue'),
  read('../src/layouts/ShipperLayout.vue'),
].join('\n');
const appToast = read('../src/components/common/AppToast.vue');
const toastStore = read('../src/stores/toast.js');
const settingsPage = read('../src/views/admin/SettingsPage.vue');
const staffLayout = read('../src/layouts/StaffLayout.vue');
const router = read('../src/router/index.js');
const initSql = read('../../database/init.sql');

test('keeps responsive, table, focus, and reduced-motion policies', () => {
  assert.match(globalCss, /@media \(max-width: 360px\)/);
  assert.match(globalCss, /\.table-wrapper\s*\{[\s\S]*overflow-x: auto/);
  assert.match(globalCss, /\.table\s*\{[\s\S]*min-width: 640px/);
  assert.match(globalCss, /:focus-visible/);
  assert.match(globalCss, /prefers-reduced-motion: reduce/);
});

test('uses one FastGuy orange role accent and Vietnamese status copy', () => {
  assert.match(variables, /--role-admin: var\(--primary\)/);
  assert.match(variables, /--role-staff: #0f766e/);
  assert.match(variables, /--role-shipper: var\(--primary\)/);
  assert.doesNotMatch(layouts, /Control cockpit|Kitchen queue|Live route/);
  assert.match(layouts, /Trung tâm quản trị|Hàng đợi bếp|Tuyến đang giao/);
});

test('toast keeps Pinia state reactive for timeout and manual dismissal', () => {
  assert.match(appToast, /storeToRefs/);
  assert.doesNotMatch(appToast, /const\s*\{\s*toasts\s*,\s*dismiss\s*\}\s*=\s*useToastStore/);
  assert.match(toastStore, /function error\(message, duration = 3000\)/);
});

test('store defaults and fresh database seed use all-day business hours', () => {
  assert.match(settingsPage, /business_open_time:\s*'00:00',\s*business_close_time:\s*'00:00'/);
  assert.match(initSql, /'business_open_time',\s*'00:00'/);
  assert.match(initSql, /'business_close_time',\s*'00:00'/);
});

test('staff shell uses a distinct teal operations direction', () => {
  assert.match(variables, /--role-staff:\s*#0f766e/);
  assert.match(variables, /--role-staff-soft:\s*rgba\(15,\s*118,\s*110/);
  assert.match(staffLayout, /AppBreadcrumbs/);
  assert.match(staffLayout, /aria-label="Điều hướng nhân viên"/);
  assert.match(staffLayout, /fg-shell-staff/);
  assert.match(staffLayout, /linear-gradient\(135deg,var\(--role-staff\)/);
});

test('staff nested routes expose breadcrumbs and modern responsive surfaces', () => {
  for (const [name, label] of [
    ['StaffDispatch', 'Điều phối giao hàng'],
    ['StaffOrderHistory', 'Lịch sử đơn'],
    ['StaffOrderDetail', 'Chi tiết đơn hàng'],
    ['StaffShifts', 'Ca làm'],
  ]) {
    assert.match(router, new RegExp(`name: '${name}'[\\s\\S]{0,320}breadcrumb:[\\s\\S]{0,180}${label}`));
  }
  assert.match(staffLayout, /staff-view/);
  assert.match(staffLayout, /@media \(max-width: 768px\)/);
  assert.match(staffLayout, /:deep\(\.table tbody tr:has\(td\[data-label\]\)\)/);
});
