import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('category mapping does not call an undefined image helper', async () => {
  const source = await readFile(new URL('../src/stores/product.js', import.meta.url), 'utf8');

  assert.doesNotMatch(source, /\bensureImage\s*\(/);
});
