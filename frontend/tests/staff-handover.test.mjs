import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const api = readFileSync(new URL('../src/api/staff.js', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/staff.js', import.meta.url), 'utf8');
const orders = readFileSync(new URL('../src/views/staff/OrdersPage.vue', import.meta.url), 'utf8');
const shifts = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');
const staffLayout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');

 test('manual handover consumer is removed after automatic rollover', () => {
  assert.doesNotMatch(api, /getHandoverOrders|claimHandover/);
  assert.doesNotMatch(store, /handoverItems|fetchHandoverOrders|claimHandover/);
  assert.doesNotMatch(orders, /HANDOVER|claimHandover|Bàn giao/);
  assert.doesNotMatch(shifts, /tab=HANDOVER|Mở danh sách bàn giao/);
});

test('staff queues retain ownership and timeout metadata', () => {
  for (const field of ['statusEnteredAt', 'expiresAt', 'remainingSeconds', 'timeoutPolicy', 'ownerShiftCode']) {
    assert.match(store, new RegExp(`${field}:`));
  }
  assert.match(orders, /remainingSeconds/);
  assert.match(orders, /expiresAt/);
  assert.match(orders, /\[409, 410\]\.includes\(error\.status\)/);
  assert.match(orders, /refresh\(\{ silent: true \}\)/);
});

test('shift page prechecks ownership and preserves authoritative checkout conflict', () => {
  assert.match(shifts, /getOwnershipCount/);
  assert.match(shifts, /activeOwnershipCount/);
  assert.match(shifts, /e\.status === 409/);
  assert.match(shifts, /e\.data\?\.activeOwnershipCount/);
  assert.match(shifts, /v-if="error"[^>]*role="alert"/);
  assert.match(shifts, /getWeek/);
  assert.match(shifts, /checkInSource/);
  assert.match(shifts, /checkOutSource/);
});

test('staff desktop layout remains overflow safe and keyboard reachable', () => {
  assert.match(orders, /tabindex/);
  assert.match(orders, /ArrowLeft/);
  assert.match(orders, /aria-live="polite"/);
  assert.match(orders, /\.order-link\s*\{[^}]*overflow-wrap:\s*anywhere/s);
  assert.match(staffLayout, /\.main-content\s*\{[^}]*min-width:\s*0;[^}]*width:\s*100%/s);
  assert.match(staffLayout, /\.staff-view\s*>\s*\*\s*\{[^}]*min-width:\s*0;[^}]*max-width:\s*100%/s);
});
