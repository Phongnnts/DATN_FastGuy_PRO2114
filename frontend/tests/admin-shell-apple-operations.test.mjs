import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const layout = readFileSync(new URL('../src/layouts/AdminLayout.vue', import.meta.url), 'utf8');
const variables = readFileSync(new URL('../src/assets/styles/variables.css', import.meta.url), 'utf8');

test('admin shell uses the approved fixed geometry and FastGuy material', () => {
  assert.match(layout, /\.sidebar\{width:224px\}/);
  assert.match(layout, /\.main-content\{min-width:0;margin-left:224px\}/);
  assert.match(layout, /\.topbar\{height:64px/);
  assert.match(layout, /backdrop-filter:blur\(/);
  assert.match(layout, /FastGuy/);
});

test('admin tokens expose the approved Operations Studio contract', () => {
  for (const token of [
    '--admin-canvas: #eef2f6',
    '--admin-surface: #ffffff',
    '--admin-surface-subtle: #f8f9fb',
    '--admin-foreground: #172033',
    '--admin-muted: #667085',
    '--admin-hairline: rgba(23, 32, 51, 0.09)',
    '--admin-sidebar: #142033',
    '--admin-brand: #f45b2a',
    '--admin-control-radius: 8px',
    '--admin-workspace-radius: 12px',
    '--admin-panel-radius: 16px',
    '--admin-shell-shadow:',
    '--admin-card-shadow:',
  ]) {
    assert.match(variables, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i'));
  }
});

test('responsive navigation retains dialog semantics and focus recovery', () => {
  assert.match(layout, /:role="isDrawerViewport && sidebarOpen \? 'dialog' : undefined"/);
  assert.match(layout, /:aria-modal="isDrawerViewport && sidebarOpen \? 'true' : undefined"/);
  for (const source of ['event.key === \'Escape\'', 'triggerToRestore', 'backgroundInert']) assert.match(layout, new RegExp(source));
});

test('admin shell uses the approved bright floating surfaces', () => {
  for (const token of ['--admin-canvas: #eef2f6', '--admin-surface: #ffffff', '--admin-foreground: #172033', '--admin-muted: #667085', '--admin-brand: #f45b2a', '--admin-brand-soft: #FFF0EA']) {
    assert.match(variables, new RegExp(token.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i'));
  }
  assert.match(layout, /class="sidebar-brand-mark"/);
  assert.match(layout, /Operations Admin/);
  assert.match(layout, /Dashboard nhân sự/);
  assert.match(variables, /--admin-shell-radius:\s*18px/);
  assert.match(layout, /box-shadow:var\(--admin-shell-shadow\)/);
  assert.match(layout, /class="page-content fg-page"/);
  assert.doesNotMatch(layout, /transition:\s*all/);
});
