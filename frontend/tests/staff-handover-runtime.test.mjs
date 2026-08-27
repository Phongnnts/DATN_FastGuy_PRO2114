import assert from 'node:assert/strict';
import test from 'node:test';
import { createPerKeyRequestGate } from '../src/utils/staffHandover.js';

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}

test('production per-status gate keeps newest queue when responses resolve newest then oldest', async () => {
  const gate = createPerKeyRequestGate();
  const cache = {};
  const older = deferred();
  const newer = deferred();
  async function fetch(status, response) {
    const request = gate.begin(status);
    const rows = await response;
    if (gate.accepts(request)) cache[status] = rows;
  }

  const oldRequest = fetch('READY', older.promise);
  const newRequest = fetch('READY', newer.promise);
  newer.resolve([{ id: 2 }]);
  await newRequest;
  older.resolve([{ id: 1 }]);
  await oldRequest;

  assert.deepEqual(cache.READY, [{ id: 2 }]);
});
