import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const adminApi = readFileSync(new URL('../src/api/admin.js', import.meta.url), 'utf8');
const shiftApi = readFileSync(new URL('../src/api/shift.js', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/views/admin/ShiftsPage.vue', import.meta.url), 'utf8');
const attendancePage = readFileSync(new URL('../src/views/admin/AttendancePage.vue', import.meta.url), 'utf8');
const staffPage = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');

test('weekly APIs match OpenAPI paths, query, and replacement body', () => {
  assert.match(adminApi, /getShiftWeek\(weekStart\)[\s\S]*client\.get\('\/admin\/shifts\/week', \{ params: \{ weekStart \} \}\)/);
  assert.match(adminApi, /replaceShiftWeek\(data\)[\s\S]*client\.put\('\/admin\/shifts\/week', data\)/);
  assert.match(adminApi, /getShiftMonitoring\(\)[\s\S]*client\.get\('\/admin\/shifts\/monitoring'\)/);
  assert.match(shiftApi, /getWeek\(weekStart\)[\s\S]*client\.get\('\/shifts\/week', \{ params: \{ weekStart \} \}\)/);
});

test('admin schedule renders seven days by three fixed Staff slots and exact payload', () => {
  for (const token of ['shift-mode-tabs', 'schedule-toolbar', 'week-calendar-shell', 'assignment-cell', 'month-inspector', 'monitoring-workspace']) assert.match(adminPage, new RegExp(token));
  assert.match(adminPage, /SHIFT_CODES\s*=\s*\['MORNING', 'AFTERNOON', 'EVENING'\]/);
  assert.match(adminPage, /v-for="day in days"/);
  assert.match(adminPage, /v-for="code in SHIFT_CODES"/);
  assert.match(adminPage, /role: 'STAFF'/);
  assert.match(adminPage, /\{ weekStart: requestedWeek, slots/);
  assert.match(adminPage, /getShiftWeek\(requestedWeek\)/);
  assert.match(adminPage, /baseline/);
  assert.match(adminPage, /roleName === 'STAFF'/);
  assert.match(adminPage, /:disabled="isCurrentWeek"/);
  assert.doesNotMatch(adminPage, /transition:\s*all/);
});

test('admin attendance filters by existing staff and includes userId in request identity', () => {
  assert.match(attendancePage, /const attendanceUserId = ref\(''\)/);
  assert.match(attendancePage, /v-model="attendanceUserId"/);
  assert.match(attendancePage, /v-for="user in staff"/);
  assert.match(attendancePage, /userId: attendanceUserId\.value \? Number\(attendanceUserId\.value\) : undefined/);
  assert.match(attendancePage, /attendanceMonth\.value\}\|\$\{attendanceStatus\.value\}\|\$\{attendanceUserId\.value/);
});

test('admin attendance rejects same-filter stale responses including conflict reloads', () => {
  assert.match(attendancePage, /let attendanceGeneration = 0/);
  assert.match(attendancePage, /const generation = \+\+attendanceGeneration/);
  assert.match(attendancePage, /generation !== attendanceGeneration/);
  assert.match(attendancePage, /generation === attendanceGeneration/);
  assert.match(attendancePage, /onUnmounted\(\(\) => \{[^}]*attendanceGeneration\+\+/);
});

test('admin monitoring refreshes every 30 seconds and rejects stale generations', () => {
  assert.match(adminPage, /setInterval\([^]*30000\)/);
  assert.match(adminPage, /monitorGeneration/);
  assert.match(adminPage, /generation !== monitorGeneration/);
  assert.match(adminPage, /clearInterval/);
  assert.match(adminPage, /alertSeverity/);
  assert.match(adminPage, /CRITICAL/);
  assert.match(adminPage, /role="alert"/);
});

test('admin schedule restores week and month calendar state from URL', () => {
  for (const token of ["VIEW_KEYS = ['week', 'month']", 'viewFromQuery', 'selectedDate', 'route.query.view', 'route.query.date']) assert.match(adminPage, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
  assert.match(adminPage, /const view = ref\(viewFromQuery\(route.query.view\)\)/);
  assert.match(adminPage, /class="month-grid"/);
  assert.match(adminPage, /class="[^"]*day-inspector[^"]*"/);
});

test('admin month overview uses at most six contracted week reads and blocks future dates', () => {
  assert.match(adminPage, /monthWeekStarts/);
  assert.match(adminPage, /slice\(0, 6\)/);
  assert.match(adminPage, /adminApi\.getShiftWeek/);
  assert.match(adminPage, /date\.isAfter|key > todayKey|key <= todayKey/);
  assert.doesNotMatch(adminPage, /getShiftMonth|createEvent|event calendar/i);
});

test('staff split calendar selects assigned days and exposes day details', () => {
  assert.match(staffPage, /shiftApi\.getWeek\(weekStart\.value\)/);
  assert.match(staffPage, /calendarDays/);
  assert.match(staffPage, /selectedDateKey/);
  assert.match(staffPage, /selectDay/);
  assert.match(staffPage, /hasShifts/);
  assert.match(staffPage, /class="calendar-day"/);
  assert.match(staffPage, /:aria-pressed="selectedDateKey === day\.key"/);
  assert.match(staffPage, /aria-label="Lịch tháng và chi tiết ca"/);
  assert.match(staffPage, /Lịch trong ngày/);
  assert.match(staffPage, /v-for="shift in selectedShifts"/);
  assert.match(staffPage, /MANUAL/);
  assert.match(staffPage, /AUTO/);
  assert.match(staffPage, /countdown/);
  assert.match(staffPage, /staffApi\.getOwnershipCount/);
  assert.match(staffPage, /shiftApi\.checkIn/);
  assert.match(staffPage, /shiftApi\.checkOut/);
  assert.match(staffPage, /aria-live="polite"/);
  assert.match(staffPage, /@media\s*\(max-width:\s*900px\)/);
});
