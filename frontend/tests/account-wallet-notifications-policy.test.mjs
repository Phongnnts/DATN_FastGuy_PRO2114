import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const tabs = read('../src/components/common/AccountTabs.vue');
const bell = read('../src/components/common/NotificationBell.vue');
const profile = read('../src/views/user/ProfilePage.vue');
const wallet = read('../src/components/common/LoyaltyWallet.vue');

test('keeps rewards and notification inbox authenticated under account', () => {
  assert.match(router, /path: 'rewards'[\s\S]*name: 'UserRewards'/);
  assert.match(router, /path: 'notifications'[\s\S]*name: 'UserNotifications'/);
  assert.match(router, /path: '\/loyalty', redirect: \{ name: 'UserRewards' \}/);
  assert.match(tabs, /\/account\/rewards/);
  assert.match(tabs, /\/account\/notifications/);
});

test('reuses loyalty wallet and exposes role-safe notification View All', () => {
  assert.match(profile, /<LoyaltyWallet compact/);
  assert.match(wallet, /loyaltyApi\.getMe\(\)/);
  assert.match(bell, /auth\.role === 'USER' \? '\/account\/notifications'/);
  assert.match(bell, /auth\.role === 'STAFF' \? '\/staff\/notifications'/);
  assert.match(bell, /v-if="inboxRoute"[\s\S]*Xem tất cả/);
});
