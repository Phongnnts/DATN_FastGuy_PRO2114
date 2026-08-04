import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const reportsPage = readFileSync(new URL('../src/views/admin/ReportsPage.vue', import.meta.url), 'utf8');

test('KPI row exposes gross net and refund cards from backend report fields', () => {
  assert.match(reportsPage, /Doanh thu kỳ/);
  assert.match(reportsPage, /formatPrice\(data\.periodRevenue \|\| 0\)/);
  assert.match(reportsPage, /Doanh thu ròng/);
  assert.match(reportsPage, /netRevenue/);
  assert.match(reportsPage, /data\.value\.netRevenue \?\?/);
  assert.match(reportsPage, /Đã hoàn tiền/);
  assert.match(reportsPage, /formatPrice\(data\.refundTotal \|\| 0\)/);
  assert.match(reportsPage, /data\.refundCount/);
});

test('net revenue uses period-level backend value with gross minus refund fallback', () => {
  assert.match(reportsPage, /Number\(data\.value\.netRevenue \?\? \(Number\(data\.value\.periodRevenue \|\| 0\) - Number\(data\.value\.refundTotal \|\| 0\)\)\)/);
});

test('revenueByDay chart stays a single gross line and does not fabricate per-day net', () => {
  const dayBranch = reportsPage.slice(reportsPage.indexOf('const day = data.value.revenueByDay'), reportsPage.indexOf('const month = data.value.revenueByMonth'));
  assert.match(dayBranch, /label: 'Doanh thu'/);
  assert.ok((dayBranch.match(/label: 'Doanh thu'/g) || []).length === 1);
  assert.doesNotMatch(dayBranch, /refund|netRevenue/);
  assert.doesNotMatch(reportsPage, /revenueByDay[\s\S]{0,120}netRevenue|netRevenue[\s\S]{0,120}revenueByDay/);
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
  assert.match(reportsPage, /'Doanh thu ròng'/);
  assert.match(reportsPage, /'Đơn giao thành công'/);
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
