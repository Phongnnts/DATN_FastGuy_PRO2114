import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const api = read('../src/api/shipper.js');
const store = read('../src/stores/shipper.js');
const router = read('../src/router/index.js');
const layout = read('../src/layouts/ShipperLayout.vue');
const dashboard = read('../src/views/shipper/DashboardPage.vue');
const orders = read('../src/views/shipper/MyOrdersPage.vue');
const sheet = read('../src/components/shipper/OrderActionSheet.vue');
const cash = read('../src/views/shipper/CashPage.vue');
const detail = read('../src/views/shipper/OrderDetailPage.vue');
const shifts = read('../src/views/shipper/ShipperShiftsPage.vue');

test('api getHistory passes params to the history endpoint', () => {
  assert.match(api, /getHistory\(params\)/);
  assert.match(api, /client\.get\('\/shipper\/orders\/history', \{ params \}\)/);
});

test('store history fetch is paginated and reads items/total with generation guard', () => {
  assert.match(store, /async function fetchHistory\(\{ page = 1, size = 20, fromDate, toDate \} = \{\}\)/);
  assert.match(store, /data\?\.items \|\| \[\]/);
  assert.match(store, /data\?\.total/);
  assert.match(store, /historyPage\.value = page/);
  assert.match(store, /historySize\.value = size/);
  assert.match(store, /historyTotal\.value/);
  assert.match(store, /historyLoading\.value/);
  assert.match(store, /historyError\.value/);
  assert.match(store, /acceptsShipperRequest/);
});

test('cash route registered with COD page title', () => {
  assert.match(router, /path: 'cash',[\s\S]*name: 'ShipperCash'[\s\S]*CashPage\.vue/);
  assert.match(router, /ShipperCash: 'Đối soát COD'/);
});

test('shipper layout has 5 fixed tabs and shift-aware header chip', () => {
  assert.match(layout, /path: '\/shipper', name: 'Trang chủ', icon: 'bi-house-door'/);
  assert.match(layout, /path: '\/shipper\/orders', name: 'Đơn giao', icon: 'bi-bicycle'/);
  assert.match(layout, /path: '\/shipper\/history', name: 'Lịch sử', icon: 'bi-clock-history'/);
  assert.match(layout, /path: '\/shipper\/shifts', name: 'Ca làm', icon: 'bi-calendar-week'/);
  assert.match(layout, /path: '\/shipper\/cash', name: 'COD', icon: 'bi-cash-coin'/);
  assert.match(layout, /chipLabel/);
  assert.match(layout, /shiftState\.value === 'CHECKED_IN' \? 'Tuyến đang giao'/);
  assert.match(layout, /shiftState\.value === 'CHECKED_OUT' \? 'Ca đã kết thúc'/);
  assert.match(layout, /'Chưa check-in'/);
  assert.match(layout, /route\.path\.startsWith\('\/shipper\/orders\/'\)/);
  assert.match(layout, /\$route\.name === 'ShipperShifts'/);
  assert.match(layout, /\$route\.name === 'ShipperOrderHistory'/);
  assert.match(layout, /\$route\.name === 'ShipperCash'/);
});

test('dashboard shows 5 stats and polls every 30s with inFlight guard', () => {
  assert.match(dashboard, /todayDelivered/);
  assert.match(dashboard, /totalDelivered/);
  assert.match(dashboard, /todayCodCollected/);
  assert.match(dashboard, /assignedCount/);
  assert.match(dashboard, /pickedUpCount/);
  assert.match(dashboard, /Chờ lấy/);
  assert.match(dashboard, /Đang giao/);
  assert.match(dashboard, /Đã giao hôm nay/);
  assert.match(dashboard, /Tổng đã giao/);
  assert.match(dashboard, /COD hôm nay/);
  assert.match(dashboard, /setInterval\(\(\) => retry\(true\), 30_000\)/);
  assert.match(dashboard, /Promise\.allSettled\(\[store\.fetchDashboard\(silent\), store\.fetchActiveOrders\(silent\)\]\)/);
  assert.match(dashboard, /async function retry\(silent = false\)/);
  assert.match(dashboard, /clearInterval\(timer\)/);
  assert.match(dashboard, /inFlight/);
});

test('history list exposes pagination controls driven by store pagination state', () => {
  assert.match(orders, /totalPages/);
  assert.match(orders, /store\.historyPage/);
  assert.match(orders, /store\.historySize/);
  assert.match(orders, /store\.historyTotal/);
  assert.match(orders, /goPrev/);
  assert.match(orders, /goNext/);
  assert.match(orders, /store\.historyPage -= 1; inFlight = false; load\(\)/);
  assert.match(orders, /store\.historyPage \+= 1; inFlight = false; load\(\)/);
  assert.match(orders, /Trang {{ store\.historyPage }} \/ {{ totalPages }}/);
  assert.match(orders, /Trước/);
  assert.match(orders, /Sau/);
});

test('order action sheet is a dialog with call, map, primary action and detail link', () => {
  assert.match(sheet, /role="dialog"/);
  assert.match(sheet, /aria-modal="true"/);
  assert.match(sheet, /@click\.self="close"/);
  assert.match(sheet, /event\.key === 'Escape'/);
  assert.match(sheet, /sheet\.value\?\.focus/);
  assert.match(sheet, /previousFocus/);
  assert.match(sheet, /tel:/);
  assert.match(sheet, /google\.com\/maps\/search\/\?api=1&query=/);
  assert.match(sheet, /emit\('updated'\)/);
  assert.match(sheet, /store\.pickUpOrder\(props\.order\.id, props\.order\.status\)/);
  assert.match(sheet, /store\.deliverOrder\(props\.order\.id[^\n]+props\.order\.status\)/);
  assert.match(sheet, /validateExactCod/);
  assert.match(sheet, /Xem chi tiết/);
  assert.match(sheet, /\/shipper\/orders\/\$\{order\.id\}/);
  assert.match(sheet, /OrderStatusBadge/);
});

test('cash page fetches dashboard and 7-day history in parallel and lists COD delivered today', () => {
  assert.match(cash, /todayCodCollected/);
  assert.match(cash, /pendingCodCollected/);
  assert.match(cash, /Tổng thu hôm nay/);
  assert.match(cash, /COD đang giữ/);
  assert.match(cash, /Chưa có luồng nộp tiền — tạm tính bằng tổng thu hôm nay/);
  assert.match(cash, /store\.fetchDashboard\(true\)/);
  assert.match(cash, /shipperApi\.getHistory\(\{ page: 1, size: 100, fromDate: daysAgo\(7\), toDate: today\.value \}\)/);
  assert.match(cash, /toLocalDateKey/);
  assert.match(cash, /daysAgo/);
  assert.match(cash, /paymentMethod === 'COD'/);
  assert.match(cash, /status === 'DELIVERED'/);
  assert.match(cash, /String\(order\.deliveredAt \|\| ''\)\.startsWith\(today\.value\)/);
  assert.match(cash, /codCollectedAmount/);
  assert.match(cash, /orderCode/);
  assert.match(cash, /customerName/);
  assert.match(cash, /Đối soát COD/);
  assert.match(cash, /load\(\)/);
});

test('detail shows back link routed by terminal status', () => {
  assert.match(detail, /← Quay lại/);
  assert.match(detail, /\/shipper\/orders/);
  assert.match(detail, /\/shipper\/history/);
  assert.match(detail, /DELIVERED', 'CANCELLED'/);
});

test('detail payment rows expose service fee and discount while total stays final', () => {
  assert.match(detail, /Phí dịch vụ/);
  assert.match(detail, /Giảm giá/);
  assert.match(detail, /serviceFee/);
  assert.match(detail, /discount/);
});

test('detail actions run through ConfirmDialog and show waiting time refreshed every 30s', () => {
  assert.match(detail, /ConfirmDialog/);
  assert.match(detail, /@confirm="mutate\(confirmAction\)"/);
  assert.match(detail, /:busy="submitting"/);
  assert.match(detail, /Đã chờ/);
  assert.match(detail, /assignedAt/);
  assert.match(detail, /30_000/);
  assert.match(detail, /validateExactCod/);
});

test('detail issue modal loads store config for hotline tel link with focus trap', () => {
  assert.match(detail, /Báo sự cố/);
  assert.match(detail, /storeApi\.getConfig/);
  assert.match(detail, /role="dialog"/);
  assert.match(detail, /aria-modal="true"/);
  assert.match(detail, /tel:\$\{storeInfo\.storePhone\}/);
  assert.match(detail, /@keydown="handleIssueKeydown"/);
  assert.match(detail, /@click\.self="closeIssue"/);
  assert.match(detail, /event\.key === 'Escape'/);
  assert.match(detail, /event\.key !== 'Tab'/);
  assert.match(detail, /previousFocus/);
});

test('shifts page keeps ShiftStatus and adds today, upcoming and history sections with load guard', () => {
  assert.match(shifts, /ShiftStatus role="SHIPPER"/);
  assert.match(shifts, /Hôm nay/);
  assert.match(shifts, /todayShifts/);
  assert.match(shifts, /Ca sắp tới/);
  assert.match(shifts, /Lịch sử ca/);
  assert.match(shifts, /shiftApi\.getMine/);
  assert.match(shifts, /toLocalDateKey/);
  assert.match(shifts, /statusLabel/);
  assert.match(shifts, /staff-shift-changed/);
  assert.match(shifts, /onBeforeUnmount/);
  assert.match(shifts, /stopped/);
  assert.match(shifts, /generation/);
});
