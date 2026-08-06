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
