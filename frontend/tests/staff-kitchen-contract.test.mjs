import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const staffStore = readFileSync(new URL('../src/stores/staff.js', import.meta.url), 'utf8');
const ordersPage = readFileSync(new URL('../src/views/staff/OrdersPage.vue', import.meta.url), 'utf8');
const orderDetailPage = readFileSync(new URL('../src/views/staff/OrderDetailPage.vue', import.meta.url), 'utf8');

test('staff store rethrows list and note API errors after setting canonical error', () => {
  for (const method of ['fetchOrders', 'fetchConfirmedOrders', 'fetchPreparingOrders', 'fetchReadyOrders', 'fetchHistory']) {
    const start = staffStore.indexOf(`async function ${method}`);
    const end = staffStore.indexOf('\n  async function ', start + 1);
    const body = staffStore.slice(start, end < 0 ? staffStore.length : end);
    assert.match(body, /error\.value = e\.message \|\| 'Không thể tải danh sách đơn hàng';/);
    assert.match(body, /throw e;/);
    assert.doesNotMatch(body, /catch \(e\) \{[^}]*return allOrders\.value;/s);
    assert.doesNotMatch(body, /catch[^}]*return \[\]/s);
  }
  const noteStart = staffStore.indexOf('async function saveInternalNote');
  const noteBody = staffStore.slice(noteStart, staffStore.indexOf('\n  return {', noteStart));
  assert.match(noteBody, /error\.value = e\.message \|\| 'Không thể lưu ghi chú nội bộ';/);
  assert.match(noteBody, /throw e;/);
});

test('staff store maps canonical kitchen list and detail fields', () => {
  assert.match(staffStore, /userId: o\.userId \?\? null/);
  assert.doesNotMatch(staffStore, /userId:\s*o\.customerName/);
  assert.match(staffStore, /customerPhone: o\.customerPhone \?\? ''/);
  assert.match(staffStore, /itemCount: kitchenItemCount\(o\)/);
  assert.match(staffStore, /modifiers: Array\.isArray\(item\.modifiers\) \? item\.modifiers : \[\]/);
  assert.match(staffStore, /serviceFee:.*o\.serviceFee/);
  assert.match(staffStore, /discount: staffOrderDiscount\(o\)/);
  assert.match(staffStore, /allowedActions: Array\.isArray\(o\.allowedActions\) \? o\.allowedActions : \[\]/);
  assert.match(staffStore, /internalNotes: Array\.isArray\(o\.internalNotes\) \? o\.internalNotes : \[\]/);
});

test('staff kitchen synchronizes tab query and searches canonical customer fields', () => {
  assert.match(ordersPage, /async function switchTab\(tab\) \{\s*activeTab\.value = normalizedTab\(tab\);\s*await router\.replace\(\{ query: \{ \.\.\.route\.query, tab: activeTab\.value \} \}\)/);
  assert.match(ordersPage, /watch\(\(\) => route\.query\.tab/);
  assert.match(ordersPage, /order\.orderCode/);
  assert.match(ordersPage, /order\.customerName/);
  assert.match(ordersPage, /order\.customerPhone/);
});

test('staff detail renders canonical data and guards refresh mutations', () => {
  assert.match(orderDetailPage, /:href="`tel:\$\{order\.customerPhone\}`"/);
  assert.match(orderDetailPage, /item\.modifiers/);
  assert.match(orderDetailPage, /order\.serviceFee/);
  assert.match(orderDetailPage, /order\.discount/);
  assert.match(orderDetailPage, /order\.refundStatus/);
  assert.match(orderDetailPage, /order\.internalNotes/);
  assert.match(orderDetailPage, /Array\.isArray\(order\.value\?\.allowedActions\)/);
  assert.match(orderDetailPage, /formatPrice\(item\.totalPrice\)/);
  assert.match(orderDetailPage, /if \(loading\.value \|\| saving\.value \|\| stopped\) return/);
  assert.match(orderDetailPage, /await load\(\{ silent: true \}\)/);
  assert.match(orderDetailPage, /shipperLoading/);
  assert.match(orderDetailPage, /shipperError/);
  assert.match(orderDetailPage, /Không có shipper khả dụng/);
});

test('staff detail maps and renders complete discounts refunds and note form', () => {
  assert.match(staffStore, /refundAmount: Number\(o\.refundAmount \?\? 0\)/);
  assert.match(staffStore, /refundedAt: o\.refundedAt \|\| null/);
  assert.match(orderDetailPage, /formatPrice\(order\.refundAmount\)/);
  assert.match(orderDetailPage, /formatDateTime\(order\.refundedAt\)/);
  assert.match(orderDetailPage, /v-model="internalNote"/);
  assert.match(orderDetailPage, /maxlength="1000"/);
  assert.match(orderDetailPage, /await staffStore\.saveInternalNote\(order\.value\.id, note\)/);
  assert.match(orderDetailPage, /await load\(\{ silent: true \}\)/);
});

test('staff detail rejects stale async work after unmount', () => {
  assert.match(orderDetailPage, /onBeforeUnmount/);
  assert.match(orderDetailPage, /let stopped = false/);
  assert.match(orderDetailPage, /let generation = 0/);
  assert.match(orderDetailPage, /function acceptsRequest\(requestGeneration\)/);
  assert.match(orderDetailPage, /!stopped && requestGeneration === generation/);
  assert.match(orderDetailPage, /stopped = true/);
  assert.match(orderDetailPage, /generation \+= 1/);
  assert.match(orderDetailPage, /if \(!acceptsRequest\(requestGeneration\)\) return/);
  assert.match(orderDetailPage, /if \(acceptsRequest\(requestGeneration\)\) toast\.error/);
});

test('staff detail preserves API errors and both dialogs manage focus keyboard and scroll', () => {
  assert.match(staffStore, /catch \(e\) \{\s*throw e;\s*\} finally/);
  assert.match(orderDetailPage, /role="dialog"/);
  assert.match(orderDetailPage, /aria-modal="true"/);
  assert.match(orderDetailPage, /assignmentDialog/);
  assert.match(orderDetailPage, /assignmentSelect/);
  assert.match(orderDetailPage, /previousFocus/);
  assert.match(orderDetailPage, /assignmentSelect\.value\?\.focus\(\)/);
  assert.match(orderDetailPage, /event\.key === 'Escape'/);
  assert.match(orderDetailPage, /event\.key !== 'Tab'/);
  assert.match(orderDetailPage, /focusable/);
  assert.match(orderDetailPage, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(orderDetailPage, /previousBodyOverflow/);
  assert.match(orderDetailPage, /previousFocus\?\.focus\(\)/);
  assert.match(orderDetailPage, /cancelDialog/);
  assert.match(orderDetailPage, /cancelReasonInput\.value\?\.focus\(\)/);
  assert.match(orderDetailPage, /handleCancelKeydown/);
  assert.match(orderDetailPage, /closeCancelModal/);
});

test('staff kitchen renders queue detail and safe polling states', () => {
  assert.match(ordersPage, /order\.itemCount/);
  assert.match(ordersPage, /modifier\.name/);
  assert.match(ordersPage, /waitingDuration/);
  assert.match(ordersPage, /setInterval\([^]*30000\)/);
  assert.match(ordersPage, /inFlight/);
  assert.match(ordersPage, /queuedRefresh/);
  assert.match(ordersPage, /loadedTab/);
  assert.match(ordersPage, /requestGeneration/);
  assert.match(ordersPage, /fetchKitchenOrders/);
  assert.match(ordersPage, /role="tabpanel"/);
  assert.match(ordersPage, /aria-controls/);
  assert.match(ordersPage, /lastUpdated/);
  assert.match(ordersPage, /const staleErrors = ref\(Object\.fromEntries\(tabs\.map\(\(tab\) => \[tab\.key, ''\]\)\)\)/);
  assert.match(ordersPage, /const staleError = computed\(\(\) => staleErrors\.value\[activeTab\.value\]\)/);
  assert.match(ordersPage, /if \(!acceptsKitchenRequest[\s\S]*return;[\s\S]*staleErrors\.value\[requestTab\] = '';[\s\S]*rows\.value = nextRows/);
  assert.match(ordersPage, /staleErrors\.value\[requestTab\] = error\.message \|\| 'Không thể tải danh sách đơn hàng'/);
  assert.doesNotMatch(ordersPage, /const staleError = ref/);
  assert.doesNotMatch(ordersPage, /staleError\.value\s*=/);
  assert.match(ordersPage, /queuedRefresh = \{ silent \};/);
  assert.match(ordersPage, /const queuedOptions = queuedRefresh;[\s\S]*queuedRefresh = null;[\s\S]*await refresh\(queuedOptions\)/);
  assert.match(ordersPage, /clearInterval\(refreshTimer\)/);
  assert.match(ordersPage, /Thử lại/);
  assert.match(ordersPage, /Không có đơn trong hàng đợi/);
});
