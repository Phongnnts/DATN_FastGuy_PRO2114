import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const reports = read('../src/views/admin/ReportsPage.vue');
const cod = read('../src/views/admin/CodSettlementsPage.vue');
const refunds = read('../src/views/admin/RefundsPage.vue');
const assets = read('../src/views/admin/FixedAssetsPage.vue');

test('reports expose finance health period and truthful tab workspaces', () => {
  assert.match(reports, /class="page-heading finance-health-header"/);
  assert.match(reports, /class="filter-panel report-period-bar"/);
  assert.match(reports, /finance-overview-workspace/);
  assert.match(reports, /menu-analysis-workspace/);
  assert.match(reports, /expense-audit-workspace/);
  assert.match(reports, /router\.push\(\{ query: \{ \.\.\.route\.query, tab:/);
  assert.match(reports, /<canvas[\s\S]*<div v-else class="empty"/);
});

test('COD reconciliation keeps submitted verified and variance evidence adjacent', () => {
  assert.match(cod, /reconciliation-workspace/);
  assert.match(cod, /class="verify-summary reconciliation-evidence"/);
  const summary = cod.slice(cod.indexOf('reconciliation-evidence'), cod.indexOf('</dl>', cod.indexOf('reconciliation-evidence')));
  assert.match(summary, /Dự kiến/);
  assert.match(summary, /Đã nộp/);
  assert.match(summary, /Chênh lệch/);
  assert.match(cod, /conflictMessage/);
});

test('refund review remains evidence first and irreversible actions stay guarded', () => {
  assert.match(refunds, /refund-review-workspace/);
  assert.match(refunds, /refund-evidence-filters/);
  assert.match(refunds, /proof/);
  assert.match(refunds, /canMutateRefund/);
  assert.match(refunds, /role="dialog" aria-modal="true"/);
});

test('fixed assets expose identity value status and lifecycle evidence', () => {
  assert.match(assets, /asset-register-workspace/);
  assert.match(assets, /asset-lifecycle-evidence/);
  assert.match(assets, /Nguyên giá/);
  assert.match(assets, /Giá trị thu hồi/);
  assert.match(assets, /Đang sử dụng/);
  assert.match(assets, /Ngừng sử dụng/);
});
