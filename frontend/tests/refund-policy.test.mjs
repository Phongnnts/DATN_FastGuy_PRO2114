import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';
import { validateRefund } from '../src/utils/refundPolicy.js';

const root = dirname(dirname(fileURLToPath(import.meta.url)));
const read = (p) => readFileSync(join(root, p), 'utf8');

test('refunded requires fixed full amount and manual reference', () => {
  assert.equal(validateRefund({ status: 'REFUNDED', amount: 100000, finalAmount: 100000, note: '', reference: 'BANK-123' }), '');
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

test('RefundsPage confirms manual full refund with required reference and fixed amount', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /Xác nhận hoàn thủ công/);
  assert.match(page, /refundReference/);
  assert.match(page, /refundReference: refundForm\.value\.status === 'REFUNDED' \? refundForm\.value\.refundReference\.trim\(\) : null/);
  assert.match(page, /:value="Number\(refundOrder\.finalAmount\)"/);
  assert.match(page, /readonly/);
});

test('RefundsPage ships accessible dialog, mutation lock, no native confirm', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /role="dialog"/);
  assert.match(page, /aria-modal="true"/);
  assert.match(page, /@keydown\.esc/);
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
  assert.match(page, /onMounted\(loadPreset\)/);
});

test('RefundsPage syncs active status tab to route query', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /setStatus/);
  assert.match(page, /router\.replace/);
  assert.match(page, /status: key \|\| undefined/);
  assert.match(page, /@click="setStatus\(item\.key\)"/);
});

test('RefundsPage dates label by createdAt and drops dead payment markup', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /Từ ngày tạo/);
  assert.match(page, /Đến ngày tạo/);
  assert.match(page, /Ngày tạo/);
  assert.match(page, /formatDate\(row\.createdAt\)/);
  assert.match(page, /KPI tính theo bộ lọc ngày tạo/);
  assert.ok(!/cancelledAt/.test(page));
  assert.ok(!/pay-attempt|row\.provider|refundOrder\.provider|attemptAmount/.test(page));
});

test('RefundsPage guards re-entry and restores focus on dialog close', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /if \(refunding\.value\) return/);
  assert.match(page, /previousFocus/);
  assert.match(page, /document\.activeElement/);
  assert.match(page, /dismissRefund/);
  assert.match(page, /\.focus\(\)/);
});

test('RefundsPage watches route query status without re-replacing', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /watch\(\(\) => route\.query\.status, \(raw\) => \{\n  const key = statusFromQuery\(raw\);\n  if \(activeStatus\.value !== key\) activeStatus\.value = key;\n\}\);/);
  assert.ok(!/router\.replace/.test(page.match(/watch\(\(\) => route\.query\.status[\s\S]*?\}\);/)?.[0] || ''));
});

test('RefundsPage guards stale load responses with generation', () => {
  const page = read('src/views/admin/RefundsPage.vue');
  assert.match(page, /let loadGeneration = 0/);
  assert.match(page, /\+\+loadGeneration/);
  assert.match(page, /generation !== loadGeneration/);
  assert.match(page, /generation === loadGeneration\) loading\.value = false/);
});


