import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
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
});

test('real E2E assertions retain exact request contracts', () => {
  assert.match(spec, /postDataJSON/);
  assert.match(spec, /\/api\/staff\/orders\/shippers/);
  assert.doesNotMatch(spec, /startsWith\('\/api\/staff\/orders\/history\?'/);
  assert.doesNotMatch(spec, /\.test\(request\.path\)/);
  assert.doesNotMatch(spec, /shell:\s*true/);
  assert.match(spec, /org\.codehaus\.plexus\.classworlds\.launcher\.Launcher/);
});
