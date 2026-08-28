import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { createServerCountdown, formatRemaining } from '../src/utils/staffTimeout.js';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/staff.js');
const store = read('../src/stores/staff.js');
const orders = read('../src/views/staff/OrdersPage.vue');
const detail = read('../src/views/staff/OrderDetailPage.vue');
const dispatch = read('../src/views/staff/DispatchPage.vue');

test('countdown derives only from server remainingSeconds and reaches zero once', () => {
  const countdown = createServerCountdown({ remainingSeconds: 2 }, 1000);
  assert.equal(countdown.remaining(1000), 2);
  assert.equal(countdown.remaining(2999), 1);
  assert.equal(countdown.remaining(3000), 0);
  assert.equal(formatRemaining(65), '01:05');
});

test('staff store preserves canonical timeout metadata', () => {
  for (const field of ['statusEnteredAt', 'expiresAt', 'remainingSeconds', 'timeoutPolicy', 'ownerShiftCode']) {
    assert.match(store, new RegExp(`${field}: o\\.${field}`));
  }
});

test('desktop staff surfaces countdown and canonical refetch on zero and 409 or 410', () => {
  for (const page of [orders, detail, dispatch]) {
    assert.match(page, /createServerCountdown/);
    assert.match(page, /formatRemaining/);
    assert.match(page, /remainingSeconds/);
    assert.match(page, /409[\s\S]{0,40}410/);
  }
});

test('manual handover frontend consumer is removed', () => {
  assert.doesNotMatch(api, /Handover|handover/);
  assert.doesNotMatch(store, /handoverItems|fetchHandoverOrders|claimHandover/);
  assert.doesNotMatch(orders, /HANDOVER|handover|Bàn giao|bàn giao/);
});

test('staff desktop pages show public cutoff config', () => {
  for (const page of [orders, detail, dispatch]) {
    assert.match(page, /storeApi\.getConfig/);
    assert.match(page, /orderCutoffTime/);
  }
});
