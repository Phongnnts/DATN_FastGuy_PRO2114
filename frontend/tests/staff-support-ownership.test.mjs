import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const page = readFileSync(new URL('../src/views/staff/SupportPage.vue', import.meta.url), 'utf8');

test('support page displays assignee fields', () => {
  assert.match(page, /Người phụ trách/);
  assert.match(page, /ticket\.staffName/);
  assert.match(page, /editing\.staffName/);
});

test('support page prevents editing tickets assigned to another staff', () => {
  assert.match(page, /currentUserId/);
  assert.match(page, /canProcess/);
  assert.match(page, /:disabled="!canProcess\(ticket\)"/);
  assert.match(page, /v-if="canProcess\(editing\) && editing\.resolvedAt == null"/);
});
