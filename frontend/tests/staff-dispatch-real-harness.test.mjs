import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, readFileSync, rmSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { spawnSync } from 'node:child_process';
import test from 'node:test';

const root = new URL('../../', import.meta.url);
const script = readFileSync(new URL('scripts/run-staff-dispatch-real-e2e.ps1', root), 'utf8');
const spec = readFileSync(new URL('frontend/tests/e2e/staff-dispatch-real-backend.spec.js', root), 'utf8');
const scriptPath = new URL('../../scripts/run-staff-dispatch-real-e2e.ps1', import.meta.url).pathname.slice(1);
const operationsScript = readFileSync(new URL('scripts/run-operations-real-e2e.ps1', root), 'utf8');
const operationsSpec = readFileSync(new URL('frontend/tests/e2e/operations-real-backend.spec.js', root), 'utf8');
const activityLogR7Spec = readFileSync(new URL('frontend/tests/e2e/admin-activity-log-r7-real-backend.spec.js', root), 'utf8');

function runSafetyPathTest(target) {
  const command = String.raw`
    param($Harness, $Target)
    $tokens = $null; $errors = $null
    $ast = [Management.Automation.Language.Parser]::ParseFile($Harness, [ref]$tokens, [ref]$errors)
    $function = $ast.Find({ param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -eq 'Assert-SafeTempRoot' }, $true)
    . ([scriptblock]::Create($function.Extent.Text))
    $safeTarget = Assert-SafeTempRoot $Target
    if (Test-Path -LiteralPath $safeTarget) { Remove-Item -LiteralPath $safeTarget -Recurse -Force }
  `;
  return spawnSync('powershell.exe', [
    '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', `& { ${command} }`, scriptPath, target,
  ], { encoding: 'utf8' });
}

function removeJunction(path) {
  const result = spawnSync('cmd.exe', ['/d', '/c', 'rmdir', path], { encoding: 'utf8' });
  assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
}

test('real E2E harness isolates listeners and process execution', () => {
  assert.match(script, /Get-NetTCPConnection[^\r\n]+LocalPort[^\r\n]+before startup/);
  assert.match(script, /<Connector port="\$BackendPort" protocol="HTTP\/1\.1"/);
  assert.doesNotMatch(script, /Copy-Item[^\r\n]+TomcatHome 'conf'/);
  assert.doesNotMatch(script, /AjpPort/);
  assert.match(script, /RandomNumberGenerator/);
  assert.match(script, /primaryFailure/);
  assert.match(script, /GetFullPath/);
  assert.match(script, /approvedRoot/);
  assert.match(script, /FileAttributes\]::ReparsePoint/);
  assert.ok((script.match(/Assert-SafeTempRoot \$TempRoot/g) || []).length >= 4);
  assert.match(script, /environmentSnapshot/);
  assert.match(script, /SetEnvironmentVariable\(\$name, \$snapshot\.Value/);
});

test('real E2E safety self-test rejects an external TempRoot without deleting it', () => {
  const external = mkdtempSync(join(tmpdir(), 'fastguy-harness-external-'));
  const marker = join(external, 'keep.txt');
  writeFileSync(marker, 'keep');
  try {
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

test('safe TempRoot accepts normal nonexistent and existing children', () => {
  const approved = join(tmpdir(), 'opencode');
  mkdirSync(approved, { recursive: true });
  for (const target of [
    join(approved, `fastguy-safe-missing-${process.pid}`),
    mkdtempSync(join(approved, 'fastguy-safe-existing-')),
  ]) {
    const result = runSafetyPathTest(target);
    assert.equal(result.status, 0, `${result.stdout}\n${result.stderr}`);
  }
});

for (const placement of ['target', 'intermediate ancestor']) {
  test(`safe TempRoot rejects a junction at ${placement} without deleting its victim`, () => {
    const approved = join(tmpdir(), 'opencode');
    const testRoot = mkdtempSync(join(approved, 'fastguy-junction-test-'));
    const victim = mkdtempSync(join(tmpdir(), 'fastguy-junction-victim-'));
    const victimTarget = placement === 'target' ? victim : join(victim, 'child');
    mkdirSync(victimTarget, { recursive: true });
    const marker = join(victimTarget, 'keep.txt');
    const junction = join(testRoot, 'redirect');
    writeFileSync(marker, 'keep');
    const linked = spawnSync('cmd.exe', ['/d', '/c', 'mklink', '/J', junction, victim], { encoding: 'utf8' });
    assert.equal(linked.status, 0, `${linked.stdout}\n${linked.stderr}`);
    const target = placement === 'target' ? junction : join(junction, 'child');
    try {
      const result = runSafetyPathTest(target);
      assert.equal(readFileSync(marker, 'utf8'), 'keep');
      assert.notEqual(result.status, 0, `accepted junction at ${placement}`);
      assert.match(`${result.stdout}\n${result.stderr}`, /reparse point/i);
    } finally {
      removeJunction(junction);
      rmSync(testRoot, { recursive: true, force: true });
      rmSync(victim, { recursive: true, force: true });
    }
  });
}

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

test('R7 harness targets only the disposable activity-log database and runs desktop plus mobile', () => {
  assert.match(script, /\[switch\]\$ActivityLogR7/);
  assert.match(script, /FastGuyDB_ActivityLog063_Test/);
  assert.match(script, /admin-activity-log-r7-real-backend\.spec\.js/);
  assert.match(script, /if \(\$ActivityLogR7\).*desktop-chrome.*mobile-chrome/s);
  assert.match(script, /OperationsBrowserFixtureIT/);
});

test('R7 spec expects the contracted pay-rate creation status', () => {
  assert.match(activityLogR7Spec, /\(await mutation\)\.status\(\)\)\.toBe\(201\)/);
  assert.match(activityLogR7Spec, /method: 'POST', status: 201/);
  assert.match(activityLogR7Spec, /getByRole\('region', \{ name: 'Danh sách nhật ký hoạt động' \}\)/);
});

test('operations harness is disposable, target locked, desktop role scoped, and always cleans fixtures', () => {
  assert.match(operationsScript, /FASTGUY_DISPOSABLE_DB -ne 'true'/);
  assert.match(operationsScript, /FastGuyDB_Operations060_Test/);
  assert.match(operationsScript, /DB_URL','DB_USER','DB_PASSWORD/);
  assert.match(script, /New-RandomSecret/);
  assert.match(script, /Invoke-Fixture 'cleanup'/);
  assert.match(operationsScript, /-Operations -Project desktop-chrome/);
  assert.match(script, /NavigationR1/);
  assert.match(script, /admin-navigation-r1-real-backend\.spec\.js/);
  assert.match(operationsScript, /shipper-field-command\.spec\.js --project=desktop-chrome --project=mobile-chrome/);
  assert.match(script, /if \(\$Operations\) \{ @\('desktop-chrome'\) \}/);
});

test('operations spec covers role matrix, API evidence, deterministic cutoff, and zero browser errors', () => {
  for (const role of ['Admin', 'Staff', 'User', 'Guest']) assert.match(operationsSpec, new RegExp(`test\\('.*${role}`, 'i'));
  assert.match(operationsSpec, /Lịch bảy ngày ba ca/);
  assert.match(operationsSpec, /\/api\/admin\/shifts\/monitoring/);
  assert.match(operationsSpec, /\/api\/admin\/operating-expenses/);
  assert.match(operationsSpec, /\/api\/admin\/fixed-assets/);
  assert.match(operationsSpec, /\/api\/admin\/reports\/operating-profit/);
  assert.match(operationsSpec, /expiresAt/);
  assert.match(operationsSpec, /Nhận đơn đến/);
  assert.match(operationsSpec, /orderCutoffTime/);
  assert.match(operationsSpec, /errors\)\.toEqual\(\[\]\)/);
});

test('real E2E assertions retain exact request contracts', () => {
  assert.match(spec, /postDataJSON/);
  assert.match(spec, /\/api\/staff\/orders\/shippers/);
  assert.doesNotMatch(spec, /startsWith\('\/api\/staff\/orders\/history\?'/);
  assert.doesNotMatch(spec, /\.test\(request\.path\)/);
  assert.doesNotMatch(spec, /shell:\s*true/);
  assert.match(spec, /org\.codehaus\.plexus\.classworlds\.launcher\.Launcher/);
  assert.match(spec, /hasPostConflictPriorityReload\(evidence\.events, conflictPath\)/);
  assert.doesNotMatch(spec, /priorityLoadsBeforeConflict/);
  assert.match(spec, /test\.slow\(\)/);
});
