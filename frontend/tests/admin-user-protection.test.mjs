import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/admin/UsersPage.vue', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/admin.js', import.meta.url), 'utf8');

test('users page disables self delete disable and demotion using authenticated user id', () => {
  assert.match(page, /useAuthStore/);
  assert.match(page, /authStore\.user\?\.id/);
  assert.match(page, /isSelf\(user\)/);
  assert.match(page, /:disabled="[^\"]*isSelf\(user\)/);
  assert.match(page, /:disabled="editingId === authStore\.user\?\.id"/);
});

test('status mutation reloads canonical users', () => {
  assert.match(store, /async function updateUserStatus/);
  assert.match(store, /adminApi\.updateUserStatus/);
  assert.match(store, /await fetchUsers\(\)/);
  assert.match(page, /adminStore\.updateUserStatus/);
  assert.doesNotMatch(page, /user\.status = nextStatus/);
});

test('users summaries expose icons proportions and truthful account context', () => {
  for (const token of ['stat-context', 'stat-meter', 'activePercent', 'workforcePercent', 'inactivePercent']) assert.match(page, new RegExp(token));
  assert.match(page, /Nhân viên và shipper/);
  assert.doesNotMatch(page, /tăng|giảm|so với tháng/i);
});

test('users detail inspector and dialogs preserve keyboard focus lifecycle', () => {
  for (const token of ['showDetail', 'detailUser', 'detailPanel', 'previousFocus', 'handleOverlayKeydown', 'event.key === \'Escape\'', 'restoreFocus']) assert.match(page, new RegExp(token));
  assert.match(page, /role="dialog"[\s\S]*aria-modal="true"[\s\S]*Chi tiết tài khoản/);
  assert.match(page, /\.icon-button \{[^}]*width: 40px;[^}]*height: 40px;/);
});
