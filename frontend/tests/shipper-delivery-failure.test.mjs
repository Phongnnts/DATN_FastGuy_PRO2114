import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { deliveryFailureLabel, failureFocusTarget, failureRestoreTarget } from '../src/utils/shipperOperations.js';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const constants = read('../src/utils/constants.js');
const api = read('../src/api/shipper.js');
const sheet = read('../src/components/shipper/OrderActionSheet.vue');
const detail = read('../src/views/shipper/OrderDetailPage.vue');
const orders = read('../src/views/shipper/MyOrdersPage.vue');

test('delivery failure API and canonical labels use exact contracts', () => {
  assert.match(api, /failOrder\(orderId, \{ expectedStatus, reasonCode, note \}\)/);
  assert.match(api, /client\.post\(`\/shipper\/orders\/\$\{orderId\}\/fail`, \{ expectedStatus, reasonCode, note \}\)/);
  for (const value of ['DELIVERY_FAILED', 'RETURNED_TO_STORE', 'CUSTOMER_UNREACHABLE', 'INVALID_ADDRESS', 'CUSTOMER_RESCHEDULED', 'CUSTOMER_REJECTED', 'SHIPPER_INCIDENT', 'PRODUCT_INCIDENT']) assert.match(constants, new RegExp(value));
});

test('focus target keeps tab navigation inside failure dialog', () => {
  assert.equal(failureFocusTarget({ activeIndex: -1, lastIndex: 2, shiftKey: false }), 0);
  assert.equal(failureFocusTarget({ activeIndex: -1, lastIndex: 2, shiftKey: true }), 2);
  assert.equal(failureFocusTarget({ activeIndex: 2, lastIndex: 2, shiftKey: false }), 0);
  assert.equal(failureFocusTarget({ activeIndex: 0, lastIndex: 2, shiftKey: true }), 2);
  assert.equal(failureFocusTarget({ activeIndex: 1, lastIndex: 2, shiftKey: false }), null);
});

test('focus restore chooses connected opener then stable fallback', () => {
  const opener = { isConnected: true };
  const removedOpener = { isConnected: false };
  const fallback = {};
  assert.equal(failureRestoreTarget(opener, fallback), opener);
  assert.equal(failureRestoreTarget(removedOpener, fallback), fallback);
  assert.equal(failureRestoreTarget(null, fallback), fallback);
});

test('failure reason helper returns owner-readable labels', () => {
  assert.equal(deliveryFailureLabel('INVALID_ADDRESS'), 'Địa chỉ không hợp lệ');
  assert.equal(deliveryFailureLabel('UNKNOWN'), 'UNKNOWN');
  assert.equal(deliveryFailureLabel(''), 'Không rõ');
});

test('picked-up action sheet exposes success and explicit detail navigation', () => {
  assert.match(sheet, /Giao thành công/);
  assert.match(sheet, /Mở chi tiết để báo thất bại/);
  assert.match(sheet, /props\.order\.status === 'PICKED_UP'/);
});

test('failure dialog validates accessible reason and note fields', () => {
  assert.match(detail, /failOrder\(order\.value\.id, \{ expectedStatus: order\.value\.status, reasonCode: failureReason\.value, note: failureNote\.value\.trim\(\) \}\)/);
  assert.match(detail, /role="dialog"/);
  assert.match(detail, /aria-modal="true"/);
  assert.match(detail, /aria-labelledby="failure-title"/);
  assert.match(detail, /for="failure-reason"/);
  assert.match(detail, /id="failure-reason"/);
  assert.match(detail, /for="failure-note"/);
  assert.match(detail, /id="failure-note"/);
  assert.match(detail, /maxlength="500"/);
  assert.match(detail, /failureNote\.value\.trim\(\)/);
  assert.match(detail, /:disabled="!canSubmitFailure"/);
  assert.match(detail, /aria-live="polite"/);
  assert.match(detail, /event\.key === 'Escape'/);
  assert.match(detail, /event\.key !== 'Tab'/);
  assert.match(detail, /previousFailureFocus/);
  assert.match(detail, /document\.addEventListener\('focusin', handleFailureFocusIn\)/);
  assert.match(detail, /document\.addEventListener\('keydown', handleFailureKeydown\)/);
  assert.match(detail, /document\.removeEventListener\('focusin', handleFailureFocusIn\)/);
  assert.match(detail, /document\.removeEventListener\('keydown', handleFailureKeydown\)/);
  assert.match(detail, /failureDialog\.value\.contains\(event\.target\)/);
  assert.match(detail, /await load\(\);\s*await nextTick\(\);\s*restoreFailureFocus\(\)/);
  assert.match(detail, /ref="failureFocusFallback"/);
  assert.match(detail, /deliveryFailureLabel\(order\.deliveryFailureCode\)/);
  assert.match(detail, /order\.failureNote/);
  assert.match(detail, /deliveryAttemptCount/);
  assert.match(detail, /deliveryAttemptLimit/);
  assert.match(detail, /error\?\.status === 409/);
  assert.match(detail, /await load\(\)/);
  assert.doesNotMatch(detail, /failOrder[\s\S]+failOrder/);
});

test('failed and returned orders stay read-only and outside active list', () => {
  assert.match(detail, /DELIVERY_FAILED/);
  assert.match(detail, /RETURNED_TO_STORE/);
  assert.match(detail, /isReadOnlyFailure/);
  assert.match(orders, /ASSIGNED', 'PICKED_UP/);
  assert.match(orders, /activeStatuses\.includes\(order\.status\)/);
});
