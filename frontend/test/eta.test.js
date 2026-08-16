import test from 'node:test';
import assert from 'node:assert/strict';
import { createEtaModel } from '../src/utils/eta.js';

test('ETA rejects null empty invalid and unsupported values', () => {
  for (const value of [null, undefined, '', '   ', 'tomorrow', '2026-02-30T10:00:00', '2026-08-16']) {
    assert.equal(createEtaModel(value), null);
  }
});

test('ETA treats backend LocalDateTime as Asia Ho Chi Minh store time', () => {
  assert.deepEqual(createEtaModel('2026-08-16T14:30:00'), {
    datetime: '2026-08-16T07:30:00.000Z',
    display: '14:30 16/08',
  });
});

test('ETA converts Z and offset instants to the same store-local display', () => {
  const expected = {
    datetime: '2026-08-16T07:30:00.000Z',
    display: '14:30 16/08',
  };
  assert.deepEqual(createEtaModel('2026-08-16T07:30:00Z'), expected);
  assert.deepEqual(createEtaModel('2026-08-16T09:30:00+02:00'), expected);
});

test('ETA model provides valid semantic datetime and Vietnamese store-local text', () => {
  const model = createEtaModel('2026-08-16T14:30:00.123');
  assert.equal(Number.isNaN(Date.parse(model.datetime)), false);
  assert.match(model.datetime, /^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/);
  assert.equal(model.display, '14:30 16/08');
});

test('ETA accepts valid leap day boundaries with Z and offset', () => {
  assert.notEqual(createEtaModel('2024-02-29T23:59:59Z'), null);
  assert.notEqual(createEtaModel('2024-02-29T23:59:59+23:59'), null);
});

test('ETA rejects invalid Z and offset calendar or time components', () => {
  for (const value of [
    '2023-02-29T10:00:00Z',
    '2026-02-30T10:00:00Z',
    '2026-13-01T10:00:00Z',
    '2026-01-01T24:00:00Z',
    '2026-01-01T23:60:00Z',
    '2026-01-01T23:59:60Z',
    '2026-02-30T10:00:00+07:00',
    '2026-13-01T10:00:00+07:00',
    '2026-01-01T24:00:00+07:00',
  ]) assert.equal(createEtaModel(value), null, value);
});

test('ETA rejects out-of-range offset components', () => {
  for (const value of [
    '2026-01-01T10:00:00+24:00',
    '2026-01-01T10:00:00-24:00',
    '2026-01-01T10:00:00+23:60',
    '2026-01-01T10:00:00-23:60',
  ]) assert.equal(createEtaModel(value), null, value);
});
