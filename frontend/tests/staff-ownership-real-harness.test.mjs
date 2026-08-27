import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const fixtureUrl = new URL('Backend/FastGuy-FastFoodSite/src/test/java/integration/StaffOwnershipBrowserFixtureIT.java', root);
const specUrl = new URL('frontend/tests/e2e/staff-ownership-real-backend.spec.js', root);
const runnerUrl = new URL('scripts/run-staff-dispatch-real-e2e.ps1', root);

test('real ownership E2E fixture spec and safe harness wiring exist', () => {
  assert.equal(existsSync(fixtureUrl), true, 'ownership browser fixture missing');
  assert.equal(existsSync(specUrl), true, 'ownership browser spec missing');
  const fixture = readFileSync(fixtureUrl, 'utf8');
  const spec = readFileSync(specUrl, 'utf8');
  const runner = readFileSync(runnerUrl, 'utf8');
  for (const action of ['seed', 'race', 'recovery-terminal', 'admin', 'cleanup']) assert.match(fixture, new RegExp(`case "${action}"`));
  assert.match(fixture, /CAST\(compatibility_level AS int\)/);
  assert.match(fixture, /case "transfer"/);
  assert.match(fixture, /case "verify-recovery"/);
  assert.match(fixture, /case "verify-terminal"/);
  assert.match(fixture, /case "verify-admin"/);
  assert.match(fixture, /staff_shift_id IS NULL/);
  assert.match(fixture, /FastGuyDB_Inventory054_Test/);
  assert.match(spec, /desktop-chrome|mobile-chrome/);
  assert.match(spec, /pageerror/);
  assert.match(spec, /scrollWidth/);
  assert.match(spec, /Promise\.all/);
  assert.match(spec, /\[200, 409\]/);
  assert.match(spec, /\/api\/shifts\/\$\{currentShiftId\}\/check-out/);
  assert.match(spec, /\/api\/staff\/orders\/\$\{recoveryId\}\/retry-delivery/);
  assert.match(spec, /\/api\/staff\/orders\/\$\{terminalItem\.orderId\}\/return-to-store/);
  assert.match(spec, /\/api\/admin\/orders\/\$\{adminOrderId\}\/status/);
  assert.match(spec, /activeOwnershipCount/);
  assert.match(runner, /StaffOwnershipBrowserFixtureIT/);
  assert.match(runner, /staff-ownership-real-backend\.spec\.js/);
});
