import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const globalCss = read('../src/assets/styles/global.css');
const variables = read('../src/assets/styles/variables.css');
const layouts = [
  read('../src/layouts/AdminLayout.vue'),
  read('../src/layouts/StaffLayout.vue'),
  read('../src/layouts/ShipperLayout.vue'),
].join('\n');

test('keeps responsive, table, focus, and reduced-motion policies', () => {
  assert.match(globalCss, /@media \(max-width: 360px\)/);
  assert.match(globalCss, /\.table-wrapper\s*\{[\s\S]*overflow-x: auto/);
  assert.match(globalCss, /\.table\s*\{[\s\S]*min-width: 640px/);
  assert.match(globalCss, /:focus-visible/);
  assert.match(globalCss, /prefers-reduced-motion: reduce/);
});

test('uses one FastGuy orange role accent and Vietnamese status copy', () => {
  assert.match(variables, /--role-admin: var\(--primary\)/);
  assert.match(variables, /--role-staff: var\(--primary\)/);
  assert.match(variables, /--role-shipper: var\(--primary\)/);
  assert.doesNotMatch(layouts, /Control cockpit|Kitchen queue|Live route/);
  assert.match(layouts, /Trung tâm quản trị|Hàng đợi bếp|Tuyến đang giao/);
});
