import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const reportsPage = readFileSync(new URL('../src/views/admin/ReportsPage.vue', import.meta.url), 'utf8');

test('report exposes reconcilable financial breakdown and operational cohort', () => {
  for (const label of ['Tiền món', 'Phí giao hàng', 'Giảm giá', 'Doanh thu gộp', 'Đã hoàn tiền', 'Dòng tiền ròng']) assert.match(reportsPage, new RegExp(label));
  assert.doesNotMatch(reportsPage, /Phí dịch vụ|serviceFeeRevenue/);
  for (const field of ['itemRevenue', 'shippingRevenue', 'discountTotal', 'grossRevenue', 'refundTotal', 'netCashRevenue', 'operationalOrderCount', 'operationalCompletedCount']) assert.match(reportsPage, new RegExp(`data\\.${field}`));
  assert.match(reportsPage, /data\.value\.completionRate/);
  assert.match(reportsPage, /Number\(data\.value\.completionRate \|\| 0\)/);
  assert.match(reportsPage, /Number\(product\.revenue \|\| 0\) \* 100 \/ data\.itemRevenue/);
});

test('report renders advanced operational analytics without fabricated datasets', () => {
  for (const key of ['revenueByHour', 'performanceByWeekday', 'refundTrend', 'exceptionReasons']) assert.match(reportsPage, new RegExp(`data\\.value\\.${key}`));
  for (const title of ['Doanh thu theo giờ', 'Hiệu suất theo thứ', 'Xu hướng hoàn tiền', 'Lý do ngoại lệ']) assert.match(reportsPage, new RegExp(title));
  assert.match(reportsPage, /grid-template-columns:repeat\(12,minmax\(0,1fr\)\)/);
});

test('core commerce charts use legible business-specific encodings', () => {
  assert.match(reportsPage, /const monthly = data\.value\.monthlyFinancialTrend/);
  assert.match(reportsPage, /label: 'Doanh thu gộp'[\s\S]*label: 'Hoàn tiền'[\s\S]*label: 'Dòng tiền ròng'/);
  assert.match(reportsPage, /const top[\s\S]*indexAxis: 'y'/);
  assert.match(reportsPage, /const category[\s\S]*indexAxis: 'y'/);
  assert.match(reportsPage, /const payment[\s\S]*indexAxis: 'y'[\s\S]*max: 100/);
  assert.match(reportsPage, /Tỷ trọng đơn thành công và doanh thu/);
});

test('KPI row exposes gross net cash and refund cards from backend report fields', () => {
  assert.match(reportsPage, /Doanh thu gộp/);
  assert.match(reportsPage, /formatPrice\(data\.grossRevenue \|\| 0\)/);
  assert.match(reportsPage, /Dòng tiền ròng/);
  assert.match(reportsPage, /data\.netCashRevenue/);
  assert.match(reportsPage, /Đã hoàn tiền/);
  assert.match(reportsPage, /formatPrice\(data\.refundTotal \|\| 0\)/);
  assert.match(reportsPage, /data\.refundCount/);
});

test('net cash revenue prefers backend value with compatibility fallback', () => {
  assert.match(reportsPage, /Number\(data\.value\.netCashRevenue \?\? data\.value\.netRevenue \?\?/);
});

test('revenueByDay chart compares gross refund events and net cash', () => {
  const dayBranch = reportsPage.slice(reportsPage.indexOf('const day = data.value.revenueByDay'), reportsPage.indexOf('const month = data.value.revenueByMonth'));
  assert.match(dayBranch, /label: 'Doanh thu gộp'/);
  assert.match(dayBranch, /label: 'Hoàn tiền'/);
  assert.match(dayBranch, /label: 'Dòng tiền ròng'/);
  assert.match(dayBranch, /data\.value\.refundTrend/);
});

test('export downloads CSV with UTF-8 BOM and semicolon delimiter', () => {
  assert.match(reportsPage, /function exportCsv\(\)/);
  assert.match(reportsPage, /'\\uFEFF'/);
  assert.match(reportsPage, /\.join\(';'\)/);
  assert.match(reportsPage, /type: 'text\/csv;charset=utf-8;'/);
});

test('csvCell neutralizes leading formula tokens with single quote', () => {
  assert.match(reportsPage, /function csvCell\(v\)/);
  assert.match(reportsPage, /\/\^\[=\+\\-@\]\/\.test\(s\)/);
  assert.match(reportsPage, /s = `'\$\{s\}`/);
});

test('refund summary row has two cells and moves count into label', () => {
  assert.match(reportsPage, /`Đã hoàn tiền \(\$\{Number\(d\.refundCount \?\? 0\)\.toLocaleString\('vi-VN'\)\} đơn\)`, refund/);
  assert.doesNotMatch(reportsPage, /'Đã hoàn tiền', refund, Number\(d\.refundCount \?\? 0\)/);
});

test('CSV filename is báo-cáo-yyyyMMdd.csv with date key', () => {
  assert.match(reportsPage, /function dateKey\(\)/);
  assert.match(reportsPage, /`báo-cáo-\$\{dateKey\(\)\}\.csv`/);
  assert.match(reportsPage, /padStart\(2, '0'\)/);
});

test('CSV includes summary rows period gross refund net orders then product rows', () => {
  assert.match(reportsPage, /'Kỳ báo cáo'/);
  assert.match(reportsPage, /'Doanh thu gộp'/);
  assert.match(reportsPage, /Đã hoàn tiền/);
  assert.match(reportsPage, /'Dòng tiền ròng'/);
  assert.match(reportsPage, /'Đơn hoàn tất cùng cohort'/);
  assert.match(reportsPage, /'Hạng', 'Sản phẩm', 'Số lượng bán', 'Doanh thu', 'Tỷ trọng'/);
  assert.match(reportsPage, /d\.topProducts \|\| \[\]\)\.map/);
});

test('export is wired to header button and guarded by load state', () => {
  assert.match(reportsPage, /@click="exportCsv"/);
  assert.match(reportsPage, /Xuất CSV/);
  assert.match(reportsPage, /:disabled="loading \|\| !Object\.keys\(data\)\.length"/);
  assert.match(reportsPage, /@click="refresh"/);
});

test('existing load requestId guard and chart rendering are preserved', () => {
  assert.match(reportsPage, /const id = \+\+requestId;/);
  assert.match(reportsPage, /if \(id !== requestId\) return;/);
  assert.match(reportsPage, /buildAllCharts\(\)/);
  assert.match(reportsPage, /if \(id === requestId\) error\.value/);
  assert.match(reportsPage, /if \(id === requestId\) loading\.value = false;/);
});
