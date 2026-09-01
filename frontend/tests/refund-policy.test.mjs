import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { validateRefund } from '../src/utils/refundPolicy.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const read = (p) => readFileSync(join(root, p), 'utf8');

test('refunded requires fixed full amount and manual reference', () => {
  assert.equal(validateRefund({ status: 'REFUNDED', amount: 100000, finalAmount: 100000, note: '', reference: 'BANK-123', proof: {} }), '');
  assert.notEqual(validateRefund({ status: 'REFUNDED', amount: 100000, finalAmount: 100000, note: '', reference: 'BANK-123', proof: null }), '');
  assert.notEqual(validateRefund({ status: 'REFUNDED', amount: 50000, finalAmount: 100000, note: '', reference: 'BANK-123' }), '');
  assert.notEqual(validateRefund({ status: 'REFUNDED', amount: 100001, finalAmount: 100000, note: '', reference: 'BANK-123' }), '');
  assert.notEqual(validateRefund({ status: 'REFUNDED', amount: 100000, finalAmount: 100000, note: '', reference: '   ' }), '');
  assert.notEqual(validateRefund({ status: 'REFUNDED', amount: 100000, finalAmount: null, note: '', reference: 'BANK-123' }), '');
});

test('rejected requires non-blank note', () => {
  assert.equal(validateRefund({ status: 'REJECTED', amount: null, finalAmount: 100000, note: 'Thiếu chứng từ' }), '');
  assert.notEqual(validateRefund({ status: 'REJECTED', amount: null, finalAmount: 100000, note: '' }), '');
  assert.notEqual(validateRefund({ status: 'REJECTED', amount: null, finalAmount: 100000, note: '   ' }), '');
});

test('unknown status is rejected', () => {
  assert.notEqual(validateRefund({ status: 'APPROVED', amount: 100000, finalAmount: 100000, note: '' }), '');
  assert.notEqual(validateRefund({ status: '', amount: null, finalAmount: 100000, note: '' }), '');
  assert.notEqual(validateRefund({ status: 'PENDING', amount: null, finalAmount: 100000, note: '' }), '');
});

test('RefundsPage exists and is routed with title', () => {
  assert.ok(read('src/views/admin/RefundsPage.vue'));
  const router = read('src/router/index.js');
  assert.match(router, /name: 'AdminRefunds'/);
  assert.match(router, /RefundsPage\.vue/);
  assert.match(router, /AdminRefunds: 'Hoàn tiền'/);
});

test('sidebar exposes Hoàn tiền link to /admin/refunds', () => {
  const layout = read('src/layouts/AdminLayout.vue');
  assert.match(layout, /Hoàn tiền/);
  assert.match(layout, /\/admin\/refunds/);
});

test('getRefunds forwards query params', () => {
  const api = read('src/api/admin.js');
  assert.match(api, /getRefunds\s*\(\s*params\s*\)/);
  assert.match(api, /client\.get\('\/admin\/refunds',\s*\{\s*params\s*\}\)/);
});

test('OrdersPage no longer processes refunds inline and links to /admin/refunds', () => {
  const page = read('src/views/admin/OrdersPage.vue');
  assert.ok(!/openRefund|saveRefund|refundDialog|refundForm|refunding|updateRefund/.test(page));
  assert.match(page, /\/admin\/refunds/);
});

test('OrderDetailPage renders payment attempt block', () => {
  const page = read('src/views/admin/OrderDetailPage.vue');
  for (const field of ['provider', 'providerReference', 'attemptStatus', 'attemptAmount']) {
    assert.match(page, new RegExp(`order\\.payment\\.${field}`));
  }
});

test('RefundsPage records external full refund with required private proof', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /Ghi nhận hoàn tiền bên ngoài/);
  assert.match(page, /refundReference/);
  assert.match(page, /new FormData\(\)/);
  assert.match(page, /data\.append\('proof'/);
  assert.match(page, /image\/jpeg,image\/png,image\/webp/);
  assert.match(page, /:value="Number\(refundOrder\.finalAmount\)"/);
  assert.match(page, /readonly/);
});

test('RefundsPage ships accessible dialog, mutation lock, no native confirm', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /role="dialog"/);
  assert.match(page, /aria-modal="true"/);
  assert.match(page, /onEscape: closeActiveDialog/);
  assert.match(page, /validateRefund/);
  assert.match(page, /refunding/);
  assert.ok(!/\bconfirm\(/.test(page));
});

test('RefundsPage reads and validates status from route query on mount', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /REFUND_STATUS_KEYS\s*=\s*\[\s*'PENDING'\s*,\s*'REFUNDED'\s*,\s*'REJECTED'\s*\]/);
  assert.match(page, /statusFromQuery/);
  assert.match(page, /REFUND_STATUS_KEYS\.includes\(raw\)/);
  assert.match(page, /route\.query\.status/);
  assert.match(page, /onMounted\(\(\) => \{[\s\S]*?loadPreset\(\);[\s\S]*?\}\);/);
});

test('RefundsPage syncs active status tab to route query', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /setStatus/);
  assert.match(page, /router\.push/);
  assert.match(page, /status: key \|\| undefined/);
  assert.match(page, /@click="setStatus\(item\.key\)"/);
});

test('RefundsPage filters by processing dates and drops dead payment markup', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /Từ ngày xử lý/);
  assert.match(page, /Đến ngày xử lý/);
  assert.match(page, /Ngày tạo/);
  assert.match(page, /formatDate\(row\.createdAt\)/);
  assert.match(page, /Bộ lọc ngày dùng thời điểm xử lý/);
  assert.ok(!/cancelledAt/.test(page));
  assert.ok(!/pay-attempt|row\.provider|refundOrder\.provider|attemptAmount/.test(page));
});

test('RefundsPage guards re-entry and restores focus on dialog close', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /if \(refunding\.value\) return/);
  assert.match(page, /refundTrigger/);
  assert.match(page, /document\.activeElement/);
  assert.match(page, /dismissRefund/);
  assert.match(page, /modalLifecycle\.close\(\)/);
});

test('RefundsPage restores all URL-backed filters without navigation loops', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /function hydrateQuery\(\)/);
  assert.match(page, /route\.query\.search/);
  assert.match(page, /route\.query\.fromDate/);
  assert.match(page, /watch\(\(\) => route\.query, \(\) => \{ hydrateQuery\(\); load\(\); \}, \{ deep: true \}\)/);
});

test('RefundsPage guards stale load responses with generation', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /let loadGeneration = 0/);
  assert.match(page, /\+\+loadGeneration/);
  assert.match(page, /generation !== loadGeneration/);
  assert.match(page, /generation === loadGeneration\) loading\.value = false/);
});

test('refund queue wires executable presentation and pending-only state policy', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /buildRefundPresentation/);
  assert.match(page, /presentation\(row\)\.paymentLabel/);
  assert.match(page, /refundAuditDetail\(refundDetailOrder\)/);
  assert.match(page, /canMutateRefund\(row\)/);
  assert.match(page, /submitPendingRefund/);
});

test('terminal rows wire explicit read-only detail modal without mutation controls', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /Xem chi tiết/);
  assert.match(page, /openRefundDetail/);
  assert.match(page, /refund-detail-title/);
  assert.match(page, /refundAuditDetail/);
  assert.match(page, /canViewRefundDetail/);
});

test('refund dialog wires production modal lifecycle and live feedback', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /createRefundModalLifecycle/);
  assert.match(page, /modalLifecycle\.attach\(\)/);
  assert.match(page, /modalLifecycle\.detach\(\)/);
  assert.match(page, /@keydown="handleRefundKeydown"/);
  assert.match(page, /aria-live="polite"/);
  assert.match(page, /role="alert"/);
});


