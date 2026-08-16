import test from 'node:test';
import assert from 'node:assert/strict';
import { canSubmitSettlement, submitSettlement } from '../src/views/shipper/cod-settlement-state.js';

test('409 refresh keeps conflict announcement and entered amount without retrying submit', async () => {
  const state = {
    current: { state: 'READY_TO_SUBMIT', shift: { shiftId: 7 }, settlement: null },
    submittedAmount: '125000',
    formError: '',
    announcement: '',
  };
  let submitCalls = 0;
  let refreshCalls = 0;

  await submitSettlement(state, {
    submit: async () => {
      submitCalls += 1;
      throw Object.assign(new Error('Conflict'), { status: 409 });
    },
    refresh: async () => {
      refreshCalls += 1;
      state.current = {
        state: 'SUBMITTED',
        shift: { shiftId: 7 },
        settlement: { status: 'SUBMITTED', submittedAmount: 120000 },
      };
    },
  });

  assert.equal(submitCalls, 1);
  assert.equal(refreshCalls, 1);
  assert.equal(canSubmitSettlement(state.current), false);
  assert.equal(state.submittedAmount, '125000');
  assert.equal(state.announcement, 'Ca này đã được gửi bàn giao. Trạng thái mới nhất đã được tải.');
  assert.equal(state.current.settlement.submittedAmount, 120000);
});
