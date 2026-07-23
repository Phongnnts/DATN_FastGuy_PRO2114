import assert from 'node:assert/strict';
import { toLocalDateKey, parseShiftEndDatetime, isShiftEndPassed } from '../src/api/shift-date.js';
import test from 'node:test';

test('toLocalDateKey returns local calendar date', () => {
  const d = new Date(2026, 0, 1, 0, 0, 0);
  assert.equal(toLocalDateKey(d), '2026-01-01');
  const late = new Date(2026, 1, 28, 23, 30, 0);
  const key = toLocalDateKey(late);
  assert.ok(key === '2026-02-28' || key === '2026-03-01');
});

test('parseShiftEndDatetime returns correct local time', () => {
  const plain = parseShiftEndDatetime('2026-05-10', '17:00');
  assert.equal(plain.getFullYear(), 2026);
  assert.equal(plain.getMonth(), 4);
  assert.equal(plain.getDate(), 10);
  assert.equal(plain.getHours(), 17);
  assert.equal(plain.getMinutes(), 0);
  assert.equal(plain.getSeconds(), 0);
  assert.equal(plain.getMilliseconds(), 0);

  const frac = parseShiftEndDatetime('2026-05-10', '17:00:00.123456');
  assert.equal(frac.getHours(), 17);
  assert.equal(frac.getMilliseconds(), 123);

  const hhmm = parseShiftEndDatetime('2026-05-10', '03:00', '23:00');
  assert.equal(hhmm.getDate(), 11);
  assert.equal(hhmm.getHours(), 3);
});

test('isShiftEndPassed works for before/at/after end', () => {
  const end = parseShiftEndDatetime('2026-05-10', '17:00');
  const before = new Date(end.getTime() - 1);
  const at = new Date(end.getTime());
  const after = new Date(end.getTime() + 1);
  assert.equal(isShiftEndPassed(before, end), false);
  assert.equal(isShiftEndPassed(at, end), true);
  assert.equal(isShiftEndPassed(after, end), true);
});

test('isShiftEndPassed handles overnight shifts', () => {
  const start = parseShiftEndDatetime('2026-05-10', '23:00');
  const end = parseShiftEndDatetime('2026-05-10', '03:00');
  const beforeEnd = new Date(end.getTime() - 1);
  const atEnd = new Date(end.getTime());
  assert.equal(isShiftEndPassed(beforeEnd, end, start), false);
  assert.equal(isShiftEndPassed(atEnd, end, start), true);
});
