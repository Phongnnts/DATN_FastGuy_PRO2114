import assert from 'node:assert/strict';
import test from 'node:test';
import { handoverFocusTarget, nextTabIndex } from '../src/utils/staffHandover.js';

test('tab keyboard wraps and supports Home End', () => {
  assert.equal(nextTabIndex(0, 'ArrowLeft', 6), 5);
  assert.equal(nextTabIndex(5, 'ArrowRight', 6), 0);
  assert.equal(nextTabIndex(3, 'Home', 6), 0);
  assert.equal(nextTabIndex(2, 'End', 6), 5);
  assert.equal(nextTabIndex(2, 'Enter', 6), 2);
});

test('claim focus chooses next previous then handover tab', () => {
  assert.deepEqual(handoverFocusTarget(1, 3), { type: 'claim', index: 1 });
  assert.deepEqual(handoverFocusTarget(2, 3), { type: 'claim', index: 1 });
  assert.deepEqual(handoverFocusTarget(0, 1), { type: 'tab' });
});
