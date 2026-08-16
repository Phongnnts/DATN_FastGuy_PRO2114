import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { customerDeliveryStatus, deliveryRetryMode, isCompletedRevenue } from '../src/utils/deliveryRecovery.js';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const staffApi = read('../src/api/staff.js');
const adminApi = read('../src/api/admin.js');
const staffOrders = read('../src/views/staff/OrdersPage.vue');
const staffDetail = read('../src/views/staff/OrderDetailPage.vue');
const userOrders = read('../src/views/user/OrdersPage.vue');
const userDetail = read('../src/views/user/OrderDetailPage.vue');
const guestTrack = read('../src/views/guest/TrackOrderPage.vue');
const adminOrders = read('../src/views/admin/OrdersPage.vue');
const adminDetail = read('../src/views/admin/OrderDetailPage.vue');
const timeline = read('../src/components/common/OrderTimeline.vue');
const staffStore = read('../src/stores/staff.js');
const orderStore = read('../src/stores/order.js');

test('recovery APIs use Task 4 endpoints and optimistic status', () => {
  assert.match(staffApi, /retryDelivery\(id, data\)/);
  assert.match(staffApi, /`\/staff\/orders\/\$\{id\}\/retry-delivery`/);
  assert.match(staffApi, /startScheduledRetry\(id, expectedStatus\)/);
  assert.match(staffApi, /returnToStore\(id, expectedStatus, note\)/);
  assert.match(adminApi, /overrideDeliveryAttempt\(id, expectedStatus, note\)/);
  assert.match(adminApi, /delivery-attempt-override/);
});

test('customer delivery state exposes exact safe copy and schedule only', () => {
  assert.deepEqual(customerDeliveryStatus({ status: 'DELIVERY_FAILED', retryScheduledAt: '2026-08-15T10:00:00' }), {
    message: 'Giao chưa thành công, cửa hàng đang xử lý',
    retryScheduledAt: '2026-08-15T10:00:00',
  });
  assert.equal(deliveryRetryMode('CUSTOMER_RESCHEDULED'), 'SCHEDULED');
  for (const page of [userOrders, userDetail, guestTrack]) {
    assert.match(page, /Giao chưa thành công, cửa hàng đang xử lý/);
    assert.doesNotMatch(page, /deliveryFailureCode|failureNote/);
  }
});

test('staff failure queue calls a defined API and maps exact recovery fields', () => {
  assert.match(staffApi, /getDeliveryFailedOrders\(params\)/);
  assert.match(staffStore, /DELIVERY_FAILED: staffApi\.getDeliveryFailedOrders/);
  for (const field of ['deliveryFailureCode', 'failureNote', 'deliveryFailedAt', 'deliveryAttemptCount', 'deliveryAttemptLimit', 'retryScheduledAt']) {
    assert.match(staffStore, new RegExp(`${field}: (?:Number\\()?o\\.${field}`));
  }
});

test('customer mappings preserve canonical schedule and remove history notes', () => {
  assert.match(orderStore, /retryScheduledAt: o\.retryScheduledAt/);
  assert.match(orderStore, /retryScheduledAt: data\.retryScheduledAt/);
  assert.doesNotMatch(orderStore, /note: entry\.note/);
  assert.match(userDetail, /retryScheduledAt/);
  assert.match(userDetail, /statusHistory: \(data\.statusHistory/);
  assert.doesNotMatch(userDetail, /note: entry\.note/);
  assert.match(guestTrack, /retryScheduledAt/);
  assert.match(adminDetail, /retryScheduledAt/);
});

test('staff queue and detail expose bounded recovery controls', () => {
  assert.match(staffOrders, /DELIVERY_FAILED/);
  for (const value of ['deliveryAttemptCount', 'deliveryAttemptLimit', 'retryScheduledAt']) assert.match(staffDetail, new RegExp(value));
  for (const value of ['deliveryFailureCode', 'failureNote', 'deliveryFailedAt', 'deliveryAttemptCount', 'deliveryAttemptLimit', 'retryScheduledAt']) assert.match(staffOrders, new RegExp(value));
  for (const action of ['retryDelivery', 'startScheduledRetry', 'returnToStore']) assert.match(staffDetail, new RegExp(action));
  assert.match(staffDetail, /CUSTOMER_RESCHEDULED/);
  assert.match(staffDetail, /Hàng hóa sẽ được ghi nhận hao hụt/);
  assert.match(staffDetail, /Mã giảm giá sẽ được giải phóng/);
  assert.match(staffDetail, /Thanh toán online có thể cần hoàn tiền/);
  assert.match(staffDetail, /error\.status === 409/);
});

test('admin override requires note and operational fields remain visible', () => {
  assert.match(adminDetail, /overrideDeliveryAttempt/);
  assert.match(adminDetail, /overrideNote\.value\.trim\(\)/);
  for (const value of ['deliveryFailureCode', 'failureNote', 'deliveryAttemptCount', 'deliveryAttemptLimit']) assert.match(adminDetail, new RegExp(value));
});

test('returned orders remain visible and terminal customer polling stops', () => {
  assert.match(userOrders, /statuses: \['DELIVERED', 'RETURNED_TO_STORE'\]/);
  assert.match(userDetail, /\['DELIVERED', 'CANCELLED', 'RETURNED_TO_STORE'\]/);
  assert.match(guestTrack, /\['DELIVERED', 'CANCELLED', 'RETURNED_TO_STORE'\]/);
});

test('exception and terminal statuses classify revenue and history correctly', () => {
  assert.equal(isCompletedRevenue({ status: 'DELIVERED', paymentStatus: 'PAID' }), true);
  assert.equal(isCompletedRevenue({ status: 'DELIVERY_FAILED', paymentStatus: 'PAID' }), false);
  assert.equal(isCompletedRevenue({ status: 'RETURNED_TO_STORE', paymentStatus: 'PAID' }), false);
  assert.match(adminOrders, /RETURNED_TO_STORE/);
  assert.match(adminOrders, /DELIVERY_FAILED/);
  assert.match(timeline, /aria-live="polite"/);
  assert.match(timeline, /DELIVERY_FAILED/);
  assert.match(timeline, /RETURNED_TO_STORE/);
  assert.match(timeline, /CANCELLED/);
  assert.match(timeline, /history\.length - 1/);
  assert.doesNotMatch(timeline, /statusOrder\.indexOf/);
});
