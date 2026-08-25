import assert from 'node:assert/strict';
import { mkdtempSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const script = readFileSync(new URL('scripts/run-staff-dispatch-real-e2e.ps1', root), 'utf8');
const spec = readFileSync(new URL('frontend/tests/e2e/staff-dispatch-real-backend.spec.js', root), 'utf8');

test('real E2E harness isolates listeners and process execution', () => {
  assert.match(script, /Get-NetTCPConnection[^\r\n]+LocalPort[^\r\n]+before startup/);
  assert.match(script, /<Connector port="\$BackendPort" protocol="HTTP\/1\.1"/);
  assert.doesNotMatch(script, /Copy-Item[^\r\n]+TomcatHome 'conf'/);
  assert.doesNotMatch(script, /AjpPort/);
  assert.match(script, /RandomNumberGenerator/);
  assert.match(script, /primaryFailure/);
  assert.match(script, /GetFullPath/);
  assert.match(script, /approvedRoot/);
  assert.match(script, /environmentSnapshot/);
  assert.match(script, /SetEnvironmentVariable\(\$name, \$snapshot\.Value/);
});

test('real E2E safety self-test rejects an external TempRoot without deleting it', () => {
  const external = mkdtempSync(join(tmpdir(), 'fastguy-harness-external-'));
  const marker = join(external, 'keep.txt');
  writeFileSync(marker, 'keep');
  try {
    const scriptPath = new URL('../../scripts/run-staff-dispatch-real-e2e.ps1', import.meta.url).pathname.slice(1);
    const workspace = new URL('../../', import.meta.url).pathname.slice(1).replace(/\/$/, '');
    for (const target of ['C:\\', workspace, join(tmpdir(), 'opencode'), external]) {
      const result = spawnSync('powershell.exe', [
        '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', scriptPath,
        '-SafetySelfTest', '-TempRoot', target,
      ], { encoding: 'utf8' });
      assert.notEqual(result.status, 0, `accepted unsafe TempRoot ${target}`);
      assert.match(`${result.stdout}\n${result.stderr}`, /TempRoot must be strictly below approved temp root/);
    }
    assert.equal(readFileSync(marker, 'utf8'), 'keep');
  } finally {
    rmSync(external, { recursive: true, force: true });
  }
});

test('real E2E safety self-test restores exact set and unset environment values', () => {
  const approved = join(tmpdir(), 'opencode', `fastguy-harness-self-test-${process.pid}`);
  const result = spawnSync('powershell.exe', [
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File',
    new URL('../../scripts/run-staff-dispatch-real-e2e.ps1', import.meta.url).pathname.slice(1),
    '-SafetySelfTest', '-TempRoot', approved,
  ], { encoding: 'utf8' });
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  assert.match(result.stdout, /Environment restoration self-test passed/);
});

test('real E2E assertions retain exact request contracts', () => {
  assert.match(spec, /postDataJSON/);
  assert.match(spec, /\/api\/staff\/orders\/shippers/);
  assert.doesNotMatch(spec, /startsWith\('\/api\/staff\/orders\/history\?'/);
  assert.doesNotMatch(spec, /\.test\(request\.path\)/);
  assert.doesNotMatch(spec, /shell:\s*true/);
  assert.match(spec, /org\.codehaus\.plexus\.classworlds\.launcher\.Launcher/);
});
