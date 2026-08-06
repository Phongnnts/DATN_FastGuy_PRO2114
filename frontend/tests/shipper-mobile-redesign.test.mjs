import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const layout = read('../src/layouts/ShipperLayout.vue');
const dashboard = read('../src/views/shipper/DashboardPage.vue');
const orders = read('../src/views/shipper/MyOrdersPage.vue');
const detail = read('../src/views/shipper/OrderDetailPage.vue');
const sheet = read('../src/components/shipper/OrderActionSheet.vue');
const shifts = read('../src/views/shipper/ShipperShiftsPage.vue');
const cash = read('../src/views/shipper/CashPage.vue');

test('shipper shell supports narrow screens safe areas and accessible navigation', () => {
  assert.match(layout, /class="skip-link"/);
  assert.match(layout, /id="shipper-main"/);
  assert.match(layout, /aria-label="Điều hướng Shipper"/);
  assert.match(layout, /env\(safe-area-inset-bottom\)/);
  assert.match(layout, /min-height:44px/);
  assert.match(layout, /@media\(max-width:360px\)/);
});

test('shipper dashboard is task first on mobile', () => {
  const nextOrder = dashboard.indexOf('Việc tiếp theo');
  const metrics = dashboard.indexOf('mini-stats');
  assert.ok(nextOrder > -1 && metrics > -1 && nextOrder < metrics);
  assert.match(dashboard, /class="next-order/);
});

test('shipper order list and detail optimize one-handed actions', () => {
  assert.match(orders, /function handleTabKeydown/);
  assert.match(orders, /ArrowRight/);
  assert.match(orders, /@media\(max-width:480px\)/);
  assert.match(detail, /class="sticky-actions"/);
  assert.match(detail, /<details/);
  assert.match(detail, /env\(safe-area-inset-bottom\)/);
});

test('shipper action sheet confirms mutations and locks background scroll', () => {
  assert.match(sheet, /ConfirmDialog/);
  assert.match(sheet, /document\.body\.style\.overflow = 'hidden'/);
  assert.match(sheet, /previousBodyOverflow/);
  assert.match(sheet, /env\(safe-area-inset-bottom\)/);
});

test('shipper shifts and COD render mobile cards and actionable orders', () => {
  assert.match(shifts, /class="shift-card/);
  assert.match(shifts, /@media\(min-width:600px\)/);
  assert.match(cash, /router-link[^>]*shipper\/orders/);
  assert.match(cash, /@media\(max-width:360px\)/);
});
