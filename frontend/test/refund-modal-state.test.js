import test from 'node:test';
import assert from 'node:assert/strict';
import { createRefundModalLifecycle, focusCycleTarget } from '../src/utils/refund-modal-state.js';

function fixture() {
  const listeners = new Map();
  const document = {
    activeElement: null,
    addEventListener(type, listener) { listeners.set(type, listener); },
    removeEventListener(type, listener) { if (listeners.get(type) === listener) listeners.delete(type); },
  };
  const element = id => ({ id, isConnected: true, focusCalls: 0, focus() { this.focusCalls += 1; document.activeElement = this; } });
  const opener = element('opener');
  const fallback = element('fallback');
  const first = element('first');
  const middle = element('middle');
  const last = element('last');
  const outside = element('outside');
  const dialog = { contains: target => [first, middle, last].includes(target), focus() {} };
  return { listeners, document, opener, fallback, first, middle, last, outside, dialog };
}

test('production modal lifecycle focuses first and contains outside focus', () => {
  const f = fixture();
  const lifecycle = createRefundModalLifecycle({ document: f.document, getDialog: () => f.dialog, getFocusable: () => [f.first, f.middle, f.last], onEscape() {}, getFallback: () => f.fallback });
  lifecycle.attach();
  lifecycle.open(f.opener);
  assert.equal(f.first.focusCalls, 1);
  f.listeners.get('focusin')({ target: f.outside });
  assert.equal(f.first.focusCalls, 2);
  lifecycle.detach();
});

test('production focus cycle handles Tab ShiftTab and outside focus', () => {
  const f = fixture();
  const controls = [f.first, f.middle, f.last];
  assert.equal(focusCycleTarget({ controls, active: f.last, shiftKey: false }), f.first);
  assert.equal(focusCycleTarget({ controls, active: f.first, shiftKey: true }), f.last);
  assert.equal(focusCycleTarget({ controls, active: f.outside, shiftKey: false }), f.first);
  assert.equal(focusCycleTarget({ controls, active: f.middle, shiftKey: false }), null);
});

test('production modal lifecycle handles Escape cleanup and connected restore fallback', () => {
  const f = fixture();
  let escapes = 0;
  const lifecycle = createRefundModalLifecycle({ document: f.document, getDialog: () => f.dialog, getFocusable: () => [f.first, f.last], onEscape: () => { escapes += 1; }, getFallback: () => f.fallback });
  lifecycle.attach();
  lifecycle.open(f.opener);
  f.listeners.get('keydown')({ key: 'Escape', preventDefault() {} });
  assert.equal(escapes, 1);
  lifecycle.close();
  assert.equal(f.opener.focusCalls, 1);
  f.opener.isConnected = false;
  lifecycle.open(f.opener);
  lifecycle.close();
  assert.equal(f.fallback.focusCalls, 1);
  lifecycle.detach();
  assert.equal(f.listeners.size, 0);
});
