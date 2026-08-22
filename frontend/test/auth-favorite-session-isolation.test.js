import assert from 'node:assert/strict';
import test from 'node:test';
import { existsSync } from 'node:fs';
import { setMaxListeners } from 'node:events';
import { registerHooks } from 'node:module';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const src = path.resolve(import.meta.dirname, '../src');
registerHooks({
  resolve(specifier, context, nextResolve) {
    if (specifier.startsWith('@/')) {
      const target = path.resolve(src, specifier.slice(2));
      const resolved = path.extname(target) ? target : existsSync(`${target}.js`) ? `${target}.js` : path.join(target, 'index.js');
      return nextResolve(pathToFileURL(resolved).href, context);
    }
    if ((specifier.startsWith('./') || specifier.startsWith('../')) && !path.extname(specifier)) {
      return nextResolve(`${specifier}.js`, context);
    }
    return nextResolve(specifier, context);
  },
  load(url, context, nextLoad) {
    const result = nextLoad(url, context);
    if (!url.endsWith('/src/utils/constants.js')) return result;
    return { ...result, source: result.source.toString().replaceAll('import.meta.env', '{}') };
  },
});

class MemoryStorage {
  #values = new Map();
  getItem(key) { return this.#values.get(key) ?? null; }
  setItem(key, value) { this.#values.set(key, String(value)); }
  removeItem(key) { this.#values.delete(key); }
  clear() { this.#values.clear(); }
}

function deferred() {
  let resolve;
  const promise = new Promise(resolvePromise => { resolve = resolvePromise; });
  return { promise, resolve };
}

const localStorage = new MemoryStorage();
globalThis.localStorage = localStorage;
globalThis.sessionStorage = new MemoryStorage();
globalThis.window = new EventTarget();
setMaxListeners(0, window);
window.location = { pathname: '/', search: '', hash: '', replace() {} };

const { createPinia, setActivePinia } = await import('pinia');
const { authApi, favoriteApi, productApi } = await import('../src/api/index.js');
const { useAuthStore } = await import('../src/stores/auth.js');
const { useFavoriteStore } = await import('../src/stores/favorite.js');

function stores() {
  localStorage.clear();
  setActivePinia(createPinia());
  return { auth: useAuthStore(), favorite: useFavoriteStore() };
}

function seedFavorite(favorite) {
  favorite.items = [{ productId: 7, name: 'Tài khoản cũ' }];
  favorite.ids = new Set([7]);
  favorite.error = 'Lỗi cũ';
  favorite.warning = 'Cảnh báo cũ';
}

function assertFavoriteCleared(favorite) {
  assert.deepEqual(favorite.items, []);
  assert.deepEqual([...favorite.ids], []);
  assert.equal(favorite.loading, false);
  assert.equal(favorite.error, '');
  assert.equal(favorite.warning, '');
}

test('fastguy-session-cleared clears and invalidates favorite state', () => {
  const { favorite } = stores();
  seedFavorite(favorite);
  window.dispatchEvent(new Event('fastguy-session-cleared'));
  assertFavoriteCleared(favorite);
});

test('favorite response completing after 401 cannot apply prior account data', async () => {
  const pendingFavorites = deferred();
  favoriteApi.getAll = () => pendingFavorites.promise;
  productApi.getAll = async () => ({ content: [] });
  const { favorite } = stores();
  const load = favorite.fetchFavorites();
  window.dispatchEvent(new Event('fastguy-session-cleared'));
  pendingFavorites.resolve([{ productId: 8, name: 'Không được áp dụng' }]);
  await load;
  assertFavoriteCleared(favorite);
});

test('catalog response completing after 401 cannot apply prior account favorites', async () => {
  const pendingCatalog = deferred();
  favoriteApi.getAll = async () => [{ productId: 9, name: 'Dữ liệu rút gọn' }];
  productApi.getAll = () => pendingCatalog.promise;
  const { favorite } = stores();
  const load = favorite.fetchFavorites();
  await Promise.resolve();
  window.dispatchEvent(new Event('fastguy-session-cleared'));
  pendingCatalog.resolve({ content: [{ productId: 9, name: 'Không được áp dụng' }] });
  await load;
  assertFavoriteCleared(favorite);
});

for (const action of ['login', 'register']) {
  test(`${action} clears favorite state before applying the new session and ignores stale completion`, async () => {
    const pendingFavorites = deferred();
    favoriteApi.getAll = () => pendingFavorites.promise;
    productApi.getAll = async () => ({ content: [] });
    authApi[action] = async () => ({ token: 'new-session-token', userId: 2, fullName: 'Tài khoản mới', role: 'USER' });
    const { auth, favorite } = stores();
    seedFavorite(favorite);
    const load = favorite.fetchFavorites();
    const credentials = action === 'login'
      ? ['new@example.com', 'password']
      : [{ fullName: 'Tài khoản mới', phone: '0900000000', email: 'new@example.com', password: 'password' }];
    await auth[action](...credentials);
    assertFavoriteCleared(favorite);
    assert.equal(auth.user.id, 2);
    pendingFavorites.resolve([{ productId: 8, name: 'Không được áp dụng' }]);
    await load;
    assertFavoriteCleared(favorite);
  });
}

test('manual logout clears and invalidates favorite state', () => {
  const { auth, favorite } = stores();
  auth.token = 'session-token';
  auth.user = { id: 1, role: 'USER' };
  seedFavorite(favorite);
  auth.logout();
  assertFavoriteCleared(favorite);
});

test('invalid validateSession clears and invalidates favorite state', () => {
  const { auth, favorite } = stores();
  auth.token = 'invalid-token';
  auth.user = { id: 1, role: 'USER' };
  seedFavorite(favorite);
  assert.equal(auth.validateSession(), false);
  assertFavoriteCleared(favorite);
});

test('check completion after clear is ignored', async () => {
  const pending = deferred();
  let calls = 0;
  favoriteApi.check = () => { calls += 1; return pending.promise; };
  const { favorite } = stores();
  const check = favorite.check(7);
  favorite.clear();
  pending.resolve({ favorite: true });
  assert.equal(await check, undefined);
  assert.equal(calls, 1);
  assertFavoriteCleared(favorite);
});

test('toggle completion after logout is ignored', async () => {
  const pending = deferred();
  let calls = 0;
  favoriteApi.toggle = () => { calls += 1; return pending.promise; };
  const { auth, favorite } = stores();
  auth.token = 'session-token';
  auth.user = { id: 1, role: 'USER' };
  const toggle = favorite.toggle({ productId: 7, name: 'Không được áp dụng' });
  auth.logout();
  pending.resolve({ favorite: true });
  assert.equal(await toggle, undefined);
  assert.equal(calls, 1);
  assertFavoriteCleared(favorite);
});

for (const action of ['login', 'register']) {
  test(`old account check completion after ${action} is ignored`, async () => {
    const pending = deferred();
    favoriteApi.check = () => pending.promise;
    authApi[action] = async () => ({ token: 'new-session-token', userId: 2, fullName: 'Tài khoản mới', role: 'USER' });
    const { auth, favorite } = stores();
    const check = favorite.check(7);
    const credentials = action === 'login'
      ? ['new@example.com', 'password']
      : [{ fullName: 'Tài khoản mới', phone: '0900000000', email: 'new@example.com', password: 'password' }];
    await auth[action](...credentials);
    pending.resolve({ favorite: true });
    assert.equal(await check, undefined);
    assertFavoriteCleared(favorite);
  });
}

test('overlapping toggles for one product apply only the latest completion', async () => {
  const older = deferred();
  const newer = deferred();
  let calls = 0;
  favoriteApi.toggle = () => (++calls === 1 ? older.promise : newer.promise);
  const { favorite } = stores();
  const product = { productId: 7, name: 'Món mới nhất' };
  const first = favorite.toggle(product);
  const second = favorite.toggle(product);
  newer.resolve({ favorite: true });
  assert.equal(await second, true);
  older.resolve({ favorite: false });
  assert.equal(await first, undefined);
  assert.equal(calls, 2);
  assert.deepEqual([...favorite.ids], [7]);
  assert.deepEqual(favorite.items, [product]);
});
