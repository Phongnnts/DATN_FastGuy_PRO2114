import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import test from 'node:test';

test('root policy enforces database API frontend verification order', async () => {
  const policy = await readFile(new URL('../../AGENTS.md', import.meta.url), 'utf8');

  for (const requirement of [
    'DATABASE → API → FRONTEND',
    'Không đoán schema',
    'Không đoán request hoặc response API',
    'OpenAPI',
    'integration test',
    'Playwright',
  ]) assert.match(policy, new RegExp(requirement));
});

test('project exposes the required workflow skills', async () => {
  for (const name of ['fastguy-backend', 'vue-frontend', 'contract-check', 'database-safety', 'testing-e2e']) {
    const skill = await readFile(new URL(`../../.opencode/skills/${name}/SKILL.md`, import.meta.url), 'utf8');
    assert.match(skill, new RegExp(`name:\\s*${name}`));
  }
});
