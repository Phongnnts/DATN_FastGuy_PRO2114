import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const read = path => readFileSync(new URL(path, import.meta.url), 'utf8');
const login = read('../src/views/guest/LoginPage.vue');
const register = read('../src/views/guest/RegisterPage.vue');
const forgot = read('../src/views/guest/ForgotPasswordPage.vue');

test('auth forms use shared fields, blur validation, live clearing, and requested placeholders', () => {
  for (const source of [login, register, forgot]) {
    assert.match(source, /FormField/);
    assert.match(source, /@blur=/);
    assert.match(source, /@input=/);
  }
  assert.match(login, /placeholder="Nhập mật khẩu"/);
  assert.match(register, /placeholder="Nhập họ và tên"/);
  assert.match(register, /placeholder="Nhập số điện thoại"/);
  assert.match(register, /placeholder="Nhập lại mật khẩu"/);
});

test('auth submission validates before calling existing API flow', () => {
  assert.match(login, /if\s*\(!validateForm\(\)\)\s+return;/);
  assert.match(register, /if\s*\(!validateForm\(\)\)\s+return;/);
  assert.match(forgot, /if\s*\(!validateForm\(\)\)\s+return;/);
  assert.match(forgot, /Vui lòng nhập email/);
  assert.match(forgot, /Email không hợp lệ/);
});
