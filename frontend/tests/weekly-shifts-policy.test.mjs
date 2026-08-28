import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const adminApi = readFileSync(new URL('../src/api/admin.js', import.meta.url), 'utf8');
const shiftApi = readFileSync(new URL('../src/api/shift.js', import.meta.url), 'utf8');
const adminPage = readFileSync(new URL('../src/views/admin/ShiftsPage.vue', import.meta.url), 'utf8');
const staffPage = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');

test('weekly APIs match OpenAPI paths, query, and replacement body', () => {
  assert.match(adminApi, /getShiftWeek\(weekStart\)[\s\S]*client\.get\('\/admin\/shifts\/week', \{ params: \{ weekStart \} \}\)/);
  assert.match(adminApi, /replaceShiftWeek\(data\)[\s\S]*client\.put\('\/admin\/shifts\/week', data\)/);
  assert.match(adminApi, /getShiftMonitoring\(\)[\s\S]*client\.get\('\/admin\/shifts\/monitoring'\)/);
  assert.match(shiftApi, /getWeek\(weekStart\)[\s\S]*client\.get\('\/shifts\/week', \{ params: \{ weekStart \} \}\)/);
});

test('admin schedule renders seven days by three fixed Staff slots and exact payload', () => {
  assert.match(adminPage, /SHIFT_CODES\s*=\s*\['MORNING', 'AFTERNOON', 'EVENING'\]/);
  assert.match(adminPage, /v-for="day in days"/);
  assert.match(adminPage, /v-for="code in SHIFT_CODES"/);
  assert.match(adminPage, /role: 'STAFF'/);
  assert.match(adminPage, /\{ weekStart: requestedWeek, slots/);
  assert.match(adminPage, /getShiftWeek\(requestedWeek\)/);
  assert.match(adminPage, /baseline/);
  assert.match(adminPage, /roleName === 'STAFF'/);
  assert.match(adminPage, /:disabled="isCurrentWeek"/);
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

test('staff weekly table preserves controls and exposes source plus automatic countdown', () => {
  assert.match(staffPage, /shiftApi\.getWeek\(weekStart\.value\)/);
  assert.match(staffPage, /v-for="day in days"/);
  assert.match(staffPage, /isToday/);
  assert.match(staffPage, /isCurrent/);
  assert.match(staffPage, /MANUAL/);
  assert.match(staffPage, /AUTO/);
  assert.match(staffPage, /countdown/);
  assert.match(staffPage, /staffApi\.getOwnershipCount/);
  assert.match(staffPage, /shiftApi\.checkIn/);
  assert.match(staffPage, /shiftApi\.checkOut/);
  assert.match(staffPage, /aria-live="polite"/);
});
