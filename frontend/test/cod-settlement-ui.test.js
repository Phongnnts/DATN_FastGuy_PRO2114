import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('shipper COD page separates expected submitted and verified money', () => {
  const page = read('../src/views/shipper/CashPage.vue');
  const state = read('../src/views/shipper/cod-settlement-state.js');
  assert.match(page, /Tiền dự kiến theo ca/);
  assert.match(page, /Số tiền thực nộp/);
  assert.match(page, /Kết quả Admin xác nhận/);
  assert.match(page, /item\.shiftId/);
  assert.doesNotMatch(page, /tạm tính bằng tổng thu hôm nay/);
  assert.match(page, /codSettlementApi\.submit/);
  assert.match(state, /error\?\.status === 409/);
  assert.match(page, /v-if="conflictMessage"[^>]*role="alert"/);
});

test('shipper COD form exposes labels errors announcements and 44px controls', () => {
  const page = read('../src/views/shipper/CashPage.vue');
  assert.match(page, /for="submitted-amount"/);
  assert.match(page, /:aria-describedby="formError \? 'submitted-warning submitted-error' : 'submitted-warning'"/);
  assert.match(page, /id="submitted-error"[^>]*role="alert"/);
  assert.match(page, /role="status"/);
  assert.match(page, /min-height:\s*44px/);
  assert.match(page, /@media\(prefers-reduced-motion:reduce\)/);
});

test('admin COD route and navigation use dedicated settlement page', () => {
  const router = read('../src/router/index.js');
  const layout = read('../src/layouts/AdminLayout.vue');
  assert.match(router, /path: 'cod-settlements'/);
  assert.match(router, /name: 'AdminCodSettlements'/);
  assert.match(router, /CodSettlementsPage\.vue/);
  assert.match(router, /AdminCodSettlements: 'Đối soát COD'/);
  assert.match(layout, /\/admin\/cod-settlements/);
});

test('admin COD page has queue states accessible verify dialog and conflict reload', () => {
  const page = read('../src/views/admin/CodSettlementsPage.vue');
  assert.match(page, /codSettlementApi\.getAdmin/);
  assert.match(page, /codSettlementApi\.verify/);
  assert.match(page, /submitVerification\(state/);
  assert.match(read('../src/views/admin/cod-settlement-state.js'), /error\?\.status === 409/);
  assert.match(page, /conflictMessage\.value/);
  assert.match(page, /aria-live="assertive"/);
  assert.match(page, /acceptsAdminCodRequest/);
  assert.match(page, /role="dialog"/);
  assert.match(page, /aria-modal="true"/);
  assert.match(page, /aria-labelledby="verify-title"/);
  assert.match(page, /createModalLifecycle/);
  assert.match(page, /modalLifecycle\.attach\(\)/);
  assert.match(page, /modalLifecycle\.detach\(\)/);
  assert.match(page, /v-if="canVerifySettlement\(row\)"[^>]*@click="openVerify/);
  assert.match(page, /row\.differenceAmount/);
  assert.match(page, /Chênh lệch/);
  assert.match(page, /differenceClass/);
  assert.match(page, /window\.matchMedia\('\(max-width: 760px\)'\)/);
});
