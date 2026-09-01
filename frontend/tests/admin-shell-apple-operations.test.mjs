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
  for (const value of ['#FFFFFF', '#FAFAFD', '#20212B', '#858794', '#A6A6AE', '#FF7448', '#C94F2A', '#FFF1EB']) {
    assert.match(variables.toUpperCase(), new RegExp(value));
  }
});

test('responsive navigation retains dialog semantics and focus recovery', () => {
  assert.match(layout, /:role="isDrawerViewport && sidebarOpen \? 'dialog' : undefined"/);
  assert.match(layout, /:aria-modal="isDrawerViewport && sidebarOpen \? 'true' : undefined"/);
  for (const source of ['event.key === \'Escape\'', 'triggerToRestore', 'backgroundInert']) assert.match(layout, new RegExp(source));
});

test('admin shell uses the approved bright floating surfaces', () => {
  for (const token of ['--admin-canvas: #FFFFFF', '--admin-foreground: #20212B', '--admin-muted: #858794', '--admin-hairline: rgba(20, 20, 35, 0.075)', '--admin-brand: #FF7448', '--admin-brand-soft: #FFF1EB']) {
    assert.match(variables, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i'));
  }
  assert.match(layout, /class="sidebar-brand-mark"/);
  assert.match(layout, /Operations Admin/);
  assert.match(layout, /Dashboard nhân sự/);
  assert.match(layout, /border-radius:16px/);
  assert.match(layout, /box-shadow:var\(--admin-shell-shadow\)/);
  assert.doesNotMatch(layout, /transition:\s*all/);
});
