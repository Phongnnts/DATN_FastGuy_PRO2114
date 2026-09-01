import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const dashboard = read('../src/views/admin/DashboardPage.vue');
const drawer = read('../src/components/admin/AdminOrderDrawer.vue');
const variables = read('../src/assets/styles/variables.css');

function luminance(hex) {
  const channels = hex.match(/[\da-f]{2}/gi).map(value => {
    const channel = Number.parseInt(value, 16) / 255;
    return channel <= 0.04045 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
}

test('dashboard primary action uses the AA admin action token with white text', () => {
  const token = variables.match(/--admin-action:\s*(#[\da-f]{6})/i)?.[1];
  assert.ok(token, 'admin action token must exist');
  assert.match(dashboard, /\.primary-action\{[^}]*background:var\(--admin-action\)[^}]*color:#fff/);
  assert.ok((1.05 / (luminance(token) + 0.05)) >= 4.5, `${token} must provide at least 4.5:1 contrast with white`);
});

test('drawer Escape cancels an idle pending action before closing the modal', () => {
  assert.match(drawer, /if \(event\.key === 'Escape'\) \{[\s\S]*?event\.preventDefault\(\);[\s\S]*?if \(props\.busy\) return;[\s\S]*?if \(props\.pendingAction\) \{[\s\S]*?emit\('cancel-action'\);[\s\S]*?return;[\s\S]*?\}[\s\S]*?requestClose\(\);/);
});
