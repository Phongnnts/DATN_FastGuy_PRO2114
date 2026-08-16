import test from 'node:test';
import assert from 'node:assert/strict';
import { createProfileHydrationController, createProfileLoadController, normalizeProfile } from '../src/utils/profileHydration.js';

function deferred() {
  let resolve;
  let reject;
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

function harness() {
  let session = { token: 'token-a', userId: 1, generation: 0 };
  let user = { id: 1, role: 'USER' };
  let persisted = 0;
  const requests = [];
  const controller = createProfileHydrationController({
    getSession: () => ({ ...session }),
    requestProfile: () => {
      const request = deferred();
      requests.push(request);
      return request.promise;
    },
    applyProfile: (profile) => { user = profile; },
    persist: () => { persisted += 1; },
  });
  return {
    controller,
    requests,
    get user() { return user; },
    get persisted() { return persisted; },
    setSession(next) { session = { ...next }; },
  };
}

const apiProfile = (overrides = {}) => ({
  userId: 1,
  fullName: 'API User',
  email: 'api@example.com',
  phone: '0901234567',
  avatarUrl: null,
  role: null,
  status: 'ACTIVE',
  loyaltyPoints: '12',
  createdAt: '2026-08-16T00:00:00Z',
  ...overrides,
});

test('normalizes profile while preserving session token outside profile state', () => {
  assert.deepEqual(normalizeProfile(apiProfile(), 'USER'), {
    id: 1,
    fullName: 'API User',
    email: 'api@example.com',
    phone: '0901234567',
    avatarUrl: '',
    role: 'USER',
    status: 'ACTIVE',
    loyaltyPoints: 12,
    createdAt: '2026-08-16T00:00:00Z',
  });
});

test('hydrates and persists only matching authenticated session', async () => {
  const state = harness();
  const hydration = state.controller.hydrate();
  state.requests[0].resolve(apiProfile());
  const result = await hydration;
  assert.equal(result.fullName, 'API User');
  assert.equal(state.user.fullName, 'API User');
  assert.equal(state.persisted, 1);
});

test('logout-login race cannot apply old profile to new session', async () => {
  const state = harness();
  const hydration = state.controller.hydrate();
  state.setSession({ token: 'token-b', userId: 2, generation: 2 });
  state.controller.invalidate();
  state.requests[0].resolve(apiProfile());
  assert.equal(await hydration, null);
  assert.deepEqual(state.user, { id: 1, role: 'USER' });
  assert.equal(state.persisted, 0);
});

test('older hydrate cannot overwrite newer hydrate', async () => {
  const state = harness();
  const older = state.controller.hydrate();
  const newer = state.controller.hydrate();
  state.requests[1].resolve(apiProfile({ fullName: 'Newer' }));
  assert.equal((await newer).fullName, 'Newer');
  state.requests[0].resolve(apiProfile({ fullName: 'Older' }));
  assert.equal(await older, null);
  assert.equal(state.user.fullName, 'Newer');
  assert.equal(state.persisted, 1);
});

test('profile update invalidation prevents pending hydrate overwrite', async () => {
  const state = harness();
  const hydration = state.controller.hydrate();
  state.setSession({ token: 'token-a', userId: 1, generation: 1 });
  state.controller.invalidate();
  state.requests[0].resolve(apiProfile({ fullName: 'Stale' }));
  assert.equal(await hydration, null);
  assert.deepEqual(state.user, { id: 1, role: 'USER' });
  assert.equal(state.persisted, 0);
});

test('401 rejection stays rejected and cleared session blocks later application', async () => {
  const state = harness();
  const hydration = state.controller.hydrate();
  state.setSession({ token: null, userId: null, generation: 1 });
  state.controller.invalidate();
  const unauthorized = Object.assign(new Error('Phiên đăng nhập hết hạn'), { status: 401 });
  state.requests[0].reject(unauthorized);
  await assert.rejects(hydration, unauthorized);
  assert.deepEqual(state.user, { id: 1, role: 'USER' });
  assert.equal(state.persisted, 0);
});

test('profile load controller ignores stale and unmounted completions', async () => {
  const first = deferred();
  const second = deferred();
  const afterUnmount = deferred();
  const calls = [first, second, afterUnmount];
  const applied = [];
  const errors = [];
  const loading = [];
  const controller = createProfileLoadController({
    hydrate: () => calls.shift().promise,
    apply: (profile) => applied.push(profile),
    fail: (error) => errors.push(error.message),
    setLoading: (value) => loading.push(value),
  });
  const older = controller.load();
  const newer = controller.load();
  first.resolve({ fullName: 'Older' });
  second.resolve({ fullName: 'Newer' });
  await Promise.all([older, newer]);
  const unmounted = controller.load();
  controller.stop();
  afterUnmount.resolve({ fullName: 'Unmounted' });
  await unmounted;
  assert.deepEqual(applied, [{ fullName: 'Newer' }]);
  assert.deepEqual(errors, []);
  assert.deepEqual(loading, [true, true, false, true]);
});
