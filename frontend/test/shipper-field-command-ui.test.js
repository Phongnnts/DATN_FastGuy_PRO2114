import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = path => fs.readFileSync(new URL(path, import.meta.url), 'utf8');

test('shipper shell exposes five mobile destinations and full desktop sidebar', () => {
  const layout = read('../src/layouts/ShipperLayout.vue');
  assert.match(layout, /mobileNavItems/);
  assert.match(layout, /shipper-bottom-nav/);
  assert.match(layout, /shipper-sidebar/);
  assert.match(layout, /aria-current/);
  assert.match(layout, /min-height:\s*44px/);
  assert.match(layout, /@media\s*\(min-width:\s*900px\)/);
});

test('shipper dashboard and order list expose field actions without changing data flow', () => {
  const dashboard = read('../src/views/shipper/DashboardPage.vue');
  const orders = read('../src/views/shipper/MyOrdersPage.vue');
  const sheet = read('../src/components/shipper/OrderActionSheet.vue');
  assert.match(dashboard, /operations-grid/);
  assert.match(dashboard, /priority-order/);
  assert.match(dashboard, /to="\/shipper\/shifts"/);
  assert.match(dashboard, /store\.fetchReadyOrders/);
  assert.match(dashboard, /store\.claimOrder/);
  assert.match(dashboard, /ready-order-card/);
  assert.match(dashboard, /Nhận đơn/);
  assert.match(orders, /order-toolbar/);
  assert.match(orders, /order-card-actions/);
  assert.match(sheet, /safe-area-inset-bottom/);
  assert.match(sheet, /props\.order\.status/);
});

test('shipper order detail keeps mutation guards inside a sticky command surface', () => {
  const detail = read('../src/views/shipper/OrderDetailPage.vue');
  assert.match(detail, /order-command-header/);
  assert.match(detail, /order-detail-grid/);
  assert.match(detail, /sticky-command-bar/);
  assert.match(detail, /safe-area-inset-bottom/);
  assert.match(detail, /order\.value\.status/);
  assert.match(detail, /error\?\.status === 409/);
  assert.match(detail, /role="dialog"/);
  assert.match(detail, /Ghi chú \(không bắt buộc\)/);
  assert.doesNotMatch(detail, /id="failure-note"[^>]*required/);
});

test('shipper shift and COD pages use command hierarchy and preserve irreversible warnings', () => {
  const shift = read('../src/components/common/ShiftStatus.vue');
  const shifts = read('../src/views/shipper/ShipperShiftsPage.vue');
  const cash = read('../src/views/shipper/CashPage.vue');
  assert.match(shift, /shift-command/);
  assert.match(shift, /if \(props\.role === 'SHIPPER' && currentShift\.value\?\.checkInAt\)/);
  assert.match(shift, /codSettlementApi\.getCurrent/);
  assert.match(shift, /SETTLED.*SHORT.*OVER/s);
  assert.match(shift, /Gửi đối soát COD trước khi kết ca/);
  assert.match(shift, /Đang chờ Admin xác nhận đối soát/);
  assert.match(shifts, /shift-sections/);
  assert.match(cash, /cod-command-summary/);
  assert.match(cash, /Số tiền không thể sửa sau khi gửi/);
  assert.match(cash, /role="alert"/);
});
