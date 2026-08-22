import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const router = read('../src/router/index.js');
const staffLayout = read('../src/layouts/StaffLayout.vue');
const page = read('../src/views/staff/SupportPage.vue');

test('staff support implementation stays dormant', () => {
  assert.match(page, /Người phụ trách/);
  assert.match(page, /canProcess/);
  assert.doesNotMatch(router, /StaffSupport|views\/staff\/SupportPage/);
  assert.doesNotMatch(staffLayout, /\/staff\/support|StaffSupport/);
});
