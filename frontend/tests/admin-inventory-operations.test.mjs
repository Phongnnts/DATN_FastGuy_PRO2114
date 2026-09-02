import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = (path) => readFileSync(new URL(path, import.meta.url), 'utf8');
const inventory = read('../src/views/admin/InventoryPage.vue');
const ledger = read('../src/views/admin/InventoryLedgerPage.vue');

test('inventory separates stock-health overview from auditable history', () => {
  assert.match(inventory, /Tổng quan sức khỏe kho/);
  assert.match(inventory, /Lịch sử có thể đối soát/);
  assert.match(inventory, /class="[^"]*\brisk-strip\b[^"]*"/);
  assert.match(inventory, /class="[^"]*\bstock-workspace\b[^"]*"/);
  assert.match(inventory, /class="[^"]*\bhistory-workspace\b[^"]*"/);
});

test('inventory tab and stock filter navigation preserve unrelated query state', () => {
  assert.match(inventory, /query: \{ \.\.\.route\.query, tab:/);
  assert.match(inventory, /query: \{ \.\.\.route\.query, filter:/);
  assert.doesNotMatch(inventory, /router\.replace/);
});

test('ledger remains bounded on desktop and becomes evidence cards on mobile', () => {
  assert.match(ledger, /class="ledger-frame"/);
  assert.match(ledger, /class="ledger-card-list"/);
  assert.match(ledger, /class="ledger-card"/);
  assert.match(ledger, /@media \(max-width: 900px\)[\s\S]*\.ledger-table \{ display: none; \}[\s\S]*\.ledger-card-list \{ display: grid;/);
});

test('ledger preserves server pagination stale guards and human-readable evidence labels', () => {
  assert.match(ledger, /page: page\.value - 1, size: size\.value/);
  assert.match(ledger, /request\.generation !== loadGeneration/);
  assert.match(ledger, /TYPE_LABELS\[row\.transactionType\] \|\| row\.transactionType/);
  assert.match(ledger, /reasonLabel\(row\.reason\)/);
});
