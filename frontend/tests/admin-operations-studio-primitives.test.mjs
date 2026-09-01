import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const header = read('../src/components/admin/AdminPageHeader.vue');
const statePanel = read('../src/components/admin/AdminStatePanel.vue');
const workspace = read('../src/components/admin/AdminWorkspace.vue');
const sources = [header, statePanel, workspace];

test('page header exposes semantic title description and action regions', () => {
  assert.match(header, /<header class="admin-page-header">/);
  assert.match(header, /<slot name="title"\s*\/>/);
  assert.match(header, /<slot name="description"\s*\/>/);
  assert.match(header, /class="admin-page-header__actions"[^>]*aria-label="Tác vụ trang"[^>]*>[\s\S]*<slot name="actions"\s*\/>/);
});

test('state panel assigns live-region semantics only to loading and error', () => {
  assert.match(statePanel, /state:\s*\{\s*type:\s*String,\s*required:\s*true,\s*validator:\s*value\s*=>\s*\['loading',\s*'error',\s*'empty'\]\.includes\(value\)/);
  assert.match(statePanel, /:role="state === 'error' \? 'alert' : state === 'loading' \? 'status' : undefined"/);
  assert.match(statePanel, /v-if="state === 'error'"[^>]*@click="emit\('retry'\)"/);
  assert.match(statePanel, /defineEmits\(\['retry'\]\)/);
  assert.doesNotMatch(statePanel, /role="alert"/);
});

test('workspace exposes labelled toolbar content and footer regions', () => {
  assert.match(workspace, /<section class="admin-workspace" aria-label="Không gian làm việc">/);
  assert.match(workspace, /<section class="admin-workspace__toolbar" aria-label="Thanh công cụ">[\s\S]*<slot name="toolbar"\s*\/>[\s\S]*<\/section>/);
  assert.match(workspace, /<section class="admin-workspace__content" aria-label="Nội dung làm việc">[\s\S]*<slot\s*\/>[\s\S]*<\/section>/);
  assert.match(workspace, /<footer class="admin-workspace__footer" aria-label="Thông tin bổ sung">[\s\S]*<slot name="footer"\s*\/>/);
});

test('primitives stay presentation-only and use shared tokens with visible focus', () => {
  for (const source of sources) {
    assert.doesNotMatch(source, /@\/api|@\/stores|vue-router|useRoute|useRouter|fetch\(|axios/);
    assert.match(source, /<style scoped>/);
    assert.doesNotMatch(source, /transition:\s*all/);
    assert.doesNotMatch(source, /#[0-9a-f]{3,8}|rgba?\(/i);
  }
  assert.match(statePanel, /:focus-visible/);
  for (const token of ['--admin-surface', '--admin-foreground', '--admin-muted', '--admin-border', '--admin-brand', '--admin-workspace-radius']) {
    assert.match(sources.join('\n'), new RegExp(`var\\(${token}\\)`));
  }
});
