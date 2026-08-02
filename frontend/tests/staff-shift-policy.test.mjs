import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const shiftApi = readFileSync(new URL('../src/api/shift.js', import.meta.url), 'utf8');
const shiftsPage = readFileSync(new URL('../src/views/staff/StaffShiftsPage.vue', import.meta.url), 'utf8');
const shiftStatus = readFileSync(new URL('../src/components/common/ShiftStatus.vue', import.meta.url), 'utf8');
const staffLayout = readFileSync(new URL('../src/layouts/StaffLayout.vue', import.meta.url), 'utf8');

test('imports local-date utility', () => {
  assert.ok(shiftApi.includes('./shift-date'));
});

test('page and component use executable local-time utility and fire change event', () => {
  for (const source of [shiftsPage, shiftStatus]) {
    assert.match(source, /parseShiftEndDatetime|isShiftEndPassed/);
    assert.match(source, /staff-shift-changed/);
  }
  assert.match(shiftStatus, /aria-live/);
  assert.match(staffLayout, /shiftSequence/);
});
