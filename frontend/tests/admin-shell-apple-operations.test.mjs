import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const layout = readFileSync(new URL('../src/layouts/AdminLayout.vue', import.meta.url), 'utf8');
const variables = readFileSync(new URL('../src/assets/styles/variables.css', import.meta.url), 'utf8');

test('admin shell uses the approved fixed geometry and FastGuy material', () => {
  assert.match(layout, /\.sidebar\{width:248px\}/);
  assert.match(layout, /\.main-content\{min-width:0;margin-left:248px\}/);
  assert.match(layout, /\.topbar\{height:64px/);
  assert.match(layout, /backdrop-filter:blur\(/);
  assert.match(layout, /FastGuy/);
});

test('admin tokens expose the approved Apple-inspired FastGuy palette', () => {
  for (const value of ['#EEF1F5', '#FFFFFF', '#182230', '#667085', '#98A2B3', '#E4E7EC', '#F45B2A', '#D9481C', '#FFF0EA']) {
    assert.match(variables.toUpperCase(), new RegExp(value));
  }
});

test('responsive navigation retains dialog semantics and focus recovery', () => {
  assert.match(layout, /:role="isDrawerViewport && sidebarOpen \? 'dialog' : undefined"/);
  assert.match(layout, /:aria-modal="isDrawerViewport && sidebarOpen \? 'true' : undefined"/);
  for (const source of ['event.key === \'Escape\'', 'triggerToRestore', 'backgroundInert']) assert.match(layout, new RegExp(source));
});
