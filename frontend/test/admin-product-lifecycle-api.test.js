import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const api = readFileSync(new URL('../src/api/admin.js', import.meta.url), 'utf8');
const store = readFileSync(new URL('../src/stores/admin.js', import.meta.url), 'utf8');

test('admin product lifecycle client matches OpenAPI paths and refreshes catalog', () => {
  assert.match(api, /restoreProduct\(id\) \{\s*return client\.put\(`\/admin\/products\/\$\{id\}\/restore`, \{\}\);\s*\}/);
  assert.match(api, /permanentlyDeleteProduct\(id\) \{\s*return client\.delete\(`\/admin\/products\/\$\{id\}\/permanent`\);\s*\}/);
  assert.match(store, /async function restoreProduct\(id\)[\s\S]*adminApi\.restoreProduct\(id\)[\s\S]*fetchProducts\(\)/);
  assert.match(store, /async function permanentlyDeleteProduct\(id\)[\s\S]*adminApi\.permanentlyDeleteProduct\(id\)[\s\S]*fetchProducts\(\)/);
});
