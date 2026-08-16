import test from 'node:test';
import assert from 'node:assert/strict';
import {
  CartMigrationError,
  createCartMigrationController,
  createLoginMigrationController,
} from '../src/utils/cartMigration.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((res, rej) => { resolve = res; reject = rej; });
  return { promise, resolve, reject };
}

function fakeStorage(items) {
  const values = new Map([['cart_guest', JSON.stringify(items)]]);
  return {
    getItem: (key) => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value),
    removeItem: (key) => values.delete(key),
    read: () => values.has('cart_guest') ? JSON.parse(values.get('cart_guest')) : null,
  };
}

function item(id) {
  return { productId: id, variantId: id * 10, quantity: id, modifiers: [] };
}

function migrationSetup(items, failures = new Set(), canonical = { items: [] }) {
  const storage = fakeStorage(items);
  const attempts = [];
  let fetches = 0;
  const controller = createCartMigrationController({
    guestStorage: storage,
    guestKey: 'cart_guest',
    addItem: async (payload) => {
      attempts.push(payload.productId);
      if (failures.has(payload.productId)) throw new Error('add failed');
    },
    fetchCanonical: async () => {
      fetches += 1;
      if (canonical instanceof Error) throw canonical;
      return canonical;
    },
  });
  return { storage, attempts, controller, fetches: () => fetches };
}

test('migration removes guest snapshot only after every item and canonical fetch succeed', async () => {
  const setup = migrationSetup([item(1), item(2)], new Set(), { items: [{ cartItemId: 9 }] });
  const result = await setup.controller.migrate();
  assert.deepEqual(setup.attempts, [1, 2]);
  assert.equal(setup.fetches(), 1);
  assert.equal(setup.storage.read(), null);
  assert.deepEqual(result, { failedCount: 0, canonical: { items: [{ cartItemId: 9 }] } });
});

test('partial migration retains only failed guest items after canonical confirmation', async () => {
  const setup = migrationSetup([item(1), item(2), item(3)], new Set([2]), { items: [{ cartItemId: 1 }] });
  await assert.rejects(setup.controller.migrate(), (error) => {
    assert.ok(error instanceof CartMigrationError);
    assert.equal(error.failedCount, 1);
    assert.equal(error.canonicalConfirmed, true);
    return true;
  });
  assert.deepEqual(setup.attempts, [1, 2, 3]);
  assert.deepEqual(setup.storage.read(), [item(2)]);
  assert.equal(setup.fetches(), 1);
});

test('all failed items remain in guest storage after one attempt each and canonical confirmation', async () => {
  const setup = migrationSetup([item(1), item(2)], new Set([1, 2]));
  await assert.rejects(setup.controller.migrate(), (error) => error.failedCount === 2);
  assert.deepEqual(setup.attempts, [1, 2]);
  assert.deepEqual(setup.storage.read(), [item(1), item(2)]);
  assert.equal(setup.fetches(), 1);
});

test('canonical fetch failure preserves original guest snapshot and blocks confirmation', async () => {
  const setup = migrationSetup([item(1), item(2)], new Set([2]), new Error('offline'));
  await assert.rejects(setup.controller.migrate(), (error) => {
    assert.ok(error instanceof CartMigrationError);
    assert.equal(error.failedCount, 1);
    assert.equal(error.canonicalConfirmed, false);
    return true;
  });
  assert.deepEqual(setup.storage.read(), [item(1), item(2)]);
  assert.equal(setup.fetches(), 1);
});

test('login submits once and navigates only after canonical confirmation with warning toast first', async () => {
  const migration = deferred();
  const events = [];
  let logins = 0;
  const controller = createLoginMigrationController({
    login: async () => { logins += 1; return { role: 'USER' }; },
    migrate: () => migration.promise,
    warn: (message) => events.push(`toast:${message}`),
    navigate: async (path) => events.push(`navigate:${path}`),
  });
  const first = controller.submit('a@b.com', 'secret', '/checkout');
  const duplicate = controller.submit('a@b.com', 'secret', '/checkout');
  await Promise.resolve();
  assert.equal(logins, 1);
  assert.deepEqual(events, []);
  migration.reject(new CartMigrationError(1, true));
  const result = await first;
  assert.equal(await duplicate, null);
  assert.match(result.warning, /1 món/);
  assert.deepEqual(events.map((event) => event.split(':')[0]), ['toast', 'navigate']);
  assert.equal(events[1], 'navigate:/checkout');
});

test('login stays authenticated but does not navigate when canonical cart is unconfirmed', async () => {
  const events = [];
  const controller = createLoginMigrationController({
    login: async () => ({ role: 'USER' }),
    migrate: async () => { throw new CartMigrationError(0, false, 'Không thể xác nhận giỏ hàng'); },
    warn: (message) => events.push(`toast:${message}`),
    navigate: async (path) => events.push(`navigate:${path}`),
  });
  const result = await controller.submit('a@b.com', 'secret', '/home');
  assert.equal(result.authenticated, true);
  assert.equal(result.canonicalConfirmed, false);
  assert.deepEqual(events.map((event) => event.split(':')[0]), ['toast']);
});
