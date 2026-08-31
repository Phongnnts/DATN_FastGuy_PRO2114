import assert from 'node:assert/strict';
import { after, afterEach, before, beforeEach, test } from 'node:test';
import { createServer } from 'vite';
import { dashboardViewState } from '../src/utils/adminDashboardViewState.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((onResolve, onReject) => {
    resolve = onResolve;
    reject = onReject;
  });
  return { promise, resolve, reject };
}

let vite;
let createPinia;
let setActivePinia;
let useAdminStore;
let adminApi;
let originalGetDashboard;

before(async () => {
  vite = await createServer({ server: { middlewareMode: true }, appType: 'custom', logLevel: 'silent' });
  const [piniaModule, storeModule, apiModule] = await Promise.all([
    vite.ssrLoadModule('pinia'),
    vite.ssrLoadModule('/src/stores/admin.js'),
    vite.ssrLoadModule('/src/api/index.js'),
  ]);
  ({ createPinia, setActivePinia } = piniaModule);
  ({ useAdminStore } = storeModule);
  adminApi = apiModule.adminApi;
  originalGetDashboard = adminApi.getDashboard;
});

beforeEach(() => setActivePinia(createPinia()));
afterEach(() => { adminApi.getDashboard = originalGetDashboard; });
after(() => vite.close());

test('newest dashboard success remains committed when the oldest resolves last', async () => {
  const oldest = deferred();
  const newest = deferred();
  let calls = 0;
  adminApi.getDashboard = () => (++calls === 1 ? oldest.promise : newest.promise);
  const store = useAdminStore();

  const oldestRequest = store.fetchDashboard();
  const newestRequest = store.fetchDashboard();
  newest.resolve({ activeOrderCount: 2 });

  assert.deepEqual(await newestRequest, { activeOrderCount: 2 });
  assert.deepEqual(store.dashboard, { activeOrderCount: 2 });
  assert.equal(store.loading, false);

  oldest.resolve({ activeOrderCount: 1 });
  assert.deepEqual(await oldestRequest, { activeOrderCount: 1 });
  assert.deepEqual(store.dashboard, { activeOrderCount: 2 });
  assert.equal(store.loading, false);
  assert.equal(store.error, '');
});

test('oldest dashboard success resolves for its caller but cannot commit before the newest settles', async () => {
  const oldest = deferred();
  const newest = deferred();
  let calls = 0;
  adminApi.getDashboard = () => (++calls === 1 ? oldest.promise : newest.promise);
  const store = useAdminStore();

  const oldestRequest = store.fetchDashboard();
  const newestRequest = store.fetchDashboard();
  oldest.resolve({ activeOrderCount: 1 });

  assert.deepEqual(await oldestRequest, { activeOrderCount: 1 });
  assert.equal(store.dashboard, null);
  assert.equal(store.loading, true);

  newest.resolve({ activeOrderCount: 2 });
  assert.deepEqual(await newestRequest, { activeOrderCount: 2 });
  assert.deepEqual(store.dashboard, { activeOrderCount: 2 });
  assert.equal(store.loading, false);
});

test('stale dashboard failure rejects its caller without replacing newest success state', async () => {
  const oldest = deferred();
  const newest = deferred();
  let calls = 0;
  adminApi.getDashboard = () => (++calls === 1 ? oldest.promise : newest.promise);
  const store = useAdminStore();

  const oldestRequest = store.fetchDashboard();
  const oldestRejection = assert.rejects(oldestRequest, (error) => error.message === 'stale failure');
  const newestRequest = store.fetchDashboard();
  newest.resolve({ activeOrderCount: 2 });
  await newestRequest;
  oldest.reject(new Error('stale failure'));
  await oldestRejection;

  assert.deepEqual(store.dashboard, { activeOrderCount: 2 });
  assert.equal(store.loading, false);
  assert.equal(store.error, '');
});

test('stale success cannot clear the latest failure or replace prior valid data', async () => {
  const oldest = deferred();
  const newest = deferred();
  let calls = 0;
  adminApi.getDashboard = () => (++calls === 1 ? oldest.promise : newest.promise);
  const store = useAdminStore();
  store.dashboard = { activeOrderCount: 9 };

  const oldestRequest = store.fetchDashboard();
  const newestRequest = store.fetchDashboard({ silent: true });
  const newestRejection = assert.rejects(newestRequest, (error) => error.message === 'latest failure');
  newest.reject(new Error('latest failure'));
  await newestRejection;

  assert.deepEqual(store.dashboard, { activeOrderCount: 9 });
  assert.equal(store.loading, false);
  assert.equal(store.error, 'latest failure');

  oldest.resolve({ activeOrderCount: 1 });
  assert.deepEqual(await oldestRequest, { activeOrderCount: 1 });
  assert.deepEqual(store.dashboard, { activeOrderCount: 9 });
  assert.equal(store.loading, false);
  assert.equal(store.error, 'latest failure');
});

test('later silent request clears prior error and preserves data without entering loading', async () => {
  const request = deferred();
  adminApi.getDashboard = () => request.promise;
  const store = useAdminStore();
  store.dashboard = { activeOrderCount: 9 };
  store.error = 'old failure';

  const pending = store.fetchDashboard({ silent: true });
  const rejection = assert.rejects(pending, (error) => error.message === 'refresh failure');
  assert.deepEqual(store.dashboard, { activeOrderCount: 9 });
  assert.equal(store.loading, false);
  assert.equal(store.error, '');

  request.reject(new Error('refresh failure'));
  await rejection;
  assert.deepEqual(store.dashboard, { activeOrderCount: 9 });
  assert.equal(store.loading, false);
  assert.equal(store.error, 'refresh failure');
});

test('newest non-silent request owns loading when an older silent request settles', async () => {
  const oldest = deferred();
  const newest = deferred();
  let calls = 0;
  adminApi.getDashboard = () => (++calls === 1 ? oldest.promise : newest.promise);
  const store = useAdminStore();

  const oldestRequest = store.fetchDashboard({ silent: true });
  const newestRequest = store.fetchDashboard();
  assert.equal(store.loading, true);

  oldest.resolve({ activeOrderCount: 1 });
  await oldestRequest;
  assert.equal(store.loading, true);
  assert.equal(store.dashboard, null);

  newest.resolve({ activeOrderCount: 2 });
  await newestRequest;
  assert.equal(store.loading, false);
  assert.deepEqual(store.dashboard, { activeOrderCount: 2 });
});

test('dashboardViewState returns the exact dashboard state enum', () => {
  const data = { activeOrderCount: 2 };
  const available = { financial: 'AVAILABLE', orders: 'AVAILABLE' };
  const partial = { ...available, orders: 'UNAVAILABLE' };
  const forbidden = Object.assign(new Error('Forbidden'), { status: 403 });
  const axiosForbidden = { response: { status: 403 } };

  assert.equal(dashboardViewState(null, 'loading', '', available), 'loading');
  assert.equal(dashboardViewState(data, 'ready', '', available), 'ready');
  assert.equal(dashboardViewState(data, 'loading', '', available), 'refreshing');
  assert.equal(dashboardViewState(null, 'error', new Error('Failed'), available), 'error');
  assert.equal(dashboardViewState(null, 'error', forbidden, available), 'forbidden');
  assert.equal(dashboardViewState(null, 'error', axiosForbidden, available), 'forbidden');
  assert.equal(dashboardViewState(data, 'error', new Error('Refresh failed'), available), 'ready');
  assert.equal(dashboardViewState(data, 'error', forbidden, available), 'ready');
  assert.equal(dashboardViewState(data, 'ready', '', partial), 'partial');
  assert.equal(dashboardViewState(data, 'loading', '', partial), 'partial');
  assert.equal(dashboardViewState(null, 'error', new Error('Failed'), partial), 'error');
});
