import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import { required, validEmail, validPhone, validPassword, matchesPassword } from '../src/utils/formValidation.js';

const field = readFileSync(new URL('../src/components/common/FormField.vue', import.meta.url), 'utf8');

test('shared validators enforce the approved policies', () => {
  assert.equal(required('  '), false);
  assert.equal(required(' FastGuy '), true);
  assert.equal(validEmail('guest@example.com'), true);
  assert.equal(validEmail('guest@'), false);
  assert.equal(validPhone('0912345678'), true);
  assert.equal(validPhone('+84912345678'), true);
  assert.equal(validPhone('0112345678'), false);
  assert.equal(validPassword('fastguy1'), true);
  assert.equal(validPassword('fastguy'), false);
  assert.equal(validPassword(`${'a'.repeat(72)}1`), false);
  assert.equal(matchesPassword('fastguy1', 'fastguy1'), true);
});

test('FormField exposes required and accessible error state to its control slot', () => {
  assert.match(field, /required-marker/);
  assert.match(field, /errorId/);
  assert.match(field, /aria-invalid/);
  assert.match(field, /aria-describedby/);
  assert.match(field, /role="alert"/);
  assert.match(field, /#ef4444/);
});
