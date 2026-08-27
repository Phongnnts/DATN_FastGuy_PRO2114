import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const api = readFileSync(new URL('../src/api/staff.js', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/staff.js', import.meta.url), 'utf8');
const orders = readFileSync(new URL('../src/views/staff/OrdersPage.vue', import.meta.url), 'utf8');
const shifts = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');
const staffLayout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');

test('handover API uses the contract URL and exact optimistic claim body', () => {
  assert.match(api, /getHandoverOrders\(\)\s*\{\s*return client\.get\('\/staff\/orders\/handover'\)/);
  assert.match(api, /claimHandover\(orderId, expectedStatus, expectedOwnerShiftId\)[\s\S]*client\.put\(`\/staff\/orders\/\$\{orderId\}\/handover`, \{ expectedStatus, expectedOwnerShiftId \}\)/);
});

test('staff store maps ownership and rejects stale handover responses', () => {
  for (const field of ['staffShiftId', 'ownerShiftLabel', 'handoverRequired', 'waitingSince']) assert.match(store, new RegExp(`${field}:`));
  assert.match(store, /handoverItems = ref\(\[\]\)/);
  assert.match(store, /handoverLoading = ref\(false\)/);
  assert.match(store, /handoverError = ref\(''\)/);
  assert.match(store, /handoverRequestGeneration/);
  assert.match(store, /if \(generation !== handoverRequestGeneration\) return handoverItems\.value/);
});

test('claim removes success and reloads conflict', () => {
  assert.match(store, /await staffApi\.claimHandover\(orderId, order\.status, order\.staffShiftId\)/);
  assert.match(store, /handoverItems\.value = handoverItems\.value\.filter\(\(item\) => item\.id !== orderId\)/);
  assert.match(store, /if \(error\.status === 409\)[\s\S]*await fetchHandoverOrders\(\)/);
});

test('orders page exposes handover route states and accessible claim action', () => {
  assert.match(orders, /\{ key: 'HANDOVER', label: 'Bàn giao' \}/);
  assert.match(orders, /handoverCount/);
  assert.match(orders, /handoverLoading/);
  assert.match(orders, /handoverError/);
  assert.match(orders, /Không có đơn cần bàn giao/);
  assert.match(orders, /ownerShiftLabel/);
  assert.match(orders, /waitingSince/);
  assert.match(orders, /claimHandover/);
  assert.match(orders, /min-height:\s*44px/);
  assert.match(orders, /aria-live="polite"/);
  assert.match(orders, /\.order-link\s*\{[^}]*overflow-wrap:\s*anywhere/s);
  assert.match(orders, /\.table\s*\{[^}]*min-width:\s*0;[^}]*table-layout:\s*fixed/s);
  assert.match(orders, /\.table tbody td > \*\s*\{[^}]*min-width:\s*0;[^}]*max-width:\s*65%;[^}]*overflow-wrap:\s*anywhere/s);
  assert.match(staffLayout, /\.main-content\s*\{[^}]*min-width:\s*0;[^}]*width:\s*100%/s);
  assert.match(staffLayout, /\.staff-view\s*>\s*\*\s*\{[^}]*min-width:\s*0;[^}]*max-width:\s*100%/s);
  assert.match(orders, /409/);
});

test('shift page prechecks ownership and preserves authoritative checkout conflict', () => {
  assert.match(shifts, /getOwnershipCount/);
  assert.doesNotMatch(shifts, /fetchHandoverOrders/);
  assert.match(shifts, /activeOwnershipCount/);
  assert.match(shifts, /\/staff\/orders\?tab=HANDOVER/);
  assert.match(shifts, /error\.status === 409/);
  assert.match(shifts, /error\.data\?\.activeOwnershipCount/);
  assert.match(shifts, /v-if="error"[^>]*role="alert"/);
  assert.match(orders, /fetchHandoverOrders\(\)/);
  assert.match(store, /await fetchKitchenOrders\(order\.status\)/);
  assert.match(store, /kitchenQueues/);
  assert.match(orders, /new Date\(a\.waitingSince \|\| 0\) - new Date\(b\.waitingSince \|\| 0\)/);
  assert.match(orders, /tabindex/);
  assert.match(orders, /ArrowLeft/);
});
