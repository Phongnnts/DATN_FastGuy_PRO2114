import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const home = readFileSync(new URL('../src/views/guest/HomePage.vue', import.meta.url), 'utf8');

test('homepage hero owns near-full viewport width with a bounded inner shell', () => {
  assert.doesNotMatch(home, /class="container hero-shell"/);
  assert.match(home, /class="hero-shell"/);
  assert.match(home, /width:calc\(100% - clamp\(/);
  assert.match(home, /max-width:none/);
  assert.match(home, /object-fit:cover/);
  assert.doesNotMatch(home, /\.home-page\{[^}]*overflow-x:hidden/);
});
