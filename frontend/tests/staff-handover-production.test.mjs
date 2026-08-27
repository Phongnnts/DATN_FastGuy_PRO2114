import assert from 'node:assert/strict';
import test from 'node:test';
import { createPerKeyRequestGate, focusAfterUnlock } from '../src/utils/staffHandover.js';

test('per-status gate accepts only newest request for that status', () => {
  const gate = createPerKeyRequestGate();
  const older = gate.begin('READY');
  const newer = gate.begin('READY');
  const other = gate.begin('PREPARING');
  assert.equal(gate.accepts(older), false);
  assert.equal(gate.accepts(newer), true);
  assert.equal(gate.accepts(other), true);
});

test('focus scheduling unlocks before focusing enabled target', async () => {
  let locked = true;
  let focusedWhileLocked = null;
  await focusAfterUnlock(
    () => { locked = false; },
    async () => {},
    () => { focusedWhileLocked = locked; },
  );
  assert.equal(focusedWhileLocked, false);
});
