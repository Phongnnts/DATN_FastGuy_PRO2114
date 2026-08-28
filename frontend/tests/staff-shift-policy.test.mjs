import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const shiftApi = readFileSync(new URL('../src/api/shift.js', import.meta.url), 'utf8');
const shiftsPage = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');
const shiftStatus = readFileSync(new URL('../src/components/common/ShiftStatus.vue', import.meta.url), 'utf8');
const staffLayout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');
const shipperLayout = readFileSync(new URL('../src/layouts/ShipperLayout.vue', import.meta.url), 'utf8');
const router = readFileSync(new URL('../src/router/index.js', import.meta.url), 'utf8');
const shipperShiftsPage = readFileSync(new URL('../src/views/shipper/ShipperShiftsPage.vue', import.meta.url), 'utf8');

test('imports local-date utility', () => {
  assert.ok(shiftApi.includes('./shift-date'));
});

test('page and component use executable local-time utility and fire change event', () => {
  for (const source of [shiftsPage, shiftStatus]) {
    assert.match(source, /parseShiftEndDatetime|isShiftEndPassed/);
    assert.match(source, /staff-shift-changed/);
  }
  assert.match(shiftStatus, /aria-live/);
  assert.match(shiftsPage, /shiftApi\.getWeek/);
  assert.match(shiftsPage, /v-for="day in calendarDays"/);
  assert.match(shiftsPage, /isCheckedIn/);
  assert.match(shiftsPage, /isCheckedOut/);
  assert.match(staffLayout, /shiftSequence/);
});

test('staff page renders selected-day shifts with independent action state', () => {
  assert.match(shiftsPage, /v-for="day in calendarDays"/);
  assert.match(shiftsPage, /v-for="shift in selectedShifts"/);
  assert.match(shiftsPage, /:key="shift\.shiftId"/);
  assert.match(shiftsPage, /savingShiftId === shift\.shiftId/);
  assert.match(shiftsPage, /aria-live="polite"/);
  assert.doesNotMatch(shiftsPage, /const todayShift = computed/);
  assert.doesNotMatch(shiftsPage, /const saving = ref/);
});

test('shipper shifts route stays unguarded and is unchecked-in fallback', () => {
  assert.match(router, /path: 'shifts',[\s\S]*name: 'ShipperShifts'/);
  assert.match(router, /SHIPPER: '\/shipper\/shifts'/);
  assert.doesNotMatch(router, /name: 'ShipperShifts',[\s\S]{0,200}requiresCheckedInShift/);
  assert.match(shipperLayout, /path: '\/shipper\/shifts'/);
  assert.match(shipperLayout, /\$route\.name === 'ShipperShifts'/);
  assert.match(shipperShiftsPage, /ShiftStatus role="SHIPPER"/);
});
