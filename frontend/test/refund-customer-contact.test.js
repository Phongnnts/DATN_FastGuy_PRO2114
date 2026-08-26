import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const source = readFileSync(new URL('../src/views/admin/RefundsPage.vue', import.meta.url), 'utf8');

test('pending refund dialog exposes a callable customer phone with empty fallback', () => {
  assert.match(source, /<span>Số điện thoại<\/span>/);
  assert.match(source, /:href="`tel:\$\{refundOrder\.customerPhone\}`"/);
  assert.match(source, /refundOrder\.customerPhone \|\| '—'/);
});
