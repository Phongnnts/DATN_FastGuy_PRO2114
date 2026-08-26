import assert from 'node:assert/strict';
import test from 'node:test';

import { hasPostConflictPriorityReload } from './e2e/staff-dispatch-request-evidence.js';

const conflictPath = '/api/staff/orders/42/assign-shipper';
const priorityPath = '/api/staff/orders/dispatch?filter=PRIORITY';

test('post-conflict canonical reload allows an extra polling GET', () => {
  assert.equal(hasPostConflictPriorityReload([
    { id: 1, sequence: 1, phase: 'request', method: 'GET', path: priorityPath },
    { id: 1, sequence: 2, phase: 'response', method: 'GET', path: priorityPath, status: 200 },
    { id: 2, sequence: 3, phase: 'request', method: 'PUT', path: conflictPath },
    { id: 2, sequence: 4, phase: 'response', method: 'PUT', path: conflictPath, status: 409 },
    { id: 3, sequence: 5, phase: 'request', method: 'GET', path: priorityPath },
    { id: 3, sequence: 6, phase: 'response', method: 'GET', path: priorityPath, status: 200 },
  ], conflictPath), true);
});

test('pre-conflict polling cannot replace the post-conflict canonical reload', () => {
  assert.equal(hasPostConflictPriorityReload([
    { id: 1, sequence: 1, phase: 'request', method: 'GET', path: priorityPath },
    { id: 2, sequence: 2, phase: 'request', method: 'PUT', path: conflictPath },
    { id: 1, sequence: 3, phase: 'response', method: 'GET', path: priorityPath, status: 200 },
    { id: 2, sequence: 4, phase: 'response', method: 'PUT', path: conflictPath, status: 409 },
  ], conflictPath), false);
});
