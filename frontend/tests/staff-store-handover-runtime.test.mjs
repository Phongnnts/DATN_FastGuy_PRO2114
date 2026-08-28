import assert from 'node:assert/strict';
import test from 'node:test';
import { createServer } from 'vite';

function deferred() {
  let resolve;
  const promise = new Promise((done) => { resolve = done; });
  return { promise, resolve };
}

test('real staff Pinia store rejects stale kitchen responses after automatic rollover refresh', async (t) => {
  const vite = await createServer({ server: { middlewareMode: true }, appType: 'custom', logLevel: 'silent' });
  t.after(() => vite.close());
  const [{ createPinia, setActivePinia }, { useStaffStore }, apiModule] = await Promise.all([
    vite.ssrLoadModule('pinia'),
    vite.ssrLoadModule('/src/stores/staff.js'),
    vite.ssrLoadModule('/src/api/index.js'),
  ]);
  setActivePinia(createPinia());
  const store = useStaffStore();
  const api = apiModule.staffApi;
  const originals = { ...api };
  t.after(() => Object.assign(api, originals));

  const oldKitchen = deferred();
  const newKitchen = deferred();
  let readyCalls = 0;
  api.getReadyOrders = () => (++readyCalls === 1 ? oldKitchen.promise : newKitchen.promise);

  const oldQueueRequest = store.fetchKitchenOrders('READY');
  const newQueueRequest = store.fetchKitchenOrders('READY');
  newKitchen.resolve([{ orderId: 7, status: 'READY', orderCode: 'FG-7', itemCount: 1 }]);
  await newQueueRequest;
  oldKitchen.resolve([{ orderId: 1, status: 'READY', orderCode: 'OLD', itemCount: 1 }]);
  await oldQueueRequest;

  assert.deepEqual(store.kitchenQueues.READY.map((order) => order.id), [7]);
});
