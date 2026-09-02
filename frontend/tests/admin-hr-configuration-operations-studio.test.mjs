import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const hr = read('../src/views/admin/HrDashboardPage.vue');
const users = read('../src/views/admin/UsersPage.vue');
const shifts = read('../src/views/admin/ShiftsPage.vue');
const attendance = read('../src/views/admin/AttendancePage.vue');
const settings = read('../src/views/admin/SettingsPage.vue');
const logs = read('../src/views/admin/ActivityLogsPage.vue');

test('HR overview and account workspace use the shared Operations Studio hierarchy', () => {
  assert.match(hr, /operations-studio-page-header/);
  assert.match(hr, /hr-command-grid/);
  assert.match(hr, /shift-timeline/);
  assert.match(hr, /attention-rail/);
  assert.match(users, /operations-studio-page-header/);
  assert.match(users, /users-summary-grid/);
  assert.match(users, /users-workspace/);
  assert.match(users, /users-mobile-list/);
});

test('shift and attendance workspaces retain exact operational modes', () => {
  assert.match(shifts, /operations-studio-page-header/);
  assert.match(shifts, /schedule-workspace/);
  assert.match(shifts, /monitoring-workspace/);
  assert.match(shifts, /month-inspector/);
  assert.match(attendance, /operations-studio-page-header/);
  assert.match(attendance, /attendance-workspace/);
  assert.match(attendance, /pay-rate-workspace/);
  assert.match(attendance, /pay-rate-history/);
});

test('settings keep independent consequential save boundaries', () => {
  assert.match(settings, /consequential-settings-workspace/);
  assert.match(settings, /@submit\.prevent="saveTab\('store'\)"/);
  assert.match(settings, /@submit\.prevent="saveTab\('hours'\)"/);
  assert.match(settings, /@submit\.prevent="saveTab\('fees'\)"/);
  assert.match(settings, /@submit\.prevent="saveTab\('delivery'\)"/);
  assert.match(settings, /@submit\.prevent="saveTab\('inventory'\)"/);
  assert.match(settings, /:disabled="saving"/);
  assert.match(settings, /field-error/);
});

test('activity logs remain immutable evidence with actor resource and time hierarchy', () => {
  assert.match(logs, /audit-filter-workspace/);
  assert.match(logs, /immutable-audit-workspace/);
  assert.match(logs, /Người thực hiện/);
  assert.match(logs, /Đối tượng/);
  assert.match(logs, /<time :datetime="item.createdAt">/);
  assert.doesNotMatch(logs, /@click="(?:edit|remove|delete)/);
  assert.match(logs, /pagination.totalPages/);
});
