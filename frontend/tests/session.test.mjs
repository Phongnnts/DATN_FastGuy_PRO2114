import assert from 'node:assert/strict';
import test from 'node:test';

globalThis.atob = (value) => Buffer.from(value, 'base64').toString('binary');

const { decodeTokenPayload, isTokenValid, parseStoredUser } = await import('../src/utils/session.js');

function token(payload) {
  const encode = (value) => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'none' })}.${encode(payload)}.`;
}

test('accepts an unexpired JWT', () => {
  const value = token({ sub: '1', exp: 2_000 });
  assert.equal(isTokenValid(value, 1_000_000), true);
  assert.equal(decodeTokenPayload(value).sub, '1');
});

test('rejects an expired JWT', () => {
  assert.equal(isTokenValid(token({ exp: 999 }), 1_000_000), false);
});

test('rejects malformed tokens and tokens without expiry', () => {
  assert.equal(isTokenValid('invalid'), false);
  assert.equal(isTokenValid(token({ sub: '1' })), false);
});

test('safely parses stored users', () => {
  assert.deepEqual(parseStoredUser('{"id":1}'), { id: 1 });
  assert.equal(parseStoredUser('{broken'), null);
  assert.equal(parseStoredUser('[]'), null);
});
