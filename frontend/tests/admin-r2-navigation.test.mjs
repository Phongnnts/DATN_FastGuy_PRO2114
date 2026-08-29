import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => { try { return readFileSync(new URL(path, import.meta.url), 'utf8'); } catch { return ''; } };
const layout = read('../src/layouts/AdminLayout.vue');
const router = read('../src/router/index.js');
const shifts = read('../src/views/admin/ShiftsPage.vue');
const attendance = read('../src/views/admin/AttendancePage.vue');

test('R2 exposes dedicated attendance and stock count destinations', () => {
  assert.match(layout, /label: 'Chấm công & tiền công', path: '\/admin\/attendance'/);
  assert.match(layout, /label: 'Kiểm kê kho', path: '\/admin\/inventory\/stock-counts'/);
  assert.match(router, /path: 'attendance'[\s\S]*name: 'AdminAttendance'[\s\S]*AttendancePage\.vue/);
  assert.match(router, /AdminAttendance: 'Chấm công & tiền công'/);
  assert.match(router, /AdminStockCounts: 'Kiểm kê kho'/);
});

test('R2 shift management keeps schedule and monitoring only', () => {
  assert.match(shifts, /\['schedule', 'monitoring'\]/);
  assert.match(shifts, /End: 1/);
  assert.doesNotMatch(shifts, /attendance|Duyệt công|Phút duyệt|getShiftAttendance|approveShiftAttendance/);
});

test('R2 attendance page owns existing approval workflow without payroll placeholders', () => {
  for (const token of ['getShiftAttendance', 'approveShiftAttendance', 'attendanceGeneration', 'expectedUpdatedAt', 'approvedMinutes', 'approvedOvertimeMinutes', 'attendanceNote']) assert.match(attendance, new RegExp(token));
  for (const label of ['Chấm công & tiền công', 'Chờ duyệt', 'Đã duyệt', 'Phút duyệt', 'OT duyệt']) assert.match(attendance, new RegExp(label));
  assert.match(attendance, /role="table"|<table/);
  assert.doesNotMatch(attendance, /Tab tiền công|Tiền công dự kiến|hourlyRate|payRate/);
});
